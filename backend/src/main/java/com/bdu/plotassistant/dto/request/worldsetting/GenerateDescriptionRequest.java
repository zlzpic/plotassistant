package com.bdu.plotassistant.dto.request.worldsetting;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GenerateDescriptionRequest {
    private String prompt;
}
