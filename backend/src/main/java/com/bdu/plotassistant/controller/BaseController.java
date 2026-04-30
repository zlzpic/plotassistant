package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.utils.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

public abstract class BaseController {

    @Autowired
    protected HttpServletRequest request;

    @Value("${jwt.secret:defaultSecretKey12345678901234567890}")
    private String jwtSecret;

    /**
     * 获取当前登录用户ID
     * @return userId，未登录则抛异常
     */
    protected Long getCurrentUserId() {
        String token = extractToken();
        Claims claims = parseToken(token);
        String userIdStr = claims.getSubject();
        return Long.valueOf(userIdStr);
    }

    /**
     * 获取当前登录用户名（可选）
     */
    protected String getCurrentUsername() {
        String token = extractToken();
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从 Header 提取 Token（去掉 Bearer 前缀）
     */
    protected String extractToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException("请先登录");
        }
        return authHeader.substring(7);
    }

    /**
     * 解析 Token（适配 JJWT 0.12.3，兼容旧代码生成的 Token）
     */
    private Claims parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new BizException("登录已过期，请重新登录");
        } catch (Exception e) {
            throw new BizException("无效的登录状态");
        }
    }
}
