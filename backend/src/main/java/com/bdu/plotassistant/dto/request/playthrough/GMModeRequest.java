package com.bdu.plotassistant.dto.request.playthrough;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GMModeRequest {

    @NotBlank(message = "GM提示词不能为空")
    private String gmPrompt;
}
