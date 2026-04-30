package com.bdu.plotassistant.dto.response.playthrough;

import lombok.Data;

import java.util.Map;

@Data
public class PlaythroughStateDTO {

    private String sessionId;
    private String currentNodeId;
    private String activeCharacterId;
    private Map<String, Object> variables;
    private String historySummary;
    private Integer turnCount;
    private String lastInput;
    private String lastReply;
}
