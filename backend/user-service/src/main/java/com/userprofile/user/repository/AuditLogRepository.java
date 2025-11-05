package com.userprofile.user.repository;

import com.userprofile.user.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志Repository
 * P1-2修复
 *
 * @author User Profile Team
 * @version 1.5.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 根据用户ID查询审计日志
     */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据动作查询审计日志
     */
    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);

    /**
     * 根据用户ID和动作查询
     */
    List<AuditLog> findByUserIdAndAction(Long userId, AuditLog.AuditAction action);

    /**
     * 根据时间范围查询
     */
    Page<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 统计用户操作次数
     */
    long countByUserIdAndAction(Long userId, AuditLog.AuditAction action);
}
