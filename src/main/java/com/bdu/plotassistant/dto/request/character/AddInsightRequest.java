package com.bdu.plotassistant.dto.request.character;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AddInsightRequest {

    @NotBlank(message = "洞察内容不能为空")
    private String insight;
}
