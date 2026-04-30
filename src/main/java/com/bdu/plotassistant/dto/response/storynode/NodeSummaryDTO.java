package com.bdu.plotassistant.dto.response.storynode;

import lombok.Data;

import javax.persistence.Column;
import java.math.BigDecimal;
import java.util.List;

@Data
public class NodeSummaryDTO {

    private String id;
    private String nodeName;
    private Integer actIndex;
    private Integer beatIndex;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private List<String> associatedCharIds;

    //景描述（直接来自 story_node 表）
    private String sceneDescription;

    //对话内容（来自 generated_content 表，type=DIALOGUE）
    private String dialogueContent;
}

