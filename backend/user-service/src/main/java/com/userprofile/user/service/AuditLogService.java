package com.userprofile.user.service;

import com.userprofile.user.entity.AuditLog;
import com.userprofile.user.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审计日志服务
 * P1-2修复
 *
 * @author User Profile Team
 * @version 1.5.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 记录审计日志 (异步)
     *
     * @param action   动作
     * @param userId   用户ID
     * @param username 用户名
     * @param details  详情
     */
    @Async("eventExecutor")
    public void log(AuditLog.AuditAction action, Long userId, String username, String details) {
        log(action, userId, username, details, null, null, "SUCCESS", null);
    }

    /**
     * 记录审计日志 (异步,完整参数)
     *
     * @param action       动作
     * @param userId       用户ID
     * @param username     用户名
     * @param details      详情
     * @param ipAddress    IP地址
     * @param userAgent    User Agent
     * @param result       结果
     * @param errorMessage 错误信息
     */
    @Async("eventExecutor")
    @Transactional
    public void log(AuditLog.AuditAction action, Long userId, String username, String details,
                    String ipAddress, String userAgent, String result, String errorMessage) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .userId(userId)
                    .username(username)
                    .details(details)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .result(result)
                    .errorMessage(errorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("审计日志记录成功: action={}, userId={}, details={}", action, userId, details);
        } catch (Exception e) {
            log.error("记录审计日志失败: action={}, userId={}, error={}",
                    action, userId, e.getMessage(), e);
            // 不抛出异常,避免影响主业务流程
        }
    }

    /**
     * 记录失败的操作
     */
    @Async("eventExecutor")
    public void logFailure(AuditLog.AuditAction action, Long userId, String username,
                           String details, String errorMessage) {
        log(action, userId, username, details, null, null, "FAILURE", errorMessage);
    }

    /**
     * 查询用户的审计日志
     */
    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * 查询指定动作的审计日志
     */
    public Page<AuditLog> getAuditLogsByAction(AuditLog.AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    /**
     * 查询时间范围内的审计日志
     */
    public Page<AuditLog> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime,
                                                   Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(startTime, endTime, pageable);
    }

    /**
     * 统计用户操作次数
     */
    public long countUserActions(Long userId, AuditLog.AuditAction action) {
        return auditLogRepository.countByUserIdAndAction(userId, action);
    }
}
