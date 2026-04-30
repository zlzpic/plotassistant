package com.bdu.plotassistant.dto.request.generatedcontent;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class GenerateOutlineRequest {

    private Integer darkness = 5;    // 黑暗程度 1-10
    private Integer complexity = 3;  // 复杂度 1-10
    private String prompt;           // 特殊要求（可选）
}
