package com.bdu.plotassistant.dto.response.playthrough;

import lombok.Data;

@Data
public class PlaythroughSessionDTO {

    private String id;
    private Long projectId;
    private String sessionType;
    private String status;
}
