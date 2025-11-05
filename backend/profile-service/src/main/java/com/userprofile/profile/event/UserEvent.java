package com.userprofile.profile.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户行为事件
 * 记录用户的各种行为轨迹，用于画像实时更新
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_events")
public class UserEvent {

    @Id
    private String id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 事件详情（JSON格式存储）
     */
    private Map<String, Object> eventData;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 事件来源（APP、Web、小程序等）
     */
    private String source;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 事件权重（用于计算画像评分）
     */
    private Double weight;

    /**
     * 是否已处理（用于批量计算）
     */
    private Boolean processed;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        // 浏览行为
        PAGE_VIEW("页面浏览"),
        PRODUCT_VIEW("商品浏览"),
        CATEGORY_VIEW("分类浏览"),

        // 搜索行为
        SEARCH("搜索"),
        FILTER("筛选"),

        // 交互行为
        CLICK("点击"),
        COLLECT("收藏"),
        SHARE("分享"),
        COMMENT("评论"),
        LIKE("点赞"),

        // 购物行为
        ADD_TO_CART("加入购物车"),
        REMOVE_FROM_CART("移除购物车"),
        PLACE_ORDER("下单"),
        PAY("支付"),
        CANCEL_ORDER("取消订单"),
        REFUND("退款"),

        // 营销行为
        COUPON_RECEIVE("领券"),
        COUPON_USE("用券"),
        ACTIVITY_PARTICIPATE("参与活动"),

        // 用户反馈
        RATE("评分"),
        REVIEW("评价"),
        COMPLAINT("投诉"),

        // 社交行为
        FOLLOW("关注"),
        INVITE("邀请"),

        // 其他
        LOGIN("登录"),
        LOGOUT("登出"),
        CUSTOM("自定义事件");

        private final String description;

        EventType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
