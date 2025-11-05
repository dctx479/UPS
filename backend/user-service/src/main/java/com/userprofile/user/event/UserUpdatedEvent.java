package com.userprofile.user.event;

import com.userprofile.common.event.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户更新事件
 *
 * @author User Profile Team
 * @version 1.3.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserUpdatedEvent extends DomainEvent {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    public UserUpdatedEvent(Long userId, String username) {
        super(String.valueOf(userId));
        this.userId = userId;
        this.username = username;
    }
}
