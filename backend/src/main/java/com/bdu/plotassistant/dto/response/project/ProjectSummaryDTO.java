package com.bdu.plotassistant.dto.response.project;

import lombok.Data;

@Data
public class ProjectSummaryDTO {

    private Long id;
    private String name;
    private String status;
    private String updatedAt;
}
