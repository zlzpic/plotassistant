package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.outline.ActStructure;
import com.bdu.plotassistant.dto.outline.BeatStructure;
import com.bdu.plotassistant.dto.outline.OutlineStructure;
import com.bdu.plotassistant.dto.request.storynode.*;
import com.bdu.plotassistant.dto.response.storynode.*;
import com.bdu.plotassistant.entity.GeneratedContent;
import com.bdu.plotassistant.entity.Project;
import com.bdu.plotassistant.entity.StoryNode;
import com.bdu.plotassistant.entity.WorldSetting;
import com.bdu.plotassistant.repository.*;
import com.bdu.plotassistant.service.AiClientService;
import com.bdu.plotassistant.service.GeneratedContentService;
import com.bdu.plotassistant.service.StoryNodeService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoryNodeServiceImpl implements StoryNodeService {

    private final StoryNodeRepository nodeRepository;
    private final ProjectRepository projectRepository;
    private final GeneratedContentRepository contentRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final CharacterRepository characterRepository;
    private final GeneratedContentService generatedContentService;
    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;

    public StoryNodeServiceImpl(StoryNodeRepository nodeRepository,
                                ProjectRepository projectRepository,
                                GeneratedContentRepository contentRepository, WorldSettingRepository worldSettingRepository, CharacterRepository characterRepository, GeneratedContentService generatedContentService, AiClientService aiClientService,
                                ObjectMapper objectMapper) {
        this.nodeRepository = nodeRepository;
        this.projectRepository = projectRepository;
        this.contentRepository = contentRepository;
        this.worldSettingRepository = worldSettingRepository;
        this.characterRepository = characterRepository;
        this.generatedContentService = generatedContentService;
        this.aiClientService = aiClientService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public String create(Long projectId, CreateNodeRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 生成UUID
        String nodeId = UUID.randomUUID().toString().replace("-", "");

        StoryNode node = new StoryNode();
        node.setId(nodeId);
        node.setProject(project);
        node.setNodeName(request.getNodeName());
        node.setSceneDescription(request.getSceneDescription());

        // 处理坐标
        if (request.getPositionX() != null) {
            node.setPositionX(request.getPositionX());
        } else {
            node.setPositionX(BigDecimal.ZERO);
        }

        if (request.getPositionY() != null) {
            node.setPositionY(request.getPositionY());
        } else {
            node.setPositionY(BigDecimal.ZERO);
        }

        // JSON序列化
        try {
            List<String> charIds = request.getAssociatedCharIds();
            if (charIds == null) {
                charIds = new ArrayList<>();
            }
            node.setAssociatedChars(objectMapper.writeValueAsString(charIds));

            Map<String, Object> vars = request.getInitialVariables();
            if (vars == null) {
                vars = new java.util.HashMap<>();
            }
            node.setInitialVariables(objectMapper.writeValueAsString(vars));
        } catch (Exception e) {
            throw new BizException("字段序列化失败: " + e.getMessage());
        }

        StoryNode saved = nodeRepository.save(node);
        return saved.getId();
    }

    @Override
    public List<NodeSummaryDTO> listByProject(Long projectId) {
        List<StoryNode> nodes = nodeRepository.findByProjectId(projectId);

        if (nodes.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取对话内容，避免 N+1 查询性能问题
        List<String> nodeIds = nodes.stream()
                .map(StoryNode::getId)
                .collect(Collectors.toList());

        // 一次性查询所有节点的对话内容
        List<GeneratedContent> dialogues = contentRepository
                .findByProjectIdAndNodeIdInAndContentType(projectId, nodeIds, "DIALOGUE");

        // 转换为 Map<nodeId, contentJson> 便于快速查找
        Map<String, String> dialogueMap = dialogues.stream()
                .collect(Collectors.toMap(
                        GeneratedContent::getNodeId,
                        GeneratedContent::getContentJson,
                        (existing, replacement) -> existing  // 处理重复情况
                ));

        // 构建 DTO 列表
        List<NodeSummaryDTO> result = new ArrayList<>();
        for (StoryNode node : nodes) {
            NodeSummaryDTO dto = convertToSummaryDTO(node);

            // 【关键】填充场景描述（直接从 node 实体获取）
            dto.setSceneDescription(node.getSceneDescription());

            // 【关键】填充对话内容（从 Map 中获取，可能为 null）
            String dialogueContent = dialogueMap.get(node.getId());
            dto.setDialogueContent(dialogueContent);

            result.add(dto);
        }
        return result;
    }

    @Override
    public NodeDetailDTO getDetail(String nodeId) {
        StoryNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new BizException("节点不存在: " + nodeId));

        NodeDetailDTO dto = convertToDetailDTO(node);

        // 查询该节点的对话生成内容
        Optional<GeneratedContent> dialogueContent = contentRepository
                .findByNodeIdAndContentType(nodeId, "DIALOGUE");

        // 如果存在对话内容，设置到 DTO
        dialogueContent.ifPresent(content -> {
            dto.setDialogueContent(content.getContentJson());
            // 如果有其他字段如 scene, context 等，也可以一并设置
            // 或者前端自己解析 JSON
        });

        return dto;
    }

    @Override
    @Transactional
    public void update(String nodeId, UpdateNodeRequest request) {
        StoryNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new BizException("节点不存在: " + nodeId));

        if (!ServiceUtil.isEmpty(request.getNodeName())) {
            node.setNodeName(request.getNodeName());
        }
        if (request.getSceneDescription() != null) {
            node.setSceneDescription(request.getSceneDescription());
        }
        if (request.getPositionX() != null) {
            node.setPositionX(request.getPositionX());
        }
        if (request.getPositionY() != null) {
            node.setPositionY(request.getPositionY());
        }
        if (request.getAssociatedCharIds() != null) {
            try {
                node.setAssociatedChars(objectMapper.writeValueAsString(request.getAssociatedCharIds()));
            } catch (Exception e) {
                throw new BizException("角色列表序列化失败");
            }
        }
        if (request.getInitialVariables() != null) {
            try {
                node.setInitialVariables(objectMapper.writeValueAsString(request.getInitialVariables()));
            } catch (Exception e) {
                throw new BizException("变量序列化失败");
            }
        }

        nodeRepository.save(node);
    }

    @Override
    @Transactional
    public void delete(String nodeId) {
        if (!nodeRepository.existsById(nodeId)) {
            throw new BizException("节点不存在: " + nodeId);
        }
        nodeRepository.deleteById(nodeId);
    }

    /**
     * L4: 批量生成故事节点
     * 依赖L3: OutlineStructure（解析后的大纲结构）
     */
    @Override
    @Transactional
    public List<String> generateNodes(Long projectId, GenerateNodesRequest request) {
        // 1. 校验项目
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 2. 获取L3数据：解析大纲结构
        OutlineStructure outline = generatedContentService.parseOutlineStructure(projectId);

        if (outline.getActs() == null || outline.getActs().isEmpty()) {
            throw new BizException("L3大纲结构为空，无法生成节点");
        }

        // 3. 清空旧节点（可选：如果前端确认重新生成）
        // 注意：这会级联删除关联的边，需谨慎
        if (request.isClearExisting()) {
            List<StoryNode> oldNodes = nodeRepository.findByProjectId(projectId);
            nodeRepository.deleteAll(oldNodes);
        }

        // 4. 根据大纲批量生成节点
        List<String> nodeIds = new ArrayList<>();
        int totalBeats = 0;

        // 遍历每一幕
        for (ActStructure act : outline.getActs()) {
            int actIndex = act.getActIndex();

            // 遍历每个节拍（生成一个节点）
            for (BeatStructure beat : act.getBeats()) {
                int beatIndex = beat.getBeatIndex();

                // 生成节点ID
                String nodeId = "node_" + UUID.randomUUID().toString().substring(0, 8);

                StoryNode node = new StoryNode();
                node.setId(nodeId);
                node.setProject(project);
                node.setNodeName(beat.getTitle());           // 使用节拍标题作为节点名
                node.setSceneDescription(beat.getDescription()); // 使用节拍描述作为初始场景描述
                node.setActIndex(actIndex);                  // 第几幕（L3结构索引）
                node.setBeatIndex(beatIndex);                // 第几节拍（幕内顺序）

                // 关联角色（存储为JSON数组）
                try {
                    node.setAssociatedChars(objectMapper.writeValueAsString(beat.getKeyCharacters()));
                } catch (Exception e) {
                    node.setAssociatedChars("[]");
                }

                // 初始变量为空
                node.setInitialVariables("{}");

                // 默认坐标（后续可在画布上调整）
                // 根据幕和节拍计算默认位置，避免重叠
                BigDecimal x = new BigDecimal(actIndex * 300);      // 每幕横向间隔300
                BigDecimal y = new BigDecimal(beatIndex * 200);     // 每节拍纵向间隔200
                node.setPositionX(x);
                node.setPositionY(y);

                nodeRepository.save(node);
                nodeIds.add(nodeId);
                totalBeats++;
            }
        }

        System.out.println("L4生成完成: 共" + outline.getActs().size() + "幕, " + totalBeats + "个节点");

        return nodeIds;
    }

    /**
     * L5: 生成单个节点的场景氛围描述
     * 依赖L1-L4: 世界观摘要 + 角色摘要 + 大纲摘要 + 当前节点信息
     */
    @Override
    @Transactional
    public String generateNodeDescription(Long projectId, String nodeId, GenerateNodeDescRequest request) {
        // 1. 校验节点存在且属于当前项目
        StoryNode node = nodeRepository.findByIdAndProjectId(nodeId, projectId)
                .orElseThrow(() -> new BizException("节点不存在或不属于该项目"));

        // 2. 获取L1-L4摘要（分层截断策略A）
        String l1Summary = buildL1Summary(projectId);      // 世界观摘要（约100字）
        String l2Summary = buildL2Summary(projectId);      // 角色摘要（约200字）
        String l3Summary = buildL3Summary(projectId);      // 大纲摘要（约150字）

        // 3. 构建L5提示词
        String systemPrompt = buildL5SystemPrompt();
        String userPrompt = buildL5UserPrompt(l1Summary, l2Summary, l3Summary, node, request);

        // 4. 调用AI生成详细场景描述
        String sceneDescription = aiClientService.syncCompletion(systemPrompt, userPrompt);
        System.out.println("L5 AI响应（节点" + nodeId + "）: " + sceneDescription.substring(0, Math.min(100, sceneDescription.length())) + "...");

        // 5. 保存到节点（覆盖L4的简略描述）
        node.setSceneDescription(sceneDescription);
        nodeRepository.save(node);

        return sceneDescription;
    }

    /**
     * 构建L1摘要（世界观，约100字）
     */
    private String buildL1Summary(Long projectId) {
        WorldSetting setting = worldSettingRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BizException("L1世界观不存在"));

        // 取description前100字 + 核心设定
        String desc = setting.getDescription();
        if (desc.length() > 100) {
            desc = desc.substring(0, 100) + "...";
        }

        return String.format("【世界观】%s %s/%s, %s",
                desc,
                setting.getGenre(),
                setting.getSubGenre() != null ? setting.getSubGenre() : "",
                setting.getCoreConflict() != null ? setting.getCoreConflict() : ""
        );
    }

    /**
     * 构建L2摘要（重要角色，约200字）
     */
    private String buildL2Summary(Long projectId) {
        List<com.bdu.plotassistant.entity.Character> characters = characterRepository.findByProjectIdAndStatus(projectId, 1);

        StringBuilder sb = new StringBuilder("【角色】");
        for (com.bdu.plotassistant.entity.Character c : characters) {
            // 每人一行，约30字
            String persona = c.getPersonaPrompt();
            if (persona.length() > 30) {
                persona = persona.substring(0, 30) + "...";
            }
            sb.append(String.format("%s(%s):%s; ",
                    c.getName(),
                    c.getRoleType(),
                    persona
            ));
        }

        String result = sb.toString();
        if (result.length() > 200) {
            result = result.substring(0, 200) + "...等" + characters.size() + "人";
        }
        return result;
    }

    /**
     * 构建L3摘要（大纲结构，约150字）
     */
    private String buildL3Summary(Long projectId) {
        try {
            OutlineStructure outline = generatedContentService.parseOutlineStructure(projectId);

            // 只取当前幕的概要（通过节点反查幕号，或取全部幕名）
            StringBuilder sb = new StringBuilder("【剧情】");
            sb.append("共").append(outline.getActs().size()).append("幕: ");

            for (ActStructure act : outline.getActs()) {
                sb.append(act.getName()).append("(")
                        .append(act.getBeats().size()).append("场) ");
            }

            sb.append("主题:").append(String.join(",", outline.getThemes()));

            String result = sb.toString();
            return result.length() > 150 ? result.substring(0, 150) + "..." : result;

        } catch (Exception e) {
            return "【剧情】大纲结构";
        }
    }

    /**
     * 构建L5系统提示词
     */
    private String buildL5SystemPrompt() {
        return "你是一位资深的场景氛围描写专家，擅长用感官细节营造沉浸感。\n" +
                "\n" +
                "【任务】\n" +
                "基于提供的世界观、角色、剧情背景和节点基础信息，生成一段详细的场景氛围描述。\n" +
                "\n" +
                "【描述要求】\n" +
                "1. 必须包含多感官细节：\n" +
                "   - 视觉：色彩、光影、材质、空间布局\n" +
                "   - 听觉：环境音、人声、机械/魔法音效\n" +
                "   - 嗅觉：气味、空气质量、腐败/清新程度\n" +
                "   - 触觉：温度、湿度、表面的触感\n" +
                "2. 必须体现该节点在剧情中的情绪基调（紧张、温馨、诡异、宏大等）\n" +
                "3. 必须与L1世界观设定一致（科技/魔法水平、社会氛围）\n" +
                "4. 暗示可能存在的互动元素（可调查物品、可对话角色、可触发事件）\n" +
                "5. 长度：200-400字，连贯的散文式描述\n" +
                "\n" +
                "【输出格式】\n" +
                "直接返回描述文本，不要JSON，不要分点，不要加入'视觉：''听觉：'这类标签。要求自然融入感官描写的连贯段落。";
    }

    /**
     * 构建L5用户提示词（L1-L4集 + 当前节点）
     */
    private String buildL5UserPrompt(String l1Summary, String l2Summary, String l3Summary,
                                     StoryNode node, GenerateNodeDescRequest request) {
        StringBuilder prompt = new StringBuilder();

        // L1-L3摘要（约450字，控制Token）
        prompt.append(l1Summary).append("\n");
        prompt.append(l2Summary).append("\n");
        prompt.append(l3Summary).append("\n\n");

        // L4当前节点信息（核心）
        prompt.append("【当前场景】\n");
        prompt.append("节点名称：").append(node.getNodeName()).append("\n");
        prompt.append("所属幕次：第").append(node.getActIndex()).append("幕\n");
        prompt.append("场景顺序：第").append(node.getBeatIndex()).append("个节拍\n");
        prompt.append("基础描述：").append(node.getSceneDescription()).append("\n");

        // 关联角色（如果有）
        if (node.getAssociatedChars() != null && !node.getAssociatedChars().equals("[]")) {
            prompt.append("出现角色：").append(node.getAssociatedChars()).append("\n");
        }

        // 用户特殊要求
        if (request != null && request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            prompt.append("\n【特殊要求】").append(request.getPrompt());
        } else {
            prompt.append("\n要求：重点描写环境如何反映当前剧情张力，为后续角色互动做铺垫。");
        }

        return prompt.toString();
    }

    /**
     * 获取项目下所有节点（按幕和节拍排序）
     */
    @Override
    public List<StoryNode> getNodesByProject(Long projectId) {
        return nodeRepository.findByProjectIdOrderByActIndexAscBeatIndexAsc(projectId);
    }

    /**
     * 获取特定幕的所有节点
     */
    @Override
    public List<StoryNode> getNodesByAct(Long projectId, Integer actIndex) {
        return nodeRepository.findByProjectIdAndActIndex(projectId, actIndex);
    }

    @Override
    public String generateDescription(String nodeId, GenerateNodeDescRequest request) {
        StoryNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new BizException("节点不存在: " + nodeId));

        String systemPrompt = "你是一位环境叙事专家，擅长用感官细节营造场景氛围。" +
                "请生成200字左右的场景描述，包含视觉、听觉、气味等细节。";

        String userPrompt = String.format("场景名称：%s\n用户要求：%s",
                node.getNodeName(), request.getPrompt());

        return aiClientService.syncCompletion(systemPrompt, userPrompt);
    }

    @Override
    @Transactional
    public void batchSave(Long projectId, BatchSaveNodesRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        List<BatchSaveNodesRequest.NodeDTO> nodes = request.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            throw new BizException("节点列表不能为空");
        }

        // 先删除该项目所有节点
        List<StoryNode> existingNodes = nodeRepository.findByProjectId(projectId);
        nodeRepository.deleteAll(existingNodes);

        // 批量插入新节点
        List<StoryNode> newNodes = new ArrayList<>();
        for (BatchSaveNodesRequest.NodeDTO dto : nodes) {
            StoryNode node = new StoryNode();
            node.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID().toString().replace("-", ""));
            node.setProject(project);
            node.setNodeName(dto.getNodeName());
            node.setSceneDescription(dto.getSceneDescription());

            // 设置大纲索引
            node.setActIndex(dto.getActIndex() != null ? dto.getActIndex() : 0);
            node.setBeatIndex(dto.getBeatIndex() != null ? dto.getBeatIndex() : 0);

            // 处理坐标
            if (dto.getPositionX() != null) {
                node.setPositionX(dto.getPositionX());
            } else {
                node.setPositionX(BigDecimal.ZERO);
            }

            if (dto.getPositionY() != null) {
                node.setPositionY(dto.getPositionY());
            } else {
                node.setPositionY(BigDecimal.ZERO);
            }

            try {
                List<String> charIds = dto.getAssociatedCharIds();
                if (charIds == null) {
                    charIds = new ArrayList<>();
                }
                node.setAssociatedChars(objectMapper.writeValueAsString(charIds));
                node.setInitialVariables(objectMapper.writeValueAsString(new HashMap<>()));
            } catch (Exception e) {
                throw new BizException("节点数据序列化失败: " + node.getNodeName());
            }

            newNodes.add(node);
        }

        nodeRepository.saveAll(newNodes);
    }

    @Override
    public List<NodeDTO> getByProject(Long projectId) {
        List<StoryNode> nodes = nodeRepository.findByProjectId(projectId);
        List<NodeDTO> result = new ArrayList<>();

        for (StoryNode node : nodes) {
            result.add(convertToDTO(node));
        }
        return result;
    }

    @Override
    public NodeDTO getById(String nodeId) {
        StoryNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new BizException("节点不存在: " + nodeId));
        return convertToDTO(node);
    }

    // ========== 私有转换方法（JDK 8兼容） ==========

    private NodeSummaryDTO convertToSummaryDTO(StoryNode node) {
        NodeSummaryDTO dto = new NodeSummaryDTO();
        dto.setId(node.getId());
        dto.setNodeName(node.getNodeName());
        dto.setPositionX(node.getPositionX());
        dto.setPositionY(node.getPositionY());
        dto.setActIndex(node.getActIndex());
        dto.setBeatIndex(node.getBeatIndex());

        // 反序列化角色ID列表
        try {
            List<String> charIds = objectMapper.readValue(node.getAssociatedChars(),
                    new TypeReference<List<String>>() {});
            dto.setAssociatedCharIds(charIds);
        } catch (Exception e) {
            dto.setAssociatedCharIds(new ArrayList<>());
        }

        return dto;
    }


    private NodeDetailDTO convertToDetailDTO(StoryNode node) {
        NodeDetailDTO dto = new NodeDetailDTO();
        dto.setId(node.getId());
        dto.setNodeName(node.getNodeName());
        dto.setSceneDescription(node.getSceneDescription());
        dto.setActIndex(node.getActIndex());
        dto.setBeatIndex(node.getBeatIndex());
        dto.setPositionX(node.getPositionX());
        dto.setPositionY(node.getPositionY());

        try {
            List<String> charIds = objectMapper.readValue(node.getAssociatedChars(),
                    new TypeReference<List<String>>() {});
            dto.setAssociatedCharIds(charIds);

            Map<String, Object> vars = objectMapper.readValue(node.getInitialVariables(),
                    new TypeReference<Map<String, Object>>() {});
            dto.setInitialVariables(vars);
        } catch (Exception e) {
            dto.setAssociatedCharIds(new ArrayList<>());
            dto.setInitialVariables(new java.util.HashMap<>());
        }

        dto.setCreatedAt(ServiceUtil.formatDateTime(node.getCreatedAt()));
        dto.setUpdatedAt(ServiceUtil.formatDateTime(node.getUpdatedAt()));

        return dto;
    }

    private NodeDTO convertToDTO(StoryNode node) {
        NodeDTO dto = new NodeDTO();
        dto.setId(node.getId());
        dto.setNodeName(node.getNodeName());
        dto.setSceneDescription(node.getSceneDescription());
        dto.setPositionX(node.getPositionX());
        dto.setPositionY(node.getPositionY());

        try {
            List<String> charIds = objectMapper.readValue(node.getAssociatedChars(),
                    new TypeReference<List<String>>() {});
            dto.setAssociatedCharIds(charIds);
        } catch (Exception e) {
            dto.setAssociatedCharIds(new ArrayList<>());
        }

        return dto;
    }
}
