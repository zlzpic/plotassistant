package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.outline.ActStructure;
import com.bdu.plotassistant.dto.outline.BeatStructure;
import com.bdu.plotassistant.dto.outline.OutlineStructure;
import com.bdu.plotassistant.dto.request.character.GenerateCharacterSetRequest;
import com.bdu.plotassistant.dto.request.generatedcontent.*;
import com.bdu.plotassistant.dto.request.storyscript.GenerateWholeLineRequest;
import com.bdu.plotassistant.dto.response.generatedcontent.GeneratedContentDTO;
import com.bdu.plotassistant.entity.GeneratedContent;
import com.bdu.plotassistant.entity.Project;
import com.bdu.plotassistant.entity.StoryNode;
import com.bdu.plotassistant.entity.WorldSetting;
import com.bdu.plotassistant.repository.*;
import com.bdu.plotassistant.service.AiClientService;
import com.bdu.plotassistant.service.CharacterService;
import com.bdu.plotassistant.service.GeneratedContentService;
import com.bdu.plotassistant.service.WorldSettingService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.var;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GeneratedContentServiceImpl implements GeneratedContentService {

    private final GeneratedContentRepository contentRepository;
    private final ProjectRepository projectRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final AiClientService aiClientService;
    private final WorldSettingService worldSettingService;
    private final CharacterService characterService;
    private final ObjectMapper objectMapper;
    private final CharacterRepository characterRepository;
    private final StoryNodeRepository storyNodeRepository;

    public GeneratedContentServiceImpl(GeneratedContentRepository contentRepository,
                                       ProjectRepository projectRepository,
                                       WorldSettingRepository worldSettingRepository, AiClientService aiClientService,
                                       WorldSettingService worldSettingService,
                                       CharacterService characterService,
                                       ObjectMapper objectMapper, CharacterRepository characterRepository, StoryNodeRepository storyNodeRepository) {
        this.contentRepository = contentRepository;
        this.projectRepository = projectRepository;
        this.worldSettingRepository = worldSettingRepository;
        this.aiClientService = aiClientService;
        this.worldSettingService = worldSettingService;
        this.characterService = characterService;
        this.objectMapper = objectMapper;
        this.characterRepository = characterRepository;
        this.storyNodeRepository = storyNodeRepository;
    }



    /**
     * L3: 生成故事大纲
     * 依赖L1: WorldSetting.description
     * 依赖L2: Character(status=1) 重要角色
     */
    @Override
    @Transactional
    public String generateOutline(Long projectId, GenerateOutlineRequest request) {
        // 1. 校验项目
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 2. 获取L1数据：世界观描述
        WorldSetting worldSetting = worldSettingRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BizException("L1世界观未生成"));
        if (worldSetting.getDescription() == null || worldSetting.getDescription().isEmpty()) {
            throw new BizException("L1世界观描述为空");
        }

        // 3. 获取L2数据：重要角色（status=1）
        List<com.bdu.plotassistant.entity.Character> importantCharacters = characterRepository.findByProjectIdAndStatus(projectId, 1);
        if (importantCharacters.isEmpty()) {
            throw new BizException("L2重要角色未生成，请先生成角色");
        }

        // 4. 构建L3提示词
        String systemPrompt = buildL3SystemPrompt();
        String userPrompt = buildL3UserPrompt(project, worldSetting, importantCharacters, request);

        // 5. 调用AI生成大纲
        String outlineJson = aiClientService.syncCompletion(systemPrompt, userPrompt);
        System.out.println("L3 AI响应: " + outlineJson);

        // 6. 验证JSON格式（确保L4能解析）
        validateOutlineFormat(outlineJson);

        // 7. 保存到数据库（content_type='OUTLINE'）
        saveGeneratedContent(project, "OUTLINE", outlineJson, request);

        return outlineJson;
    }

    /**
     * 构建L3系统提示词
     */
    private String buildL3SystemPrompt() {
        return "你是一位资深的游戏剧情设计师，擅长构建三幕式或五幕式故事结构。\n" +
                "\n" +
                "【任务】\n" +
                "基于世界观和已有角色，设计完整的剧情大纲。\n" +
                "\n" +
                "【结构要求】\n" +
                "1. 必须包含3-5幕（Acts），每幕有明确的起承转合\n" +
                "2. 每幕包含3-5个节拍（Beats），每个节拍是一个具体场景或事件\n" +
                "3. 必须体现世界观核心冲突的 escalation（升级）\n" +
                "4. 必须让L2提供的重要角色有明确的登场时机和弧光变化\n" +
                "5. 包含主题（Themes）和结局类型（结局可以是开放式、悲剧、喜剧等）\n" +
                "\n" +
                "【输出格式】\n" +
                "必须严格返回JSON格式，便于后续解析：\n" +
                "{\n" +
                "  \"title\": \"大纲标题\",\n" +
                "  \"themes\": [\"主题1\", \"主题2\"],\n" +
                "  \"endingType\": \"TRAGIC(悲剧)/HAPPY(喜剧)/OPEN(开放式)/BITTERSWEET( bittersweet)\",\n" +
                "  \"acts\": [\n" +
                "    {\n" +
                "      \"actNumber\": 1,\n" +
                "      \"name\": \"幕名称（如：觉醒）\",\n" +
                "      \"summary\": \"幕概要（100字以内）\",\n" +
                "      \"beats\": [\n" +
                "        {\n" +
                "          \"beatNumber\": 1,\n" +
                "          \"title\": \"节拍标题（如：诊所遭遇）\",\n" +
                "          \"description\": \"场景描述（50字以内）\",\n" +
                "          \"keyCharacters\": [\"角色名1\", \"角色名2\"],\n" +
                "          \"conflict\": \"冲突点\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /**
     * 构建L3用户提示词（L1集 + L2集 + 用户要求）
     */
    private String buildL3UserPrompt(Project project, WorldSetting worldSetting,
                                     List<com.bdu.plotassistant.entity.Character> characters, GenerateOutlineRequest request) {
        StringBuilder prompt = new StringBuilder();

        // L1集：世界观描述（核心上下文）
        prompt.append("【世界观氛围】（L1）\n");
        prompt.append(worldSetting.getDescription()).append("\n\n");

        // L1基础设定补充
        prompt.append("【世界观基础】\n");
        prompt.append("题材：").append(worldSetting.getGenre());
        if (worldSetting.getSubGenre() != null) {
            prompt.append("/").append(worldSetting.getSubGenre());
        }
        prompt.append("\n");
        prompt.append("核心冲突：").append(worldSetting.getCoreConflict()).append("\n\n");

        // L2集：重要角色摘要（截断避免过长）
        prompt.append("【重要角色】（L2，必须在剧情中安排登场和弧光）\n");
        prompt.append(buildCharacterSummaryForL3(characters)).append("\n");

        // 用户要求
        prompt.append("【剧情要求】\n");
        prompt.append("黑暗程度：").append(request.getDarkness()).append("/10\n");
        prompt.append("复杂度：").append(request.getComplexity()).append("/10\n");

        if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            prompt.append("特殊要求：").append(request.getPrompt()).append("\n");
        } else {
            prompt.append("要求：确保每个重要角色至少有一个高光时刻，剧情节奏张弛有度\n");
        }

        return prompt.toString();
    }

    /**
     * 构建L3用的角色摘要（精简版，每人一行）
     */
    private String buildCharacterSummaryForL3(List<com.bdu.plotassistant.entity.Character> characters) {
        StringBuilder summary = new StringBuilder();

        for (com.bdu.plotassistant.entity.Character c : characters) {
            // 截断personaPrompt前50字，避免过长
            String persona = c.getPersonaPrompt();
            if (persona.length() > 50) {
                persona = persona.substring(0, 50) + "...";
            }

            summary.append(String.format("- %s（%s）: %s\n",
                    c.getName(),
                    c.getRoleType(),  // PROTAGONIST/ANTAGONIST/SUPPORT
                    persona
            ));
        }

        return summary.toString();
    }

    /**
     * 验证大纲JSON格式（确保包含acts数组供L4使用）
     */
    private void validateOutlineFormat(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode acts = root.path("acts");
            if (!acts.isArray() || acts.size() == 0) {
                throw new BizException("大纲格式错误：缺少acts数组");
            }

            // 验证每个act有beats
            for (int i = 0; i < acts.size(); i++) {
                JsonNode beats = acts.get(i).path("beats");
                if (!beats.isArray() || beats.size() == 0) {
                    throw new BizException("大纲格式错误：第" + (i+1) + "幕缺少beats");
                }
            }

        } catch (Exception e) {
            throw new BizException("大纲JSON格式验证失败: " + e.getMessage());
        }
    }

    /**
     * 保存生成内容
     */
    private void saveGeneratedContent(Project project, String contentType,
                                      String contentJson, Object request) {
        // 查询是否已存在（覆盖更新）
        GeneratedContent content = contentRepository
                .findByProjectIdAndContentType(project.getId(), contentType)
                .orElse(new GeneratedContent());

        content.setProject(project);
        content.setContentType(contentType);
        content.setContentJson(contentJson);

        // 保存生成参数（可选）
        try {
            content.setGenerationParams(objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            content.setGenerationParams("{}");
        }

        content.setAiModel("gpt-4"); // 实际从配置获取
        content.setTokenUsage(contentJson.length());
        content.setGenerationTimeMs(0); // 可计算实际耗时
        content.setIsEdited(0);

        contentRepository.save(content);
    }

    /**
     * 解析大纲结构（供L4使用）
     * 提取幕和节拍信息，用于生成StoryNode的act_index和beat_index
     */
    @Override
    public OutlineStructure parseOutlineStructure(Long projectId) {
        GeneratedContent content = contentRepository
                .findByProjectIdAndContentType(projectId, "OUTLINE")
                .orElseThrow(() -> new BizException("L3大纲不存在"));

        try {
            JsonNode root = objectMapper.readTree(content.getContentJson());
            OutlineStructure structure = new OutlineStructure();

            List<ActStructure> acts = new ArrayList<>();
            JsonNode actsNode = root.path("acts");

            for (int i = 0; i < actsNode.size(); i++) {
                JsonNode actNode = actsNode.get(i);
                ActStructure act = new ActStructure();
                act.setActIndex(i + 1);  // 从1开始
                act.setName(actNode.path("name").asText("第" + (i+1) + "幕"));
                act.setSummary(actNode.path("summary").asText(""));

                List<BeatStructure> beats = new ArrayList<>();
                JsonNode beatsNode = actNode.path("beats");

                for (int j = 0; j < beatsNode.size(); j++) {
                    JsonNode beatNode = beatsNode.get(j);
                    BeatStructure beat = new BeatStructure();
                    beat.setBeatIndex(j + 1);  // 从1开始
                    beat.setTitle(beatNode.path("title").asText("场景" + (j+1)));
                    beat.setDescription(beatNode.path("description").asText(""));
                    beat.setKeyCharacters(extractStringArray(beatNode.path("keyCharacters")));
                    beats.add(beat);
                }

                act.setBeats(beats);
                acts.add(act);
            }

            structure.setActs(acts);
            structure.setTitle(root.path("title").asText("未命名大纲"));
            structure.setThemes(extractStringArray(root.path("themes")));
            structure.setEndingType(root.path("endingType").asText("OPEN"));

            return structure;

        } catch (Exception e) {
            throw new BizException("解析大纲结构失败: " + e.getMessage());
        }
    }

    /**
     * 辅助：提取字符串数组
     */
    private List<String> extractStringArray(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(n -> list.add(n.asText()));
        }
        return list;
    }

    // ========== L7方法占位（后续实现） ==========

    /**
     * L7: 生成场景对话
     */
    /**
     * L7: 生成场景对话
     * 依赖L2: 重要角色（status=1）
     * 依赖L6: NPC（status=2）
     * 依赖L5: 节点详细氛围描述（sceneDescription）
     */
    @Override
    @Transactional
    public String generateDialogueSample(Long projectId, GenerateDialogueRequest request) {
        // 1. 校验节点存在（L5）
        StoryNode node = storyNodeRepository.findById(request.getNodeId())
                .orElseThrow(() -> new BizException("节点不存在"));

        if (!node.getProject().getId().equals(projectId)) {
            throw new BizException("节点不属于该项目");
        }

        // 2. 检查L5是否已生成
        if (node.getSceneDescription() == null || node.getSceneDescription().isEmpty()) {
            throw new BizException("L5节点描述未生成");
        }

        // 3. 获取参与对话的角色（L2 + L6）
        List<com.bdu.plotassistant.entity.Character> participants = getDialogueParticipants(projectId, request.getCharacterIds(), node);
        if (participants.size() < 2) {
            throw new BizException("对话至少需要2个角色，该场景角色不足");
        }

        // 4. 构建L7提示词
        String systemPrompt = buildL7SystemPrompt();
        String userPrompt = buildL7UserPrompt(node, participants, request);

        // 5. 调用AI生成对话
        String dialogueJson = aiClientService.syncCompletion(systemPrompt, userPrompt);
        System.out.println("L7 AI响应: " + dialogueJson);

        // 6. 保存到数据库（content_type='DIALOGUE'，标记node_id）
        saveDialogueContent(projectId, request.getNodeId(), dialogueJson, request);

        return dialogueJson;
    }

    /**
     * 获取对话参与者（L2重要角色 + L6 NPC）
     */
    private List<com.bdu.plotassistant.entity.Character> getDialogueParticipants(
            Long projectId, List<String> characterIds, StoryNode node) {

        List<com.bdu.plotassistant.entity.Character> participants = new ArrayList<>();

        // 如果指定了角色ID，按ID查询
        if (characterIds != null && !characterIds.isEmpty()) {
            for (String charId : characterIds) {
                Optional<com.bdu.plotassistant.entity.Character> optional = characterRepository.findById(charId);
                if (optional.isPresent()) {
                    com.bdu.plotassistant.entity.Character c = optional.get();
                    if (c.getProject().getId().equals(projectId)) {
                        participants.add(c);
                    }
                }
            }
        } else {
            // 自动选取：优先选与节点关联的角色（L2重要角色 + L6本节点NPC）
            // 先取L2重要角色（status=1）
            List<com.bdu.plotassistant.entity.Character> important = characterRepository.findByProjectIdAndStatus(projectId, 1);
            participants.addAll(important);

            // 再取L6 NPC（status=2）
            List<com.bdu.plotassistant.entity.Character> npcs = characterRepository.findByProjectIdAndStatus(projectId, 2);
            participants.addAll(npcs);

            // 限制数量，避免Token爆炸（最多4-5人对话）
            if (participants.size() > 5) {
                participants = participants.subList(0, 5);
            }
        }

        return participants;
    }
    /**
     * 构建L7系统提示词
     */
    private String buildL7SystemPrompt() {
        return "你是一位资深游戏编剧，擅长写潜台词丰富的角色对话。\n" +
                "\n" +
                "【任务】\n" +
                "基于场景氛围和角色人设，生成符合人物性格的对话。\n" +
                "\n" +
                "【对话要求】\n" +
                "1. 每个人物的台词必须符合其speechPattern（语言特征）\n" +
                "2. 对话必须推动剧情或揭示人物关系，避免无意义的寒暄\n" +
                "3. 必须体现场景的tension（紧张感/和谐感/诡异感等）\n" +
                "4. 包含潜台词（subtext）：表面说的和实际想的可以不同\n" +
                "5. 可以包含选择分支点（玩家插话选项）\n" +
                "\n" +
                "【输出格式】\n" +
                "必须严格返回JSON格式：\n" +
                "{\n" +
                "  \"scene\": \"场景名称\",\n" +
                "  \"context\": \"对话前的剧情背景（1句话）\",\n" +
                "  \"lines\": [\n" +
                "    {\n" +
                "      \"speaker\": \"角色名\",\n" +
                "      \"line\": \"台词内容（符合角色语言特征）\",\n" +
                "      \"subtext\": \"潜台词/内心OS（可选）\",\n" +
                "      \"emotion\": \"情绪标签（如：愤怒、嘲讽、恐惧）\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"playerChoices\": [\n" +
                "    {\n" +
                "      \"text\": \"玩家可选回应\",\n" +
                "      \"condition\": \"前置条件（可选）\",\n" +
                "      \"impact\": \"选择后的影响简述\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /**
     * 构建L7用户提示词（L2角色 + L6角色 + L5场景）
     */
    private String buildL7UserPrompt(StoryNode node, List<com.bdu.plotassistant.entity.Character> participants,
                                     GenerateDialogueRequest request) {
        StringBuilder prompt = new StringBuilder();

        // L5场景氛围（核心上下文）
        prompt.append("【场景氛围】（L5）\n");
        prompt.append(node.getSceneDescription()).append("\n");
        prompt.append("场景名：").append(node.getNodeName()).append("\n");
        prompt.append("剧情位置：第").append(node.getActIndex()).append("幕第")
                .append(node.getBeatIndex()).append("场\n\n");

        // L2 + L6角色信息（对话参与者）
        prompt.append("【对话角色】\n");
        for (com.bdu.plotassistant.entity.Character c : participants) {
            prompt.append("角色：").append(c.getName()).append("\n");
            prompt.append("  类型：").append(c.getRoleType()).append("\n");
            prompt.append("  人设：").append(c.getPersonaPrompt()).append("\n");
            if (c.getSpeechPattern() != null && !c.getSpeechPattern().isEmpty()) {
                prompt.append("  语言特征：").append(c.getSpeechPattern()).append("\n");
            }
            prompt.append("\n");
        }

        // 对话要求
        prompt.append("【对话要求】\n");
        if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            prompt.append(request.getPrompt()).append("\n");
        } else {
            prompt.append("生成一场能展现角色冲突或揭示关键信息的对话，包含2-3轮交锋。\n");
        }

        return prompt.toString();
    }

    /**
     * 保存对话内容（标记node_id）
     */
    private void saveDialogueContent(Long projectId, String nodeId, String dialogueJson,
                                     GenerateDialogueRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在，ID: " + projectId));

        GeneratedContent content = contentRepository
                .findByProjectIdAndContentTypeAndNodeId(projectId, "DIALOGUE", nodeId)
                .orElse(new GeneratedContent());

        content.setProject(project);
        content.setContentType("DIALOGUE");
        content.setNodeId(nodeId);  // 关键：标记属于哪个节点（L7）
        content.setContentJson(dialogueJson);

        try {
            content.setGenerationParams(objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            content.setGenerationParams("{}");
        }

        content.setAiModel("gpt-4");
        content.setTokenUsage(dialogueJson.length());
        content.setIsEdited(0);

        contentRepository.save(content);
    }

    /**
     * L9: 生成完整剧情
     */
    @Override
    public String generateWholeLine(Long projectId, GenerateWholeLineRequest request) {
        // 将在L8完成后实现
        throw new BizException("L9功能待L8完成后实现");
    }
    /**
     * 构建角色上下文
     */
    private String buildCharactersContext(Long projectId) {
        // 需要注入 CharacterRepository
        List<com.bdu.plotassistant.entity.Character> characters = characterRepository.findByProjectId(projectId);

        if (characters.isEmpty()) {
            return "暂无角色，请自由创造适合故事的角色。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("共").append(characters.size()).append("位角色：\n");

        for (com.bdu.plotassistant.entity.Character c : characters) {
            sb.append(String.format(
                    "- %s（%s）：%s\n",
                    c.getName(),
                    c.getRoleType(),  // PROTAGONIST/ANTAGONIST/NPC
                    c.getPersonaPrompt()  // 人设描述，可截断前100字避免过长
            ));
        }

        return sb.toString();
    }

    @Override
    public String generateCharacterSet(Long projectId, GenerateCharacterSetRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        String worldContext = buildWorldContext(projectId);

        // 获取现有大纲（如果存在）作为上下文
        Optional<GeneratedContent> existingOutline = contentRepository
                .findByProjectIdAndContentType(projectId, "OUTLINE");

        String outlineContext = existingOutline.map(GeneratedContent::getContentJson)
                .orElse("暂无大纲");

        String systemPrompt = "你是一位角色设计专家，基于世界观和大纲创造立体角色。" +
                "请严格按JSON格式输出角色数组，包含：id, name, roleType, personaPrompt, speechPattern, knowledgeScope";

        String userPrompt = String.format("世界观：\n%s\n\n现有大纲：\n%s\n\n生成要求：%s\n数量：%d个",
                worldContext, outlineContext, request.getPrompt(), request.getCount());

        // 添加日志
        System.out.println("Prompt 总长度: " + userPrompt.length() + " 字符");

        // 改为同步调用
        return aiClientService.syncCompletion(systemPrompt, userPrompt);
    }

    @Override
    public GeneratedContentDTO getByType(Long projectId, String contentType) {
        GeneratedContent content = contentRepository
                .findByProjectIdAndContentType(projectId, contentType)
                .orElseThrow(() -> new BizException("该类型内容尚未生成"));

        return convertToDTO(content);
    }

    @Override
    @Transactional
    public void save(Long projectId, String contentType, SaveContentRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 查询是否已存在（覆盖更新）
        GeneratedContent content = contentRepository
                .findByProjectIdAndContentType(projectId, contentType)
                .orElse(new GeneratedContent());

        content.setProject(project);
        content.setContentType(contentType);
        content.setContentJson(request.getContentJson());
        content.setAiModel("gpt-4"); // 实际从配置或请求中获取
        content.setTokenUsage(request.getContentJson().length()); // 简化统计
        content.setGenerationTimeMs(0); // 实际应记录生成耗时
        content.setIsEdited(0);

        contentRepository.save(content);
    }

    @Override
    public String regenerate(Long projectId, String contentType, RegenerateRequest request) {
        // 重新生成即再次调用生成逻辑，改为同步返回
        String type = contentType.toUpperCase();

        switch (type) {
            case "OUTLINE":
                // 构造 Outline 请求对象
                GenerateOutlineRequest outlineReq = new GenerateOutlineRequest();
                outlineReq.setPrompt(request.getPrompt());
                outlineReq.setDarkness(5); // 默认值，或从 request 获取
                outlineReq.setComplexity(3); // 默认值
                return generateOutline(projectId, outlineReq);

            case "CHARACTER_SET":
                GenerateCharacterSetRequest characterSetRequest = new GenerateCharacterSetRequest();
                characterSetRequest.setPrompt(request.getPrompt());
                characterSetRequest.setCount(3); // 默认数量
                return generateCharacterSet(projectId, characterSetRequest);

            case "DIALOGUE_SAMPLE":
                throw new BizException("对话样本需指定场景和角色，请使用对应接口");

            default:
                throw new BizException("未知内容类型: " + contentType);
        }
    }

    @Override
    public boolean existsByType(Long projectId, String contentType) {
        return contentRepository.existsByProjectIdAndContentType(projectId, contentType);
    }

    // ========== 私有辅助方法（保持不变）=========

    private String buildWorldContext(Long projectId) {
        try {
            var world = worldSettingService.getByProjectId(projectId);
            return String.format("题材：%s/%s，科技/魔法：%d/%d，时间：%s，地点：%s，核心冲突：%s",
                    world.getGenre(),
                    world.getSubGenre() != null ? world.getSubGenre() : "无",
                    world.getTechLevel(),
                    world.getMagicLevel(),
                    world.getTimeBackground() != null ? world.getTimeBackground() : "未设定",
                    world.getGeoBackground() != null ? world.getGeoBackground() : "未设定",
                    world.getCoreConflict() != null ? world.getCoreConflict() : "未设定"
            );
        } catch (Exception e) {
            return "世界观暂未设定";
        }
    }

    private GeneratedContentDTO convertToDTO(GeneratedContent content) {
        GeneratedContentDTO dto = new GeneratedContentDTO();
        dto.setContentType(content.getContentType());
        dto.setContentJson(content.getContentJson());
        dto.setAiModel(content.getAiModel());
        dto.setTokenUsage(content.getTokenUsage());
        dto.setGenerationTimeMs(content.getGenerationTimeMs());
        dto.setIsEdited(content.getIsEdited() == 1);
        dto.setUpdatedAt(ServiceUtil.formatDateTime(content.getUpdatedAt()));
        return dto;
    }
}
