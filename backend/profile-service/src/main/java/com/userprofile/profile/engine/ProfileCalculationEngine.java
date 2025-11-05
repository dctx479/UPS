package com.userprofile.profile.engine;

import com.userprofile.profile.entity.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 用户画像计算引擎
 * 基于用户行为数据计算用户画像
 */
@Slf4j
@Component
public class ProfileCalculationEngine {

    /**
     * 计算综合画像评分
     */
    public Double calculateProfileScore(UserProfile profile) {
        double score = 0.0;

        // 数字行为评分 (30%)
        if (profile.getDigitalBehavior() != null) {
            score += calculateDigitalBehaviorScore(profile.getDigitalBehavior()) * 0.3;
        }

        // 价值评分 (40%)
        if (profile.getValueAssessment() != null) {
            score += calculateValueScore(profile.getValueAssessment()) * 0.4;
        }

        // 粘性评分 (30%)
        if (profile.getStickinessAndLoyalty() != null) {
            score += calculateStickinessScore(profile.getStickinessAndLoyalty()) * 0.3;
        }

        return Math.round(score * 100.0) / 100.0;
    }

    /**
     * 计算数字行为评分
     */
    private double calculateDigitalBehaviorScore(UserProfile.DigitalBehavior behavior) {
        double score = 0.0;

        // 产品品类多样性 (0-40分)
        if (behavior.getProductCategories() != null) {
            int categoryCount = behavior.getProductCategories().size();
            score += Math.min(categoryCount * 8, 40);
        }

        // 品牌偏好数量 (0-30分)
        if (behavior.getBrandPreferences() != null) {
            int brandCount = behavior.getBrandPreferences().size();
            score += Math.min(brandCount * 10, 30);
        }

        // 信息获取习惯和购买决策偏好存在 (各15分)
        if (behavior.getInfoAcquisitionHabit() != null && !behavior.getInfoAcquisitionHabit().isEmpty()) {
            score += 15;
        }
        if (behavior.getPurchaseDecisionPreference() != null && !behavior.getPurchaseDecisionPreference().isEmpty()) {
            score += 15;
        }

        return Math.min(score, 100);
    }

    /**
     * 计算价值评分
     */
    private double calculateValueScore(UserProfile.ValueAssessment assessment) {
        double score = 50.0; // 基础分

        // 偏好分析权重
        if (assessment.getPreferenceAnalysis() != null) {
            double avgPreference = assessment.getPreferenceAnalysis().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            score += avgPreference * 30;
        }

        // 画像质量加分
        if ("high".equalsIgnoreCase(assessment.getProfileQuality())) {
            score += 20;
        } else if ("medium".equalsIgnoreCase(assessment.getProfileQuality())) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    /**
     * 计算粘性评分
     */
    private double calculateStickinessScore(UserProfile.StickinessAndLoyalty stickiness) {
        double score = 0.0;

        // 忠诚度评分直接使用
        if (stickiness.getLoyaltyScore() != null) {
            score = stickiness.getLoyaltyScore();
        }

        // 核心关注点数量加分
        if (stickiness.getConcerns() != null) {
            score += stickiness.getConcerns().size() * 5;
        }

        return Math.min(score, 100);
    }

    /**
     * 分析用户画像类型
     * 根据行为特征将用户分类
     */
    public String analyzeUserType(UserProfile profile) {
        double profileScore = profile.getProfileScore() != null ? profile.getProfileScore() : 0;

        if (profileScore >= 80) {
            return "高价值用户";
        } else if (profileScore >= 60) {
            return "活跃用户";
        } else if (profileScore >= 40) {
            return "潜力用户";
        } else if (profileScore >= 20) {
            return "普通用户";
        } else {
            return "新用户";
        }
    }

    /**
     * 生成用户标签
     */
    public List<String> generateUserTags(UserProfile profile) {
        Set<String> tags = new HashSet<>();

        // 基于画像评分
        double score = profile.getProfileScore() != null ? profile.getProfileScore() : 0;
        if (score >= 80) tags.add("VIP客户");
        if (score >= 60) tags.add("优质客户");

        // 基于数字行为
        if (profile.getDigitalBehavior() != null) {
            UserProfile.DigitalBehavior behavior = profile.getDigitalBehavior();

            if (behavior.getProductCategories() != null && behavior.getProductCategories().size() >= 5) {
                tags.add("多品类消费者");
            }

            if (behavior.getBrandPreferences() != null && behavior.getBrandPreferences().size() >= 3) {
                tags.add("品牌忠诚");
            }

            if (behavior.getPurchaseDecisionPreference() != null) {
                if (behavior.getPurchaseDecisionPreference().contains("价格")) {
                    tags.add("价格敏感");
                }
                if (behavior.getPurchaseDecisionPreference().contains("品质")) {
                    tags.add("品质导向");
                }
            }
        }

        // 基于粘性
        if (profile.getStickinessAndLoyalty() != null) {
            Double loyaltyScore = profile.getStickinessAndLoyalty().getLoyaltyScore();
            if (loyaltyScore != null && loyaltyScore >= 70) {
                tags.add("高忠诚度");
            }
        }

        return new ArrayList<>(tags);
    }

    /**
     * 推荐策略分析
     */
    public Map<String, String> recommendStrategy(UserProfile profile) {
        Map<String, String> strategy = new HashMap<>();

        String userType = analyzeUserType(profile);
        strategy.put("用户类型", userType);

        // 根据用户类型推荐策略
        switch (userType) {
            case "高价值用户":
                strategy.put("营销策略", "提供专属优惠和VIP服务");
                strategy.put("互动频率", "每周1-2次");
                strategy.put("内容类型", "高端产品推荐、新品尝鲜");
                break;
            case "活跃用户":
                strategy.put("营销策略", "定期促销活动和积分奖励");
                strategy.put("互动频率", "每周2-3次");
                strategy.put("内容类型", "热销产品、组合优惠");
                break;
            case "潜力用户":
                strategy.put("营销策略", "培育计划和体验活动");
                strategy.put("互动频率", "每周1次");
                strategy.put("内容类型", "产品教育、使用技巧");
                break;
            default:
                strategy.put("营销策略", "基础触达和兴趣培养");
                strategy.put("互动频率", "每两周1次");
                strategy.put("内容类型", "品牌故事、入门产品");
        }

        return strategy;
    }
}
