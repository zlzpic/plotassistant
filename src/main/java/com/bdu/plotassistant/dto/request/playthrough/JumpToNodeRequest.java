package com.bdu.plotassistant.dto.request.playthrough;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JumpToNodeRequest {

    @NotBlank(message = "目标节点不能为空")
    private String targetNodeId;
}
