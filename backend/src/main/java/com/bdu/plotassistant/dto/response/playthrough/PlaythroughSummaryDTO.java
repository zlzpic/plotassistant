package com.bdu.plotassistant.dto.response.playthrough;

import lombok.Data;

@Data
public class PlaythroughSummaryDTO {

    private String id;
    private Long projectId;
    private String sessionType;
    private String status;
    private String startedAt;
    private String endedAt;
}
