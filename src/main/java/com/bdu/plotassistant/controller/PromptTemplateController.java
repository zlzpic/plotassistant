package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.template.*;
import com.bdu.plotassistant.dto.response.template.*;
import com.bdu.plotassistant.service.PromptTemplateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/template")
@Validated
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    public PromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @PostMapping("/create")
    public ApiResult<Long> create(@RequestBody @Validated CreateTemplateRequest req) {
        Long templateId = promptTemplateService.create(req);
        return ApiResult.success(templateId);
    }

    @GetMapping("/list")
    public ApiResult<List<TemplateSummaryDTO>> list(
            @RequestParam(required = false) Boolean isActive) {

        List<TemplateSummaryDTO> list = promptTemplateService.list(isActive);
        return ApiResult.success(list);
    }

    @GetMapping("/{templateCode}/detail")
    public ApiResult<TemplateDetailDTO> getDetail(@PathVariable String templateCode) {
        TemplateDetailDTO dto = promptTemplateService.getByCode(templateCode);
        return ApiResult.success(dto);
    }

    @GetMapping("/{templateCode}/{version}/detail")
    public ApiResult<TemplateDetailDTO> getSpecificVersion(
            @PathVariable String templateCode,
            @PathVariable Integer version) {

        TemplateDetailDTO dto = promptTemplateService.getByCodeAndVersion(templateCode, version);
        return ApiResult.success(dto);
    }

    @PostMapping("/{templateCode}/update")
    public ApiResult<Void> update(
            @PathVariable String templateCode,
            @RequestBody @Validated UpdateTemplateRequest req) {

        promptTemplateService.update(templateCode, req);
        return ApiResult.success(null);
    }

    @PostMapping("/{templateCode}/new-version")
    public ApiResult<Integer> createNewVersion(
            @PathVariable String templateCode,
            @RequestBody @Validated CreateVersionRequest req) {

        Integer version = promptTemplateService.createNewVersion(templateCode, req);
        return ApiResult.success(version);
    }

    @PostMapping("/{templateCode}/{version}/activate")
    public ApiResult<Void> activateVersion(
            @PathVariable String templateCode,
            @PathVariable Integer version) {

        promptTemplateService.activateVersion(templateCode, version);
        return ApiResult.success(null);
    }

    @PostMapping("/{templateCode}/{version}/delete")
    public ApiResult<Void> deleteVersion(
            @PathVariable String templateCode,
            @PathVariable Integer version) {

        promptTemplateService.deleteVersion(templateCode, version);
        return ApiResult.success(null);
    }

    @PostMapping("/{templateCode}/preview")
    public ApiResult<String> preview(
            @PathVariable String templateCode,
            @RequestBody @Validated PreviewTemplateRequest req) {

        String rendered = promptTemplateService.preview(templateCode, req);
        return ApiResult.success(rendered);
    }
}
