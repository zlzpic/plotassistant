package com.bdu.plotassistant.dto.response.storynode;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class NodeDetailDTO extends NodeSummaryDTO {

    private String sceneDescription;
    private Map<String, Object> initialVariables;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private String createdAt;
    private String updatedAt;
    private String dialogueContent;

    public void setDialogueContent(String dialogueContent) {
        this.dialogueContent = dialogueContent;
    }

    public String getDialogueContent() {
        return dialogueContent;
    }
}
