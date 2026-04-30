package com.bdu.plotassistant.dto.outline;

import lombok.Data;
import java.util.List;

@Data
public class ActStructure {
    private Integer actIndex;      // 对应StoryNode.act_index
    private String name;
    private String summary;
    private List<BeatStructure> beats;
}
