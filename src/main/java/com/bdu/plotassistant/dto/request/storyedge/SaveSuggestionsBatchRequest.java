package com.bdu.plotassistant.dto.request.storyedge;

import com.bdu.plotassistant.dto.response.storyedge.EdgeSuggestionDTO;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class SaveSuggestionsBatchRequest {

    @NotBlank(message = "源节点ID不能为空")
    private String sourceId;

    @NotBlank(message = "目标节点ID不能为空")
    private String targetId;

    @NotEmpty(message = "建议列表不能为空")
    @Valid
    private List<EdgeSuggestionDTO> suggestions;
}
