package com.userprofile.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 * P1-2修复
 *
 * @author User Profile Team
 * @version 1.5.0
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_action", columnList = "action"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 审计动作
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 用户名
     */
    @Column(length = 100)
    private String username;

    /**
     * 操作详情
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * User Agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 结果 (SUCCESS/FAILURE)
     */
    @Column(length = 20)
    private String result;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 时间戳
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * 审计动作枚举
     */
    public enum AuditAction {
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        USER_LOGIN,
        USER_LOGOUT,
        PASSWORD_CHANGED,
        PASSWORD_RESET,
        EMAIL_SENT,
        PROFILE_INITIALIZED,
        PROFILE_UPDATED,
        TAG_CREATED,
        TAG_UPDATED,
        TAG_DELETED
    }
}
