package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.request.storyedge.*;
import com.bdu.plotassistant.dto.response.storyedge.*;
import com.bdu.plotassistant.entity.*;
import com.bdu.plotassistant.repository.*;
import com.bdu.plotassistant.service.AiClientService;
import com.bdu.plotassistant.service.StoryEdgeService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class StoryEdgeServiceImpl implements StoryEdgeService {

    private final StoryEdgeRepository edgeRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final GeneratedContentRepository contentRepository;
    private final ProjectRepository projectRepository;
    private final StoryNodeRepository nodeRepository;
    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;

    public StoryEdgeServiceImpl(StoryEdgeRepository edgeRepository,
                                WorldSettingRepository worldSettingRepository, GeneratedContentRepository contentRepository, ProjectRepository projectRepository,
                                StoryNodeRepository nodeRepository,
                                AiClientService aiClientService, ObjectMapper objectMapper) {
        this.edgeRepository = edgeRepository;
        this.worldSettingRepository = worldSettingRepository;
        this.contentRepository = contentRepository;
        this.projectRepository = projectRepository;
        this.nodeRepository = nodeRepository;
        this.aiClientService = aiClientService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Long create(Long projectId, CreateEdgeRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 校验源节点和目标节点存在且属于该项目
        StoryNode sourceNode = nodeRepository.findById(request.getSourceId())
                .orElseThrow(() -> new BizException("源节点不存在: " + request.getSourceId()));

        StoryNode targetNode = nodeRepository.findById(request.getTargetId())
                .orElseThrow(() -> new BizException("目标节点不存在: " + request.getTargetId()));

        // 校验节点属于当前项目（防止跨项目操作）
        if (!sourceNode.getProject().getId().equals(projectId) ||
                !targetNode.getProject().getId().equals(projectId)) {
            throw new BizException("节点不属于当前项目");
        }

        // 检查是否已存在相同边（源-目标对）
        List<StoryEdge> existingEdges = edgeRepository.findBySourceId(request.getSourceId());
        for (StoryEdge edge : existingEdges) {
            if (edge.getTarget().getId().equals(request.getTargetId())) {
                throw new BizException("该边已存在");
            }
        }

        StoryEdge edge = new StoryEdge();
        edge.setProject(project);
        edge.setSource(sourceNode);
        edge.setTarget(targetNode);
        edge.setLabel(request.getLabel());
        edge.setConditionExpr(request.getConditionExpr());

        StoryEdge saved = edgeRepository.save(edge);
        return saved.getId();
    }

    @Override
    public List<EdgeDTO> listByProject(Long projectId) {
        List<StoryEdge> edges = edgeRepository.findByProjectId(projectId);
        List<EdgeDTO> result = new ArrayList<>();

        for (StoryEdge edge : edges) {
            result.add(convertToDTO(edge));
        }
        return result;
    }

    @Override
    public EdgeDetailDTO getDetail(Long edgeId) {
        StoryEdge edge = edgeRepository.findById(edgeId)
                .orElseThrow(() -> new BizException("边不存在: " + edgeId));

        return convertToDetailDTO(edge);
    }

    @Override
    @Transactional
    public void update(Long edgeId, UpdateEdgeRequest request) {
        StoryEdge edge = edgeRepository.findById(edgeId)
                .orElseThrow(() -> new BizException("边不存在: " + edgeId));

        if (request.getLabel() != null) {
            edge.setLabel(request.getLabel());
        }
        if (request.getConditionExpr() != null) {
            edge.setConditionExpr(request.getConditionExpr());
        }

        edgeRepository.save(edge);
    }

    @Override
    @Transactional
    public void delete(Long edgeId) {
        if (!edgeRepository.existsById(edgeId)) {
            throw new BizException("边不存在: " + edgeId);
        }
        edgeRepository.deleteById(edgeId);
    }



    /**
     * L8: 生成剧情边（选项分支）
     * 依赖L5: 源节点和目标节点的场景描述
     * 依赖L7: 源节点的对话内容（如果有）
     */
    @Override
    public List<EdgeSuggestionDTO> generateSuggestions(Long projectId, GenerateEdgeRequest request) {
        // 1. 获取源节点和目标节点（L5）
        StoryNode sourceNode = nodeRepository.findById(request.getSourceId())
                .orElseThrow(() -> new BizException("源节点不存在"));
        StoryNode targetNode = nodeRepository.findById(request.getTargetId())
                .orElseThrow(() -> new BizException("目标节点不存在"));

        // 2. 检查L5是否已生成（需要详细的场景描述）
        if (sourceNode.getSceneDescription() == null || sourceNode.getSceneDescription().isEmpty()) {
            throw new BizException("源节点L5场景描述未生成");
        }
        if (targetNode.getSceneDescription() == null || targetNode.getSceneDescription().isEmpty()) {
            throw new BizException("目标节点L5场景描述未生成");
        }

        // 3. 获取L7数据：源节点的对话内容（如果有）
        Optional<GeneratedContent> dialogueOpt = contentRepository
                .findByProjectIdAndContentTypeAndNodeId(projectId, "DIALOGUE", request.getSourceId());

        String dialogueSummary = dialogueOpt.map(content ->
                extractDialogueSummary(content.getContentJson())
        ).orElse("该场景暂无对话内容");

        // 4. 构建L8提示词
        String systemPrompt = buildL8SystemPrompt();
        String userPrompt = buildL8UserPrompt(projectId, sourceNode, targetNode, dialogueSummary, request);

        // 5. 调用AI生成选项
        String aiResponse = aiClientService.syncCompletion(systemPrompt, userPrompt);
        System.out.println("L8 AI响应: " + aiResponse);

        // 6. 解析为标准格式
        return parseEdgeSuggestions(aiResponse);
    }

    @Override
    @Transactional
    public List<EdgeSuggestionDTO> generateAndSaveSuggestions(Long projectId, GenerateEdgeRequest request) {
        // 1. 获取源节点和目标节点（L5）
        StoryNode sourceNode = nodeRepository.findById(request.getSourceId())
                .orElseThrow(() -> new BizException("源节点不存在: " + request.getSourceId()));
        StoryNode targetNode = nodeRepository.findById(request.getTargetId())
                .orElseThrow(() -> new BizException("目标节点不存在: " + request.getTargetId()));

        // 2. 检查L5是否已生成（需要详细的场景描述）
        if (sourceNode.getSceneDescription() == null || sourceNode.getSceneDescription().isEmpty()) {
            throw new BizException("源节点L5场景描述未生成");
        }
        if (targetNode.getSceneDescription() == null || targetNode.getSceneDescription().isEmpty()) {
            throw new BizException("目标节点L5场景描述未生成");
        }

        // 3. 获取L7数据：源节点的对话内容（如果有）
        Optional<GeneratedContent> dialogueOpt = contentRepository
                .findByProjectIdAndContentTypeAndNodeId(projectId, "DIALOGUE", request.getSourceId());

        String dialogueSummary = dialogueOpt.map(content ->
                extractDialogueSummary(content.getContentJson())
        ).orElse("该场景暂无对话内容");

        // 4. 构建L8提示词
        String systemPrompt = buildL8SystemPrompt();
        String userPrompt = buildL8UserPrompt(projectId, sourceNode, targetNode, dialogueSummary, request);

        // 5. 调用AI生成选项
        String aiResponse = aiClientService.syncCompletion(systemPrompt, userPrompt);
        System.out.println("L8 AI响应: " + aiResponse);

        // 6. 解析为标准格式
        List<EdgeSuggestionDTO> suggestions = parseEdgeSuggestions(aiResponse);

        // 7.直接保存所有生成的边，不检查重复
        if (!suggestions.isEmpty()) {
            saveEdgeSuggestions(projectId, sourceNode, targetNode, suggestions);
        }

        return suggestions;
    }

    /**
     * 保存边建议到数据库（追加模式，不检查重复）
     */
    private void saveEdgeSuggestions(Long projectId, StoryNode sourceNode,
                                     StoryNode targetNode, List<EdgeSuggestionDTO> suggestions) {
        // 获取Project实体用于关联
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在: " + projectId));

        List<StoryEdge> edgesToSave = new ArrayList<>();

        for (EdgeSuggestionDTO dto : suggestions) {
            StoryEdge edge = new StoryEdge();
            edge.setProject(project);
            edge.setSource(sourceNode);
            edge.setTarget(targetNode);
            edge.setLabel(dto.getLabel());
            edge.setConditionExpr(dto.getConditionExpr());
            edge.setReason(dto.getReason());
            edge.setOnSuccess(dto.getOnSuccess());
            edge.setOnFailure(dto.getOnFailure());
            edge.setEffect(dto.getEffect());
            // updatedAt 由 @UpdateTimestamp 自动管理

            edgesToSave.add(edge);
        }

        // 批量保存（即使label重复也全部保存）
        List<StoryEdge> savedEdges = edgeRepository.saveAll(edgesToSave);
        System.out.println("L8 成功保存 " + savedEdges.size() + " 条边选项到数据库 [项目ID: "
                + projectId + ", 源节点: " + sourceNode.getId()
                + ", 目标节点: " + targetNode.getId() + "]");
    }

    /**
     * 提取对话关键信息（摘要）
     */
    private String extractDialogueSummary(String dialogueJson) {
        try {
            JsonNode root = objectMapper.readTree(dialogueJson);
            JsonNode lines = root.path("lines");

            StringBuilder summary = new StringBuilder();
            summary.append("场景：").append(root.path("scene").asText()).append("\n");
            summary.append("对话摘要：");

            // 提取前3句关键对话
            int count = 0;
            for (JsonNode line : lines) {
                if (count >= 3) break;
                summary.append(line.path("speaker").asText())
                        .append(":")
                        .append(line.path("line").asText().substring(0,
                                Math.min(20, line.path("line").asText().length())))
                        .append("... ");
                count++;
            }

            return summary.toString();
        } catch (Exception e) {
            return "对话内容解析失败";
        }
    }

    /**
     * 构建L8系统提示词
     */
    private String buildL8SystemPrompt() {
        return "你是一位资深的游戏关卡设计师，擅长设计有意义的剧情分支选项。\n" +
                "\n" +
                "【任务】\n" +
                "基于源场景和目标场景的描述，以及场景中的对话，设计连接两个场景的过渡选项。\n" +
                "\n" +
                "【选项设计要求】\n" +
                "1. 必须提供2-3个不同的选项，体现不同的解决思路（暴力/智取/社交/潜行等）\n" +
                "2. 每个选项必须有明确的条件表达式（如player.hasItem('key') && player.strength > 5）\n" +
                "3. 必须体现从源场景到目标场景的逻辑：玩家（主角即玩家）做了什么导致场景转换\n" +
                "4. 成功结果必须导向目标场景，失败结果可以是留在源场景或进入其他分支\n" +
                "5. 选项文本要有代入感（第二人称，动词开头，如\"威胁医生交出钥匙\"）\n" +
                "\n" +
                "【输出格式】\n" +
                "必须严格返回JSON格式：\n" +
                "{\n" +
                "  \"suggestions\": [\n" +
                "    {\n" +
                "      \"label\": \"选项按钮文本（15字以内，动词开头）\",\n" +
                "      \"conditionExpr\": \"条件表达式，如player.hasItem('gun') && player.courage > 5\",\n" +
                "      \"reason\": \"设计理由（30字以内，说明这个选项的玩法风格）\",\n" +
                "      \"onSuccess\": \"成功后的剧情描述（50字以内，描述如何到达目标场景）\",\n" +
                "      \"onFailure\": \"失败后的剧情描述（可选，30字以内）\",\n" +
                "      \"effect\": \"游戏效果JSON字符串，如{\\\"addItem\\\":\\\"key\\\",\\\"reputation\\\":-10}\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /**
     * 构建L8用户提示词（L5场景 + L7对话 + L1世界观）
     */
    private String buildL8UserPrompt(Long projectId, StoryNode sourceNode, StoryNode targetNode,
                                     String dialogueSummary, GenerateEdgeRequest request) {
        StringBuilder prompt = new StringBuilder();

        // L1世界观（简要，约50字）
        String l1Summary = buildL1BriefSummary(projectId);
        prompt.append("【世界观】").append(l1Summary).append("\n\n");

        // L5源场景（当前位置）
        prompt.append("【源场景】（当前位置）\n");
        prompt.append("名称：").append(sourceNode.getNodeName()).append("\n");
        prompt.append("描述：").append(sourceNode.getSceneDescription().substring(0,
                Math.min(100, sourceNode.getSceneDescription().length()))).append("...\n");
        prompt.append("剧情位置：第").append(sourceNode.getActIndex()).append("幕第")
                .append(sourceNode.getBeatIndex()).append("场\n\n");

        // L7对话（如果有）
        if (!dialogueSummary.equals("该场景暂无对话内容")) {
            prompt.append("【场景对话】（L7）\n");
            prompt.append(dialogueSummary).append("\n\n");
        }

        // L5目标场景（要去往的位置）
        prompt.append("【目标场景】（要到达的位置）\n");
        prompt.append("名称：").append(targetNode.getNodeName()).append("\n");
        prompt.append("描述：").append(targetNode.getSceneDescription().substring(0,
                Math.min(100, targetNode.getSceneDescription().length()))).append("...\n");
        prompt.append("剧情位置：第").append(targetNode.getActIndex()).append("幕第")
                .append(targetNode.getBeatIndex()).append("场\n\n");

        // 逻辑关系提示
        prompt.append("【场景关系】\n");
        if (sourceNode.getActIndex().equals(targetNode.getActIndex())) {
            prompt.append("同幕转场，通常是同一地点的不同区域或时间段\n");
        } else {
            prompt.append("跨幕转场，标志着剧情阶段的重大推进\n");
        }

        // 用户要求
        if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            prompt.append("\n【特殊要求】").append(request.getPrompt());
        } else {
            prompt.append("\n要求：设计能让玩家感受到场景转换逻辑的选项，每个选项要有明显的风险和收益差异。");
        }

        return prompt.toString();
    }

    /**
     * 构建L1简要摘要（用于L8）
     */
    private String buildL1BriefSummary(Long projectId) {
        WorldSetting setting = worldSettingRepository.findByProjectId(projectId).orElse(null);
        if (setting == null) return "";
        return String.format("%s/%s，%s",
                setting.getGenre(),
                setting.getSubGenre() != null ? setting.getSubGenre() : "",
                setting.getCoreConflict() != null ? setting.getCoreConflict().substring(0,
                        Math.min(30, setting.getCoreConflict().length())) : ""
        );
    }

    /**
     * 解析AI响应为EdgeSuggestionDTO列表
     */
    private List<EdgeSuggestionDTO> parseEdgeSuggestions(String aiResponse) {
        List<EdgeSuggestionDTO> suggestions = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode suggestionsNode = root.path("suggestions");

            if (suggestionsNode.isArray()) {
                for (int i = 0; i < suggestionsNode.size(); i++) {
                    JsonNode sugNode = suggestionsNode.get(i);
                    EdgeSuggestionDTO dto = new EdgeSuggestionDTO();

                    dto.setId(String.valueOf(i + 1));
                    dto.setLabel(sugNode.path("label").asText("选项" + (i+1)));
                    dto.setConditionExpr(sugNode.path("conditionExpr").asText(""));
                    dto.setReason(sugNode.path("reason").asText(""));
                    dto.setOnSuccess(sugNode.path("onSuccess").asText(""));
                    dto.setOnFailure(sugNode.path("onFailure").asText(""));
                    dto.setEffect(sugNode.path("effect").asText(""));

                    suggestions.add(dto);
                }
            }

            if (suggestions.isEmpty()) {
                suggestions.add(createDefaultSuggestion());
            }

        } catch (Exception e) {
            // 解析失败时返回默认选项
            suggestions.add(createDefaultSuggestion());
        }

        return suggestions;
    }

    /**
     * 创建默认选项
     */
    private EdgeSuggestionDTO createDefaultSuggestion() {
        EdgeSuggestionDTO dto = new EdgeSuggestionDTO();
        dto.setId("1");
        dto.setLabel("前往下一场景");
        dto.setConditionExpr("");
        dto.setReason("默认过渡");
        dto.setOnSuccess("你顺利到达了目的地");
        dto.setOnFailure("");
        dto.setEffect("{}");
        return dto;
    }

    @Override
    @Transactional
    public void batchSave(Long projectId, BatchSaveEdgesRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        List<BatchSaveEdgesRequest.EdgeDTO> edges = request.getEdges();
        if (edges == null || edges.isEmpty()) {
            return;
        }

        // 删除指定的旧边
        List<Long> deleteIds = request.getDeleteIds();
        if (deleteIds != null && !deleteIds.isEmpty()) {
            edgeRepository.deleteAllByIdInBatch(deleteIds);
        }

        List<StoryEdge> newEdges = new ArrayList<>();

        for (BatchSaveEdgesRequest.EdgeDTO dto : edges) {
            StoryEdge edge;

            if (dto.getId() != null && edgeRepository.existsById(dto.getId())) {
                edge = edgeRepository.findById(dto.getId()).get();
                if (!edge.getProject().getId().equals(projectId)) {
                    throw new BizException("无权修改该边: " + dto.getId());
                }
            } else {
                edge = new StoryEdge();
                edge.setProject(project);
            }

            StoryNode source = nodeRepository.findById(dto.getSourceId())
                    .orElseThrow(() -> new BizException("源节点不存在: " + dto.getSourceId()));
            StoryNode target = nodeRepository.findById(dto.getTargetId())
                    .orElseThrow(() -> new BizException("目标节点不存在: " + dto.getTargetId()));

            edge.setSource(source);
            edge.setTarget(target);
            edge.setLabel(dto.getLabel());
            edge.setConditionExpr(dto.getConditionExpr());
            // 新增字段映射
            edge.setReason(dto.getReason());
            edge.setOnSuccess(dto.getOnSuccess());
            edge.setOnFailure(dto.getOnFailure());
            edge.setEffect(dto.getEffect());

            newEdges.add(edge);
        }

        edgeRepository.saveAll(newEdges);
    }

    @Override
    @Transactional
    public Long saveFromSuggestion(Long projectId, String sourceId, String targetId,
                                   EdgeSuggestionDTO suggestion) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        StoryNode source = nodeRepository.findById(sourceId)
                .orElseThrow(() -> new BizException("源节点不存在"));
        StoryNode target = nodeRepository.findById(targetId)
                .orElseThrow(() -> new BizException("目标节点不存在"));

        // 检查是否已存在完全相同的选项（避免重复）
        Optional<StoryEdge> existing = edgeRepository
                .findBySourceIdAndTargetIdAndLabel(sourceId, targetId, suggestion.getLabel());

        if (existing.isPresent()) {
            // 更新已有选项（覆盖所有字段）
            StoryEdge edge = existing.get();
            edge.setConditionExpr(suggestion.getConditionExpr());
            edge.setReason(suggestion.getReason());
            edge.setOnSuccess(suggestion.getOnSuccess());
            edge.setOnFailure(suggestion.getOnFailure());
            edge.setEffect(suggestion.getEffect());
            StoryEdge saved = edgeRepository.save(edge);
            return saved.getId();
        }

        // 创建新选项（包含所有字段）
        StoryEdge edge = new StoryEdge();
        edge.setProject(project);
        edge.setSource(source);
        edge.setTarget(target);
        edge.setLabel(suggestion.getLabel());
        edge.setConditionExpr(suggestion.getConditionExpr());
        edge.setReason(suggestion.getReason());
        edge.setOnSuccess(suggestion.getOnSuccess());
        edge.setOnFailure(suggestion.getOnFailure());
        edge.setEffect(suggestion.getEffect());

        StoryEdge saved = edgeRepository.save(edge);
        return saved.getId();
    }

    @Override
    @Transactional
    public List<Long> saveSuggestionsAsEdges(Long projectId, String sourceId, String targetId,
                                             List<EdgeSuggestionDTO> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> edgeIds = new ArrayList<>();

        for (EdgeSuggestionDTO suggestion : suggestions) {
            try {
                Long edgeId = saveFromSuggestion(projectId, sourceId, targetId, suggestion);
                edgeIds.add(edgeId);
            } catch (Exception e) {
                System.err.println("保存建议失败: " + suggestion.getLabel() + ", 错误: " + e.getMessage());
            }
        }

        return edgeIds;
    }

    @Override
    public List<EdgeDTO> getBySourceNode(String sourceNodeId) {
        List<StoryEdge> edges = edgeRepository.findBySourceId(sourceNodeId);
        List<EdgeDTO> result = new ArrayList<>();

        for (StoryEdge edge : edges) {
            result.add(convertToDTO(edge));
        }
        return result;
    }

    // ========== 私有转换方法（JDK 8兼容） ==========

    private EdgeDTO convertToDTO(StoryEdge edge) {
        EdgeDTO dto = new EdgeDTO();
        dto.setId(edge.getId());
        dto.setSourceId(edge.getSource().getId());
        dto.setTargetId(edge.getTarget().getId());
        dto.setLabel(edge.getLabel());
        dto.setConditionExpr(edge.getConditionExpr());
        dto.setReason(edge.getReason());
        dto.setOnSuccess(edge.getOnSuccess());
        dto.setOnFailure(edge.getOnFailure());
        dto.setEffect(edge.getEffect());

        return dto;
    }

    private EdgeDetailDTO convertToDetailDTO(StoryEdge edge) {
        EdgeDetailDTO dto = new EdgeDetailDTO();
        dto.setId(edge.getId());
        dto.setSourceId(edge.getSource().getId());
        dto.setTargetId(edge.getTarget().getId());
        dto.setSourceNodeName(edge.getSource().getNodeName());
        dto.setTargetNodeName(edge.getTarget().getNodeName());
        dto.setLabel(edge.getLabel());
        dto.setConditionExpr(edge.getConditionExpr());
        // 新增字段
        dto.setReason(edge.getReason());
        dto.setOnSuccess(edge.getOnSuccess());
        dto.setOnFailure(edge.getOnFailure());
        dto.setEffect(edge.getEffect());
        dto.setUpdatedAt(ServiceUtil.formatDateTime(edge.getUpdatedAt()));
        return dto;
    }
}
