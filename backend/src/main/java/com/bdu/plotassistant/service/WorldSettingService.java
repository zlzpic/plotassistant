package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.*;
import com.bdu.plotassistant.dto.request.worldsetting.GenerateDescriptionRequest;
import com.bdu.plotassistant.dto.request.worldsetting.UpdateWorldSettingRequest;
import com.bdu.plotassistant.dto.response.*;
import com.bdu.plotassistant.dto.response.worldsetting.WorldSettingDTO;

public interface WorldSettingService {

    WorldSettingDTO getByProjectId(Long projectId);

    void update(Long projectId, UpdateWorldSettingRequest request);

    String generateDescription(Long projectId, GenerateDescriptionRequest request);

    WorldSettingDTO getOrCreateDefault(Long projectId);
}
