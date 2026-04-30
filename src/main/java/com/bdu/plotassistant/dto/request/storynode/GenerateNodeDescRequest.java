package com.bdu.plotassistant.dto.request.storynode;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GenerateNodeDescRequest {

    private String prompt;  // 特殊要求（如"强调压抑感"、"突出科技元素"）
}
