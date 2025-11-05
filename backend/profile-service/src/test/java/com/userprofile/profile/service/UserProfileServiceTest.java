package com.userprofile.profile.service;

import com.userprofile.profile.dto.UserProfileDTO;
import com.userprofile.profile.engine.ProfileCalculationEngine;
import com.userprofile.profile.entity.UserProfile;
import com.userprofile.profile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * UserProfileService单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService测试")
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private ProfileCalculationEngine calculationEngine;

    @InjectMocks
    private UserProfileService profileService;

    private UserProfile testProfile;
    private UserProfileDTO testProfileDTO;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testProfile = new UserProfile();
        testProfile.setUserId(1L);
        testProfile.setUsername("testuser");
        testProfile.setProfileScore(85.5);
        testProfile.setCreateTime(LocalDateTime.now());
        testProfile.setUpdateTime(LocalDateTime.now());

        // 数字行为
        UserProfile.DigitalBehavior behavior = new UserProfile.DigitalBehavior();
        behavior.setProductCategories(Arrays.asList("电子产品", "图书"));
        behavior.setInfoAcquisitionHabit("搜索引擎");
        testProfile.setDigitalBehavior(behavior);

        // 价值评估
        UserProfile.ValueAssessment valueAssessment = new UserProfile.ValueAssessment();
        valueAssessment.setProfileQuality("高");
        valueAssessment.setConsumptionLevel("中高");
        testProfile.setValueAssessment(valueAssessment);

        // 准备DTO
        testProfileDTO = new UserProfileDTO();
        testProfileDTO.setUserId(1L);
        testProfileDTO.setUsername("testuser");

        UserProfileDTO.DigitalBehaviorDTO behaviorDTO = new UserProfileDTO.DigitalBehaviorDTO();
        behaviorDTO.setProductCategories(Arrays.asList("电子产品", "图书"));
        behaviorDTO.setInfoAcquisitionHabit("搜索引擎");
        testProfileDTO.setDigitalBehavior(behaviorDTO);

        UserProfileDTO.ValueAssessmentDTO valueDTO = new UserProfileDTO.ValueAssessmentDTO();
        valueDTO.setProfileQuality("高");
        valueDTO.setConsumptionLevel("中高");
        testProfileDTO.setValueAssessment(valueDTO);
    }

    @Test
    @DisplayName("创建新用户画像 - 成功")
    void createOrUpdateProfile_CreateNew_Success() {
        // Given
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(calculationEngine.calculateProfileScore(any(UserProfile.class))).thenReturn(85.5);
        when(profileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

        // When
        UserProfile result = profileService.createOrUpdateProfile(testProfileDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getProfileScore()).isEqualTo(85.5);

        // 验证方法调用
        verify(profileRepository, times(1)).findByUserId(1L);
        verify(calculationEngine, times(1)).calculateProfileScore(any(UserProfile.class));
        verify(profileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("更新已有用户画像 - 成功")
    void createOrUpdateProfile_UpdateExisting_Success() {
        // Given
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(calculationEngine.calculateProfileScore(any(UserProfile.class))).thenReturn(90.0);
        when(profileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

        // When
        UserProfile result = profileService.createOrUpdateProfile(testProfileDTO);

        // Then
        assertThat(result).isNotNull();
        verify(profileRepository, times(1)).findByUserId(1L);
        verify(profileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("创建画像 - 字段映射验证(P0-1修复)")
    void createOrUpdateProfile_FieldMapping_Correct() {
        // Given
        when(profileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(calculationEngine.calculateProfileScore(any())).thenReturn(85.5);
        when(profileRepository.save(any())).thenReturn(testProfile);

        // When
        profileService.createOrUpdateProfile(testProfileDTO);

        // Then
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(captor.capture());

        UserProfile savedProfile = captor.getValue();
        assertThat(savedProfile.getValueAssessment()).isNotNull();
        assertThat(savedProfile.getValueAssessment().getProfileQuality()).isEqualTo("高");
        assertThat(savedProfile.getValueAssessment().getConsumptionLevel()).isEqualTo("中高");
    }

    @Test
    @DisplayName("根据用户ID获取画像 - 成功")
    void getProfileByUserId_Found_Success() {
        // Given
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        // When
        UserProfile result = profileService.getProfileByUserId(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(profileRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("根据用户ID获取画像 - 不存在抛出异常")
    void getProfileByUserId_NotFound_ThrowException() {
        // Given
        when(profileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> profileService.getProfileByUserId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("用户画像不存在");

        verify(profileRepository, times(1)).findByUserId(999L);
    }

    @Test
    @DisplayName("根据用户ID获取画像(可选) - 不存在返回null")
    void getProfileByUserIdOptional_NotFound_ReturnNull() {
        // Given
        when(profileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // When
        UserProfile result = profileService.getProfileByUserIdOptional(999L);

        // Then
        assertThat(result).isNull();
        verify(profileRepository, times(1)).findByUserId(999L);
    }

    @Test
    @DisplayName("获取所有画像 - 已废弃方法(P1-4修复)")
    @SuppressWarnings("deprecation")
    void getAllProfiles_Deprecated_LimitResults() {
        // Given
        List<UserProfile> profiles = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            UserProfile profile = new UserProfile();
            profile.setUserId((long) i);
            profile.setUpdateTime(LocalDateTime.now().minusDays(i));
            profiles.add(profile);
        }
        when(profileRepository.findAll()).thenReturn(profiles);

        // When
        List<UserProfile> result = profileService.getAllProfiles();

        // Then
        assertThat(result).hasSize(100); // 限制返回100条
        verify(profileRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("删除用户画像 - 成功")
    void deleteProfile_Success() {
        // Given
        doNothing().when(profileRepository).deleteByUserId(1L);

        // When
        profileService.deleteProfile(1L);

        // Then
        verify(profileRepository, times(1)).deleteByUserId(1L);
    }

    @Test
    @DisplayName("分析用户类型 - 成功")
    void analyzeUserType_Success() {
        // Given
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(calculationEngine.analyzeUserType(testProfile)).thenReturn("高价值用户");

        // When
        String result = profileService.analyzeUserType(1L);

        // Then
        assertThat(result).isEqualTo("高价值用户");
        verify(calculationEngine, times(1)).analyzeUserType(testProfile);
    }

    @Test
    @DisplayName("生成用户标签 - 成功")
    void generateUserTags_Success() {
        // Given
        List<String> expectedTags = Arrays.asList("高消费", "科技爱好者", "活跃用户");
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(calculationEngine.generateUserTags(testProfile)).thenReturn(expectedTags);

        // When
        List<String> result = profileService.generateUserTags(1L);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).contains("高消费", "科技爱好者", "活跃用户");
        verify(calculationEngine, times(1)).generateUserTags(testProfile);
    }

    @Test
    @DisplayName("推荐营销策略 - 成功")
    void recommendStrategy_Success() {
        // Given
        Map<String, String> expectedStrategy = new HashMap<>();
        expectedStrategy.put("优惠券", "电子产品8折券");
        expectedStrategy.put("推荐商品", "最新款手机");

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(calculationEngine.recommendStrategy(testProfile)).thenReturn(expectedStrategy);

        // When
        Map<String, String> result = profileService.recommendStrategy(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("优惠券")).isEqualTo("电子产品8折券");
        verify(calculationEngine, times(1)).recommendStrategy(testProfile);
    }

    @Test
    @DisplayName("重新计算画像评分 - 成功")
    void recalculateScore_Success() {
        // Given
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(calculationEngine.calculateProfileScore(testProfile)).thenReturn(92.0);
        when(profileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

        // When
        UserProfile result = profileService.recalculateScore(1L);

        // Then
        assertThat(result).isNotNull();
        verify(calculationEngine, times(1)).calculateProfileScore(testProfile);
        verify(profileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("创建画像 - 所有字段完整映射")
    void createOrUpdateProfile_AllFields_Complete() {
        // Given - 完整的DTO
        UserProfileDTO completeDTO = new UserProfileDTO();
        completeDTO.setUserId(2L);
        completeDTO.setUsername("complete_user");

        // 数字行为
        UserProfileDTO.DigitalBehaviorDTO behaviorDTO = new UserProfileDTO.DigitalBehaviorDTO();
        behaviorDTO.setProductCategories(Arrays.asList("家电", "服饰"));
        behaviorDTO.setInfoAcquisitionHabit("社交媒体");
        behaviorDTO.setPurchaseDecisionPreference("价格敏感");
        behaviorDTO.setBrandPreferences(Arrays.asList("小米", "华为"));
        completeDTO.setDigitalBehavior(behaviorDTO);

        // 核心需求
        UserProfileDTO.CoreNeedsDTO coreNeedsDTO = new UserProfileDTO.CoreNeedsDTO();
        coreNeedsDTO.setTopConcerns(Arrays.asList("性价比", "品质"));
        coreNeedsDTO.setDecisionPainPoint("选择困难");
        completeDTO.setCoreNeeds(coreNeedsDTO);

        // 价值评估
        UserProfileDTO.ValueAssessmentDTO valueDTO = new UserProfileDTO.ValueAssessmentDTO();
        valueDTO.setProfileQuality("中");
        valueDTO.setConsumptionLevel("中");
        valueDTO.setPreferenceAnalysis("偏好国产品牌");
        valueDTO.setAvgOrderValue("500元");
        valueDTO.setFeedingMethod("自然增长");
        completeDTO.setValueAssessment(valueDTO);

        // 粘性
        UserProfileDTO.StickinessDTO stickinessDTO = new UserProfileDTO.StickinessDTO();
        stickinessDTO.setLoyaltyScore(75);
        completeDTO.setStickiness(stickinessDTO);

        when(profileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(calculationEngine.calculateProfileScore(any())).thenReturn(75.0);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserProfile result = profileService.createOrUpdateProfile(completeDTO);

        // Then - 验证所有字段都正确映射
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(captor.capture());

        UserProfile saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(2L);
        assertThat(saved.getUsername()).isEqualTo("complete_user");

        // 验证数字行为
        assertThat(saved.getDigitalBehavior()).isNotNull();
        assertThat(saved.getDigitalBehavior().getProductCategories()).contains("家电", "服饰");
        assertThat(saved.getDigitalBehavior().getBrandPreferences()).contains("小米", "华为");

        // 验证核心需求
        assertThat(saved.getCoreNeeds()).isNotNull();
        assertThat(saved.getCoreNeeds().getTopConcerns()).contains("性价比", "品质");

        // 验证价值评估(关键字段映射)
        assertThat(saved.getValueAssessment()).isNotNull();
        assertThat(saved.getValueAssessment().getProfileQuality()).isEqualTo("中");
        assertThat(saved.getValueAssessment().getConsumptionLevel()).isEqualTo("中");
        assertThat(saved.getValueAssessment().getAvgOrderValue()).isEqualTo("500元");
        assertThat(saved.getValueAssessment().getFeedingMethod()).isEqualTo("自然增长");

        // 验证粘性
        assertThat(saved.getStickinessAndLoyalty()).isNotNull();
        assertThat(saved.getStickinessAndLoyalty().getLoyaltyScore()).isEqualTo(75);
    }

    @Test
    @DisplayName("创建画像 - 部分字段为空也能正常处理")
    void createOrUpdateProfile_PartialFields_Success() {
        // Given - 只有基本字段的DTO
        UserProfileDTO minimalDTO = new UserProfileDTO();
        minimalDTO.setUserId(3L);
        minimalDTO.setUsername("minimal_user");
        // 其他字段为null

        when(profileRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(calculationEngine.calculateProfileScore(any())).thenReturn(50.0);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserProfile result = profileService.createOrUpdateProfile(minimalDTO);

        // Then
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(captor.capture());

        UserProfile saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(3L);
        assertThat(saved.getUsername()).isEqualTo("minimal_user");
        // 空字段不会导致错误
        assertThat(saved.getDigitalBehavior()).isNull();
        assertThat(saved.getCoreNeeds()).isNull();
    }
}
