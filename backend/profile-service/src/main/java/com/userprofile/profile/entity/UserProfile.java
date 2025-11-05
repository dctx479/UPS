package com.userprofile.profile.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户画像实体（存储在MongoDB）
 * P0-6修复: 添加索引提升查询性能
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_profiles")
@CompoundIndexes({
    @CompoundIndex(name = "idx_userId", def = "{'userId': 1}", unique = true),
    @CompoundIndex(name = "idx_username", def = "{'username': 1}"),
    @CompoundIndex(name = "idx_updateTime", def = "{'updateTime': -1}"),
    @CompoundIndex(name = "idx_profileScore", def = "{'profileScore': -1}"),
    @CompoundIndex(name = "idx_userId_updateTime", def = "{'userId': 1, 'updateTime': -1}")
})
public class UserProfile {

    @Id
    private String id;

    /**
     * 用户ID（关联users表）
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 数字行为分析
     */
    private DigitalBehavior digitalBehavior;

    /**
     * 核心需求
     */
    private CoreNeeds coreNeeds;

    /**
     * 价值评估
     */
    private ValueAssessment valueAssessment;

    /**
     * 粘性与原感
     */
    private StickinessAndLoyalty stickinessAndLoyalty;

    /**
     * 画像分数（综合评分）
     */
    private Double profileScore;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 获取评分等级
     */
    public String getScoreLevel() {
        if (profileScore == null) {
            return "未评分";
        }
        if (profileScore >= 80) {
            return "优秀";
        } else if (profileScore >= 60) {
            return "良好";
        } else if (profileScore >= 40) {
            return "一般";
        } else {
            return "较差";
        }
    }

    /**
     * 数字行为数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DigitalBehavior {
        /**
         * 常用农产品品类及渠道
         */
        private List<String> productCategories;

        /**
         * 信息获取习惯
         */
        private String infoAcquisitionHabit;

        /**
         * 购买决策偏好
         */
        private String purchaseDecisionPreference;

        /**
         * 品牌偏好
         */
        private List<String> brandPreferences;
    }

    /**
     * 核心需求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoreNeeds {
        /**
         * 操心属性（Top3）
         */
        private List<String> topConcerns;

        /**
         * 决策痛点
         */
        private String decisionPainPoint;
    }

    /**
     * 价值评估
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValueAssessment {
        /**
         * 编好画像
         */
        private String profileQuality;

        /**
         * 消费水平
         */
        private String consumptionLevel;

        /**
         * 偏好分析
         */
        private Map<String, Object> preferenceAnalysis;

        /**
         * 平均订单价值
         */
        private Double avgOrderValue;

        /**
         * 喂养方式
         */
        private String feedingMethod;

        /**
         * 易教性任
         */
        private String teachability;
    }

    /**
     * 粘性与原感
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StickinessAndLoyalty {
        /**
         * 操心属性
         */
        private List<String> concerns;

        /**
         * 决策痛点
         */
        private String painPoint;

        /**
         * 忠诚度评分
         */
        private Double loyaltyScore;
    }
}
