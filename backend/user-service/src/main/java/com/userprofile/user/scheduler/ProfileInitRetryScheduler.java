package com.userprofile.user.scheduler;

import com.userprofile.user.client.ProfileClient;
import com.userprofile.user.dto.InitialProfileRequest;
import com.userprofile.user.entity.AuditLog;
import com.userprofile.user.entity.User;
import com.userprofile.user.repository.UserRepository;
import com.userprofile.user.service.AuditLogService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 画像初始化重试调度器
 * P1-7修复: 实现分布式事务补偿机制
 *
 * 用于处理用户创建成功但画像初始化失败的情况
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileInitRetryScheduler {

    private final UserRepository userRepository;
    private final ProfileClient profileClient;
    private final AuditLogService auditLogService;

    /**
     * 每小时检查一次最近24小时内创建的用户
     * 确保所有用户都有画像初始化
     */
    @Scheduled(cron = "0 0 * * * *")  // 每小时执行一次
    public void retryFailedProfileInitialization() {
        log.info("开始执行画像初始化补偿检查...");

        try {
            // 查找最近24小时内创建的用户
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<User> recentUsers = userRepository.findRecentUsers(since);

            if (recentUsers.isEmpty()) {
                log.info("没有最近创建的用户需要检查");
                return;
            }

            log.info("发现 {} 个最近创建的用户,开始检查画像初始化状态", recentUsers.size());

            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (User user : recentUsers) {
                try {
                    // 尝试初始化画像
                    InitialProfileRequest request = InitialProfileRequest.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .build();

                    profileClient.initializeProfile(request);
                    successCount++;

                    // 记录审计日志
                    auditLogService.log(
                            AuditLog.AuditAction.PROFILE_INITIALIZED,
                            user.getId(),
                            user.getUsername(),
                            "补偿机制重试画像初始化成功"
                    );

                    log.info("补偿执行成功: userId={}, username={}", user.getId(), user.getUsername());

                } catch (FeignException.Conflict e) {
                    // 409冲突 - 画像已存在,这是正常的
                    skipCount++;
                    log.debug("画像已存在,跳过: userId={}", user.getId());

                } catch (Exception e) {
                    // 其他错误 - 记录并继续
                    failCount++;
                    log.error("补偿执行失败: userId={}, error={}", user.getId(), e.getMessage());

                    // 记录失败审计日志
                    auditLogService.log(
                            AuditLog.AuditAction.PROFILE_INITIALIZED,
                            user.getId(),
                            user.getUsername(),
                            "补偿机制重试画像初始化失败: " + e.getMessage()
                    );
                }
            }

            log.info("画像初始化补偿检查完成: 总数={}, 成功={}, 跳过={}, 失败={}",
                    recentUsers.size(), successCount, skipCount, failCount);

        } catch (Exception e) {
            log.error("画像初始化补偿检查异常", e);
        }
    }

    /**
     * 启动时执行一次全面检查（可选）
     * 检查最近7天内的用户
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)  // 启动1分钟后执行一次
    public void startupCheck() {
        log.info("执行启动时画像初始化全面检查...");

        try {
            // 查找最近7天内创建的用户
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<User> recentUsers = userRepository.findRecentUsers(since);

            if (recentUsers.isEmpty()) {
                log.info("没有最近7天创建的用户需要检查");
                return;
            }

            log.info("发现 {} 个最近7天创建的用户,开始全面检查", recentUsers.size());

            int checkedCount = 0;
            int repairedCount = 0;

            for (User user : recentUsers) {
                try {
                    checkedCount++;

                    // 尝试初始化（如果已存在会返回409，我们会捕获并忽略）
                    InitialProfileRequest request = InitialProfileRequest.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .build();

                    profileClient.initializeProfile(request);
                    repairedCount++;

                    log.info("启动检查修复成功: userId={}", user.getId());

                } catch (FeignException.Conflict e) {
                    // 画像已存在,正常情况
                    log.debug("画像已存在: userId={}", user.getId());
                } catch (Exception e) {
                    // 记录错误但继续
                    log.warn("启动检查失败: userId={}, error={}", user.getId(), e.getMessage());
                }
            }

            log.info("启动时全面检查完成: 检查={}, 修复={}", checkedCount, repairedCount);

        } catch (Exception e) {
            log.error("启动时全面检查异常", e);
        }
    }
}
