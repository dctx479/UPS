package com.userprofile.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 分布式锁AOP切面
 *
 * <p>拦截带有{@link DistributedLock}注解的方法，自动加锁解锁。
 *
 * <p>功能特性:
 * <ul>
 *   <li>自动获取和释放锁</li>
 *   <li>支持锁自动续约（防止任务执行时间过长）</li>
 *   <li>线程安全检查，只释放当前线程持有的锁</li>
 *   <li>详细的日志记录</li>
 * </ul>
 *
 * @author User Profile Team
 * @version 1.2.0
 * @since 1.2.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    /**
     * 环绕通知：在方法执行前后加锁解锁
     *
     * @param pjp 切点
     * @param distributedLock 锁注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        String lockKey = distributedLock.key();
        RLock lock = redissonClient.getLock(lockKey);

        // 获取方法信息用于日志
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        boolean acquired = false;
        try {
            // 尝试获取锁
            log.debug("尝试获取分布式锁: key={}, method={}, waitTime={}s, leaseTime={}s",
                    lockKey, methodName, distributedLock.waitTime(), distributedLock.leaseTime());

            acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (acquired) {
                log.info("✓ 获取分布式锁成功: key={}, method={}", lockKey, methodName);

                // 执行业务方法
                long startTime = System.currentTimeMillis();
                Object result = pjp.proceed();
                long duration = System.currentTimeMillis() - startTime;

                log.info("✓ 方法执行完成: method={}, duration={}ms", methodName, duration);
                return result;

            } else {
                // 获取锁失败
                log.warn("✗ 获取分布式锁失败，任务跳过: key={}, method={}", lockKey, methodName);

                if (distributedLock.throwOnFailure()) {
                    throw new RuntimeException(String.format(
                            "Failed to acquire distributed lock: key=%s, method=%s",
                            lockKey, methodName));
                }

                return null;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: key={}, method={}", lockKey, methodName, e);

            if (distributedLock.throwOnFailure()) {
                throw new RuntimeException("Lock acquisition interrupted", e);
            }

            return null;

        } finally {
            // 释放锁（只释放当前线程持有的锁）
            if (acquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.info("✓ 释放分布式锁: key={}, method={}", lockKey, methodName);
                } catch (IllegalMonitorStateException e) {
                    log.warn("释放锁异常（可能已过期）: key={}, method={}", lockKey, methodName, e);
                }
            }
        }
    }
}
