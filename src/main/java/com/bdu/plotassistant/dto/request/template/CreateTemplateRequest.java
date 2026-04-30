package com.bdu.plotassistant.dto.request.template;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateTemplateRequest {

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码最长64")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称最长128")
    private String templateName;

    private String systemPrompt;

    @NotBlank(message = "用户提示模板不能为空")
    private String userPromptTemplate;

    private String paramSchema;
}
