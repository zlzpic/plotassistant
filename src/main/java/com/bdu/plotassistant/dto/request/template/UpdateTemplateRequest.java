package com.bdu.plotassistant.dto.request.template;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateTemplateRequest {

    @Size(max = 128, message = "模板名称最长128")
    private String templateName;

    private String systemPrompt;

    private String userPromptTemplate;

    private String paramSchema;
}
