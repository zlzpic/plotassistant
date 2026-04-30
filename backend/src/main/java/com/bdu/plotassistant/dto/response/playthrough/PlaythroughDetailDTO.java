package com.bdu.plotassistant.dto.response.playthrough;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlaythroughDetailDTO extends PlaythroughSummaryDTO {

    private String sourceNodeId;
    private String initialPrompt;
}
