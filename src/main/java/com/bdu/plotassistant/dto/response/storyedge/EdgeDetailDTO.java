package com.bdu.plotassistant.dto.response.storyedge;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class EdgeDetailDTO {
    private Long id;
    private String sourceId;
    private String targetId;
    private String sourceNodeName;
    private String targetNodeName;
    private String label;
    private String conditionExpr;
    private String reason;
    private String onSuccess;
    private String onFailure;
    private String effect;
    private String updatedAt;
}
