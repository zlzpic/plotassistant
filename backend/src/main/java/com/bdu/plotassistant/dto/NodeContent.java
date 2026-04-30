package com.bdu.plotassistant.dto;

import com.bdu.plotassistant.entity.StoryEdge;
import lombok.Data;

@Data
public
class NodeContent {
    private String nodeId;
    private String nodeName;
    private Integer actIndex;
    private Integer beatIndex;
    private String sceneDescription;    // L5
    private String dialogue;            // L7 (JSON string)
    private StoryEdge chosenEdge;       // L8 (到下一节点的边)
}
