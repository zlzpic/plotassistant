package com.bdu.plotassistant.service;


import com.bdu.plotassistant.dto.request.user.ChangePasswordRequest;
import com.bdu.plotassistant.dto.request.user.LoginRequest;
import com.bdu.plotassistant.dto.request.user.RegisterRequest;
import com.bdu.plotassistant.dto.request.user.UpdateProfileRequest;
import com.bdu.plotassistant.dto.response.user.LoginResponse;
import com.bdu.plotassistant.dto.response.user.UserDTO;
import com.bdu.plotassistant.dto.response.user.UserProfileDTO;

public interface UserService {

    Long register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String token);

    UserProfileDTO getProfile(Long userId);

    void updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserDTO getById(Long userId);

    boolean existsByUsername(String username);
}
