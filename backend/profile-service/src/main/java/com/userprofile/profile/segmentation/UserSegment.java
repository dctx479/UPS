package com.userprofile.profile.segmentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户分群
 * 根据画像特征和行为数据对用户进行分组
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_segments")
public class UserSegment {

    @Id
    private String id;

    /**
     * 分群名称
     */
    private String name;

    /**
     * 分群描述
     */
    private String description;

    /**
     * 分群类型
     */
    private SegmentType type;

    /**
     * 筛选条件（JSON格式存储）
     */
    private List<SegmentCondition> conditions;

    /**
     * 用户ID列表
     */
    private List<Long> userIds;

    /**
     * 用户数量
     */
    private Integer userCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 是否启用
     */
    private Boolean active;

    /**
     * 分群类型枚举
     */
    public enum SegmentType {
        STATIC("静态分群", "手动创建，不自动更新"),
        DYNAMIC("动态分群", "根据条件自动更新"),
        RFM("RFM分群", "基于RFM模型"),
        BEHAVIOR("行为分群", "基于用户行为"),
        CUSTOM("自定义分群", "自定义规则");

        private final String name;
        private final String description;

        SegmentType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 分群条件
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentCondition {
        /**
         * 字段名
         */
        private String field;

        /**
         * 操作符
         */
        private Operator operator;

        /**
         * 值
         */
        private Object value;

        /**
         * 逻辑关系（AND/OR）
         */
        private String logic;

        public enum Operator {
            EQUALS("等于"),
            NOT_EQUALS("不等于"),
            GREATER_THAN("大于"),
            LESS_THAN("小于"),
            GREATER_OR_EQUAL("大于等于"),
            LESS_OR_EQUAL("小于等于"),
            CONTAINS("包含"),
            NOT_CONTAINS("不包含"),
            IN("在列表中"),
            NOT_IN("不在列表中"),
            BETWEEN("介于"),
            IS_NULL("为空"),
            IS_NOT_NULL("不为空");

            private final String description;

            Operator(String description) {
                this.description = description;
            }

            public String getDescription() {
                return description;
            }
        }
    }
}
