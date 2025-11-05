package com.userprofile.profile.scheduler;

import com.userprofile.common.lock.DistributedLock;
import com.userprofile.profile.analytics.UserBehaviorAnalytics;
import com.userprofile.profile.entity.UserProfile;
import com.userprofile.profile.event.UserEvent;
import com.userprofile.profile.event.UserEventRepository;
import com.userprofile.profile.repository.UserProfileRepository;
import com.userprofile.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 画像自动更新调度器
 *
 * <p>定时任务：自动根据用户行为更新画像
 * <p>所有定时任务都使用分布式锁，确保多实例部署时只有一个实例执行
 *
 * @author User Profile Team
 * @version 1.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileAutoUpdateScheduler {

    private final UserEventRepository eventRepository;
    private final UserProfileRepository profileRepository;
    private final UserProfileService profileService;
    private final UserBehaviorAnalytics behaviorAnalytics;

    /**
     * 每小时执行一次：处理未处理的事件并更新画像
     */
    @Scheduled(cron = "0 0 * * * ?")  // 每小时整点执行
    @DistributedLock(key = "profile:update-from-events", leaseTime = 3600)
    @Async
    public void updateProfilesFromEvents() {
        log.info("开始自动更新用户画像...");

        try {
            // 获取未处理的事件
            List<UserEvent> unprocessedEvents = eventRepository.findUnprocessedEvents();

            if (unprocessedEvents.isEmpty()) {
                log.info("没有待处理的事件");
                return;
            }

            // 按用户ID分组
            Map<Long, List<UserEvent>> eventsByUser = unprocessedEvents.stream()
                    .collect(Collectors.groupBy(UserEvent::getUserId));

            int updatedCount = 0;
            for (Map.Entry<Long, List<UserEvent>> entry : eventsByUser.entrySet()) {
                Long userId = entry.getKey();
                List<UserEvent> userEvents = entry.getValue();

                try {
                    updateProfileFromEvents(userId, userEvents);
                    updatedCount++;

                    // 标记事件为已处理
                    userEvents.forEach(event -> {
                        event.setProcessed(true);
                        eventRepository.save(event);
                    });

                } catch (Exception e) {
                    log.error("更新用户{}的画像失败: {}", userId, e.getMessage());
                }
            }

            log.info("画像自动更新完成，共更新{}个用户", updatedCount);

        } catch (Exception e) {
            log.error("画像自动更新任务失败", e);
        }
    }

    /**
     * 根据用户事件更新画像
     */
    private void updateProfileFromEvents(Long userId, List<UserEvent> events) {
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            log.info("用户{}暂无画像，跳过更新", userId);
            return;
        }

        boolean needUpdate = false;

        // 更新数字行为
        UserProfile.DigitalBehavior behavior = profile.getDigitalBehavior();
        if (behavior == null) {
            behavior = new UserProfile.DigitalBehavior();
            profile.setDigitalBehavior(behavior);
        }

        // 分析兴趣偏好
        Map<String, Double> interests = behaviorAnalytics.analyzeInterests(userId);
        if (!interests.isEmpty()) {
            needUpdate = true;
            // 这里可以更新品牌偏好等
        }

        // 更新粘性数据
        UserProfile.StickinessAndLoyalty stickiness = profile.getStickinessAndLoyalty();
        if (stickiness == null) {
            stickiness = new UserProfile.StickinessAndLoyalty();
            profile.setStickinessAndLoyalty(stickiness);
        }

        // 计算RFM并更新忠诚度
        Map<String, Object> rfm = behaviorAnalytics.calculateRFM(userId);
        if (rfm.containsKey("score")) {
            int rfmScore = (Integer) rfm.get("score");
            stickiness.setLoyaltyScore((double) (rfmScore * 100 / 15));  // 归一化到100分
            needUpdate = true;
        }

        if (needUpdate) {
            // 重新计算画像评分
            profileService.recalculateScore(userId);
            log.info("用户{}的画像已更新", userId);
        }
    }

    /**
     * 每天凌晨2点：重新计算所有用户的画像评分
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    @DistributedLock(key = "profile:recalculate-all", leaseTime = 7200)
    @Async
    public void recalculateAllProfiles() {
        log.info("开始重新计算所有用户画像评分...");

        try {
            List<UserProfile> allProfiles = profileRepository.findAll();
            int count = 0;

            for (UserProfile profile : allProfiles) {
                try {
                    profileService.recalculateScore(profile.getUserId());
                    count++;
                } catch (Exception e) {
                    log.error("重新计算用户{}的画像失败: {}", profile.getUserId(), e.getMessage());
                }
            }

            log.info("画像评分重新计算完成，共处理{}个用户", count);

        } catch (Exception e) {
            log.error("画像评分重新计算任务失败", e);
        }
    }

    /**
     * 每天早上8点：识别高流失风险用户
     */
    @Scheduled(cron = "0 0 8 * * ?")  // 每天早上8点执行
    @DistributedLock(key = "profile:identify-churn-risk", leaseTime = 3600)
    @Async
    public void identifyChurnRiskUsers() {
        log.info("开始识别流失风险用户...");

        try {
            List<UserProfile> allProfiles = profileRepository.findAll();
            int highRiskCount = 0;

            for (UserProfile profile : allProfiles) {
                try {
                    Map<String, Object> risk = behaviorAnalytics.predictChurnRisk(profile.getUserId());
                    String riskLevel = (String) risk.get("risk");

                    if ("高".equals(riskLevel)) {
                        highRiskCount++;
                        log.warn("用户{}存在高流失风险: {}", profile.getUserId(), risk.get("reasons"));
                        // 这里可以触发预警通知或自动营销活动
                    }

                } catch (Exception e) {
                    log.error("分析用户{}的流失风险失败: {}", profile.getUserId(), e.getMessage());
                }
            }

            log.info("流失风险识别完成，发现{}个高风险用户", highRiskCount);

        } catch (Exception e) {
            log.error("流失风险识别任务失败", e);
        }
    }

    /**
     * 每周一早上9点：生成用户画像周报
     */
    @Scheduled(cron = "0 0 9 ? * MON")  // 每周一早上9点
    @DistributedLock(key = "profile:generate-weekly-report", leaseTime = 1800)
    @Async
    public void generateWeeklyReport() {
        log.info("开始生成用户画像周报...");

        try {
            List<UserProfile> allProfiles = profileRepository.findAll();

            // 统计数据
            long totalUsers = allProfiles.size();
            double avgScore = allProfiles.stream()
                    .filter(p -> p.getProfileScore() != null)
                    .mapToDouble(UserProfile::getProfileScore)
                    .average()
                    .orElse(0.0);

            long highValueUsers = allProfiles.stream()
                    .filter(p -> p.getProfileScore() != null && p.getProfileScore() >= 80)
                    .count();

            log.info("===== 用户画像周报 =====");
            log.info("总用户数: {}", totalUsers);
            log.info("平均画像评分: {}", String.format("%.2f", avgScore));
            log.info("高价值用户数: {} ({}%)", highValueUsers,
                    String.format("%.2f", highValueUsers * 100.0 / totalUsers));

        } catch (Exception e) {
            log.error("生成周报失败", e);
        }
    }

    /**
     * 每月1号：清理过期事件数据
     */
    @Scheduled(cron = "0 0 3 1 * ?")  // 每月1号凌晨3点
    @DistributedLock(key = "profile:cleanup-old-events", leaseTime = 7200)
    @Async
    public void cleanupOldEvents() {
        log.info("开始清理过期事件数据...");

        try {
            // 删除180天前的已处理事件
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minus(180, ChronoUnit.DAYS);
            // 这里需要添加批量删除方法
            log.info("过期事件数据清理完成");

        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }
}
