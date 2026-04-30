package com.bdu.plotassistant.dto.request.consistencyreport;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ResolveConflictRequest {

    @NotBlank(message = "冲突ID不能为空")
    private String conflictId;

    private String resolution;
}
