package com.userprofile.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步事件监听器基类
 *
 * <p>提供异步事件处理能力，所有事件监听器可继承此类
 *
 * @author User Profile Team
 * @version 1.3.0
 */
@Slf4j
@Component
public abstract class AsyncEventListener {

    /**
     * 事件处理前置钩子
     */
    protected void beforeHandle(DomainEvent event) {
        log.info("开始处理事件: eventId={}, eventType={}, aggregateId={}",
                event.getEventId(), event.getEventType(), event.getAggregateId());
    }

    /**
     * 事件处理后置钩子
     */
    protected void afterHandle(DomainEvent event, boolean success, Exception error) {
        if (success) {
            log.info("事件处理成功: eventId={}, eventType={}",
                    event.getEventId(), event.getEventType());
        } else {
            log.error("事件处理失败: eventId={}, eventType={}, error={}",
                    event.getEventId(), event.getEventType(), error.getMessage(), error);
        }
    }

    /**
     * 异步处理事件
     */
    @Async
    @EventListener
    public void handleAsync(DomainEvent event) {
        beforeHandle(event);
        boolean success = false;
        Exception error = null;

        try {
            doHandle(event);
            success = true;
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            afterHandle(event, success, error);
        }
    }

    /**
     * 子类实现具体的事件处理逻辑
     */
    protected abstract void doHandle(DomainEvent event);
}
