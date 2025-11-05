package com.userprofile.user.listener;

import com.userprofile.user.client.ProfileClient;
import com.userprofile.user.dto.InitialProfileRequest;
import com.userprofile.user.entity.AuditLog;
import com.userprofile.user.event.UserCreatedEvent;
import com.userprofile.user.service.AuditLogService;
import com.userprofile.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户事件监听器
 *
 * <p>异步处理用户相关事件
 * <p>P1-2修复: 实现TODO功能(邮件发送、画像初始化、审计日志)
 *
 * @author User Profile Team
 * @version 1.5.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;
    private final ProfileClient profileClient;
    private final AuditLogService auditLogService;

    /**
     * 监听用户创建事件 - 发送欢迎邮件
     * P1-2修复: 实现邮件发送功能
     */
    @Async("eventExecutor")
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("处理用户创建事件 - 发送欢迎邮件: userId={}, email={}",
                event.getUserId(), event.getEmail());

        try {
            // 发送欢迎邮件
            emailService.sendWelcomeEmail(event.getEmail(), event.getName());
            log.info("欢迎邮件发送成功: userId={}, email={}", event.getUserId(), event.getEmail());

            // 记录审计日志
            auditLogService.log(AuditLog.AuditAction.EMAIL_SENT,
                    event.getUserId(),
                    event.getUsername(),
                    "发送欢迎邮件到: " + event.getEmail());
        } catch (Exception e) {
            log.error("发送欢迎邮件失败: userId={}, email={}, error={}",
                    event.getUserId(), event.getEmail(), e.getMessage(), e);

            // 记录失败日志
            auditLogService.logFailure(AuditLog.AuditAction.EMAIL_SENT,
                    event.getUserId(),
                    event.getUsername(),
                    "发送欢迎邮件失败",
                    e.getMessage());
        }
    }

    /**
     * 监听用户创建事件 - 初始化用户画像
     * P1-2修复: 实现画像初始化功能
     */
    @Async("eventExecutor")
    @EventListener
    public void initializeUserProfile(UserCreatedEvent event) {
        log.info("处理用户创建事件 - 初始化用户画像: userId={}", event.getUserId());

        try {
            // 调用profile-service创建初始画像
            InitialProfileRequest request = InitialProfileRequest.builder()
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .email(event.getEmail())
                    .name(event.getName())
                    .build();

            profileClient.initializeProfile(request);
            log.info("用户画像初始化成功: userId={}", event.getUserId());

            // 记录审计日志
            auditLogService.log(AuditLog.AuditAction.PROFILE_INITIALIZED,
                    event.getUserId(),
                    event.getUsername(),
                    "初始化用户画像成功");
        } catch (Exception e) {
            log.error("用户画像初始化失败: userId={}, error={}", event.getUserId(), e.getMessage(), e);

            // 记录失败日志
            auditLogService.logFailure(AuditLog.AuditAction.PROFILE_INITIALIZED,
                    event.getUserId(),
                    event.getUsername(),
                    "初始化用户画像失败",
                    e.getMessage());
        }
    }

    /**
     * 监听用户创建事件 - 记录审计日志
     * P1-2修复: 实现审计日志功能
     */
    @Async("eventExecutor")
    @EventListener
    public void auditUserCreation(UserCreatedEvent event) {
        log.info("处理用户创建事件 - 记录审计日志: userId={}, username={}",
                event.getUserId(), event.getUsername());

        try {
            // 记录用户创建审计日志
            String details = String.format("新用户注册: username=%s, email=%s, name=%s",
                    event.getUsername(), event.getEmail(), event.getName());

            auditLogService.log(AuditLog.AuditAction.USER_CREATED,
                    event.getUserId(),
                    event.getUsername(),
                    details);

            log.info("审计日志记录成功: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("记录审计日志失败: userId={}, error={}", event.getUserId(), e.getMessage(), e);
            // 审计日志失败不影响主流程,仅记录错误
        }
    }
}
