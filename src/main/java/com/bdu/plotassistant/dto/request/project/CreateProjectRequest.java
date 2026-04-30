package com.bdu.plotassistant.dto.request.project;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 128, message = "项目名称最长128")
    private String name;

    @Size(max = 500, message = "项目描述最长500")
    private String description;
}
