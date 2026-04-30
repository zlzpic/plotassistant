package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.request.character.*;
import com.bdu.plotassistant.dto.response.character.*;
import com.bdu.plotassistant.entity.Character;
import com.bdu.plotassistant.entity.Project;
import com.bdu.plotassistant.entity.StoryNode;
import com.bdu.plotassistant.entity.WorldSetting;
import com.bdu.plotassistant.repository.CharacterRepository;
import com.bdu.plotassistant.repository.ProjectRepository;
import com.bdu.plotassistant.repository.StoryNodeRepository;
import com.bdu.plotassistant.repository.WorldSettingRepository;
import com.bdu.plotassistant.service.AiClientService;
import com.bdu.plotassistant.service.CharacterService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CharacterServiceImpl implements CharacterService {

    private final CharacterRepository characterRepository;
    private final ProjectRepository projectRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;

    public CharacterServiceImpl(CharacterRepository characterRepository,
                                ProjectRepository projectRepository,
                                StoryNodeRepository storyNodeRepository, WorldSettingRepository worldSettingRepository, AiClientService aiClientService,
                                ObjectMapper objectMapper) {
        this.characterRepository = characterRepository;
        this.projectRepository = projectRepository;
        this.storyNodeRepository = storyNodeRepository;
        this.worldSettingRepository = worldSettingRepository;
        this.aiClientService = aiClientService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public String create(Long projectId, CreateCharacterRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 生成UUID作为主键
        String charId = UUID.randomUUID().toString().replace("-", "");

        Character character = new Character();
        character.setId(charId);
        character.setProject(project);
        character.setName(request.getName());
        character.setRoleType(request.getRoleType());
        character.setPersonaPrompt(request.getPersonaPrompt());
        character.setSpeechPattern(request.getSpeechPattern());

        // JSON序列化
        try {
            character.setKnowledgeScope(objectMapper.writeValueAsString(
                    request.getKnowledgeScope() != null ? request.getKnowledgeScope() : new ArrayList<>()
            ));
            character.setValidatedInsights(objectMapper.writeValueAsString(new ArrayList<>()));
        } catch (Exception e) {
            throw new BizException("知识范围序列化失败: " + e.getMessage());
        }

        Character saved = characterRepository.save(character);
        return saved.getId();
    }

    @Override
    public List<CharacterSummaryDTO> listByProject(Long projectId) {
        return characterRepository.findByProjectId(projectId).stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CharacterDetailDTO getDetail(String charId) {
        Character character = characterRepository.findById(charId)
                .orElseThrow(() -> new BizException("角色不存在: " + charId));

        return convertToDetailDTO(character);
    }

    @Override
    @Transactional
    public void update(String charId, UpdateCharacterRequest request) {
        Character character = characterRepository.findById(charId)
                .orElseThrow(() -> new BizException("角色不存在: " + charId));

        if (!ServiceUtil.isEmpty(request.getName())) {
            character.setName(request.getName());
        }
        if (request.getRoleType() != null) {
            character.setRoleType(request.getRoleType());
        }
        if (request.getPersonaPrompt() != null) {
            character.setPersonaPrompt(request.getPersonaPrompt());
        }
        if (request.getSpeechPattern() != null) {
            character.setSpeechPattern(request.getSpeechPattern());
        }
        if (request.getKnowledgeScope() != null) {
            try {
                character.setKnowledgeScope(objectMapper.writeValueAsString(request.getKnowledgeScope()));
            } catch (Exception e) {
                throw new BizException("知识范围序列化失败: " + e.getMessage());
            }
        }

        characterRepository.save(character);
    }

    @Override
    @Transactional
    public void delete(String charId) {
        if (!characterRepository.existsById(charId)) {
            throw new BizException("角色不存在: " + charId);
        }
        characterRepository.deleteById(charId);
    }

    /**
     * L2: 生成重要角色（status=1）
     * 依赖L1: 需要WorldSetting.description（世界观描述）
     */
    @Override
    @Transactional
    public List<String> generateBatch(Long projectId, GenerateCharacterSetRequest request) {
        // 1. 校验项目存在
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 【关键修复】校验 count 参数
        if (request.getCount() == null || request.getCount() <= 0) {
            throw new BizException("生成数量必须大于0");
        }

        // 2. 获取L1数据...
        WorldSetting worldSetting = worldSettingRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BizException("L1世界观未生成，请先调用generateDescription"));

        if (worldSetting.getDescription() == null || worldSetting.getDescription().isEmpty()) {
            throw new BizException("L1世界观描述为空，请先生成世界观描述");
        }

        // 3. 构建L2级提示词...
        String systemPrompt = buildL2SystemPrompt();
        String userPrompt = buildL2UserPrompt(project, worldSetting, request);

        // 4. 调用AI生成角色JSON
        String aiResponse = aiClientService.syncCompletion(systemPrompt, userPrompt);
        System.out.println("L2 AI响应: " + aiResponse);

        // 5. 解析并保存【不传 count，让方法自己判断实际返回数量】
        return parseAndSaveCharacters(aiResponse, project);
    }

    /**
     * 构建L2系统提示词
     */
    private String buildL2SystemPrompt() {
        return "你是一位专业的角色设计师，擅长基于世界观创造有深度、有矛盾、有独特语言风格的角色。\n" +
                "\n" +
                "【任务】\n" +
                "基于提供的世界观描述，生成指定数量的重要角色（主角、反派或关键配角）。\n" +
                "\n" +
                "【角色设计要求】\n" +
                "1. 角色必须与L1世界观高度契合（科技/魔法水平、社会结构、核心冲突）\n" +
                "2. 角色必须有内在矛盾（如：医生却从事黑市交易、警察却包庇罪犯）\n" +
                "3. 角色类型必须多样：至少包含1个主角、1个反派、若干关键NPC\n" +
                "4. 语言风格必须符合世界观基调\n" +
                "\n" +
                "【输出格式】\n" +
                "必须严格返回JSON格式，不要Markdown代码块标记：\n" +
                "{\n" +
                "  \"characters\": [\n" +
                "    {\n" +
                "      \"name\": \"角色姓名（2-4字中文或符合题材的外文名）\",\n" +
                "      \"roleType\": \"PROTAGONIST(主角)/ANTAGONIST(反派)/SUPPORT(重要配角)\",\n" +
                "      \"personaPrompt\": \"人格描述（200字以内）：背景、动机、矛盾、性格特征\",\n" +
                "      \"speechPattern\": \"语言特征（50字以内）：口头禅、用词习惯、语气\",\n" +
                "      \"knowledgeScope\": [\"知晓的关键信息1\", \"关键信息2\", \"最多5条\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /**
     * 构建L2用户提示词（L1集：世界观描述 + 用户要求）
     */
    private String buildL2UserPrompt(Project project, WorldSetting worldSetting,
                                     GenerateCharacterSetRequest request) {
        StringBuilder prompt = new StringBuilder();

        // L1集核心：世界观描述（从L1获取的详细描述）
        prompt.append("【世界观氛围描述】（L1生成）\n");
        prompt.append(worldSetting.getDescription()).append("\n\n");

        // L1基础设定（作为补充上下文）
        prompt.append("【世界观基础】\n");
        prompt.append("题材：").append(worldSetting.getGenre());
        if (worldSetting.getSubGenre() != null) {
            prompt.append("/").append(worldSetting.getSubGenre());
        }
        prompt.append("\n");
        prompt.append("科技/魔法水平：").append(worldSetting.getTechLevel())
                .append("/").append(worldSetting.getMagicLevel()).append("\n");
        if (worldSetting.getCoreConflict() != null) {
            prompt.append("核心冲突：").append(worldSetting.getCoreConflict()).append("\n");
        }

        // 生成要求
        prompt.append("\n【生成要求】\n");
        prompt.append("生成数量：").append(request.getCount()).append("个重要角色\n");

        if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            prompt.append("特殊要求：").append(request.getPrompt()).append("\n");
        } else {
            prompt.append("要求：角色必须深度参与世界观核心冲突，具有鲜明个性和不可替代性\n");
        }

        return prompt.toString();
    }

    /**
     * 解析AI响应并保存角色（status=1，重要角色）
     */
    private List<String> parseAndSaveCharacters(String aiResponse, Project project, int expectedCount) {
        List<String> characterIds = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode charactersNode = root.path("characters");

            if (!charactersNode.isArray()) {
                throw new BizException("AI返回格式错误：缺少characters数组");
            }

            for (int i = 0; i < charactersNode.size() && i < expectedCount; i++) {
                JsonNode charNode = charactersNode.get(i);

                // 生成UUID
                String charId = UUID.randomUUID().toString().replace("-", "");

                Character character = new Character();
                character.setId(charId);
                character.setProject(project);
                character.setName(charNode.path("name").asText("未命名角色" + (i+1)));
                character.setRoleType(charNode.path("roleType").asText("NPC"));
                character.setPersonaPrompt(charNode.path("personaPrompt").asText(""));
                character.setSpeechPattern(charNode.path("speechPattern").asText(""));

                // 序列化knowledgeScope数组
                JsonNode knowledgeNode = charNode.path("knowledgeScope");
                List<String> knowledgeList = new ArrayList<>();
                if (knowledgeNode.isArray()) {
                    knowledgeNode.forEach(node -> knowledgeList.add(node.asText()));
                }
                character.setKnowledgeScope(objectMapper.writeValueAsString(knowledgeList));
                character.setValidatedInsights(objectMapper.writeValueAsString(new ArrayList<>()));

                // 关键：标记为重要角色（status=1）
                character.setStatus(1);

                characterRepository.save(character);
                characterIds.add(charId);
            }

        } catch (Exception e) {
            throw new BizException("解析AI生成的角色数据失败: " + e.getMessage());
        }

        return characterIds;
    }

    // ========== L6方法占位（后续实现） ==========

    /**
     * L6: 生成NPC（次要角色，status=2）
     * 依赖L5: 需要节点氛围描述
     */
    /**
     * L6: 生成NPC（次要角色，status=2，绑定到特定节点）
     * 依赖L5: 节点详细氛围描述（sceneDescription）
     */
    @Override
    @Transactional
    public List<String> generateNPCBatch(Long projectId, String nodeId, GenerateNPCRequest request) {
        // 1. 校验节点存在
        StoryNode node = storyNodeRepository.findById(nodeId)
                .orElseThrow(() -> new BizException("节点不存在"));

        if (!node.getProject().getId().equals(projectId)) {
            throw new BizException("节点不属于该项目");
        }

        // 2. 检查L5是否已生成（需要详细的场景描述）
        if (node.getSceneDescription() == null || node.getSceneDescription().isEmpty()) {
            throw new BizException("L5节点描述未生成，请先生成场景氛围");
        }

        // 3. 获取L1摘要（世界观）
        String l1Summary = buildL1SummaryForNPC(projectId);

        // 4. 获取L2重要角色名单（用于排除，避免NPC和主角重名或人设重复）
        String excludeNames = buildImportantCharacterNames(projectId);

        // 5. 构建L6提示词
        String systemPrompt = buildL6SystemPrompt();
        String userPrompt = buildL6UserPrompt(l1Summary, node, excludeNames, request);

        // 6. 调用AI生成NPC
        String aiResponse = aiClientService.syncCompletion(systemPrompt, userPrompt);
        System.out.println("L6 AI响应: " + aiResponse);

        // 7. 解析并保存（status=2，标记为NPC）
        return parseAndSaveNPCs(aiResponse, projectId, nodeId, request.getCount());
    }

    /**
     * 构建L1摘要（用于NPC生成，精简版）
     */
    private String buildL1SummaryForNPC(Long projectId) {
        WorldSetting setting = worldSettingRepository.findByProjectId(projectId)
                .orElse(null);
        if (setting == null) return "";

        return String.format("世界观：%s/%s，%s",
                setting.getGenre(),
                setting.getSubGenre() != null ? setting.getSubGenre() : "",
                setting.getDescription() != null ? setting.getDescription().substring(0, Math.min(50, setting.getDescription().length())) : ""
        );
    }

    /**
     * 构建重要角色名单（排除列表，避免NPC重复）
     */
    private String buildImportantCharacterNames(Long projectId) {
        List<Character> importantChars = characterRepository.findByProjectIdAndStatus(projectId, 1);
        if (importantChars.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("已有重要角色（不可重复）：");
        for (Character c : importantChars) {
            sb.append(c.getName()).append("(").append(c.getRoleType()).append("), ");
        }
        return sb.toString();
    }

    /**
     * 构建L6系统提示词
     */
    private String buildL6SystemPrompt() {
        return "你是一位NPC设计师，擅长设计鲜活的路人、商贩、守卫、接待员等次要角色。\n" +
                "\n" +
                "【任务】\n" +
                "基于场景氛围，生成适合该环境的NPC（非主角的次要人物）。\n" +
                "\n" +
                "【NPC设计要求】\n" +
                "1. 必须是次要角色：店主、路人、守卫、接待员、流浪者等，不是剧情主角\n" +
                "2. 必须与场景氛围契合：压抑场景生成阴郁NPC，热闹场景生成活泼NPC\n" +
                "3. 必须与世界观一致：赛博朋克有义体改造，奇幻有种族特征\n" +
                "4. 必须有职业/身份功能：这些NPC要能提供信息、交易、任务或阻碍\n" +
                "5. 禁止与重要角色重名或人设重复\n" +
                "\n" +
                "【输出格式】\n" +
                "必须严格返回JSON格式：\n" +
                "{\n" +
                "  \"npcs\": [\n" +
                "    {\n" +
                "      \"name\": \"NPC姓名（简洁，符合世界观）\",\n" +
                "      \"roleType\": \"NPC\",\n" +
                "      \"identity\": \"身份/职业（如：黑市商人、诊所保安、数据贩子）\",\n" +
                "      \"personaPrompt\": \"人设描述（100字以内）：外貌、性格、与场景的关系\",\n" +
                "      \"speechPattern\": \"语言特征（30字以内）：口头禅、口音、说话方式\",\n" +
                "      \"function\": \"功能：此NPC能提供什么（情报/商品/任务/障碍）\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /**
     * 构建L6用户提示词（L1 + L5 + 排除名单）
     */
    private String buildL6UserPrompt(String l1Summary, StoryNode node,
                                     String excludeNames, GenerateNPCRequest request) {
        StringBuilder prompt = new StringBuilder();

        // L1世界观（简要）
        prompt.append("【世界观】").append(l1Summary).append("\n\n");

        // L5节点描述（核心上下文）
        prompt.append("【场景氛围】（L5生成）\n");
        prompt.append(node.getSceneDescription()).append("\n\n");

        // 排除名单（避免重复）
        if (!excludeNames.isEmpty()) {
            prompt.append("【排除名单】").append(excludeNames).append("\n");
            prompt.append("注意：NPC不能与上述重要角色重名，也不能是主角的克隆人设。\n\n");
        }

        // 生成要求
        prompt.append("【生成要求】\n");
        prompt.append("生成数量：").append(request.getCount()).append("个NPC\n");
        prompt.append("场景：").append(node.getNodeName()).append("\n");

        if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            prompt.append("特殊要求：").append(request.getPrompt()).append("\n");
        } else {
            prompt.append("要求：这些NPC要能让场景活起来，提供探索价值或剧情线索。\n");
        }

        return prompt.toString();
    }

    /**
     * 解析并保存NPC（status=2）
     */
    private List<String> parseAndSaveNPCs(String aiResponse, Long projectId,
                                          String nodeId, int expectedCount) {
        List<String> npcIds = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode npcsNode = root.path("npcs");

            if (!npcsNode.isArray()) {
                throw new BizException("AI返回格式错误：缺少npcs数组");
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new BizException("项目不存在，ID: " + projectId));

            for (int i = 0; i < npcsNode.size() && i < expectedCount; i++) {
                JsonNode npcNode = npcsNode.get(i);

                String npcId = UUID.randomUUID().toString().replace("-", "");

                Character npc = new Character();
                npc.setId(npcId);
                npc.setProject(project);
                npc.setName(npcNode.path("name").asText("NPC" + (i+1)));
                npc.setRoleType("NPC");  // 统一标记为NPC

                // 组合人设：identity + personaPrompt
                String identity = npcNode.path("identity").asText("");
                String persona = npcNode.path("personaPrompt").asText("");
                npc.setPersonaPrompt(identity + "：" + persona);

                npc.setSpeechPattern(npcNode.path("speechPattern").asText(""));

                // function存到knowledgeScope或单独字段（这里用knowledgeScope存功能描述）
                List<String> knowledge = new ArrayList<>();
                knowledge.add(npcNode.path("function").asText("提供信息"));
                npc.setKnowledgeScope(objectMapper.writeValueAsString(knowledge));

                npc.setValidatedInsights(objectMapper.writeValueAsString(new ArrayList<>()));

                // 关键：标记为NPC（status=2）
                npc.setStatus(2);

                characterRepository.save(npc);
                npcIds.add(npcId);
            }

        } catch (Exception e) {
            throw new BizException("解析NPC数据失败: " + e.getMessage());
        }

        return npcIds;
    }

    /**
     * 获取重要角色（L2生成，status=1）
     * 用于L3及更高层构建上下文
     */


    @Override
    @Transactional
    public void addValidatedInsight(String charId, AddInsightRequest request) {
        Character character = characterRepository.findById(charId)
                .orElseThrow(() -> new BizException("角色不存在: " + charId));

        List<String> insights = new ArrayList<>();
        try {
            // 反序列化现有洞察
            if (character.getValidatedInsights() != null) {
                insights = objectMapper.readValue(character.getValidatedInsights(),
                        new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            // 如果解析失败，重置为空列表
            insights = new ArrayList<>();
        }

        // 添加新洞察
        insights.add(request.getInsight());

        // 序列化保存
        try {
            character.setValidatedInsights(objectMapper.writeValueAsString(insights));
        } catch (Exception e) {
            throw new BizException("洞察序列化失败: " + e.getMessage());
        }

        characterRepository.save(character);
    }

    @Override
    public List<CharacterDTO> getByIds(List<String> charIds) {
        return characterRepository.findAllById(charIds).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ========== 私有转换方法 ==========

    private CharacterSummaryDTO convertToSummaryDTO(Character character) {
        CharacterSummaryDTO dto = new CharacterSummaryDTO();
        dto.setId(character.getId());
        dto.setName(character.getName());
        dto.setRoleType(character.getRoleType());
        dto.setRoleDescription(character.getPersonaPrompt());

        return dto;
    }

    private CharacterDetailDTO convertToDetailDTO(Character character) {
        CharacterDetailDTO dto = new CharacterDetailDTO();
        dto.setId(character.getId());
        dto.setName(character.getName());
        dto.setRoleType(character.getRoleType());
        dto.setPersonaPrompt(character.getPersonaPrompt());
        dto.setSpeechPattern(character.getSpeechPattern());

        // JSON反序列化
        try {
            if (character.getKnowledgeScope() != null) {
                dto.setKnowledgeScope(objectMapper.readValue(character.getKnowledgeScope(),
                        new TypeReference<List<String>>() {}));
            } else {
                dto.setKnowledgeScope(new ArrayList<>());
            }

            if (character.getValidatedInsights() != null) {
                dto.setValidatedInsights(objectMapper.readValue(character.getValidatedInsights(),
                        new TypeReference<List<String>>() {}));
            } else {
                dto.setValidatedInsights(new ArrayList<>());
            }
        } catch (Exception e) {
            dto.setKnowledgeScope(new ArrayList<>());
            dto.setValidatedInsights(new ArrayList<>());
        }

        dto.setCreatedAt(ServiceUtil.formatDateTime(character.getCreatedAt()));
        dto.setUpdatedAt(ServiceUtil.formatDateTime(character.getUpdatedAt()));
        return dto;
    }

    private CharacterDTO convertToDTO(Character character) {
        CharacterDTO dto = new CharacterDTO();
        dto.setId(character.getId());
        dto.setName(character.getName());
        dto.setRoleType(character.getRoleType());
        dto.setPersonaPrompt(character.getPersonaPrompt());
        return dto;
    }

    /**
     * 解析AI返回的JSON并批量保存角色
     */
    private List<String> parseAndSaveCharacters(String jsonResult, Project project) {
        try {
            // 解析最外层结构
            JsonNode root = objectMapper.readTree(jsonResult);
            JsonNode charactersNode = root.path("characters");

            if (!charactersNode.isArray()) {
                throw new BizException("AI返回格式错误：缺少characters数组");
            }

            List<Character> charactersToSave = new ArrayList<>();

            for (JsonNode charNode : charactersNode) {
                // 生成UUID作为主键
                String charId = UUID.randomUUID().toString().replace("-", "");

                Character character = new Character();
                character.setId(charId);
                character.setProject(project);
                character.setName(charNode.path("name").asText());

                // 获取角色类型并设置
                String roleType = charNode.path("roleType").asText();
                character.setRoleType(roleType);

                // 根据roleType判断status：1为重要角色，2为NPC
                int status = determineStatusByRoleType(roleType);
                character.setStatus(status);

                character.setPersonaPrompt(charNode.path("personaPrompt").asText());
                character.setSpeechPattern(charNode.path("speechPattern").asText());

                // 处理 knowledgeScope 数组
                JsonNode knowledgeNode = charNode.path("knowledgeScope");
                List<String> knowledgeList = new ArrayList<>();
                if (knowledgeNode.isArray()) {
                    knowledgeNode.forEach(node -> knowledgeList.add(node.asText()));
                }

                // 序列化为JSON存储
                character.setKnowledgeScope(objectMapper.writeValueAsString(knowledgeList));
                character.setValidatedInsights(objectMapper.writeValueAsString(new ArrayList<>()));

                charactersToSave.add(character);
            }

            // 批量保存
            List<Character> saved = characterRepository.saveAll(charactersToSave);

            // 返回保存的角色ID列表
            return saved.stream()
                    .map(Character::getId)
                    .collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            throw new BizException("解析AI生成的角色数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据角色类型确定状态值
     * status: 1=重要角色(PROTAGONIST/SUPPORT/ANTAGONIST), 2=NPC配角, 0=不启用/软删除
     */
    private int determineStatusByRoleType(String roleType) {
        if (roleType == null || roleType.trim().isEmpty()) {
            return 2; // 默认按NPC处理
        }

        String upperRoleType = roleType.toUpperCase();

        // L2重要角色：主角、重要配角、反派
        if ("PROTAGONIST".equals(upperRoleType) ||
                "SUPPORT".equals(upperRoleType) ||
                "ANTAGONIST".equals(upperRoleType)) {
            return 1;
        }

        // NPC配角
        if ("NPC".equals(upperRoleType)) {
            return 2;
        }

        // 其他未知类型默认设为NPC
        return 2;
    }
    @Override
    public List<CharacterProfileDTO> getImportantCharacters(Long projectId) {
        List<Character> characters = characterRepository.findByProjectIdAndStatus(projectId, 1);
        List<CharacterProfileDTO> dtos = new ArrayList<>();

        for (Character c : characters) {
            CharacterProfileDTO dto = new CharacterProfileDTO();
            dto.setId(c.getId());
            dto.setName(c.getName());
            dto.setRoleType(c.getRoleType());
            dto.setPersonaPrompt(c.getPersonaPrompt());
            dto.setSpeechPattern(c.getSpeechPattern());
            dto.setKnowledgeScope(c.getKnowledgeScope());
            dto.setStatus(c.getStatus());
            dtos.add(dto);
        }
        return dtos;
    }
}
