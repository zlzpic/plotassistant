package com.bdu.plotassistant.dto.response.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TemplateDetailDTO extends TemplateSummaryDTO {

    private String systemPrompt;
    private String userPromptTemplate;
    private String paramSchema;
    private Integer version;
    private String createdAt;
    private String updatedAt;
}
