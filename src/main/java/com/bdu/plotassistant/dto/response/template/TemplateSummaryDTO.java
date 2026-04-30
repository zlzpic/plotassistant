package com.bdu.plotassistant.dto.response.template;

import lombok.Data;

@Data
public class TemplateSummaryDTO {

    private String templateCode;
    private String templateName;
    private Boolean isActive;
}
