package com.userprofile.tag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户标签实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_tags", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_tag_name", columnList = "tagName"),
        @Index(name = "idx_category", columnList = "category")
})
public class UserTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 标签名称
     */
    @Column(nullable = false, length = 100)
    private String tagName;

    /**
     * 标签分类（行为标签、属性标签、价值标签等）
     */
    @Column(length = 50)
    private String category;

    /**
     * 标签来源（系统自动、手动添加、算法生成）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagSource source;

    /**
     * 标签权重（0-1）
     */
    @Column(columnDefinition = "DECIMAL(3,2) DEFAULT 1.0")
    private Double weight;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否有效
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    private LocalDateTime updateTime;

    /**
     * 标签来源枚举
     */
    public enum TagSource {
        SYSTEM("系统自动"),
        MANUAL("手动添加"),
        ALGORITHM("算法生成"),
        EVENT("事件触发");

        private final String description;

        TagSource(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
