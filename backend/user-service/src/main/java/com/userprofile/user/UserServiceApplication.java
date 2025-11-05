package com.userprofile.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 用户服务启动类
 * P1-2修复: 启用FeignClients用于服务间调用
 * P1-7修复: 启用Scheduling用于分布式事务补偿
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
