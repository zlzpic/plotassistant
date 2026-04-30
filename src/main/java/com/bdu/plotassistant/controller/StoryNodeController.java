package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.storynode.*;
import com.bdu.plotassistant.dto.response.storynode.*;
import com.bdu.plotassistant.service.StoryNodeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project/{projectId}/node")
@Validated
public class StoryNodeController {

    private final StoryNodeService storyNodeService;

    public StoryNodeController(StoryNodeService storyNodeService) {
        this.storyNodeService = storyNodeService;
    }

    @PostMapping("/create")
    public ApiResult<String> create(
            @PathVariable Long projectId,
            @RequestBody @Validated CreateNodeRequest req) {

        String nodeId = storyNodeService.create(projectId, req);
        return ApiResult.success(nodeId);
    }

    @GetMapping("/list")
    public ApiResult<List<NodeSummaryDTO>> list(@PathVariable Long projectId) {
        List<NodeSummaryDTO> list = storyNodeService.listByProject(projectId);
        return ApiResult.success(list);
    }

    @GetMapping("/{nodeId}/detail")
    public ApiResult<NodeDetailDTO> getDetail(
            @PathVariable Long projectId,
            @PathVariable String nodeId) {

        NodeDetailDTO detail = storyNodeService.getDetail(nodeId);
        return ApiResult.success(detail);
    }

    @PostMapping("/{nodeId}/update")
    public ApiResult<Void> update(
            @PathVariable Long projectId,
            @PathVariable String nodeId,
            @RequestBody @Validated UpdateNodeRequest req) {

        storyNodeService.update(nodeId, req);
        return ApiResult.success(null);
    }

    @PostMapping("/{nodeId}/delete")
    public ApiResult<Void> delete(
            @PathVariable Long projectId,
            @PathVariable String nodeId) {

        storyNodeService.delete(nodeId);
        return ApiResult.success(null);
    }

    @PostMapping("/{nodeId}/generate-description")
    public ApiResult<String> generateDescription(
            @PathVariable Long projectId,
            @PathVariable String nodeId,
            @RequestBody @Validated GenerateNodeDescRequest req) {

        String description = storyNodeService.generateDescription(nodeId, req);
        return ApiResult.success(description);
    }

    @PostMapping("/batch-save")
    public ApiResult<Void> batchSave(
            @PathVariable Long projectId,
            @RequestBody @Validated BatchSaveNodesRequest req) {

        storyNodeService.batchSave(projectId, req);
        return ApiResult.success(null);
    }
    /*
    * L4:批量生成节点ai
    * */
    @PostMapping("/generate-batch")
    public ApiResult<List<String>> generateNodes(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateNodesRequest request) {

        // L4: 基于L3大纲批量生成所有StoryNode(带act_index/beat_index)
        List<String> nodeIds = storyNodeService.generateNodes(projectId, request);
        return ApiResult.success(nodeIds);
    }
    /*
    * L5:节点场景描述ai
    * */
    @PostMapping("/{nodeId}/description")
    public ApiResult<String> generateNodeDescription(
            @PathVariable Long projectId,
            @PathVariable String nodeId,
            @RequestBody @Validated GenerateNodeDescRequest request) {

        // L5: 基于L1+L2+L3+L4生成单个节点的详细氛围描述
        String description = storyNodeService.generateNodeDescription(projectId, nodeId, request);
        return ApiResult.success(description);
    }
}
