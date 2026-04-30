package com.bdu.plotassistant.dto.request.generatedcontent;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SaveContentRequest {

    @NotBlank(message = "内容不能为空")
    private String contentJson;
}
