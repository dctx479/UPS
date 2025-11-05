package com.userprofile.profile.recommendation;

import com.userprofile.profile.analytics.UserBehaviorAnalytics;
import com.userprofile.profile.entity.UserProfile;
import com.userprofile.profile.event.UserEvent;
import com.userprofile.profile.event.UserEventRepository;
import com.userprofile.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化推荐引擎
 * 基于用户画像和行为数据的智能推荐系统
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationEngine {

    private final UserProfileRepository profileRepository;
    private final UserEventRepository eventRepository;
    private final UserBehaviorAnalytics behaviorAnalytics;

    /**
     * 协同过滤推荐 - 基于用户的协同过滤
     * 找到相似用户，推荐他们喜欢的商品
     */
    public List<RecommendationResult> collaborativeFilteringByUser(Long userId, int limit) {
        log.info("执行基于用户的协同过滤推荐: userId={}", userId);

        // 1. 获取目标用户画像
        UserProfile targetProfile = profileRepository.findByUserId(userId).orElse(null);
        if (targetProfile == null) {
            return Collections.emptyList();
        }

        // 2. 找到相似用户
        List<UserProfile> allProfiles = profileRepository.findAll();
        List<SimilarUser> similarUsers = allProfiles.stream()
                .filter(p -> !p.getUserId().equals(userId))
                .map(p -> new SimilarUser(p.getUserId(), calculateSimilarity(targetProfile, p)))
                .filter(s -> s.similarity > 0.5)  // 相似度阈值
                .sorted(Comparator.comparingDouble(SimilarUser::similarity).reversed())
                .limit(10)
                .collect(Collectors.toList());

        log.info("找到{}个相似用户", similarUsers.size());

        // 3. 获取相似用户的购买记录
        Map<String, Double> recommendedItems = new HashMap<>();
        for (SimilarUser similarUser : similarUsers) {
            List<UserEvent> purchases = eventRepository.findByUserIdAndEventType(
                    similarUser.userId, UserEvent.EventType.PAY
            );

            for (UserEvent event : purchases) {
                String productId = (String) event.getEventData().get("productId");
                if (productId != null && !hasUserPurchased(userId, productId)) {
                    double weight = similarUser.similarity * event.getWeight();
                    recommendedItems.put(productId, recommendedItems.getOrDefault(productId, 0.0) + weight);
                }
            }
        }

        // 4. 排序并返回推荐结果
        return recommendedItems.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new RecommendationResult(
                        e.getKey(),
                        e.getValue(),
                        "协同过滤",
                        "相似用户喜欢的商品"
                ))
                .collect(Collectors.toList());
    }

    /**
     * 基于内容的推荐
     * 根据用户兴趣偏好推荐相关品类商品
     */
    public List<RecommendationResult> contentBasedRecommendation(Long userId, int limit) {
        log.info("执行基于内容的推荐: userId={}", userId);

        // 1. 分析用户兴趣偏好
        Map<String, Double> interests = behaviorAnalytics.analyzeInterests(userId);
        if (interests.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取用户最近浏览但未购买的商品
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<UserEvent> recentViews = eventRepository.findRecentEvents(userId, thirtyDaysAgo).stream()
                .filter(e -> e.getEventType() == UserEvent.EventType.PRODUCT_VIEW)
                .collect(Collectors.toList());

        // 3. 根据兴趣权重计算推荐分数
        Map<String, Double> scores = new HashMap<>();
        for (UserEvent view : recentViews) {
            String productId = (String) view.getEventData().get("productId");
            String category = (String) view.getEventData().get("category");

            if (productId != null && category != null && !hasUserPurchased(userId, productId)) {
                double interestWeight = interests.getOrDefault(category, 0.0);
                double recencyWeight = calculateRecencyWeight(view.getEventTime());
                double score = interestWeight * recencyWeight * 100;

                scores.put(productId, scores.getOrDefault(productId, 0.0) + score);
            }
        }

        // 4. 返回推荐结果
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new RecommendationResult(
                        e.getKey(),
                        e.getValue(),
                        "基于内容",
                        "根据您的兴趣推荐"
                ))
                .collect(Collectors.toList());
    }

    /**
     * 热门商品推荐
     * 推荐当前热销的商品
     */
    public List<RecommendationResult> trendingRecommendation(int limit) {
        log.info("执行热门商品推荐");

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);

        // 统计最近7天的购买次数
        Map<String, Long> purchaseCount = new HashMap<>();
        List<UserEvent> recentPurchases = eventRepository.findUnprocessedEvents().stream()
                .filter(e -> e.getEventType() == UserEvent.EventType.PAY)
                .filter(e -> e.getEventTime().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());

        for (UserEvent event : recentPurchases) {
            String productId = (String) event.getEventData().get("productId");
            if (productId != null) {
                purchaseCount.put(productId, purchaseCount.getOrDefault(productId, 0L) + 1);
            }
        }

        return purchaseCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new RecommendationResult(
                        e.getKey(),
                        e.getValue().doubleValue(),
                        "热门推荐",
                        "最近热销商品"
                ))
                .collect(Collectors.toList());
    }

    /**
     * 智能混合推荐
     * 综合多种推荐算法，提供多样化推荐
     */
    public List<RecommendationResult> hybridRecommendation(Long userId, int limit) {
        log.info("执行混合推荐策略: userId={}", userId);

        List<RecommendationResult> results = new ArrayList<>();

        // 1. 协同过滤（权重40%）
        List<RecommendationResult> cfResults = collaborativeFilteringByUser(userId, limit * 2);
        cfResults.forEach(r -> r.setScore(r.getScore() * 0.4));
        results.addAll(cfResults);

        // 2. 基于内容（权重40%）
        List<RecommendationResult> cbResults = contentBasedRecommendation(userId, limit * 2);
        cbResults.forEach(r -> r.setScore(r.getScore() * 0.4));
        results.addAll(cbResults);

        // 3. 热门商品（权重20%）
        List<RecommendationResult> trendingResults = trendingRecommendation(limit);
        trendingResults.forEach(r -> r.setScore(r.getScore() * 0.2));
        results.addAll(trendingResults);

        // 4. 合并去重，按分数排序
        Map<String, RecommendationResult> mergedResults = new HashMap<>();
        for (RecommendationResult result : results) {
            if (mergedResults.containsKey(result.getItemId())) {
                RecommendationResult existing = mergedResults.get(result.getItemId());
                existing.setScore(existing.getScore() + result.getScore());
                existing.setReason(existing.getReason() + " + " + result.getMethod());
            } else {
                mergedResults.put(result.getItemId(), result);
            }
        }

        return mergedResults.values().stream()
                .sorted(Comparator.comparingDouble(RecommendationResult::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 计算用户相似度
     * 使用余弦相似度算法
     */
    private double calculateSimilarity(UserProfile p1, UserProfile p2) {
        double score1 = p1.getProfileScore() != null ? p1.getProfileScore() : 0;
        double score2 = p2.getProfileScore() != null ? p2.getProfileScore() : 0;

        // 简化的相似度计算：基于画像评分差异
        double scoreDiff = Math.abs(score1 - score2);
        double similarity = 1.0 - (scoreDiff / 100.0);

        return Math.max(0, similarity);
    }

    /**
     * 检查用户是否已购买该商品
     */
    private boolean hasUserPurchased(Long userId, String productId) {
        List<UserEvent> purchases = eventRepository.findByUserIdAndEventType(userId, UserEvent.EventType.PAY);
        return purchases.stream()
                .anyMatch(e -> productId.equals(e.getEventData().get("productId")));
    }

    /**
     * 计算时间衰减权重
     * 越近的行为权重越高
     */
    private double calculateRecencyWeight(LocalDateTime eventTime) {
        long daysAgo = ChronoUnit.DAYS.between(eventTime, LocalDateTime.now());
        return Math.exp(-daysAgo / 30.0);  // 30天半衰期
    }

    /**
     * 相似用户内部类
     */
    private record SimilarUser(Long userId, double similarity) {
    }
}
