package com.bdu.plotassistant.dto.request.storynode;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class BatchSaveNodesRequest {

    @NotEmpty(message = "节点列表不能为空")
    private List<NodeDTO> nodes;

    @Data
    public static class NodeDTO {
        private String id;
        private String nodeName;
        private String sceneDescription;
        private List<String> associatedCharIds;
        private java.math.BigDecimal positionX;
        private java.math.BigDecimal positionY;
        private Integer actIndex;   // 第几幕（1,2,3）
        private Integer beatIndex;  // 第几节拍（1,2,3...）
    }
}
