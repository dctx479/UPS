package com.userprofile.user.event;

import com.userprofile.common.event.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户创建事件
 *
 * @author User Profile Team
 * @version 1.3.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserCreatedEvent extends DomainEvent {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 邮箱
     */
    private String email;

    public UserCreatedEvent(Long userId, String username, String name, String email) {
        super(String.valueOf(userId));
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.email = email;
    }
}
