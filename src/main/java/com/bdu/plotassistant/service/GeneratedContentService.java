package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.outline.OutlineStructure;
import com.bdu.plotassistant.dto.request.*;
import com.bdu.plotassistant.dto.request.character.GenerateCharacterSetRequest;
import com.bdu.plotassistant.dto.request.generatedcontent.GenerateDialogueRequest;
import com.bdu.plotassistant.dto.request.generatedcontent.GenerateOutlineRequest;
import com.bdu.plotassistant.dto.request.generatedcontent.RegenerateRequest;
import com.bdu.plotassistant.dto.request.generatedcontent.SaveContentRequest;
import com.bdu.plotassistant.dto.request.storyscript.GenerateWholeLineRequest;
import com.bdu.plotassistant.dto.response.*;
import com.bdu.plotassistant.dto.response.generatedcontent.GeneratedContentDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface GeneratedContentService {

    String generateOutline(Long projectId, GenerateOutlineRequest request);

    String generateCharacterSet(Long projectId, GenerateCharacterSetRequest request);

    String generateDialogueSample(Long projectId, GenerateDialogueRequest request);

    GeneratedContentDTO getByType(Long projectId, String contentType);

    void save(Long projectId, String contentType, SaveContentRequest request);

    String regenerate(Long projectId, String contentType, RegenerateRequest request);

    boolean existsByType(Long projectId, String contentType);

    String generateWholeLine(Long projectId, GenerateWholeLineRequest request);

    OutlineStructure parseOutlineStructure(Long projectId);
}
