package com.userprofile.profile.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userprofile.profile.dto.InitialProfileRequest;
import com.userprofile.profile.dto.UserProfileDTO;
import com.userprofile.profile.entity.UserProfile;
import com.userprofile.profile.repository.UserProfileRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户画像服务集成测试
 * P2-1修复: 端到端业务流程测试
 *
 * @author User Profile Team
 * @version 1.6.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("用户画像服务集成测试")
class ProfileServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProfileRepository profileRepository;

    private static final Long TEST_USER_ID = 1000L;
    private static final String TEST_USERNAME = "profiletest";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        profileRepository.deleteByUserId(TEST_USER_ID);
    }

    @Test
    @Order(1)
    @DisplayName("集成测试 1: 初始化用户画像")
    void testInitializeProfileFlow() throws Exception {
        // 1. 准备初始化数据
        InitialProfileRequest request = InitialProfileRequest.builder()
                .userId(TEST_USER_ID)
                .username(TEST_USERNAME)
                .email("profile@test.com")
                .name("Profile Test User")
                .build();

        // 2. 发送初始化请求
        mockMvc.perform(post("/api/profiles/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.profileScore").value(0.0));

        // 3. 验证画像已保存
        UserProfile savedProfile = profileRepository.findByUserId(TEST_USER_ID).orElse(null);
        assertThat(savedProfile).isNotNull();
        assertThat(savedProfile.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedProfile.getProfileScore()).isEqualTo(0.0);
    }

    @Test
    @Order(2)
    @DisplayName("集成测试 2: 完善用户画像")
    void testUpdateProfileFlow() throws Exception {
        // 1. 准备完整的画像数据
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(TEST_USER_ID);
        dto.setUsername(TEST_USERNAME);

        // 数字行为
        UserProfileDTO.DigitalBehaviorDTO behavior = new UserProfileDTO.DigitalBehaviorDTO();
        behavior.setProductCategories(Arrays.asList("宠物食品", "宠物用品", "医疗保健"));
        behavior.setInfoAcquisitionHabit("搜索引擎+社交媒体");
        behavior.setPurchaseDecisionPreference("品牌口碑");
        behavior.setBrandPreferences(Arrays.asList("皇家", "冠能", "希尔斯"));
        dto.setDigitalBehavior(behavior);

        // 核心需求
        UserProfileDTO.CoreNeedsDTO needs = new UserProfileDTO.CoreNeedsDTO();
        needs.setTopConcerns(Arrays.asList("宠物健康", "食品安全", "性价比"));
        needs.setDecisionPainPoint("选择困难症");
        dto.setCoreNeeds(needs);

        // 价值评估
        UserProfileDTO.ValueAssessmentDTO value = new UserProfileDTO.ValueAssessmentDTO();
        value.setConsumptionLevel("MEDIUM");
        // preferenceAnalysis 是 Map<String, Double>
        value.setPreferenceAnalysis(java.util.Map.of("organic", 0.8, "imported", 0.6));
        // avgOrderValue 是 String
        value.setAvgOrderValue("500.0");
        dto.setValueAssessment(value);

        // 粘性
        UserProfileDTO.StickinessDTO stickiness = new UserProfileDTO.StickinessDTO();
        stickiness.setLoyaltyScore(85.0);
        dto.setStickiness(stickiness);

        // 2. 发送更新请求
        MvcResult result = mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.data.profileScore").exists())
                .andReturn();

        // 3. 验证评分已计算
        String responseBody = result.getResponse().getContentAsString();
        double score = objectMapper.readTree(responseBody)
                .path("data")
                .path("profileScore")
                .asDouble();
        assertThat(score).isGreaterThan(0.0);
    }

    @Test
    @Order(3)
    @DisplayName("集成测试 3: 查询用户画像")
    void testGetProfileFlow() throws Exception {
        mockMvc.perform(get("/api/profiles/user/" + TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.data.digitalBehavior.productCategories").isArray())
                .andExpect(jsonPath("$.data.valueAssessment.profileQuality").value("HIGH"));
    }

    @Test
    @Order(4)
    @DisplayName("集成测试 4: 分析用户类型")
    void testAnalyzeUserTypeFlow() throws Exception {
        mockMvc.perform(get("/api/profiles/user/" + TEST_USER_ID + "/type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userType").exists());
    }

    @Test
    @Order(5)
    @DisplayName("集成测试 5: 生成用户标签")
    void testGenerateUserTagsFlow() throws Exception {
        mockMvc.perform(get("/api/profiles/user/" + TEST_USER_ID + "/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("集成测试 6: 推荐营销策略")
    void testRecommendStrategyFlow() throws Exception {
        mockMvc.perform(get("/api/profiles/user/" + TEST_USER_ID + "/strategy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @Order(7)
    @DisplayName("集成测试 7: 重新计算评分")
    void testRecalculateScoreFlow() throws Exception {
        mockMvc.perform(put("/api/profiles/user/" + TEST_USER_ID + "/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.profileScore").exists());
    }

    @Test
    @Order(8)
    @DisplayName("集成测试 8: 查询画像统计信息")
    void testGetStatisticsFlow() throws Exception {
        mockMvc.perform(get("/api/profiles/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalProfiles").exists())
                .andExpect(jsonPath("$.data.averageScore").exists());
    }

    @Test
    @Order(9)
    @DisplayName("集成测试 9: 查询不存在的画像")
    void testGetNonExistentProfile() throws Exception {
        Long nonExistentUserId = 999999L;
        mockMvc.perform(get("/api/profiles/user/" + nonExistentUserId))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @Order(10)
    @DisplayName("集成测试 10: 删除用户画像")
    void testDeleteProfileFlow() throws Exception {
        mockMvc.perform(delete("/api/profiles/user/" + TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证画像已删除
        boolean exists = profileRepository.findByUserId(TEST_USER_ID).isPresent();
        assertThat(exists).isFalse();
    }
}
