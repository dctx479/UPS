package com.userprofile.tag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 标签服务启动类
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class TagServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TagServiceApplication.class, args);
    }
}
