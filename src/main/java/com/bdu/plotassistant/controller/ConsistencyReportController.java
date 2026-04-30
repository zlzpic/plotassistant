package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.consistencyreport.*;
import com.bdu.plotassistant.dto.response.consistencyreport.*;
import com.bdu.plotassistant.service.ConsistencyReportService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project/{projectId}/consistency")
@Validated
public class ConsistencyReportController {

    private final ConsistencyReportService consistencyReportService;

    public ConsistencyReportController(ConsistencyReportService consistencyReportService) {
        this.consistencyReportService = consistencyReportService;
    }

    @PostMapping("/check")
    public ApiResult<Long> triggerCheck(
            @PathVariable Long projectId,
            @RequestBody @Validated TriggerCheckRequest req) {

        Long reportId = consistencyReportService.triggerCheck(projectId, req);
        return ApiResult.success(reportId);
    }

    @GetMapping("/latest")
    public ApiResult<ConsistencyReportDTO> getLatest(@PathVariable Long projectId) {
        ConsistencyReportDTO dto = consistencyReportService.getLatest(projectId);
        return ApiResult.success(dto);
    }

    @GetMapping("/history")
    public ApiResult<List<ReportHistoryDTO>> listHistory(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "10") Integer limit) {

        List<ReportHistoryDTO> list = consistencyReportService.listHistory(projectId, limit);
        return ApiResult.success(list);
    }

    @GetMapping("/{reportId}/detail")
    public ApiResult<ConsistencyReportDetailDTO> getDetail(
            @PathVariable Long projectId,
            @PathVariable Long reportId) {

        ConsistencyReportDetailDTO dto = consistencyReportService.getDetail(reportId);
        return ApiResult.success(dto);
    }

    @PostMapping("/{reportId}/resolve")
    public ApiResult<Void> markResolved(
            @PathVariable Long projectId,
            @PathVariable Long reportId,
            @RequestBody @Validated ResolveConflictRequest req) {

        consistencyReportService.markResolved(reportId, req);
        return ApiResult.success(null);
    }
}
