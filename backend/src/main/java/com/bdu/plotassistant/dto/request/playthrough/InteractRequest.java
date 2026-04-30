package com.bdu.plotassistant.dto.request.playthrough;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class InteractRequest {

    @NotBlank(message = "输入内容不能为空")
    private String userInput;
}
