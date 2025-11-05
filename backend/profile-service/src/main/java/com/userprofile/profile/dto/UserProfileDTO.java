package com.userprofile.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 用户画像DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private Long userId;
    private String username;

    /**
     * 数字行为分析
     */
    private DigitalBehaviorDTO digitalBehavior;

    /**
     * 核心需求
     */
    private CoreNeedsDTO coreNeeds;

    /**
     * 价值评估
     */
    private ValueAssessmentDTO valueAssessment;

    /**
     * 粘性与原感
     */
    private StickinessDTO stickiness;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DigitalBehaviorDTO {
        /**
         * 常用农产品品类及渠道
         */
        private List<String> productCategories;

        /**
         * 信息获取习惯 (如: "微信公众号", "抖音", "朋友圈")
         */
        private String infoAcquisitionHabit;

        /**
         * 购买决策偏好 (如: "价格敏感", "品质优先", "品牌导向")
         */
        private String purchaseDecisionPreference;

        /**
         * 品牌偏好
         */
        private List<String> brandPreferences;

        /**
         * 活跃度评分 (1-5分)
         */
        private Double activityScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoreNeedsDTO {
        /**
         * 操心属性Top3 (如: ["营养成分", "安全性", "性价比"])
         */
        private List<String> topConcerns;

        /**
         * 决策痛点 (如: "不知道如何挑选", "担心质量问题")
         */
        private String decisionPainPoint;

        /**
         * 需求强度 (1-5分)
         */
        private Double needIntensity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValueAssessmentDTO {
        /**
         * 消费能力等级 (如: "高", "中", "低")
         */
        private String consumptionLevel;

        /**
         * 偏好分析 (如: {"有机": 0.8, "进口": 0.6})
         */
        private Map<String, Double> preferenceAnalysis;

        /**
         * 客单价区间
         */
        private String avgOrderValue;

        /**
         * 价值评分 (1-100分)
         */
        private Double valueScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StickinessDTO {
        /**
         * 复购率 (0-1)
         */
        private Double repurchaseRate;

        /**
         * 忠诚度评分 (1-100分)
         */
        private Double loyaltyScore;

        /**
         * 活跃天数
         */
        private Integer activeDays;

        /**
         * 最后活跃时间
         */
        private String lastActiveTime;
    }
}
