package com.bdu.plotassistant.dto.response.storynode;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class NodeDTO {

    private String id;
    private String nodeName;
    private String sceneDescription;
    private List<String> associatedCharIds;
    private BigDecimal positionX;
    private BigDecimal positionY;
}
