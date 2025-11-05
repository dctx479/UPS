package com.userprofile.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Gateway配置
 * P1-5修复: 添加限流KeyResolver配置
 */
@Configuration
public class GatewayConfig {

    /**
     * 基于IP地址的限流KeyResolver (默认)
     * 防止单个IP地址恶意请求
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ipAddress = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
            return Mono.just(ipAddress);
        };
    }

    /**
     * 基于用户ID的限流KeyResolver
     * 可以针对不同用户设置不同的限流策略
     * 使用方式: 在路由配置中指定 key-resolver: "#{@userKeyResolver}"
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst("X-User-Id")
        ).defaultIfEmpty("anonymous");
    }

    /**
     * 基于API路径的限流KeyResolver
     * 可以针对不同的API端点设置不同的限流策略
     * 使用方式: 在路由配置中指定 key-resolver: "#{@pathKeyResolver}"
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getPath().value()
        );
    }

    /**
     * 组合限流KeyResolver (IP + 用户ID)
     * 同时基于IP和用户ID进行限流，更加精细
     * 使用方式: 在路由配置中指定 key-resolver: "#{@combinedKeyResolver}"
     */
    @Bean
    public KeyResolver combinedKeyResolver() {
        return exchange -> {
            String ipAddress = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();

            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");

            String key = ipAddress + ":" + (userId != null ? userId : "anonymous");
            return Mono.just(key);
        };
    }
}
