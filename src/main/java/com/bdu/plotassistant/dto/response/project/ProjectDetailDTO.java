package com.bdu.plotassistant.dto.response.project;

import com.bdu.plotassistant.dto.response.worldsetting.WorldSettingDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectDetailDTO extends ProjectSummaryDTO {

    private String description;
    private WorldSettingDTO worldSetting;
}
