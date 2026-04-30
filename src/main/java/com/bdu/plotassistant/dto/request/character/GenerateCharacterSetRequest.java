package com.bdu.plotassistant.dto.request.character;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class GenerateCharacterSetRequest {
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "至少生成1个角色")
    @Max(value = 10, message = "最多生成10个角色")
    private Integer count;
    private String prompt;


}
