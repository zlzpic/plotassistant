package com.bdu.plotassistant.dto.response.character;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CharacterDetailDTO extends CharacterSummaryDTO {

    private String personaPrompt;
    private String speechPattern;
    private List<String> knowledgeScope;
    private List<String> validatedInsights;
    private String createdAt;
    private String updatedAt;
}
