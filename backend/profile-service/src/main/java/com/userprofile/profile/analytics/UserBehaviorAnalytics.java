package com.userprofile.profile.analytics;

import com.userprofile.profile.event.UserEvent;
import com.userprofile.profile.event.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户行为分析服务
 * 基于用户事件进行深度分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorAnalytics {

    private final UserEventRepository eventRepository;

    /**
     * 计算用户活跃度
     * 基于最近30天的行为频率
     */
    public double calculateActivityScore(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<UserEvent> recentEvents = eventRepository.findRecentEvents(userId, thirtyDaysAgo);

        if (recentEvents.isEmpty()) {
            return 0.0;
        }

        // 活跃天数
        long activeDays = recentEvents.stream()
                .map(e -> e.getEventTime().toLocalDate())
                .distinct()
                .count();

        // 事件总数
        int totalEvents = recentEvents.size();

        // 加权计算：活跃天数占60%，事件数量占40%
        double dayScore = Math.min((activeDays / 30.0) * 60, 60);
        double eventScore = Math.min((totalEvents / 100.0) * 40, 40);

        return Math.round((dayScore + eventScore) * 100.0) / 100.0;
    }

    /**
     * 分析用户兴趣偏好
     * 基于浏览和搜索行为
     */
    public Map<String, Double> analyzeInterests(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<UserEvent> events = eventRepository.findRecentEvents(userId, thirtyDaysAgo);

        Map<String, Integer> interestCount = new HashMap<>();

        for (UserEvent event : events) {
            if (event.getEventType() == UserEvent.EventType.PRODUCT_VIEW ||
                event.getEventType() == UserEvent.EventType.CATEGORY_VIEW ||
                event.getEventType() == UserEvent.EventType.SEARCH) {

                String category = (String) event.getEventData().get("category");
                if (category != null) {
                    interestCount.put(category, interestCount.getOrDefault(category, 0) + 1);
                }
            }
        }

        // 归一化为0-1的权重
        int maxCount = interestCount.values().stream().max(Integer::compareTo).orElse(1);
        Map<String, Double> interests = new HashMap<>();
        for (Map.Entry<String, Integer> entry : interestCount.entrySet()) {
            interests.put(entry.getKey(), (double) entry.getValue() / maxCount);
        }

        return interests;
    }

    /**
     * 计算购买转化率
     */
    public double calculateConversionRate(Long userId) {
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minus(90, ChronoUnit.DAYS);
        List<UserEvent> events = eventRepository.findRecentEvents(userId, ninetyDaysAgo);

        long viewCount = events.stream()
                .filter(e -> e.getEventType() == UserEvent.EventType.PRODUCT_VIEW)
                .count();

        long purchaseCount = events.stream()
                .filter(e -> e.getEventType() == UserEvent.EventType.PAY)
                .count();

        if (viewCount == 0) {
            return 0.0;
        }

        return Math.round((purchaseCount * 100.0 / viewCount) * 100.0) / 100.0;
    }

    /**
     * 分析用户购买周期
     * 返回平均购买间隔天数
     */
    public Double analyzePurchaseCycle(Long userId) {
        List<UserEvent> purchases = eventRepository.findByUserIdAndEventType(
                userId, UserEvent.EventType.PAY
        );

        if (purchases.size() < 2) {
            return null;
        }

        List<LocalDateTime> purchaseTimes = purchases.stream()
                .map(UserEvent::getEventTime)
                .sorted()
                .collect(Collectors.toList());

        long totalDays = 0;
        for (int i = 1; i < purchaseTimes.size(); i++) {
            long days = ChronoUnit.DAYS.between(purchaseTimes.get(i - 1), purchaseTimes.get(i));
            totalDays += days;
        }

        return (double) totalDays / (purchaseTimes.size() - 1);
    }

    /**
     * 计算用户价值（RFM模型）
     * R: Recency（最近一次购买）
     * F: Frequency（购买频率）
     * M: Monetary（购买金额）
     */
    public Map<String, Object> calculateRFM(Long userId) {
        List<UserEvent> purchases = eventRepository.findByUserIdAndEventType(
                userId, UserEvent.EventType.PAY
        );

        if (purchases.isEmpty()) {
            return Map.of(
                    "R", 0,
                    "F", 0,
                    "M", 0.0,
                    "score", 0,
                    "level", "无购买记录"
            );
        }

        // R: 最近购买距今天数
        LocalDateTime lastPurchase = purchases.stream()
                .map(UserEvent::getEventTime)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        long recencyDays = ChronoUnit.DAYS.between(lastPurchase, LocalDateTime.now());
        int rScore = recencyDays <= 30 ? 5 : recencyDays <= 60 ? 4 : recencyDays <= 90 ? 3 : recencyDays <= 180 ? 2 : 1;

        // F: 购买频次
        int frequency = purchases.size();
        int fScore = frequency >= 10 ? 5 : frequency >= 5 ? 4 : frequency >= 3 ? 3 : frequency >= 2 ? 2 : 1;

        // M: 购买金额（从事件数据中获取）
        double totalAmount = purchases.stream()
                .mapToDouble(e -> {
                    Object amount = e.getEventData().get("amount");
                    return amount != null ? ((Number) amount).doubleValue() : 0.0;
                })
                .sum();
        int mScore = totalAmount >= 10000 ? 5 : totalAmount >= 5000 ? 4 : totalAmount >= 2000 ? 3 : totalAmount >= 500 ? 2 : 1;

        // 综合评分
        int totalScore = rScore + fScore + mScore;
        String level = totalScore >= 13 ? "重要价值客户" :
                      totalScore >= 10 ? "重要发展客户" :
                      totalScore >= 7 ? "重要保持客户" :
                      totalScore >= 4 ? "一般客户" : "低价值客户";

        return Map.of(
                "R", rScore,
                "F", fScore,
                "M", mScore,
                "score", totalScore,
                "level", level,
                "recencyDays", recencyDays,
                "frequency", frequency,
                "totalAmount", totalAmount
        );
    }

    /**
     * 预测用户流失风险
     * 基于活跃度和购买行为
     */
    public Map<String, Object> predictChurnRisk(Long userId) {
        // 最近活跃时间
        List<UserEvent> recentEvents = eventRepository.findByUserIdOrderByEventTimeDesc(userId);
        if (recentEvents.isEmpty()) {
            return Map.of("risk", "高", "score", 100, "reason", "无活跃记录");
        }

        LocalDateTime lastActive = recentEvents.get(0).getEventTime();
        long daysSinceActive = ChronoUnit.DAYS.between(lastActive, LocalDateTime.now());

        // 购买行为
        List<UserEvent> purchases = eventRepository.findByUserIdAndEventType(
                userId, UserEvent.EventType.PAY
        );

        int riskScore = 0;
        List<String> reasons = new ArrayList<>();

        // 长时间未活跃
        if (daysSinceActive > 60) {
            riskScore += 40;
            reasons.add("超过60天未活跃");
        } else if (daysSinceActive > 30) {
            riskScore += 20;
            reasons.add("超过30天未活跃");
        }

        // 购买频率下降
        if (!purchases.isEmpty()) {
            LocalDateTime lastPurchase = purchases.get(0).getEventTime();
            long daysSincePurchase = ChronoUnit.DAYS.between(lastPurchase, LocalDateTime.now());

            if (daysSincePurchase > 90) {
                riskScore += 30;
                reasons.add("超过90天未购买");
            } else if (daysSincePurchase > 60) {
                riskScore += 15;
                reasons.add("超过60天未购买");
            }
        } else {
            riskScore += 20;
            reasons.add("从未购买");
        }

        // 活跃度下降
        double activityScore = calculateActivityScore(userId);
        if (activityScore < 20) {
            riskScore += 30;
            reasons.add("活跃度过低");
        } else if (activityScore < 40) {
            riskScore += 15;
            reasons.add("活跃度下降");
        }

        String riskLevel = riskScore >= 70 ? "高" : riskScore >= 40 ? "中" : "低";

        return Map.of(
                "risk", riskLevel,
                "score", riskScore,
                "reasons", reasons,
                "daysSinceActive", daysSinceActive,
                "activityScore", activityScore
        );
    }

    /**
     * 分析用户购物路径
     * 返回从浏览到购买的转化漏斗
     */
    public Map<String, Object> analyzePurchaseFunnel(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<UserEvent> events = eventRepository.findRecentEvents(userId, thirtyDaysAgo);

        long views = events.stream()
                .filter(e -> e.getEventType() == UserEvent.EventType.PRODUCT_VIEW)
                .count();

        long addToCarts = events.stream()
                .filter(e -> e.getEventType() == UserEvent.EventType.ADD_TO_CART)
                .count();

        long orders = events.stream()
                .filter(e -> e.getEventType() == UserEvent.EventType.PLACE_ORDER)
                .count();

        long payments = events.stream()
                .filter(e -> e.getEventType() == UserEvent.EventType.PAY)
                .count();

        return Map.of(
                "浏览商品", views,
                "加入购物车", addToCarts,
                "提交订单", orders,
                "完成支付", payments,
                "浏览转化率", views > 0 ? Math.round((addToCarts * 100.0 / views) * 100.0) / 100.0 : 0,
                "下单转化率", addToCarts > 0 ? Math.round((orders * 100.0 / addToCarts) * 100.0) / 100.0 : 0,
                "支付转化率", orders > 0 ? Math.round((payments * 100.0 / orders) * 100.0) / 100.0 : 0
        );
    }
}
