package com.bdu.plotassistant.dto.response.generatedcontent;

import lombok.Data;

@Data
public class GeneratedContentDTO {

    private String contentType;
    private String contentJson;
    private String aiModel;
    private Integer tokenUsage;
    private Integer generationTimeMs;
    private Boolean isEdited;
    private String updatedAt;
}
