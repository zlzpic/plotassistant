package com.bdu.plotassistant.dto.request.storyedge;

import com.bdu.plotassistant.dto.response.storyedge.EdgeSuggestionDTO;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class SaveFromSuggestionRequest {

    @NotBlank(message = "源节点ID不能为空")
    private String sourceId;

    @NotBlank(message = "目标节点ID不能为空")
    private String targetId;

    @NotNull(message = "建议内容不能为空")
    @Valid
    private EdgeSuggestionDTO suggestion;
}
