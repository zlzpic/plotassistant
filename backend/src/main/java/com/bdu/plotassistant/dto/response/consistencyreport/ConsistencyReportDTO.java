package com.bdu.plotassistant.dto.response.consistencyreport;

import lombok.Data;

@Data
public class ConsistencyReportDTO {

    private Long id;
    private String status;
    private Boolean isActive;
    private Integer checkedItemsCount;
    private Integer conflictsCount;
    private String checkedAt;
}
