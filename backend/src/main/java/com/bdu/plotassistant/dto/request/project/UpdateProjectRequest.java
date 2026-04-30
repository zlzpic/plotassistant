package com.bdu.plotassistant.dto.request.project;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateProjectRequest {

    @Size(max = 128, message = "项目名称最长128")
    private String name;

    @Size(max = 500, message = "项目描述最长500")
    private String description;

    private String status;
}
