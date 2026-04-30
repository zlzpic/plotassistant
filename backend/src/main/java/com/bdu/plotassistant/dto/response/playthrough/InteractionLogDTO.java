package com.bdu.plotassistant.dto.response.playthrough;

import lombok.Data;

@Data
public class InteractionLogDTO {

    private Integer sequenceNum;
    private String logType;
    private String userInput;
    private String aiReply;
    private String createdAt;
}
