package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.*;
import com.bdu.plotassistant.dto.request.playthrough.CreateSavePointRequest;
import com.bdu.plotassistant.dto.request.playthrough.GMModeRequest;
import com.bdu.plotassistant.dto.request.playthrough.InteractRequest;
import com.bdu.plotassistant.dto.request.playthrough.RestoreRequest;
import com.bdu.plotassistant.dto.response.*;
import com.bdu.plotassistant.dto.response.playthrough.InteractionLogDTO;
import com.bdu.plotassistant.dto.response.playthrough.InteractionLogDetailDTO;
import com.bdu.plotassistant.dto.response.playthrough.PlaythroughStateDTO;
import com.bdu.plotassistant.dto.response.playthrough.SavePointDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface PlaythroughInteractionService {

    PlaythroughStateDTO getCurrentState(String sessionId);

    SseEmitter interact(String sessionId, InteractRequest request);

    void enableGMMode(String sessionId, GMModeRequest request);

    void disableGMMode(String sessionId);

    List<InteractionLogDTO> getHistory(String sessionId, Integer limit);

    InteractionLogDetailDTO getSpecificLog(String sessionId, Integer sequenceNum);

    Long createSavePoint(String sessionId, CreateSavePointRequest request);

    List<SavePointDTO> listSavePoints(String sessionId);

    void restoreToSavePoint(String sessionId, RestoreRequest request);
}
