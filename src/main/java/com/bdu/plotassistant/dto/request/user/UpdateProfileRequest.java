package com.bdu.plotassistant.dto.request.user;

import lombok.Data;

import javax.validation.constraints.Email;

@Data
public class UpdateProfileRequest {

    @Email(message = "邮箱格式错误")
    private String email;
}
