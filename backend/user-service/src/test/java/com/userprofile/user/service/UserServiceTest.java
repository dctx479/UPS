package com.userprofile.user.service;

import com.userprofile.common.exception.BusinessException;
import com.userprofile.user.dto.UserDTO;
import com.userprofile.user.entity.User;
import com.userprofile.user.event.UserCreatedEvent;
import com.userprofile.user.event.UserUpdatedEvent;
import com.userprofile.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserService单元测试
 *
 * @author User Profile Team
 * @version 1.5.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService单元测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private UserDTO testUserDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testUserDTO = new UserDTO();
        testUserDTO.setUsername("testuser");
        testUserDTO.setPassword("password123");
        testUserDTO.setName("Test User");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setAge(25);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setStatus(1);
    }

    @Test
    @DisplayName("创建用户 - 成功")
    void createUser_Success() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser(testUserDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPassword()).isEqualTo("$2a$10$encodedPassword");

        // 验证密码加密
        verify(passwordEncoder, times(1)).encode("password123");

        // 验证保存用户
        verify(userRepository, times(1)).save(any(User.class));

        // 验证发布事件
        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        UserCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getUserId()).isEqualTo(1L);
        assertThat(capturedEvent.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("创建用户 - 用户名已存在")
    void createUser_UsernameExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(testUserDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");

        // 验证没有保存
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("创建用户 - 密码为空")
    void createUser_EmptyPassword() {
        // Given
        testUserDTO.setPassword("");

        // When & Then
        assertThatThrownBy(() -> userService.createUser(testUserDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能为空");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("创建用户 - 密码为null")
    void createUser_NullPassword() {
        // Given
        testUserDTO.setPassword(null);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(testUserDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能为空");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("更新用户 - 成功")
    void updateUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO updateDTO = new UserDTO();
        updateDTO.setUsername("testuser");
        updateDTO.setName("Updated Name");
        updateDTO.setEmail("updated@example.com");

        // When
        User result = userService.updateUser(1L, updateDTO);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository, times(1)).save(any(User.class));

        // 验证发布更新事件
        ArgumentCaptor<UserUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(UserUpdatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        UserUpdatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("更新用户 - 用户不存在")
    void updateUser_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, testUserDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("更新用户 - 更新密码")
    void updateUser_UpdatePassword() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO updateDTO = new UserDTO();
        updateDTO.setUsername("testuser");
        updateDTO.setPassword("newpassword456");

        // When
        userService.updateUser(1L, updateDTO);

        // Then
        verify(passwordEncoder, times(1)).encode("newpassword456");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("根据ID查询用户 - 成功")
    void getUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("根据ID查询用户 - 用户不存在")
    void getUserById_NotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("根据用户名查询用户 - 成功")
    void getUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("删除用户 - 成功")
    void deleteUser_Success() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("删除用户 - 用户不存在")
    void deleteUser_NotFound() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

        verify(userRepository, never()).deleteById(any());
    }
}
