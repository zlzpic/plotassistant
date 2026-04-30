package com.bdu.plotassistant.dto.response.user;

import lombok.Data;

@Data
public class LoginResponse {

    private Long userId;
    private String token;
    private String username;
}
