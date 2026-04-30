package com.bdu.plotassistant.dto.request.storynode;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class CreateNodeRequest {

    @NotBlank(message = "节点名称不能为空")
    @Size(max = 128, message = "节点名称最长128")
    private String nodeName;

    private String sceneDescription;

    @NotEmpty(message = "至少关联一个角色")
    private List<String> associatedCharIds;

    private Map<String, Object> initialVariables;

    private BigDecimal positionX = BigDecimal.ZERO;

    private BigDecimal positionY = BigDecimal.ZERO;
}
