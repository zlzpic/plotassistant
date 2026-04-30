package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.*;
import com.bdu.plotassistant.dto.request.playthrough.CreatePlaythroughRequest;
import com.bdu.plotassistant.dto.request.playthrough.JumpToNodeRequest;
import com.bdu.plotassistant.dto.response.*;
import com.bdu.plotassistant.dto.response.playthrough.PlaythroughDetailDTO;
import com.bdu.plotassistant.dto.response.playthrough.PlaythroughSessionDTO;
import com.bdu.plotassistant.dto.response.playthrough.PlaythroughSummaryDTO;

import java.util.List;

public interface PlaythroughSessionService {

    String create(Long userId, CreatePlaythroughRequest request);

    List<PlaythroughSummaryDTO> list(Long userId, Long projectId, String sessionType);

    PlaythroughDetailDTO getDetail(String sessionId);

    void delete(String sessionId);

    void complete(String sessionId);

    void abandon(String sessionId);

    void jumpToNode(String sessionId, JumpToNodeRequest request);

    void reset(String sessionId);

    String convertToOfficial(String sessionId);

    PlaythroughSessionDTO getById(String sessionId);
}
