package com.bdu.plotassistant.dto.request.project;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class ExportRequest {

    @NotBlank(message = "导出格式不能为空")
    @Pattern(regexp = "JSON|EXCEL", message = "格式只能是JSON或EXCEL")
    private String format;
}
