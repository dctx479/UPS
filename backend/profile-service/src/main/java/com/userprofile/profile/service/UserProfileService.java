package com.userprofile.profile.service;

import com.userprofile.profile.dto.InitialProfileRequest;
import com.userprofile.profile.dto.UserProfileDTO;
import com.userprofile.profile.engine.ProfileCalculationEngine;
import com.userprofile.profile.entity.UserProfile;
import com.userprofile.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户画像服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final ProfileCalculationEngine calculationEngine;

    /**
     * 创建或更新用户画像
     */
    @Transactional
    @CachePut(value = "userProfiles", key = "#dto.userId")
    public UserProfile createOrUpdateProfile(UserProfileDTO dto) {
        UserProfile profile = profileRepository.findByUserId(dto.getUserId())
                .orElse(new UserProfile());

        // 基本信息
        profile.setUserId(dto.getUserId());
        profile.setUsername(dto.getUsername());

        // 数字行为
        if (dto.getDigitalBehavior() != null) {
            UserProfile.DigitalBehavior behavior = new UserProfile.DigitalBehavior();
            behavior.setProductCategories(dto.getDigitalBehavior().getProductCategories());
            behavior.setInfoAcquisitionHabit(dto.getDigitalBehavior().getInfoAcquisitionHabit());
            behavior.setPurchaseDecisionPreference(dto.getDigitalBehavior().getPurchaseDecisionPreference());
            behavior.setBrandPreferences(dto.getDigitalBehavior().getBrandPreferences());
            profile.setDigitalBehavior(behavior);
        }

        // 核心需求
        if (dto.getCoreNeeds() != null) {
            UserProfile.CoreNeeds coreNeeds = new UserProfile.CoreNeeds();
            coreNeeds.setTopConcerns(dto.getCoreNeeds().getTopConcerns());
            coreNeeds.setDecisionPainPoint(dto.getCoreNeeds().getDecisionPainPoint());
            profile.setCoreNeeds(coreNeeds);
        }

        // 价值评估
        if (dto.getValueAssessment() != null) {
            UserProfile.ValueAssessment valueAssessment = new UserProfile.ValueAssessment();
            // 修复字段映射错误 - P0-1
            valueAssessment.setProfileQuality(dto.getValueAssessment().getProfileQuality());
            valueAssessment.setConsumptionLevel(dto.getValueAssessment().getConsumptionLevel());
            valueAssessment.setPreferenceAnalysis(dto.getValueAssessment().getPreferenceAnalysis());
            valueAssessment.setAvgOrderValue(dto.getValueAssessment().getAvgOrderValue());
            valueAssessment.setFeedingMethod(dto.getValueAssessment().getFeedingMethod());
            profile.setValueAssessment(valueAssessment);
        }

        // 粘性
        if (dto.getStickiness() != null) {
            UserProfile.StickinessAndLoyalty stickiness = new UserProfile.StickinessAndLoyalty();
            stickiness.setLoyaltyScore(dto.getStickiness().getLoyaltyScore());
            profile.setStickinessAndLoyalty(stickiness);
        }

        // 计算画像评分
        double score = calculationEngine.calculateProfileScore(profile);
        profile.setProfileScore(score);

        // 时间戳
        if (profile.getCreateTime() == null) {
            profile.setCreateTime(LocalDateTime.now());
        }
        profile.setUpdateTime(LocalDateTime.now());

        log.info("保存用户画像: userId={}, score={}", dto.getUserId(), score);
        return profileRepository.save(profile);
    }

    /**
     * 初始化用户画像
     * P1-2修复: 用户注册时创建初始画像
     */
    @Transactional
    @CachePut(value = "userProfiles", key = "#request.userId")
    public UserProfile initializeProfile(InitialProfileRequest request) {
        log.info("初始化用户画像: userId={}, username={}", request.getUserId(), request.getUsername());

        // 检查是否已存在
        if (profileRepository.findByUserId(request.getUserId()).isPresent()) {
            log.warn("用户画像已存在,跳过初始化: userId={}", request.getUserId());
            return profileRepository.findByUserId(request.getUserId()).get();
        }

        // 创建初始画像
        UserProfile profile = new UserProfile();
        profile.setUserId(request.getUserId());
        profile.setUsername(request.getUsername());

        // 初始化默认值
        profile.setProfileScore(0.0);
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());

        // 初始化空的数字行为
        UserProfile.DigitalBehavior behavior = new UserProfile.DigitalBehavior();
        behavior.setProductCategories(List.of());
        behavior.setBrandPreferences(List.of());
        profile.setDigitalBehavior(behavior);

        // 初始化空的核心需求
        UserProfile.CoreNeeds coreNeeds = new UserProfile.CoreNeeds();
        coreNeeds.setTopConcerns(List.of());
        profile.setCoreNeeds(coreNeeds);

        // 初始化空的价值评估
        UserProfile.ValueAssessment valueAssessment = new UserProfile.ValueAssessment();
        valueAssessment.setProfileQuality("INCOMPLETE");
        valueAssessment.setConsumptionLevel("UNKNOWN");
        profile.setValueAssessment(valueAssessment);

        // 初始化空的粘性
        UserProfile.StickinessAndLoyalty stickiness = new UserProfile.StickinessAndLoyalty();
        stickiness.setLoyaltyScore(0.0);
        profile.setStickinessAndLoyalty(stickiness);

        UserProfile saved = profileRepository.save(profile);
        log.info("用户画像初始化成功: userId={}", request.getUserId());
        return saved;
    }

    /**
     * 获取用户画像
     * 添加sync=true防止缓存击穿(P2-4修复)
     */
    @Cacheable(value = "userProfiles", key = "#userId", unless = "#result == null", sync = true)
    public UserProfile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("用户画像不存在"));
    }

    /**
     * 获取用户画像（可选）
     * 添加sync=true防止缓存击穿(P2-4修复)
     */
    @Cacheable(value = "userProfiles", key = "#userId", unless = "#result == null", sync = true)
    public UserProfile getProfileByUserIdOptional(Long userId) {
        return profileRepository.findByUserId(userId).orElse(null);
    }

    /**
     * 获取所有画像 (已废弃,请使用分页查询)
     * @deprecated 为避免数据量过大,请添加分页查询方法
     */
    @Deprecated
    public List<UserProfile> getAllProfiles() {
        log.warn("调用了废弃的getAllProfiles()方法,建议实现分页查询");
        // 限制返回最近100条,避免数据量过大
        return profileRepository.findAll().stream()
                .sorted((p1, p2) -> p2.getUpdateTime().compareTo(p1.getUpdateTime()))
                .limit(100)
                .toList();
    }

    /**
     * 分页查询用户画像列表
     * P1-6修复: 替代getAllProfiles()的分页方法
     */
    public Page<UserProfile> getProfileList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "updateTime"));
        return profileRepository.findAll(pageRequest);
    }

    /**
     * 删除用户画像
     */
    @Transactional
    @CacheEvict(value = "userProfiles", key = "#userId")
    public void deleteProfile(Long userId) {
        profileRepository.deleteByUserId(userId);
        log.info("删除用户画像: userId={}", userId);
    }

    /**
     * 分析用户类型
     */
    public String analyzeUserType(Long userId) {
        UserProfile profile = getProfileByUserId(userId);
        return calculationEngine.analyzeUserType(profile);
    }

    /**
     * 生成用户标签
     */
    public List<String> generateUserTags(Long userId) {
        UserProfile profile = getProfileByUserId(userId);
        return calculationEngine.generateUserTags(profile);
    }

    /**
     * 推荐营销策略
     */
    public Map<String, String> recommendStrategy(Long userId) {
        UserProfile profile = getProfileByUserId(userId);
        return calculationEngine.recommendStrategy(profile);
    }

    /**
     * 重新计算画像评分
     */
    @Transactional
    @CachePut(value = "userProfiles", key = "#userId")
    public UserProfile recalculateScore(Long userId) {
        UserProfile profile = getProfileByUserId(userId);
        double score = calculationEngine.calculateProfileScore(profile);
        profile.setProfileScore(score);
        profile.setUpdateTime(LocalDateTime.now());
        return profileRepository.save(profile);
    }
}
