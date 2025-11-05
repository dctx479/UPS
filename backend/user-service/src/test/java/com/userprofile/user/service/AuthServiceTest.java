package com.userprofile.user.service;

import com.userprofile.common.dto.AuthResponse;
import com.userprofile.common.dto.LoginRequest;
import com.userprofile.common.exception.BusinessException;
import com.userprofile.common.security.JwtTokenProvider;
import com.userprofile.user.entity.User;
import com.userprofile.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService单元测试
 *
 * @author User Profile Team
 * @version 1.5.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService单元测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // 设置JWT过期时间
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 86400000L);

        // 准备测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setName("Test User");
        testUser.setStatus(1);

        // 准备登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("登录 - 成功")
    void login_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString(), anyMap())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("testuser");

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("password123", "$2a$10$encodedPassword");
    }

    @Test
    @DisplayName("登录 - 用户不存在")
    void login_UserNotFound() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("登录 - 密码错误")
    void login_WrongPassword() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(jwtTokenProvider, never()).generateToken(anyString(), anyMap());
    }

    @Test
    @DisplayName("登录 - 账号被禁用")
    void login_AccountDisabled() {
        // Given
        testUser.setStatus(0);  // 禁用状态
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$encodedPassword")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号已被禁用");

        verify(jwtTokenProvider, never()).generateToken(anyString(), anyMap());
    }

    @Test
    @DisplayName("刷新令牌 - 成功")
    void refreshToken_Success() {
        // Given
        String oldRefreshToken = "old-refresh-token";
        when(jwtTokenProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(oldRefreshToken)).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(anyString(), anyMap())).thenReturn("new-access-token");

        // When
        AuthResponse response = authService.refreshToken(oldRefreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(oldRefreshToken);
        assertThat(response.getUsername()).isEqualTo("testuser");

        verify(jwtTokenProvider, times(1)).validateToken(oldRefreshToken);
        verify(jwtTokenProvider, times(1)).getUsernameFromToken(oldRefreshToken);
    }

    @Test
    @DisplayName("刷新令牌 - 令牌无效")
    void refreshToken_InvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("刷新令牌无效");

        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
    }

    @Test
    @DisplayName("刷新令牌 - 用户不存在")
    void refreshToken_UserNotFound() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(refreshToken)).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("验证令牌 - 有效")
    void validateToken_Valid() {
        // Given
        String token = "valid-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertThat(result).isTrue();
        verify(jwtTokenProvider, times(1)).validateToken(token);
    }

    @Test
    @DisplayName("验证令牌 - 无效")
    void validateToken_Invalid() {
        // Given
        String token = "invalid-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("从令牌获取用户名")
    void getUsernameFromToken() {
        // Given
        String token = "valid-token";
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("testuser");

        // When
        String username = authService.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo("testuser");
        verify(jwtTokenProvider, times(1)).getUsernameFromToken(token);
    }
}
