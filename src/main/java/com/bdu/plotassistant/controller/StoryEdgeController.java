package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.storyedge.*;
import com.bdu.plotassistant.dto.response.storyedge.*;
import com.bdu.plotassistant.service.StoryEdgeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/project/{projectId}/edge")
@Validated
public class StoryEdgeController {

    private final StoryEdgeService storyEdgeService;

    public StoryEdgeController(StoryEdgeService storyEdgeService) {
        this.storyEdgeService = storyEdgeService;
    }

    /*
    * L8:选项分支ai
    * */
    @PostMapping("/generate-suggestions")
    public ApiResult<List<EdgeSuggestionDTO>> generate(@PathVariable Long projectId,
                                                    @RequestBody GenerateEdgeRequest request) {
        // 每次调用都会追加新的边记录
        List<EdgeSuggestionDTO> suggestions = storyEdgeService.generateAndSaveSuggestions(projectId, request);
        return ApiResult.success(suggestions);
    }

    @PostMapping("/from-suggestion")
    public ApiResult<Long> saveFromSuggestion(
            @PathVariable Long projectId,
            @RequestBody @Validated SaveFromSuggestionRequest request) {

        // 采用单条建议创建边
        Long edgeId = storyEdgeService.saveFromSuggestion(projectId,
                request.getSourceId(), request.getTargetId(), request.getSuggestion());
        return ApiResult.success(edgeId);
    }

    @PostMapping("/create")
    public ApiResult<Long> create(
            @PathVariable Long projectId,
            @RequestBody @Validated CreateEdgeRequest req) {

        Long edgeId = storyEdgeService.create(projectId, req);
        return ApiResult.success(edgeId);
    }

    @GetMapping("/list")
    public ApiResult<List<EdgeDTO>> list(@PathVariable Long projectId) {
        List<EdgeDTO> list = storyEdgeService.listByProject(projectId);
        return ApiResult.success(list);
    }

    @GetMapping("/{edgeId}/detail")
    public ApiResult<EdgeDetailDTO> getDetail(
            @PathVariable Long projectId,
            @PathVariable Long edgeId) {

        EdgeDetailDTO detail = storyEdgeService.getDetail(edgeId);
        return ApiResult.success(detail);
    }

    @PostMapping("/{edgeId}/update")
    public ApiResult<Void> update(
            @PathVariable Long projectId,
            @PathVariable Long edgeId,
            @RequestBody @Validated UpdateEdgeRequest req) {

        storyEdgeService.update(edgeId, req);
        return ApiResult.success(null);
    }

    @PostMapping("/{edgeId}/delete")
    public ApiResult<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long edgeId) {

        storyEdgeService.delete(edgeId);
        return ApiResult.success(null);
    }

    @PostMapping("/generate-suggestion")
    public ApiResult<List<EdgeSuggestionDTO>> generateSuggestions(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateEdgeRequest req) {

        List<EdgeSuggestionDTO> suggestions = storyEdgeService.generateSuggestions(projectId, req);
        return ApiResult.success(suggestions);
    }

    @PostMapping("/batch-save")
    public ApiResult<Void> batchSave(
            @PathVariable Long projectId,
            @RequestBody @Validated BatchSaveEdgesRequest req) {

        storyEdgeService.batchSave(projectId, req);
        return ApiResult.success(null);
    }

    /**
     * 保存单条 AI 建议为边（用户从多个建议中选择一个）
     *
     * 请求示例：
     * POST /api/project/1/edges/from-suggestion
     * {
     *   "sourceId": "node-001",
     *   "targetId": "node-002",
     *   "suggestion": {
     *     "label": "威胁医生",
     *     "conditionExpr": "player.hasItem('gun')",
     *     "reason": "快速但高风险",
     *     "onSuccess": "医生颤抖着交出钥匙",
     *     "onFailure": "警报响起"
     *   }
     * }
     */
    /*@PostMapping("/from-suggestion")
    public ApiResult<Long> saveFromSuggestion(
            @PathVariable Long projectId,
            @RequestBody @Valid SaveFromSuggestionRequest request) {

        Long edgeId = storyEdgeService.saveFromSuggestion(
                projectId,
                request.getSourceId(),
                request.getTargetId(),
                request.getSuggestion()
        );

        return ApiResult.success(edgeId);
    }*/

    /**
     * 批量保存多条 AI 建议为边（将多个候选方案都保存为平行边）
     *
     * 请求示例：
     * POST /api/project/1/edges/from-suggestions
     * {
     *   "sourceId": "node-001",
     *   "targetId": "node-002",
     *   "suggestions": [
     *     {
     *       "label": "威胁医生",
     *       "conditionExpr": "player.disposition == 'aggressive'",
     *       "reason": "高风险"
     *     },
     *     {
     *       "label": "贿赂守卫",
     *       "conditionExpr": "player.credits >= 500",
     *       "reason": "稳妥"
     *     }
     *   ]
     * }
     */
    @PostMapping("/from-suggestions")
    public ApiResult<List<Long>> saveSuggestionsAsEdges(
            @PathVariable Long projectId,
            @RequestBody @Valid SaveSuggestionsBatchRequest request) {

        List<Long> edgeIds = storyEdgeService.saveSuggestionsAsEdges(
                projectId,
                request.getSourceId(),
                request.getTargetId(),
                request.getSuggestions()
        );

        return ApiResult.success(edgeIds);
    }
}
