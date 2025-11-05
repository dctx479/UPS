package com.userprofile.common.event;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件基类
 *
 * @author User Profile Team
 * @version 1.3.0
 */
@Data
public abstract class DomainEvent {

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件发生时间
     */
    private LocalDateTime occurredOn;

    /**
     * 聚合根ID
     */
    private String aggregateId;

    /**
     * 构造函数
     */
    public DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
    }

    /**
     * 带聚合根ID的构造函数
     */
    public DomainEvent(String aggregateId) {
        this();
        this.aggregateId = aggregateId;
    }
}
