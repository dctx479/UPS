package com.userprofile.user.controller;

import com.userprofile.common.dto.AuthResponse;
import com.userprofile.common.dto.LoginRequest;
import com.userprofile.common.response.Result;
import com.userprofile.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码登录，获取JWT令牌")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());

        AuthResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public Result<AuthResponse> refreshToken(
            @Parameter(description = "刷新令牌") @RequestParam String refreshToken) {

        log.info("刷新令牌请求");

        AuthResponse response = authService.refreshToken(refreshToken);
        return Result.success(response);
    }

    /**
     * 验证令牌
     */
    @GetMapping("/validate")
    @Operation(summary = "验证令牌", description = "验证访问令牌是否有效")
    public Result<Boolean> validateToken(
            @Parameter(description = "访问令牌") @RequestParam String token) {

        boolean valid = authService.validateToken(token);
        return Result.success(valid);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "通过令牌获取当前登录用户信息")
    public Result<String> getCurrentUser(
            @Parameter(description = "访问令牌") @RequestHeader("Authorization") String authHeader) {

        // 提取令牌
        String token = authHeader.replace("Bearer ", "");
        String username = authService.getUsernameFromToken(token);

        return Result.success(username);
    }
}
