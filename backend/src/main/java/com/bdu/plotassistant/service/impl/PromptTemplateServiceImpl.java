package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.request.template.*;
import com.bdu.plotassistant.dto.response.template.*;
import com.bdu.plotassistant.entity.PromptTemplate;
import com.bdu.plotassistant.repository.PromptTemplateRepository;
import com.bdu.plotassistant.service.PromptTemplateService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final PromptTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public PromptTemplateServiceImpl(PromptTemplateRepository templateRepository,
                                     ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Long create(CreateTemplateRequest request) {
        // 校验编码唯一性（版本1）
        if (templateRepository.existsByTemplateCodeAndVersion(request.getTemplateCode(), 1)) {
            throw new BizException("模板编码已存在: " + request.getTemplateCode());
        }

        PromptTemplate template = new PromptTemplate();
        template.setTemplateCode(request.getTemplateCode());
        template.setTemplateName(request.getTemplateName());
        template.setSystemPrompt(request.getSystemPrompt());
        template.setUserPromptTemplate(request.getUserPromptTemplate());
        template.setParamSchema(request.getParamSchema());
        template.setVersion(1);
        template.setIsActive(1); // 默认激活

        PromptTemplate saved = templateRepository.save(template);
        return saved.getId();
    }

    @Override
    public List<TemplateSummaryDTO> list(Boolean isActive) {
        List<PromptTemplate> templates;

        if (isActive != null) {
            // 查询指定状态的最新版本
            templates = templateRepository.findByIsActive(isActive ? 1 : 0);
        } else {
            templates = templateRepository.findAll();
        }

        // 去重：同一编码只取最新版本
        List<TemplateSummaryDTO> result = new ArrayList<>();
        List<String> seenCodes = new ArrayList<>();

        for (PromptTemplate template : templates) {
            if (!seenCodes.contains(template.getTemplateCode())) {
                seenCodes.add(template.getTemplateCode());
                result.add(convertToSummaryDTO(template));
            }
        }

        return result;
    }

    @Override
    public TemplateDetailDTO getByCode(String templateCode) {
        PromptTemplate template = templateRepository
                .findTopByTemplateCodeAndIsActiveOrderByVersionDesc(templateCode, 1)
                .orElseThrow(() -> new BizException("模板不存在或未激活: " + templateCode));

        return convertToDetailDTO(template);
    }

    @Override
    public TemplateDetailDTO getByCodeAndVersion(String templateCode, Integer version) {
        PromptTemplate template = templateRepository
                .findByTemplateCodeAndVersion(templateCode, version)
                .orElseThrow(() -> new BizException("模板版本不存在: " + templateCode + " v" + version));

        return convertToDetailDTO(template);
    }

    @Override
    @Transactional
    public void update(String templateCode, UpdateTemplateRequest request) {
        // 更新即创建新版本，而非修改旧版本
        CreateVersionRequest versionRequest = new CreateVersionRequest();
        versionRequest.setTemplateCode(templateCode);
        versionRequest.setTemplateName(request.getTemplateName());
        versionRequest.setSystemPrompt(request.getSystemPrompt());
        versionRequest.setUserPromptTemplate(request.getUserPromptTemplate());
        versionRequest.setParamSchema(request.getParamSchema());

        createNewVersion(templateCode, versionRequest);
    }

    @Override
    @Transactional
    public Integer createNewVersion(String templateCode, CreateVersionRequest request) {
        // 查询当前最大版本号
        Integer maxVersion = templateRepository.findMaxVersionByTemplateCode(templateCode)
                .orElse(0);

        Integer newVersion = maxVersion + 1;

        PromptTemplate template = new PromptTemplate();
        template.setTemplateCode(templateCode);
        template.setTemplateName(request.getTemplateName());
        template.setSystemPrompt(request.getSystemPrompt());
        template.setUserPromptTemplate(request.getUserPromptTemplate());
        template.setParamSchema(request.getParamSchema());
        template.setVersion(newVersion);
        template.setIsActive(0); // 新版本默认不激活，需手动激活

        templateRepository.save(template);
        return newVersion;
    }

    @Override
    @Transactional
    public void activateVersion(String templateCode, Integer version) {
        // 先取消该编码所有版本的激活状态
        templateRepository.deactivateAllByTemplateCode(templateCode);

        // 激活指定版本
        PromptTemplate template = templateRepository
                .findByTemplateCodeAndVersion(templateCode, version)
                .orElseThrow(() -> new BizException("模板版本不存在: " + templateCode + " v" + version));

        template.setIsActive(1);
        templateRepository.save(template);
    }

    @Override
    @Transactional
    public void deleteVersion(String templateCode, Integer version) {
        PromptTemplate template = templateRepository
                .findByTemplateCodeAndVersion(templateCode, version)
                .orElseThrow(() -> new BizException("模板版本不存在: " + templateCode + " v" + version));

        // 不能删除已激活的版本
        if (template.getIsActive() != null && template.getIsActive() == 1) {
            throw new BizException("不能删除已激活的版本，请先切换其他版本");
        }

        templateRepository.delete(template);
    }

    @Override
    public String preview(String templateCode, PreviewTemplateRequest request) {
        // 直接调用渲染方法即可，内部会获取模板
        return getRenderedTemplate(templateCode, request.getParams());
    }

    @Override
    public String getRenderedTemplate(String templateCode, Object params) {
        // 直接查询 Entity，而非调用返回 DTO 的 getByCode
        PromptTemplate template = templateRepository
                .findTopByTemplateCodeAndIsActiveOrderByVersionDesc(templateCode, 1)
                .orElseThrow(() -> new BizException("模板不存在或未激活: " + templateCode));

        String rendered = template.getUserPromptTemplate();

        // 简单占位符替换 {{key}} -> value
        if (params instanceof Map) {
            Map<String, Object> paramMap = (Map<String, Object>) params;
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                rendered = rendered.replace(placeholder, value);
            }
        }

        // 组合系统提示词和用户提示词
        if (!ServiceUtil.isEmpty(template.getSystemPrompt())) {
            return template.getSystemPrompt() + "\n\n" + rendered;
        }

        return rendered;
    }

    // ========== 私有转换方法 ==========

    private TemplateSummaryDTO convertToSummaryDTO(PromptTemplate template) {
        TemplateSummaryDTO dto = new TemplateSummaryDTO();
        dto.setTemplateCode(template.getTemplateCode());
        dto.setTemplateName(template.getTemplateName());
        dto.setIsActive(template.getIsActive() != null && template.getIsActive() == 1);
        return dto;
    }

    private TemplateDetailDTO convertToDetailDTO(PromptTemplate template) {
        TemplateDetailDTO dto = new TemplateDetailDTO();
        dto.setTemplateCode(template.getTemplateCode());
        dto.setTemplateName(template.getTemplateName());
        dto.setSystemPrompt(template.getSystemPrompt());
        dto.setUserPromptTemplate(template.getUserPromptTemplate());
        dto.setParamSchema(template.getParamSchema());
        dto.setVersion(template.getVersion());
        dto.setIsActive(template.getIsActive() != null && template.getIsActive() == 1);
        dto.setCreatedAt(ServiceUtil.formatDateTime(template.getCreatedAt()));
        dto.setUpdatedAt(ServiceUtil.formatDateTime(template.getUpdatedAt()));
        return dto;
    }
}
