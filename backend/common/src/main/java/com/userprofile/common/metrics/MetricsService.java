package com.userprofile.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 业务指标服务
 *
 * <p>提供自定义业务指标的记录功能，用于Prometheus监控
 *
 * <p>使用示例:
 * <pre>{@code
 * // 记录用户创建
 * metricsService.recordUserCreated();
 *
 * // 记录执行时间
 * long startTime = System.currentTimeMillis();
 * // ... 业务逻辑
 * metricsService.recordExecutionTime("createUser",
 *     System.currentTimeMillis() - startTime);
 *
 * // 记录缓存命中
 * metricsService.recordCacheHit("userProfiles", true);
 * }</pre>
 *
 * @author User Profile Team
 * @version 1.2.0
 * @since 1.2.0
 */
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    /**
     * 记录用户创建
     */
    public void recordUserCreated() {
        Counter.builder("users.created.total")
                .description("Total number of users created")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录用户删除
     */
    public void recordUserDeleted() {
        Counter.builder("users.deleted.total")
                .description("Total number of users deleted")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录画像计算
     */
    public void recordProfileCalculated() {
        Counter.builder("profiles.calculated.total")
                .description("Total number of profiles calculated")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录标签创建
     *
     * @param source 标签来源（SYSTEM, MANUAL, ALGORITHM, EVENT）
     */
    public void recordTagCreated(String source) {
        Counter.builder("tags.created.total")
                .description("Total number of tags created")
                .tag("source", source)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录事件处理
     *
     * @param eventType 事件类型
     */
    public void recordEventProcessed(String eventType) {
        Counter.builder("events.processed.total")
                .description("Total number of events processed")
                .tag("type", eventType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录操作执行时间
     *
     * @param operation 操作名称
     * @param timeMillis 执行时间（毫秒）
     */
    public void recordExecutionTime(String operation, long timeMillis) {
        Timer.builder("operation.execution.time")
                .description("Operation execution time")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(timeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录缓存命中
     *
     * @param cacheName 缓存名称
     * @param hit 是否命中
     */
    public void recordCacheHit(String cacheName, boolean hit) {
        Counter.builder("cache.requests.total")
                .description("Total cache requests")
                .tag("cache", cacheName)
                .tag("result", hit ? "hit" : "miss")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录API调用
     *
     * @param endpoint 端点路径
     * @param method HTTP方法
     * @param statusCode 状态码
     */
    public void recordApiCall(String endpoint, String method, int statusCode) {
        Counter.builder("api.requests.total")
                .description("Total API requests")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录业务异常
     *
     * @param exceptionType 异常类型
     */
    public void recordBusinessException(String exceptionType) {
        Counter.builder("business.exceptions.total")
                .description("Total business exceptions")
                .tag("type", exceptionType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 设置活跃用户数
     *
     * @param count 活跃用户数
     */
    public void setActiveUsersCount(long count) {
        meterRegistry.gauge("users.active.count", count);
    }

    /**
     * 设置画像总数
     *
     * @param count 画像总数
     */
    public void setProfilesCount(long count) {
        meterRegistry.gauge("profiles.total.count", count);
    }

    /**
     * 设置高价值用户数
     *
     * @param count 高价值用户数
     */
    public void setHighValueUsersCount(long count) {
        meterRegistry.gauge("users.high_value.count", count);
    }

    /**
     * 记录定时任务执行
     *
     * @param taskName 任务名称
     * @param success 是否成功
     * @param durationMs 执行时长
     */
    public void recordScheduledTask(String taskName, boolean success, long durationMs) {
        Counter.builder("scheduled.tasks.total")
                .description("Total scheduled task executions")
                .tag("task", taskName)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();

        Timer.builder("scheduled.tasks.duration")
                .description("Scheduled task duration")
                .tag("task", taskName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
