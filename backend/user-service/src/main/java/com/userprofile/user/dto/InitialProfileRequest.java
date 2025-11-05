package com.userprofile.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 初始化用户画像请求
 * P1-2修复
 *
 * @author User Profile Team
 * @version 1.5.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialProfileRequest implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 姓名
     */
    private String name;
}
