package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.request.user.*;
import com.bdu.plotassistant.dto.response.user.*;
import com.bdu.plotassistant.entity.User;
import com.bdu.plotassistant.repository.UserRepository;
import com.bdu.plotassistant.service.UserService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Value("${jwt.secret:defaultSecretKey12345678901234567890}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Long register(RegisterRequest request) {
        // 校验用户名唯一
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BizException("用户名已存在");
        }

        // 校验邮箱唯一（如有）
        if (!ServiceUtil.isEmpty(request.getEmail())) {
            userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
                throw new BizException("邮箱已被注册");
            });
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPassword());
        user.setEmail(request.getEmail());
        user.setStatus(1);

        User saved = userRepository.save(user);
        return saved.getId();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BizException("用户名或密码错误"));

        // 校验密码
        if (!verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }

        // 校验状态
        if (user.getStatus() == 0) {
            throw new BizException("账户已被禁用");
        }

        // 生成响应
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setToken(generateToken(user));

        return response;
    }

    @Override
    public void logout(String token) {
        // JWT无状态，服务端无需处理
        ServiceUtil.requireNonNull(token, "token不能为空");
    }

    @Override
    public UserProfileDTO getProfile(Long userId) {
        User user = getUserById(userId);

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setStatus(user.getStatus().toString());
        dto.setCreatedAt(ServiceUtil.formatDateTime(user.getCreatedAt()));

        return dto;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        // 邮箱唯一性校验
        if (!ServiceUtil.isEmpty(request.getEmail()) &&
                !request.getEmail().equals(user.getEmail())) {
            userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
                throw new BizException("邮箱已被使用");
            });
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        // 校验旧密码
        if (!verifyPassword(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException("旧密码错误");
        }


        user.setPasswordHash(request.getNewPassword());
        userRepository.save(user);
    }

    @Override
    public UserDTO getById(Long userId) {
        User user = getUserById(userId);

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        return dto;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // ========== 私有方法 ==========

    private User getUserById(Long userId) {
        ServiceUtil.requireNonNull(userId, "用户ID不能为空");
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
    }

    private String hashPassword(String password) {
        // MD5简化实现，生产环境使用BCrypt
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }


    //现阶段先使用明文
   private boolean verifyPassword(String password, String hash) {
       return (password).equals(hash);
   }

    private String generateToken(User user) {
        SecretKeySpec key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS256.getJcaName()
        );

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .claim("username", user.getUsername())
                .signWith(key)
                .compact();
    }
}
