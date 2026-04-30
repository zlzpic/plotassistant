package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.PageResult;
import com.bdu.plotassistant.dto.request.project.*;
import com.bdu.plotassistant.dto.response.project.*;
import com.bdu.plotassistant.service.ProjectService;
import lombok.var;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/project")
@Validated
public class ProjectController extends BaseController{

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/create")
    public ApiResult<Long> create(@RequestBody @Validated CreateProjectRequest req) {
        Long userId = getCurrentUserId();
        Long projectId = projectService.create(userId, req);
        return ApiResult.success(projectId);
    }

    @GetMapping("/list")
    public PageResult<ProjectSummaryDTO> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page-1, size);

        var result = projectService.list(userId, status, keyword, pageable);

        return PageResult.success(
                result.getContent(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize()
        );
    }

    @GetMapping("/{id}/detail")
    public ApiResult<ProjectDetailDTO> getDetail(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        // 权限校验在Service层
        ProjectDetailDTO detail = projectService.getDetail(id);
        return ApiResult.success(detail);
    }

    @PostMapping("/{id}/update")
    public ApiResult<Void> update(
            @PathVariable Long id,
            @RequestBody @Validated UpdateProjectRequest req) {

        Long userId = getCurrentUserId();
        projectService.checkOwnership(id, userId); // 提前校验或放Service
        projectService.update(id, req);
        return ApiResult.success(null);
    }

    @PostMapping("/{id}/delete")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        projectService.delete(id);
        return ApiResult.success(null);
    }

    @PostMapping("/{id}/complete")
    public ApiResult<Void> complete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        projectService.complete(id);
        return ApiResult.success(null);
    }

    @PostMapping("/{id}/export")
    public ApiResult<ExportResultDTO> export(
            @PathVariable Long id,
            @RequestBody @Validated ExportRequest req) {

        Long userId = getCurrentUserId();
        ExportResultDTO result = projectService.export(id, req);
        return ApiResult.success(result);
    }

}
