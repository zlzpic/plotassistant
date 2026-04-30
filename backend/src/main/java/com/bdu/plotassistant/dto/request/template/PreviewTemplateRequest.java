package com.bdu.plotassistant.dto.request.template;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Data
public class PreviewTemplateRequest {

    @NotEmpty(message = "参数不能为空")
    private Map<String, Object> params;
}
