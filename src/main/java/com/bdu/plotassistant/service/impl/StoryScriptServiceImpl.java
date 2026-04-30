package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.NodeContent;
import com.bdu.plotassistant.dto.outline.ActStructure;
import com.bdu.plotassistant.dto.outline.BeatStructure;
import com.bdu.plotassistant.dto.outline.OutlineStructure;
import com.bdu.plotassistant.dto.request.storyscript.GenerateWholeLineRequest;
import com.bdu.plotassistant.dto.response.storyscript.StoryScriptDTO;
import com.bdu.plotassistant.entity.*;
import com.bdu.plotassistant.entity.Character;
import com.bdu.plotassistant.repository.*;
import com.bdu.plotassistant.service.AiClientService;
import com.bdu.plotassistant.service.StoryScriptService;
import com.bdu.plotassistant.utils.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;
import java.util.Optional;


@Service
public class StoryScriptServiceImpl implements StoryScriptService {

    private final StoryScriptRepository scriptRepository;
    private final ProjectRepository projectRepository;
    private final StoryNodeRepository nodeRepository;
    private final StoryEdgeRepository edgeRepository;
    private final GeneratedContentRepository contentRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final CharacterRepository characterRepository;
    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;

    public StoryScriptServiceImpl(StoryScriptRepository scriptRepository,
                                  ProjectRepository projectRepository,
                                  StoryNodeRepository nodeRepository,
                                  StoryEdgeRepository edgeRepository,
                                  GeneratedContentRepository contentRepository,
                                  WorldSettingRepository worldSettingRepository,
                                  CharacterRepository characterRepository,
                                  AiClientService aiClientService,
                                  ObjectMapper objectMapper) {
        this.scriptRepository = scriptRepository;
        this.projectRepository = projectRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.contentRepository = contentRepository;
        this.worldSettingRepository = worldSettingRepository;
        this.characterRepository = characterRepository;
        this.aiClientService = aiClientService;
        this.objectMapper = objectMapper;
    }

    /**
     * L9: 生成完整剧情（指定分支路径）
     * 支持格式：node_001|edge_001->node_002|edge_002->node_003
     */
    @Override
    @Transactional
    public String generateWholeLine(Long projectId, GenerateWholeLineRequest request) {
        // 1. 校验项目
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 2. 解析路径（新的格式：包含节点和边）
        List<PathStep> pathSteps = parseBranchPath(request.getBranchPath());
        if (pathSteps.size() < 2) {
            throw new BizException("分支路径至少需要包含2个节点");
        }

        // 3. 收集路径上的所有内容（L5, L7, L8）
        List<NodeContent> pathContents = collectPathContents(projectId, pathSteps);

        // 4. 获取L3大纲结构（作为骨架）
        OutlineStructure outline = parseOutlineStructure(projectId);

        // 5. 获取L1世界观和L2角色（作为基调）
        String worldContext = buildL9WorldContext(projectId);
        String characterContext = buildL9CharacterContext(projectId);

        // 6. 构建L9提示词（组装所有层级）
        String systemPrompt = buildL9SystemPrompt();
        String userPrompt = buildL9UserPrompt(worldContext, characterContext, outline, pathContents, request);

        // 7. 调用AI生成完整剧情（长文本）
        String wholeStory = aiClientService.syncCompletion(systemPrompt, userPrompt);
        System.out.println("L9生成完成，长度：" + wholeStory.length() + "字符");

        // 8. 保存到story_script表（提取纯节点ID数组）
        String[] nodeIds = extractNodeIds(pathSteps);
        Long scriptId = saveWholeLine(projectId, request.getBranchPath(), wholeStory, nodeIds);

        return wholeStory;
    }

    /**
     * 路径步骤内部类
     */
    public static class PathStep {
        private final String nodeId;
        private final Long edgeId; // 如果是最后一段，可能为null

        public PathStep(String nodeId, Long edgeId) {
            this.nodeId = nodeId;
            this.edgeId = edgeId;
        }

