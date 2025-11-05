package com.userprofile.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    @Column(nullable = false, length = 100)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * 姓名
     */
    @Column(length = 50)
    private String name;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 电话号码
     */
    @Column(length = 20)
    private String phone;

    /**
     * 电子邮箱
     */
    @Column(length = 100)
    private String email;

    /**
     * 地域/地区
     */
    @Column(length = 100)
    private String region;

    /**
     * 星座
     */
    @Column(length = 20)
    private String zodiacSign;

    /**
     * 职业
     */
    @Column(length = 50)
    private String occupation;

    /**
     * 收入水平
     */
    @Column(length = 50)
    private String incomeLevel;

    /**
     * 资产负债比
     */
    private Double debtToAssetRatio;

    /**
     * 用户状态：0-禁用，1-正常
     */
    @Column(nullable = false)
    private Integer status = 1;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    private LocalDateTime updateTime;
}
