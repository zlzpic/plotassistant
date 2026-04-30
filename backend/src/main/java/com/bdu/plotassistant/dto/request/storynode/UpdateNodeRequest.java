package com.bdu.plotassistant.dto.request.storynode;

import lombok.Data;

import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class UpdateNodeRequest {

    @Size(max = 128, message = "节点名称最长128")
    private String nodeName;

    private String sceneDescription;

    private List<String> associatedCharIds;

    private Map<String, Object> initialVariables;

    private BigDecimal positionX;

    private BigDecimal positionY;
}
