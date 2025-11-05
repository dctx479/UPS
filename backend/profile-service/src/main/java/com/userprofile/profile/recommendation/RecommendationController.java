package com.userprofile.profile.recommendation;

import com.userprofile.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐引擎控制器
 */
@Tag(name = "个性化推荐", description = "基于用户画像的智能推荐系统")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationEngine recommendationEngine;

    @Operation(summary = "协同过滤推荐")
    @GetMapping("/user/{userId}/collaborative")
    public Result<List<RecommendationResult>> collaborativeFiltering(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<RecommendationResult> results = recommendationEngine.collaborativeFilteringByUser(userId, limit);
        return Result.success(results);
    }

    @Operation(summary = "基于内容推荐")
    @GetMapping("/user/{userId}/content-based")
    public Result<List<RecommendationResult>> contentBased(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<RecommendationResult> results = recommendationEngine.contentBasedRecommendation(userId, limit);
        return Result.success(results);
    }

    @Operation(summary = "热门商品推荐")
    @GetMapping("/trending")
    public Result<List<RecommendationResult>> trending(
            @RequestParam(defaultValue = "10") int limit) {
        List<RecommendationResult> results = recommendationEngine.trendingRecommendation(limit);
        return Result.success(results);
    }

    @Operation(summary = "智能混合推荐（推荐使用）")
    @GetMapping("/user/{userId}/hybrid")
    public Result<List<RecommendationResult>> hybrid(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<RecommendationResult> results = recommendationEngine.hybridRecommendation(userId, limit);
        return Result.success(results);
    }
}
