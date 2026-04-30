package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.character.GenerateCharacterSetRequest;
import com.bdu.plotassistant.dto.request.generatedcontent.*;
import com.bdu.plotassistant.dto.response.generatedcontent.GeneratedContentDTO;
import com.bdu.plotassistant.service.GeneratedContentService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/project/{projectId}/generate")
@Validated
public class GeneratedContentController {

    private final GeneratedContentService generatedContentService;

    public GeneratedContentController(GeneratedContentService generatedContentService) {
        this.generatedContentService = generatedContentService;
    }

    /*
    *L3:故事大纲ai
    * */
    @PostMapping("/outline")
    public ApiResult<String> generateOutline(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateOutlineRequest request) {

        // L3: 基于L1+L2生成三幕式大纲(acts/beats结构)
        String outlineJson = generatedContentService.generateOutline(projectId, request);
        return ApiResult.success(outlineJson);
    }
    /*
    * L7:场景对话ai
    * */
    @PostMapping("/dialogue")
    public ApiResult<String> generateDialogue(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateDialogueRequest request) {

        // L7: 基于L2重要角色+L6 NPC+L5场景生成本节点对话
        String dialogueJson = generatedContentService.generateDialogueSample(projectId, request);
        return ApiResult.success(dialogueJson);
    }

    @PostMapping("/character-set")
    public ApiResult<String> generateCharacterSet(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateCharacterSetRequest req) {

        String content = generatedContentService.generateCharacterSet(projectId, req);
        return ApiResult.success(content);
    }

    @PostMapping("/dialogue-sample")
    public ApiResult<String> generateDialogueSample(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateDialogueRequest req) {

        String content = generatedContentService.generateDialogueSample(projectId, req);
        return ApiResult.success(content);
    }

    @GetMapping("/{contentType}/detail")
    public ApiResult<GeneratedContentDTO> getGeneratedContent(
            @PathVariable Long projectId,
            @PathVariable String contentType) {

        GeneratedContentDTO dto = generatedContentService.getByType(projectId, contentType);
        return ApiResult.success(dto);
    }

    @PostMapping("/{contentType}/save")
    public ApiResult<Void> saveGeneratedContent(
            @PathVariable Long projectId,
            @PathVariable String contentType,
            @RequestBody @Validated SaveContentRequest req) {

        generatedContentService.save(projectId, contentType, req);
        return ApiResult.success(null);
    }

    @PostMapping("/{contentType}/regenerate")
    public ApiResult<String> regenerateContent(
            @PathVariable Long projectId,
            @PathVariable String contentType,
            @RequestBody @Validated RegenerateRequest req) {

        String content = generatedContentService.regenerate(projectId, contentType, req);
        return ApiResult.success(content);
    }
}
