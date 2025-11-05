package com.userprofile.user.service;

import com.userprofile.common.dto.AuthResponse;
import com.userprofile.common.dto.LoginRequest;
import com.userprofile.common.exception.BusinessException;
import com.userprofile.common.security.JwtTokenProvider;
import com.userprofile.user.entity.User;
import com.userprofile.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;

    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        log.info("用户登录: username={}", request.getUsername());

        // 查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("密码验证失败: username={}", request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 生成令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("name", user.getName());

        String accessToken = jwtTokenProvider.generateToken(user.getUsername(), claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        log.info("用户登录成功: username={}, userId={}", user.getUsername(), user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtExpirationMs,
                user.getUsername()
        );
    }

    /**
     * 刷新令牌
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.info("刷新令牌");

        // 验证刷新令牌
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(401, "刷新令牌无效");
        }

        // 获取用户名
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 生成新的访问令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("name", user.getName());

        String newAccessToken = jwtTokenProvider.generateToken(user.getUsername(), claims);

        log.info("令牌刷新成功: username={}", username);

        return new AuthResponse(
                newAccessToken,
                refreshToken,  // 刷新令牌保持不变
                jwtExpirationMs,
                user.getUsername()
        );
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    /**
     * 从令牌获取用户名
     */
    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }
}
