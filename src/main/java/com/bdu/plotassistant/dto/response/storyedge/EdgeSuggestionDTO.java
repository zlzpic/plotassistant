package com.bdu.plotassistant.dto.response.storyedge;

import lombok.Data;

@Data
public class EdgeSuggestionDTO {

    private String id;
    private String label;
    private String conditionExpr;
    private String reason;
    private String onSuccess;
    private String onFailure;
    private String effect;
}
