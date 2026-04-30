package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.dto.request.user.*;
import com.bdu.plotassistant.dto.response.user.*;
import com.bdu.plotassistant.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/user")
@Validated
public class UserController extends BaseController{

    private final UserService userService;
    private final HttpServletRequest request;

    public UserController(UserService userService, HttpServletRequest request) {
        this.userService = userService;
        this.request = request;
    }

    @PostMapping("/register")
    public ApiResult<Long> register(@RequestBody @Validated RegisterRequest req) {
        Long userId = userService.register(req);
        return ApiResult.success(userId);
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody @Validated LoginRequest req) {
        LoginResponse resp = userService.login(req);
        return ApiResult.success(resp);
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        String token = extractToken();
        userService.logout(token);
        return ApiResult.success(null);
    }

    @GetMapping("/profile")
    public ApiResult<UserProfileDTO> getProfile() {
        Long userId = getCurrentUserId();
        UserProfileDTO profile = userService.getProfile(userId);
        return ApiResult.success(profile);
    }

    @PostMapping("/profile/update")
    public ApiResult<Void> updateProfile(@RequestBody @Validated UpdateProfileRequest req) {
        Long userId = getCurrentUserId();
        userService.updateProfile(userId, req);
        return ApiResult.success(null);
    }

    @PostMapping("/password/change")
    public ApiResult<Void> changePassword(@RequestBody @Validated ChangePasswordRequest req) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, req);
        return ApiResult.success(null);
    }

}
