package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.*;
import com.bdu.plotassistant.dto.request.template.CreateTemplateRequest;
import com.bdu.plotassistant.dto.request.template.CreateVersionRequest;
import com.bdu.plotassistant.dto.request.template.PreviewTemplateRequest;
import com.bdu.plotassistant.dto.request.template.UpdateTemplateRequest;
import com.bdu.plotassistant.dto.response.*;
import com.bdu.plotassistant.dto.response.template.TemplateDetailDTO;
import com.bdu.plotassistant.dto.response.template.TemplateSummaryDTO;

import java.util.List;

public interface PromptTemplateService {

    Long create(CreateTemplateRequest request);

    List<TemplateSummaryDTO> list(Boolean isActive);

    TemplateDetailDTO getByCode(String templateCode);

    TemplateDetailDTO getByCodeAndVersion(String templateCode, Integer version);

    void update(String templateCode, UpdateTemplateRequest request);

    Integer createNewVersion(String templateCode, CreateVersionRequest request);

    void activateVersion(String templateCode, Integer version);

    void deleteVersion(String templateCode, Integer version);

    String preview(String templateCode, PreviewTemplateRequest request);

    String getRenderedTemplate(String templateCode, Object params);
}
