package com.bdu.plotassistant.dto.request.storyedge;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class BatchSaveEdgesRequest {

    @NotEmpty(message = "边列表不能为空")
    private List<EdgeDTO> edges;

    private List<Long> deleteIds;

    @Data
    public static class EdgeDTO {
        private Long id;
        private String sourceId;
        private String targetId;
        private String label;
        private String conditionExpr;
        private String reason;
        private String onSuccess;
        private String onFailure;
        private String effect;
    }
}
