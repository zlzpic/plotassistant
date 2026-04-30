package com.bdu.plotassistant.dto.response.project;

import lombok.Data;

@Data
public class ExportResultDTO {

    private String downloadUrl;
    private String format;
    private Long fileSize;
    private String generatedAt;
}
