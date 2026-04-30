package com.bdu.plotassistant.dto.request.playthrough;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class RestoreRequest {

    @NotNull(message = "存档点ID不能为空")
    private Long savePointId;
}
