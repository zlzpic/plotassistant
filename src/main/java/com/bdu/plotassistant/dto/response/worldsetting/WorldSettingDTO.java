package com.bdu.plotassistant.dto.response.worldsetting;

import lombok.Data;

@Data
public class WorldSettingDTO {

    private String genre;
    private String subGenre;
    private Integer techLevel;
    private Integer magicLevel;
    private String timeBackground;
    private String geoBackground;
    private String coreConflict;
    private String specialRules;
    private String description;
    private String updatedAt;
}
