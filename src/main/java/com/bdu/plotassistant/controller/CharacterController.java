package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.character.*;
import com.bdu.plotassistant.dto.response.character.*;
import com.bdu.plotassistant.service.CharacterService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/project/{projectId}/character")
@Validated
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @PostMapping("/create")
    public ApiResult<String> create(
            @PathVariable Long projectId,
            @RequestBody @Validated CreateCharacterRequest req) {

        String charId = characterService.create(projectId, req);
        return ApiResult.success(charId);
    }

    @GetMapping("/list")
    public ApiResult<List<CharacterSummaryDTO>> list(@PathVariable Long projectId) {
        List<CharacterSummaryDTO> list = characterService.listByProject(projectId);
        return ApiResult.success(list);
    }

    @GetMapping("/{charId}/detail")
    public ApiResult<CharacterDetailDTO> getDetail(
            @PathVariable Long projectId,
            @PathVariable String charId) {

        CharacterDetailDTO detail = characterService.getDetail(charId);
        return ApiResult.success(detail);
    }

    @PostMapping("/{charId}/update")
    public ApiResult<Void> update(
            @PathVariable Long projectId,
            @PathVariable String charId,
            @RequestBody @Validated UpdateCharacterRequest req) {

        characterService.update(charId, req);
        return ApiResult.success(null);
    }

    @PostMapping("/{charId}/delete")
    public ApiResult<Void> delete(
            @PathVariable Long projectId,
            @PathVariable String charId) {

        characterService.delete(charId);
        return ApiResult.success(null);
    }

    @PostMapping("/generate-batch")
    public ApiResult<List<String>> generateBatch(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateCharacterSetRequest req) {

        List<String> characterIds = characterService.generateBatch(projectId, req);
        return ApiResult.success(characterIds);
    }
    /*
    * L2:重要角色ai
    * */
    @PostMapping("/characters/generate-important")
    public ApiResult<List<String>> generateImportantCharacters(
            @PathVariable Long projectId,
            @RequestBody @Validated GenerateCharacterSetRequest request) {

        // L2: 基于L1世界观描述生成重要角色(status=1)
        List<String> characterIds = characterService.generateBatch(projectId, request);
        return ApiResult.success(characterIds);
    }
    /*
    *
    *L6:场景NPCai
    * */
    @PostMapping("/nodes/{nodeId}/npcs/generate")
    public ApiResult<List<String>> generateNPCs(
            @PathVariable Long projectId,
            @PathVariable String nodeId,
            @RequestBody @Validated GenerateNPCRequest request) {

        // L6: 基于L5节点氛围生成次要角色(status=2)，排除L2重要角色
        List<String> npcIds = characterService.generateNPCBatch(projectId, nodeId, request);
        return ApiResult.success(npcIds);
    }

    @PostMapping("/{charId}/add-insight")
    public ApiResult<Void> addInsight(
            @PathVariable Long projectId,
            @PathVariable String charId,
            @RequestBody @Validated AddInsightRequest req) {

        characterService.addValidatedInsight(charId, req);
        return ApiResult.success(null);
    }
}
