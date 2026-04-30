package com.bdu.plotassistant.dto.request.consistencyreport;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class TriggerCheckRequest {

    @NotBlank(message = "检查范围不能为空")
    private String scope;

    private List<String> targetNodeIds;

    private String prompt;
}
