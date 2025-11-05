package com.userprofile.common.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 *
 * <p>使用Redisson实现的分布式锁，适用于多实例部署场景。
 *
 * <p>使用示例:
 * <pre>{@code
 * @Scheduled(cron = "0 0 * * * ?")
 * @DistributedLock(key = "profile:auto-update", leaseTime = 3600)
 * public void updateProfiles() {
 *     // 只有获取到锁的实例才会执行
 * }
 * }</pre>
 *
 * @author User Profile Team
 * @version 1.2.0
 * @since 1.2.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的key
     * <p>建议使用业务前缀，如: "profile:update", "tag:cleanup"
     *
     * @return 锁key
     */
    String key();

    /**
     * 等待时间（秒）
     * <p>默认0表示不等待，立即返回。
     * <p>如果设置为正数，线程会等待指定时间尝试获取锁。
     *
     * @return 等待时间
     */
    long waitTime() default 0;

    /**
     * 锁持有时间（秒）
     * <p>默认30秒。Redisson会自动续约，防止任务执行时间超过leaseTime导致锁被释放。
     * <p>建议设置为任务预期执行时间的2倍。
     *
     * @return 锁持有时间
     */
    long leaseTime() default 30;

    /**
     * 时间单位
     * <p>默认为秒
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 获取锁失败时是否抛出异常
     * <p>默认false，静默跳过。
     * <p>如果设置为true，获取锁失败时会抛出RuntimeException。
     *
     * @return 是否抛异常
     */
    boolean throwOnFailure() default false;
}
