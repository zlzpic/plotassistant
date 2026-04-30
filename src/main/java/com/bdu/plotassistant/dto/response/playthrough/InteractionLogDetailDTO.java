package com.bdu.plotassistant.dto.response.playthrough;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class InteractionLogDetailDTO extends InteractionLogDTO {

    private String characterId;
    private String fromNodeId;
    private String toNodeId;
    private String transitionReason;
    private Map<String, Object> variablesSnapshot;
    private String promptUsed;
}
