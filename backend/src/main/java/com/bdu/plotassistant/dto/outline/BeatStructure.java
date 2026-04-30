package com.bdu.plotassistant.dto.outline;

import lombok.Data;
import java.util.List;

@Data
public class BeatStructure {
    private Integer beatIndex;     // 对应StoryNode.beat_index
    private String title;          // 可作为默认nodeName
    private String description;    // 可作为默认sceneDescription
    private List<String> keyCharacters;  // 关联角色
}
