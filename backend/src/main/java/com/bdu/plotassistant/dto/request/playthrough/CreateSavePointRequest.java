package com.bdu.plotassistant.dto.request.playthrough;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateSavePointRequest {

    @NotBlank(message = "存档名称不能为空")
    @Size(max = 128, message = "存档名称最长128")
    private String saveName;
}
