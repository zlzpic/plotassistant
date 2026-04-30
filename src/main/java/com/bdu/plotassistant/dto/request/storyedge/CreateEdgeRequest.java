package com.bdu.plotassistant.dto.request.storyedge;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateEdgeRequest {

    @NotBlank(message = "源节点不能为空")
    private String sourceId;

    @NotBlank(message = "目标节点不能为空")
    private String targetId;

    @Size(max = 255, message = "选项文本最长255")
    private String label;

    @Size(max = 500, message = "条件表达式最长500")
    private String conditionExpr;
}
