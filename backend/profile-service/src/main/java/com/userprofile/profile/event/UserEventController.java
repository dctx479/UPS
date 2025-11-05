package com.userprofile.profile.event;

import com.userprofile.common.response.Result;
import com.userprofile.profile.analytics.UserBehaviorAnalytics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户事件控制器
 */
@Tag(name = "用户事件追踪", description = "用户行为事件记录和分析")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class UserEventController {

    private final UserEventService eventService;
    private final UserBehaviorAnalytics behaviorAnalytics;

    @Operation(summary = "记录用户事件")
    @PostMapping
    public Result<UserEvent> trackEvent(@RequestBody UserEvent event) {
        UserEvent saved = eventService.saveEvent(event);
        return Result.success("事件记录成功", saved);
    }

    @Operation(summary = "批量记录事件")
    @PostMapping("/batch")
    public Result<List<UserEvent>> trackEvents(@RequestBody List<UserEvent> events) {
        List<UserEvent> saved = eventService.saveEvents(events);
        return Result.success("批量记录成功", saved);
    }

    @Operation(summary = "获取用户事件列表")
    @GetMapping("/user/{userId}")
    public Result<List<UserEvent>> getUserEvents(@PathVariable Long userId) {
        List<UserEvent> events = eventService.getUserEvents(userId);
        return Result.success(events);
    }

    @Operation(summary = "计算用户活跃度")
    @GetMapping("/user/{userId}/activity")
    public Result<Map<String, Object>> getUserActivity(@PathVariable Long userId) {
        double score = behaviorAnalytics.calculateActivityScore(userId);
        return Result.success(Map.of("activityScore", score));
    }

    @Operation(summary = "分析用户兴趣偏好")
    @GetMapping("/user/{userId}/interests")
    public Result<Map<String, Double>> analyzeInterests(@PathVariable Long userId) {
        Map<String, Double> interests = behaviorAnalytics.analyzeInterests(userId);
        return Result.success(interests);
    }

    @Operation(summary = "计算RFM模型")
    @GetMapping("/user/{userId}/rfm")
    public Result<Map<String, Object>> calculateRFM(@PathVariable Long userId) {
        Map<String, Object> rfm = behaviorAnalytics.calculateRFM(userId);
        return Result.success(rfm);
    }

    @Operation(summary = "预测流失风险")
    @GetMapping("/user/{userId}/churn-risk")
    public Result<Map<String, Object>> predictChurnRisk(@PathVariable Long userId) {
        Map<String, Object> risk = behaviorAnalytics.predictChurnRisk(userId);
        return Result.success(risk);
    }

    @Operation(summary = "分析购物漏斗")
    @GetMapping("/user/{userId}/funnel")
    public Result<Map<String, Object>> analyzeFunnel(@PathVariable Long userId) {
        Map<String, Object> funnel = behaviorAnalytics.analyzePurchaseFunnel(userId);
        return Result.success(funnel);
    }

    @Operation(summary = "获取购买转化率")
    @GetMapping("/user/{userId}/conversion")
    public Result<Map<String, Object>> getConversionRate(@PathVariable Long userId) {
        double rate = behaviorAnalytics.calculateConversionRate(userId);
        return Result.success(Map.of("conversionRate", rate));
    }
}
