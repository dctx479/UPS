package com.userprofile.profile.recommendation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResult {

    /**
     * 推荐项ID（商品ID/内容ID等）
     */
    private String itemId;

    /**
     * 推荐分数
     */
    private Double score;

    /**
     * 推荐方法
     */
    private String method;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 扩展数据
     */
    private Object metadata;

    public RecommendationResult(String itemId, Double score, String method, String reason) {
        this.itemId = itemId;
        this.score = score;
        this.method = method;
        this.reason = reason;
    }
}
