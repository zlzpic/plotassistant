package com.bdu.plotassistant.dto.request.storyedge;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GenerateEdgeRequest {

    @NotBlank(message = "源节点不能为空")
    private String sourceId;

    @NotBlank(message = "目标节点不能为空")
    private String targetId;

    private String prompt;
}
