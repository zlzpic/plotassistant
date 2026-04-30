package com.bdu.plotassistant.dto.request.worldsetting;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UpdateWorldSettingRequest {

    @NotBlank(message = "题材不能为空")
    @Size(max = 64, message = "题材最长64")
    private String genre;

    @Size(max = 64, message = "子题材最长64")
    private String subGenre;

    @Min(0)
    @Max(10)
    private Integer techLevel = 0;

    @Min(0)
    @Max(10)
    private Integer magicLevel = 0;

    @Size(max = 64, message = "时间背景最长64")
    private String timeBackground;

    @Size(max = 128, message = "地理背景最长128")
    private String geoBackground;

    private String coreConflict;

    private String specialRules;
}
