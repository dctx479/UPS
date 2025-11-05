package com.userprofile.profile.controller;

import com.userprofile.common.response.Result;
import com.userprofile.profile.dto.InitialProfileRequest;
import com.userprofile.profile.dto.UserProfileDTO;
import com.userprofile.profile.entity.UserProfile;
import com.userprofile.profile.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户画像控制器
 */
@Tag(name = "用户画像管理", description = "用户画像分析和管理接口")
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @Operation(summary = "创建或更新用户画像")
    @PostMapping
    public Result<UserProfile> createOrUpdateProfile(@RequestBody UserProfileDTO dto) {
        UserProfile profile = profileService.createOrUpdateProfile(dto);
        return Result.success("画像保存成功", profile);
    }

    @Operation(summary = "初始化用户画像", description = "P1-2修复: 用户注册时创建初始画像")
    @PostMapping("/initialize")
    public Result<UserProfile> initializeProfile(@RequestBody InitialProfileRequest request) {
        UserProfile profile = profileService.initializeProfile(request);
        return Result.success("画像初始化成功", profile);
    }

    @Operation(summary = "根据用户ID获取画像")
    @GetMapping("/user/{userId}")
    public Result<UserProfile> getProfileByUserId(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        UserProfile profile = profileService.getProfileByUserId(userId);
        return Result.success(profile);
    }

    @Operation(summary = "获取所有用户画像")
    @GetMapping
    public Result<List<UserProfile>> getAllProfiles() {
        List<UserProfile> profiles = profileService.getAllProfiles();
        return Result.success(profiles);
    }

    @Operation(summary = "删除用户画像")
    @DeleteMapping("/user/{userId}")
    public Result<Void> deleteProfile(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        profileService.deleteProfile(userId);
        return Result.success("画像删除成功", null);
    }

    @Operation(summary = "分析用户类型")
    @GetMapping("/user/{userId}/type")
    public Result<Map<String, String>> analyzeUserType(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        String userType = profileService.analyzeUserType(userId);
        return Result.success(Map.of("userType", userType));
    }

    @Operation(summary = "生成用户标签")
    @GetMapping("/user/{userId}/tags")
    public Result<List<String>> generateUserTags(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        List<String> tags = profileService.generateUserTags(userId);
        return Result.success(tags);
    }

    @Operation(summary = "推荐营销策略")
    @GetMapping("/user/{userId}/strategy")
    public Result<Map<String, String>> recommendStrategy(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        Map<String, String> strategy = profileService.recommendStrategy(userId);
        return Result.success(strategy);
    }

    @Operation(summary = "重新计算画像评分")
    @PutMapping("/user/{userId}/recalculate")
    public Result<UserProfile> recalculateScore(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        UserProfile profile = profileService.recalculateScore(userId);
        return Result.success("评分重新计算完成", profile);
    }

    @Operation(summary = "获取画像统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        List<UserProfile> allProfiles = profileService.getAllProfiles();

        long total = allProfiles.size();
        double avgScore = allProfiles.stream()
                .filter(p -> p.getProfileScore() != null)
                .mapToDouble(UserProfile::getProfileScore)
                .average()
                .orElse(0.0);

        long highValueUsers = allProfiles.stream()
                .filter(p -> p.getProfileScore() != null && p.getProfileScore() >= 80)
                .count();

        Map<String, Object> statistics = Map.of(
                "totalProfiles", total,
                "averageScore", Math.round(avgScore * 100.0) / 100.0,
                "highValueUsers", highValueUsers,
                "highValueRate", total > 0 ? Math.round((highValueUsers * 100.0 / total) * 100.0) / 100.0 : 0.0
        );

        return Result.success(statistics);
    }
}
