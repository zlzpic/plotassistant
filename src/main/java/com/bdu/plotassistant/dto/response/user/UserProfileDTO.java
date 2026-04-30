package com.bdu.plotassistant.dto.response.user;

import lombok.Data;

@Data
public class UserProfileDTO {

    private Long id;
    private String username;
    private String email;
    private String status;
    private String createdAt;
}
