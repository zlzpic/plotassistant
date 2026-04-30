package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.storyscript.GenerateWholeLineRequest;
import com.bdu.plotassistant.dto.response.storyscript.StoryScriptDTO;
import com.bdu.plotassistant.entity.StoryScript;
import com.bdu.plotassistant.service.StoryScriptService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/project/{projectId}/scripts")
@Validated
public class StoryScriptController{

    private final StoryScriptService storyScriptService;

    public StoryScriptController(StoryScriptService storyScriptService) {
        this.storyScriptService = storyScriptService;
    }

    @PostMapping("/generate-whole")
    public ApiResult<String> generateWholeStory(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateWholeLineRequest request) {

        // L9: 基于L3骨架+L5场景+L7对话+L8边，按指定路径生成完整小说
        String storyContent = storyScriptService.generateWholeLine(projectId, request);
        return ApiResult.success(storyContent);
    }

    @PostMapping("/{scriptId}/mark-canon")
    public ApiResult<Void> markAsCanon(
            @PathVariable Long projectId,
            @PathVariable Long scriptId) {

        // 标记为正史（主线）
        storyScriptService.markAsCanon(scriptId);
        return ApiResult.success(null);
    }

    @GetMapping("/list")
    public ApiResult<List<StoryScriptDTO>> listScripts(@PathVariable Long projectId) {
        // 直接返回 Service 处理好的 DTO 列表
        return ApiResult.success(storyScriptService.listByProject(projectId));
    }

    @GetMapping("/{scriptId}")
    public ApiResult<StoryScriptDTO> getScriptDetail(
            @PathVariable Long projectId,
            @PathVariable Long scriptId) {

        // Service 层处理查询和权限校验
        StoryScriptDTO detail = storyScriptService.getScriptDetail(projectId, scriptId);
        return ApiResult.success(detail);
    }
}