        public String getNodeId() {
            return nodeId;
        }

        public Long getEdgeId() {
            return edgeId;
        }
    }

    /**
     * 解析分支路径（支持 node_001|1->node_002|5->node_003 格式）
     */
    private List<PathStep> parseBranchPath(String branchPath) {
        List<PathStep> steps = new ArrayList<>();

        if (!StringUtils.hasText(branchPath)) {
            return steps;
        }

        String[] segments = branchPath.split("->");

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.isEmpty()) {
                continue;
            }

            // 分割节点ID和边ID（用|分隔）
            String[] parts = segment.split("\\|");
            String nodeId = parts[0];

            Long edgeId = null;
            // 只有不是最后一段时才应该有边ID（因为边是连接当前节点到下一个节点的）
            // 但前端可以选择是否传递，所以这里兼容处理
            if (parts.length > 1 && i < segments.length - 1) {
                try {
                    edgeId = Long.valueOf(parts[1]);
                } catch (NumberFormatException e) {
                    System.err.println("边ID格式错误: " + parts[1] + "，跳过该边ID");
                }
            }

            steps.add(new PathStep(nodeId, edgeId));
        }

        return steps;
    }

    /**
     * 提取纯节点ID数组（用于保存到referenced_nodes字段）
     */
    private String[] extractNodeIds(List<PathStep> steps) {
        String[] nodeIds = new String[steps.size()];
        for (int i = 0; i < steps.size(); i++) {
            nodeIds[i] = steps.get(i).getNodeId();
        }
        return nodeIds;
    }

    /**
     * 收集路径上的所有内容（L5描述 + L7对话 + L8具体边选项）
     */
    private List<NodeContent> collectPathContents(Long projectId, List<PathStep> steps) {
        List<NodeContent> contents = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            PathStep step = steps.get(i);
            String nodeId = step.getNodeId();

            StoryNode node = nodeRepository.findById(nodeId)
                    .orElseThrow(() -> new BizException("节点不存在: " + nodeId));

            NodeContent content = new NodeContent();
            content.setNodeId(nodeId);
            content.setNodeName(node.getNodeName());
            content.setActIndex(node.getActIndex());
            content.setBeatIndex(node.getBeatIndex());

            // L5: 场景描述
            content.setSceneDescription(node.getSceneDescription());

            // L7: 对话内容（如果有）
            Optional<GeneratedContent> dialogueOpt = contentRepository
                    .findByProjectIdAndContentTypeAndNodeId(projectId, "DIALOGUE", nodeId);

            if (dialogueOpt.isPresent()) {
                content.setDialogue(dialogueOpt.get().getContentJson());
            }

            // L8: 具体选择的边（关键修改：使用具体的边ID，而不是随便查一条）
            if (step.getEdgeId() != null) {
                Optional<StoryEdge> edgeOpt = edgeRepository.findById(step.getEdgeId());
                if (edgeOpt.isPresent()) {
                    StoryEdge edge = edgeOpt.get();
                    // 验证边是否属于当前节点（安全检查）
                    if (!edge.getSource().getId().equals(nodeId)) {
                        System.err.println("警告：边 " + step.getEdgeId() + " 的源节点不是 " + nodeId);
                    }
                    content.setChosenEdge(edge);
                } else {
                    System.err.println("警告：边ID " + step.getEdgeId() + " 不存在");
                }
            } else if (i < steps.size() - 1) {
                // 如果不是最后一段但没有边ID，尝试根据下一个节点查找（兼容旧格式）
                String nextNodeId = steps.get(i + 1).getNodeId();
                List<StoryEdge> edges = edgeRepository.findBySourceIdAndTargetId(nodeId, nextNodeId);
                if (!edges.isEmpty()) {
                    content.setChosenEdge(edges.get(0)); // 取第一条（兼容旧逻辑）
                }
            }

            contents.add(content);
        }

        return contents;
    }

    /**
     * 保存到story_script表
     */
    @Override
    @Transactional
    public Long saveWholeLine(Long projectId, String branchPath, String content, String[] nodeIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在，ID: " + projectId));

        StoryScript script = new StoryScript();
        script.setProject(project);
        script.setBranchPath(branchPath); // 保存完整路径（含边ID）
        script.setContentType("FULL_STORY");
        script.setContent(content);

        // 构建JSON数组字符串（JDK 8兼容方式）
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < nodeIds.length; i++) {
            jsonBuilder.append("\"").append(nodeIds[i]).append("\"");
            if (i < nodeIds.length - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("]");
        script.setReferencedNodes(jsonBuilder.toString());

        script.setIsCanon(false);

        StoryScript saved = scriptRepository.save(script);
        return saved.getId();
    }

    /**
     * 标记为正史（主线）
     */
    @Override
    @Transactional
    public void markAsCanon(Long scriptId) {
        StoryScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BizException("剧本不存在"));

        // 取消该项目的其他正史标记
        List<StoryScript> scripts = scriptRepository.findByProjectId(script.getProject().getId());
        for (StoryScript s : scripts) {
            if (s.getIsCanon() != null && s.getIsCanon()) {
                s.setIsCanon(false);
                scriptRepository.save(s);
            }
        }

        script.setIsCanon(true);
        scriptRepository.save(script);
    }

    @Override
    public List<StoryScriptDTO> listByProject(Long projectId) {
        // 查询实体列表
        List<StoryScript> scripts = scriptRepository.findByProjectId(projectId);
        List<StoryScriptDTO> dtoList = new ArrayList<>();

        // JDK 8 兼容的转换逻辑
        for (StoryScript script : scripts) {
            if (script == null) {
                continue;
            }

            StoryScriptDTO dto = new StoryScriptDTO();

            // 基础字段复制
            dto.setId(script.getId());
            dto.setBranchPath(script.getBranchPath());
            dto.setContentType(script.getContentType());
            dto.setReferencedNodes(script.getReferencedNodes());
            dto.setIsCanon(script.getIsCanon());
            dto.setCreatedAt(script.getCreatedAt());
            dto.setUpdatedAt(script.getUpdatedAt());

            // 处理小说内容（列表接口返回摘要，避免数据过大）
            String fullContent = script.getContent();
            if (fullContent != null) {
                // 列表页只显示前 200 字，详情页再查完整内容
                if (fullContent.length() > 200) {
                    dto.setContentSummary(fullContent.substring(0, 200) + "...");
                } else {
                    dto.setContentSummary(fullContent);
                }
                // dto.setContent(fullContent);
            }

            // 处理项目信息，断开实体关联，只取需要的字段
            Project project = script.getProject();
            if (project != null) {
                dto.setProjectId(project.getId());
                String projectName = project.getName();
                if (projectName == null && project.getName() != null) {
                    projectName = project.getName();
                }
                dto.setProjectName(projectName != null ? projectName : "未命名项目");
            }

            dtoList.add(dto);
        }

        return dtoList;
    }

    @Override
    public StoryScriptDTO getScriptDetail(Long projectId, Long scriptId) {
        // 1. 查询剧本
        StoryScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BizException("剧本不存在: " + scriptId));

        // 2. 权限校验：确保剧本属于指定项目（防止越权访问）
        if (script.getProject() == null || !script.getProject().getId().equals(projectId)) {
            throw new BizException("剧本不属于该项目");
        }

        // 3. 转换为 DTO（返回完整内容）
        StoryScriptDTO dto = new StoryScriptDTO();
        dto.setId(script.getId());
        dto.setBranchPath(script.getBranchPath());
        dto.setContentType(script.getContentType());
        dto.setReferencedNodes(script.getReferencedNodes());
        dto.setIsCanon(script.getIsCanon());
        dto.setCreatedAt(script.getCreatedAt());
        dto.setUpdatedAt(script.getUpdatedAt());

        // 关键区别：详情页返回完整小说内容
        dto.setContent(script.getContent());
        // 摘要字段可以设为空或也填充（前端可选展示）
        dto.setContentSummary(null);

        // 项目信息
        Project project = script.getProject();
        if (project != null) {
            dto.setProjectId(project.getId());
            String projectName = project.getName();
            if (projectName == null && project.getName() != null) {
                projectName = project.getName();
            }
            dto.setProjectName(projectName);
        }

        return dto;
    }

    // ==================== 原有的辅助方法保持不变 ====================

    private String buildL9SystemPrompt() {
        return "你是一位资深游戏编剧兼小说家，擅长将分散的剧情元素整合成连贯的叙事文本。\n" +
                "\n" +
                "【任务】\n" +
                "基于提供的世界观、角色、大纲骨架和具体场景内容，生成一篇连贯的完整剧情小说。\n" +
                "\n" +
                "【写作要求】\n" +
                "1. 以大纲为骨架：严格按照幕和节拍顺序组织章节\n" +
                "2. 融入场景描述：将L5的场景氛围转化为小说的环境描写\n" +
                "3. 嵌入对话：将L7的对话自然融入叙事，不要生硬堆砌\n" +
                "4. 体现选择逻辑：说明为什么选择这条路径（基于L8的边选项）\n" +
                "5. 保持角色一致性：符合L2角色的性格、语言风格、人物弧光\n" +
                "6. 语言风格：符合L1世界观的基调（赛博朋克冷硬、奇幻诗意等）\n" +
                "7. 长度：每个场景300-500字，全文根据场景数决定（通常3000-8000字）\n" +
                "8. 格式：标准小说格式，分章节，有标题，不要JSON\n" +
                "\n" +
                "【结构要求】\n" +
                "第一幕：...（包含该幕所有场景，用===场景名===分隔）\n" +
                "场景内：环境描写->角色行动->对话->剧情推进->过渡到下一场景的逻辑\n" +
                "\n" +
                "【输出格式】\n" +
                "直接返回Markdown格式的小说文本，包含：\n" +
                "# 剧情标题\n" +
                "## 第一幕：幕名\n" +
                "### 场景1：场景名\n" +
                "正文...";
    }

    private String buildL9UserPrompt(String worldContext, String characterContext,
                                     OutlineStructure outline, List<NodeContent> pathContents,
                                     GenerateWholeLineRequest request) {
        StringBuilder prompt = new StringBuilder();

        // L1: 世界观基调
        prompt.append("【世界观设定】（L1）\n").append(worldContext).append("\n\n");

        // L2: 重要角色（保持人设）
        prompt.append("【重要角色】（L2）\n").append(characterContext).append("\n\n");

        // L3: 大纲骨架（结构指导）
        prompt.append("【剧情大纲骨架】（L3）\n");
        prompt.append("标题：").append(outline.getTitle()).append("\n");
        prompt.append("主题：").append(String.join(", ", outline.getThemes())).append("\n");
        prompt.append("结局类型：").append(outline.getEndingType()).append("\n\n");

        // 路径上的具体内容（L5+L7+L8）
        prompt.append("【选定路径的具体内容】\n");
        prompt.append("分支路径：").append(request.getBranchPath()).append("\n");
        prompt.append("共").append(pathContents.size()).append("个场景\n\n");

        for (NodeContent content : pathContents) {
            prompt.append("=== ").append(content.getNodeName()).append(" ===\n");
            prompt.append("【场景氛围】（L5）\n").append(content.getSceneDescription()).append("\n");

            if (content.getDialogue() != null) {
                prompt.append("【关键对话】（L7）\n");
                prompt.append(extractKeyDialogueLines(content.getDialogue())).append("\n");
            }

            if (content.getChosenEdge() != null) {
                StoryEdge edge = content.getChosenEdge();
                prompt.append("【玩家选择】（L8）\n");
                prompt.append("选择：").append(edge.getLabel()).append("\n");
                prompt.append("条件：").append(edge.getConditionExpr()).append("\n");
                prompt.append("结果：").append(edge.getOnSuccess()).append("\n");
                if (edge.getEffect() != null && !edge.getEffect().isEmpty()) {
                    prompt.append("影响：").append(edge.getEffect()).append("\n");
                }
            }
            prompt.append("\n");
        }

        // 特殊要求
        if (StringUtils.hasText(request.getStyle())) {
            prompt.append("【写作风格要求】").append(request.getStyle()).append("\n");
        }

        prompt.append("要求：将以上内容整合成连贯的小说，注意场景间的过渡自然，保持悬疑或张力直到结局。");

        return prompt.toString();
    }

    private String extractKeyDialogueLines(String dialogueJson) {
        try {
            JsonNode root = objectMapper.readTree(dialogueJson);
            JsonNode lines = root.path("lines");
            StringBuilder sb = new StringBuilder();

            int count = 0;
            for (JsonNode line : lines) {
                if (count++ > 5) break;
                sb.append(line.path("speaker").asText())
                        .append("：")
                        .append(line.path("line").asText())
                        .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "对话内容";
        }
    }

    private OutlineStructure parseOutlineStructure(Long projectId) {
        GeneratedContent content = contentRepository
                .findByProjectIdAndContentType(projectId, "OUTLINE")
                .orElseThrow(() -> new BizException("L3大纲不存在"));

        try {
            JsonNode root = objectMapper.readTree(content.getContentJson());
            OutlineStructure structure = new OutlineStructure();
            structure.setTitle(root.path("title").asText("未命名剧情"));
            structure.setEndingType(root.path("endingType").asText("OPEN"));

            List<String> themes = new ArrayList<>();
            root.path("themes").forEach(n -> themes.add(n.asText()));
            structure.setThemes(themes);

            List<ActStructure> acts = new ArrayList<>();
            JsonNode actsNode = root.path("acts");
            for (int i = 0; i < actsNode.size(); i++) {
                JsonNode actNode = actsNode.get(i);
                ActStructure act = new ActStructure();
                act.setActIndex(i + 1);
                act.setName(actNode.path("name").asText("第" + (i+1) + "幕"));

                List<BeatStructure> beats = new ArrayList<>();
                JsonNode beatsNode = actNode.path("beats");
                for (int j = 0; j < beatsNode.size(); j++) {
                    JsonNode beatNode = beatsNode.get(j);
                    BeatStructure beat = new BeatStructure();
                    beat.setBeatIndex(j + 1);
                    beat.setTitle(beatNode.path("title").asText("场景" + (j+1)));
                    beats.add(beat);
                }
                act.setBeats(beats);
                acts.add(act);
            }
            structure.setActs(acts);
            return structure;
        } catch (Exception e) {
            throw new BizException("解析大纲失败: " + e.getMessage());
        }
    }

    private String buildL9WorldContext(Long projectId) {
        WorldSetting setting = worldSettingRepository.findByProjectId(projectId).orElse(null);
        if (setting == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("题材：").append(setting.getGenre());
        if (setting.getSubGenre() != null) sb.append("/").append(setting.getSubGenre());
        sb.append("；");

        if (setting.getDescription() != null) {
            sb.append("氛围：").append(setting.getDescription().substring(0,
                    Math.min(100, setting.getDescription().length()))).append("...");
        }
        sb.append("；核心冲突：").append(setting.getCoreConflict());

        return sb.toString();
    }

    private String buildL9CharacterContext(Long projectId) {
        List<Character> characters = characterRepository.findByProjectIdAndStatus(projectId, 1);
        if (characters.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (Character c : characters) {
            sb.append(c.getName()).append("(").append(c.getRoleType()).append(")：");
            String persona = c.getPersonaPrompt();
            if (persona.length() > 50) persona = persona.substring(0, 50) + "...";
            sb.append(persona).append("；");
        }
        return sb.toString();
    }
}
