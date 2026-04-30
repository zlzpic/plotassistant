package com.bdu.plotassistant.dto.response.storyedge;

import lombok.Data;

@Data
public class EdgeDTO {

    private Long id;
    private String sourceId;
    private String targetId;
    private String label;
    private String conditionExpr;
    private String reason;
    private  String onSuccess;
    private String onFailure;
    private String effect;


}
