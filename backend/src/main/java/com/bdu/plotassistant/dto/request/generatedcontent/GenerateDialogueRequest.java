package com.bdu.plotassistant.dto.request.generatedcontent;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class GenerateDialogueRequest {

    private String nodeId;              // 场景节点ID（L5）
    private List<String> characterIds;  // 指定参与角色（可选，不传则自动选取）
    private String prompt;              // 特殊要求（如"展现两人的矛盾"、"揭示秘密"）
}
