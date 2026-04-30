package com.bdu.plotassistant.dto.response.consistencyreport;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConsistencyReportDetailDTO extends ConsistencyReportDTO {

    private List<ConflictDTO> conflicts;

    @Data
    public static class ConflictDTO {
        private String type;
        private String severity;
        private String description;
        private List<String> locations;
    }
}
