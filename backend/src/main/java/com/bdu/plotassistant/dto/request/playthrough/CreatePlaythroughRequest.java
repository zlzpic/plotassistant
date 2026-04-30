package com.bdu.plotassistant.dto.request.playthrough;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class CreatePlaythroughRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Pattern(regexp = "TRIAL|PLAYTHROUGH", message = "类型只能是TRIAL或PLAYTHROUGH")
    private String sessionType = "TRIAL";

    private String sourceNodeId;

    private String initialPrompt;
}
