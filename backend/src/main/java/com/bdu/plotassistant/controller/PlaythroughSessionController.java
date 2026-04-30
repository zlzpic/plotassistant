package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.playthrough.*;
import com.bdu.plotassistant.dto.response.playthrough.*;
import com.bdu.plotassistant.service.PlaythroughSessionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playthrough")
@Validated
public class PlaythroughSessionController extends BaseController{

    private final PlaythroughSessionService playthroughSessionService;

    public PlaythroughSessionController(PlaythroughSessionService playthroughSessionService) {
        this.playthroughSessionService = playthroughSessionService;
    }

    @PostMapping("/create")
    public ApiResult<String> create(@RequestBody @Validated CreatePlaythroughRequest req) {
        // 实际应从JWT获取userId，这里简化
        Long userId = getCurrentUserId();
        String sessionId = playthroughSessionService.create(userId, req);
        return ApiResult.success(sessionId);
    }

    @GetMapping("/list")
    public ApiResult<List<PlaythroughSummaryDTO>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String sessionType) {

        Long userId = getCurrentUserId();
        List<PlaythroughSummaryDTO> list = playthroughSessionService.list(userId, projectId, sessionType);
        return ApiResult.success(list);
    }

    @GetMapping("/{sessionId}/detail")
    public ApiResult<PlaythroughDetailDTO> getDetail(@PathVariable String sessionId) {
        PlaythroughDetailDTO detail = playthroughSessionService.getDetail(sessionId);
        return ApiResult.success(detail);
    }

    @PostMapping("/{sessionId}/delete")
    public ApiResult<Void> delete(@PathVariable String sessionId) {
        playthroughSessionService.delete(sessionId);
        return ApiResult.success(null);
    }

    @PostMapping("/{sessionId}/complete")
    public ApiResult<Void> complete(@PathVariable String sessionId) {
        playthroughSessionService.complete(sessionId);
        return ApiResult.success(null);
    }

    @PostMapping("/{sessionId}/abandon")
    public ApiResult<Void> abandon(@PathVariable String sessionId) {
        playthroughSessionService.abandon(sessionId);
        return ApiResult.success(null);
    }

    @PostMapping("/{sessionId}/jump")
    public ApiResult<Void> jump(
            @PathVariable String sessionId,
            @RequestBody @Validated JumpToNodeRequest req) {

        playthroughSessionService.jumpToNode(sessionId, req);
        return ApiResult.success(null);
    }

    @PostMapping("/{sessionId}/reset")
    public ApiResult<Void> reset(@PathVariable String sessionId) {
        playthroughSessionService.reset(sessionId);
        return ApiResult.success(null);
    }

    @PostMapping("/{sessionId}/convert-to-official")
    public ApiResult<String> convertToOfficial(@PathVariable String sessionId) {
        String newSessionId = playthroughSessionService.convertToOfficial(sessionId);
        return ApiResult.success(newSessionId);
    }

}
