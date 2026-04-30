package com.bdu.plotassistant.dto.outline;

import lombok.Data;
import java.util.List;

@Data
public class OutlineStructure {
    private String title;
    private List<String> themes;
    private String endingType;
    private List<ActStructure> acts;
}
