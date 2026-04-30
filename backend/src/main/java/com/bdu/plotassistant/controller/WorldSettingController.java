package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.worldsetting.*;
import com.bdu.plotassistant.dto.response.worldsetting.WorldSettingDTO;
import com.bdu.plotassistant.service.WorldSettingService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project/{projectId}/world")
@Validated
public class WorldSettingController {

    private final WorldSettingService worldSettingService;

    public WorldSettingController(WorldSettingService worldSettingService) {
        this.worldSettingService = worldSettingService;
    }

    @GetMapping("/detail")
    public ApiResult<WorldSettingDTO> getDetail(@PathVariable Long projectId) {
        WorldSettingDTO dto = worldSettingService.getByProjectId(projectId);
        return ApiResult.success(dto);
    }

    @PostMapping("/update")
    public ApiResult<Void> update(
            @PathVariable Long projectId,
            @RequestBody @Validated UpdateWorldSettingRequest req) {

        worldSettingService.update(projectId, req);
        return ApiResult.success(null);
    }
    /*
    * L1:世界观基础ai
    * */
    @PostMapping("/generate-description")
    public ApiResult<String> generateDescription(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateDescriptionRequest request) {

        // L1: 基于项目名+基础设定生成世界观描述
        String description = worldSettingService.generateDescription(projectId, request);
        return ApiResult.success(description);
    }
}
