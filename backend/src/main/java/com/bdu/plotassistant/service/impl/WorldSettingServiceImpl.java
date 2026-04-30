package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.request.worldsetting.*;
import com.bdu.plotassistant.dto.response.worldsetting.WorldSettingDTO;
import com.bdu.plotassistant.entity.Project;
import com.bdu.plotassistant.entity.WorldSetting;
import com.bdu.plotassistant.repository.ProjectRepository;
import com.bdu.plotassistant.repository.WorldSettingRepository;
import com.bdu.plotassistant.service.AiClientService;
import com.bdu.plotassistant.service.WorldSettingService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class WorldSettingServiceImpl implements WorldSettingService {

    private final WorldSettingRepository worldSettingRepository;
    private final ProjectRepository projectRepository;
    private final AiClientService aiClientService;

    public WorldSettingServiceImpl(WorldSettingRepository worldSettingRepository,
                                   ProjectRepository projectRepository,
                                   AiClientService aiClientService) {
        this.worldSettingRepository = worldSettingRepository;
        this.projectRepository = projectRepository;
        this.aiClientService = aiClientService;
    }

    @Override
    public WorldSettingDTO getByProjectId(Long projectId) {
        WorldSetting setting = worldSettingRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BizException("世界观设定不存在，项目ID: " + projectId));

        return convertToDTO(setting);
    }

    @Override
    @Transactional
    public void update(Long projectId, UpdateWorldSettingRequest request) {
        WorldSetting setting = worldSettingRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BizException("世界观设定不存在"));

        // 全量更新（允许部分字段为null，但保留原值需前端处理）
        // 此处简化：非空则更新
        if (!ServiceUtil.isEmpty(request.getGenre())) {
            setting.setGenre(request.getGenre());
        }
        if (request.getSubGenre() != null) {
            setting.setSubGenre(request.getSubGenre());
        }
        if (request.getTechLevel() != null) {
            setting.setTechLevel(request.getTechLevel());
        }
        if (request.getMagicLevel() != null) {
            setting.setMagicLevel(request.getMagicLevel());
        }
        if (request.getTimeBackground() != null) {
            setting.setTimeBackground(request.getTimeBackground());
        }
        if (request.getGeoBackground() != null) {
            setting.setGeoBackground(request.getGeoBackground());
        }
        if (request.getCoreConflict() != null) {
            setting.setCoreConflict(request.getCoreConflict());
        }
        if (request.getSpecialRules() != null) {
            setting.setSpecialRules(request.getSpecialRules());
        }

        worldSettingRepository.save(setting);
    }

    @Override
    @Transactional
    public String generateDescription(Long projectId, GenerateDescriptionRequest request) {
        // 1. 获取项目信息（项目名是L1的一部分）
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 2. 获取世界观设定（L1基础数据）
        WorldSetting setting = worldSettingRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BizException("世界观设定不存在，请先创建基础设定"));

        // 3. 构建L1级提示词（包含所有世界观基础元素）
        String systemPrompt = buildL1SystemPrompt();
        String userPrompt = buildL1UserPrompt(project, setting, request);

        // 4. 调用AI生成描述
        String description = aiClientService.syncCompletion(systemPrompt, userPrompt);
        System.out.println(description);

        // 5. 保存到数据库（L1结果持久化，供L2使用）
        setting.setDescription(description);
        worldSettingRepository.save(setting);

        return description;
    }

    /**
     * 构建L1级系统提示词（定义AI角色和输出格式）
     */
    private String buildL1SystemPrompt() {
        return "你是一位资深的世界观架构师，擅长将零散的世界观设定转化为沉浸式的场景描述。\n" +
                "\n" +
                "【任务】\n" +
                "基于提供的项目名、题材、背景设定等信息，生成一段300-500字的世界观氛围描述。\n" +
                "\n" +
                "【描述要求】\n" +
                "1. 必须包含感官细节：视觉（色彩、光影）、听觉（声音、音调）、嗅觉（气味、空气质量）\n" +
                "2. 必须体现核心冲突的氛围（如压抑、紧张、神秘、荒凉）\n" +
                "3. 必须融入科技/魔法水平的具象表现（不要抽象数字，要具体场景）\n" +
                "4. 语言风格要符合题材（赛博朋克用冷硬词汇，奇幻用诗意词汇）\n" +
                "\n" +
                "【输出格式】\n" +
                "直接返回描述文本，不要JSON，不要Markdown，不要分点说明。要求连贯的散文式描述。";
    }

    /**
     * 构建L1级用户提示词（组装L1集所有元素）
     */
    private String buildL1UserPrompt(Project project, WorldSetting setting, GenerateDescriptionRequest request) {
        StringBuilder prompt = new StringBuilder();

        // L1集：基础世界观元素
        prompt.append("【项目名】").append(project.getName()).append("\n");
        prompt.append("【题材】").append(setting.getGenre());
        if (setting.getSubGenre() != null && !setting.getSubGenre().isEmpty()) {
            prompt.append("/").append(setting.getSubGenre());
        }
        prompt.append("\n");

        // 具象化科技/魔法水平（将0-10数字转为描述词）
        prompt.append("【文明程度】科技水平:").append(setting.getTechLevel()).append("/10");
        prompt.append("（").append(getTechLevelDesc(setting.getTechLevel())).append("）");
        prompt.append(", 魔法水平:").append(setting.getMagicLevel()).append("/10");
        prompt.append("（").append(getMagicLevelDesc(setting.getMagicLevel())).append("）\n");

        // 时空背景
        if (setting.getTimeBackground() != null) {
            prompt.append("【时间背景】").append(setting.getTimeBackground()).append("\n");
        }
        if (setting.getGeoBackground() != null) {
            prompt.append("【地理背景】").append(setting.getGeoBackground()).append("\n");
        }

        // 核心冲突（L1的关键）
        if (setting.getCoreConflict() != null) {
            prompt.append("【核心冲突】").append(setting.getCoreConflict()).append("\n");
        }

        // 特殊规则（如果有）
        if (setting.getSpecialRules() != null && !setting.getSpecialRules().isEmpty()) {
            prompt.append("【特殊规则】").append(setting.getSpecialRules()).append("\n");
        }

        // 用户额外要求（优先级最高）
        if (request != null && request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            prompt.append("\n【特殊要求】").append(request.getPrompt());
        }

        return prompt.toString();
    }

    /**
     * 科技水平描述词（0-10映射为具象描述）
     */
    private String getTechLevelDesc(int level) {
        if (level <= 2) return "原始/部落";
        if (level <= 4) return "中世纪/蒸汽";
        if (level <= 6) return "现代/信息";
        if (level <= 8) return "近未来/赛博";
        return "远未来/星际";
    }

    /**
     * 魔法水平描述词（0-10映射为具象描述）
     */
    private String getMagicLevelDesc(int level) {
        if (level <= 2) return "无魔/传说";
        if (level <= 4) return "低魔/罕见";
        if (level <= 6) return "中魔/学院";
        if (level <= 8) return "高魔/普及";
        return "超魔/神性";
    }

    @Override
    @Transactional
    public WorldSettingDTO getOrCreateDefault(Long projectId) {
        return worldSettingRepository.findByProjectId(projectId)
                .map(this::convertToDTO)
                .orElseGet(() -> createDefault(projectId));
    }

    // ========== 私有方法 ==========

    private WorldSettingDTO createDefault(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在，ID: " + projectId));

        WorldSetting setting = new WorldSetting();
        setting.setProject(project);
        setting.setGenre("未设定");
        setting.setSubGenre(null);
        setting.setTechLevel(0);
        setting.setMagicLevel(0);
        setting.setTimeBackground(null);
        setting.setGeoBackground(null);
        setting.setCoreConflict(null);
        setting.setSpecialRules(null);

        WorldSetting saved = worldSettingRepository.save(setting);
        return convertToDTO(saved);
    }

    private WorldSettingDTO convertToDTO(WorldSetting setting) {
        WorldSettingDTO dto = new WorldSettingDTO();
        dto.setGenre(setting.getGenre());
        dto.setSubGenre(setting.getSubGenre());
        dto.setTechLevel(setting.getTechLevel());
        dto.setMagicLevel(setting.getMagicLevel());
        dto.setTimeBackground(setting.getTimeBackground());
        dto.setGeoBackground(setting.getGeoBackground());
        dto.setCoreConflict(setting.getCoreConflict());
        dto.setSpecialRules(setting.getSpecialRules());
        dto.setDescription(setting.getDescription());
        dto.setUpdatedAt(ServiceUtil.formatDateTime(setting.getUpdatedAt()));
        return dto;
    }
}
