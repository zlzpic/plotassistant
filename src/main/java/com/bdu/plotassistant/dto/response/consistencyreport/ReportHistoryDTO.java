package com.bdu.plotassistant.dto.response.consistencyreport;

import lombok.Data;

@Data
public class ReportHistoryDTO {

    private Long id;
    private String status;
    private Boolean isActive;
    private String checkedAt;
}
