package com.userprofile.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userprofile.user.dto.LoginRequest;
import com.userprofile.user.dto.UserDTO;
import com.userprofile.user.entity.User;
import com.userprofile.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户服务集成测试
 * P2-1修复: 端到端业务流程测试
 *
 * @author User Profile Team
 * @version 1.6.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("用户服务集成测试")
class UserServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static String authToken;
    private static Long createdUserId;

    @BeforeEach
    void setUp() {
        // 每个测试前清理数据
        if (createdUserId == null) {
            userRepository.deleteAll();
        }
    }

    @Test
    @Order(1)
    @DisplayName("集成测试 1: 用户注册流程")
    void testUserRegistrationFlow() throws Exception {
        // 1. 准备注册数据
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("integrationtest");
        userDTO.setPassword("Test@123456");
        userDTO.setEmail("integration@test.com");
        userDTO.setName("Integration Test User");

        // 2. 发送注册请求
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("用户创建成功"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value("integrationtest"))
                .andExpect(jsonPath("$.data.email").value("integration@test.com"))
                .andReturn();

        // 3. 提取创建的用户ID
        String responseBody = result.getResponse().getContentAsString();
        createdUserId = objectMapper.readTree(responseBody)
                .path("data")
                .path("id")
                .asLong();

        assertThat(createdUserId).isNotNull().isPositive();

        // 4. 验证用户已保存到数据库
        User savedUser = userRepository.findById(createdUserId).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("integrationtest");
        assertThat(savedUser.getEmail()).isEqualTo("integration@test.com");
    }

    @Test
    @Order(2)
    @DisplayName("集成测试 2: 用户登录流程")
    void testUserLoginFlow() throws Exception {
        // 1. 准备登录数据
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("integrationtest");
        loginRequest.setPassword("Test@123456");

        // 2. 发送登录请求
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.username").value("integrationtest"))
                .andReturn();

        // 3. 提取访问令牌
        String responseBody = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(responseBody)
                .path("data")
                .path("accessToken")
                .asText();

        assertThat(authToken).isNotNull().isNotEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("集成测试 3: 使用Token访问受保护资源")
    void testAccessProtectedResourceWithToken() throws Exception {
        // 使用Token查询用户信息
        mockMvc.perform(get("/api/users/" + createdUserId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(createdUserId))
                .andExpect(jsonPath("$.data.username").value("integrationtest"));
    }

    @Test
    @Order(4)
    @DisplayName("集成测试 4: 更新用户信息")
    void testUpdateUserFlow() throws Exception {
        // 1. 准备更新数据
        UserDTO updateDTO = new UserDTO();
        updateDTO.setEmail("updated@test.com");
        updateDTO.setName("Updated Name");

        // 2. 发送更新请求
        mockMvc.perform(put("/api/users/" + createdUserId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("updated@test.com"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));

        // 3. 验证数据库中已更新
        User updatedUser = userRepository.findById(createdUserId).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getEmail()).isEqualTo("updated@test.com");
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
    }

    @Test
    @Order(5)
    @DisplayName("集成测试 5: 查询用户列表")
    void testGetUserListFlow() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + authToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("集成测试 6: 无效Token访问失败")
    void testAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/" + createdUserId)
                        .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("集成测试 7: 重复用户名注册失败")
    void testDuplicateUsernameRegistration() throws Exception {
        UserDTO duplicateUser = new UserDTO();
        duplicateUser.setUsername("integrationtest");
        duplicateUser.setPassword("Test@123456");
        duplicateUser.setEmail("another@test.com");
        duplicateUser.setName("Another User");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(8)
    @DisplayName("集成测试 8: 弱密码注册失败")
    void testWeakPasswordRegistration() throws Exception {
        UserDTO weakPasswordUser = new UserDTO();
        weakPasswordUser.setUsername("weakpassworduser");
        weakPasswordUser.setPassword("123456");
        weakPasswordUser.setEmail("weak@test.com");
        weakPasswordUser.setName("Weak User");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakPasswordUser)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(9)
    @DisplayName("集成测试 9: 错误密码登录失败")
    void testLoginWithWrongPassword() throws Exception {
        LoginRequest wrongPasswordRequest = new LoginRequest();
        wrongPasswordRequest.setUsername("integrationtest");
        wrongPasswordRequest.setPassword("WrongPassword@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(10)
    @DisplayName("集成测试 10: 删除用户")
    void testDeleteUserFlow() throws Exception {
        mockMvc.perform(delete("/api/users/" + createdUserId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证用户已被删除
        boolean exists = userRepository.existsById(createdUserId);
        assertThat(exists).isFalse();
    }
}
