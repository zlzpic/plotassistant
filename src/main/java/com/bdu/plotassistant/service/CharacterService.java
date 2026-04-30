package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.character.*;
import com.bdu.plotassistant.dto.response.character.CharacterDTO;
import com.bdu.plotassistant.dto.response.character.CharacterDetailDTO;
import com.bdu.plotassistant.dto.response.character.CharacterProfileDTO;
import com.bdu.plotassistant.dto.response.character.CharacterSummaryDTO;
import java.util.List;

public interface CharacterService {

    String create(Long projectId, CreateCharacterRequest request);

    List<CharacterSummaryDTO> listByProject(Long projectId);

    CharacterDetailDTO getDetail(String charId);

    void update(String charId, UpdateCharacterRequest request);

    void delete(String charId);

    List<String> generateBatch(Long projectId, GenerateCharacterSetRequest request);

    void addValidatedInsight(String charId, AddInsightRequest request);

    List<CharacterDTO> getByIds(List<String> charIds);
    List<String> generateNPCBatch(Long projectId, String nodeId, GenerateNPCRequest request);
    List<CharacterProfileDTO> getImportantCharacters(Long projectId);
}
