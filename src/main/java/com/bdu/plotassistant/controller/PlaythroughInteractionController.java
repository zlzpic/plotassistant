package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.playthrough.*;
import com.bdu.plotassistant.dto.response.playthrough.*;
import com.bdu.plotassistant.service.PlaythroughInteractionService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/playthrough/{sessionId}")
@Validated
public class PlaythroughInteractionController {

    private final PlaythroughInteractionService playthroughInteractionService;

    public PlaythroughInteractionController(PlaythroughInteractionService playthroughInteractionService) {
        this.playthroughInteractionService = playthroughInteractionService;
    }

    @GetMapping("/state")
    public ApiResult<PlaythroughStateDTO> getState(@PathVariable String sessionId) {
        PlaythroughStateDTO state = playthroughInteractionService.getCurrentState(sessionId);
        return ApiResult.success(state);
    }

    @PostMapping(value = "/interact", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter interact(
            @PathVariable String sessionId,
            @RequestBody @Validated InteractRequest req) {

        return playthroughInteractionService.interact(sessionId, req);
    }

    @PostMapping("/gm-mode/enable")
    public ApiResult<Void> enableGMMode(
            @PathVariable String sessionId,
            @RequestBody @Validated GMModeRequest req) {

        playthroughInteractionService.enableGMMode(sessionId, req);
        return ApiResult.success(null);
    }

    @PostMapping("/gm-mode/disable")
    public ApiResult<Void> disableGMMode(@PathVariable String sessionId) {
        playthroughInteractionService.disableGMMode(sessionId);
        return ApiResult.success(null);
    }

    @GetMapping("/history")
    public ApiResult<List<InteractionLogDTO>> getHistory(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "50") Integer limit) {

        List<InteractionLogDTO> history = playthroughInteractionService.getHistory(sessionId, limit);
        return ApiResult.success(history);
    }

    @GetMapping("/history/{sequenceNum}/detail")
    public ApiResult<InteractionLogDetailDTO> getHistoryDetail(
            @PathVariable String sessionId,
            @PathVariable Integer sequenceNum) {

        InteractionLogDetailDTO detail = playthroughInteractionService.getSpecificLog(sessionId, sequenceNum);
        return ApiResult.success(detail);
    }

    @PostMapping("/save-point/create")
    public ApiResult<Long> createSavePoint(
            @PathVariable String sessionId,
            @RequestBody @Validated CreateSavePointRequest req) {

        Long savePointId = playthroughInteractionService.createSavePoint(sessionId, req);
        return ApiResult.success(savePointId);
    }

    @GetMapping("/save-points/list")
    public ApiResult<List<SavePointDTO>> listSavePoints(@PathVariable String sessionId) {
        List<SavePointDTO> list = playthroughInteractionService.listSavePoints(sessionId);
        return ApiResult.success(list);
    }

    @PostMapping("/restore")
    public ApiResult<Void> restore(
            @PathVariable String sessionId,
            @RequestBody @Validated RestoreRequest req) {

        playthroughInteractionService.restoreToSavePoint(sessionId, req);
        return ApiResult.success(null);
    }
}
