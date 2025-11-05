package com.userprofile.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger聚合配置
 *
 * <p>将所有微服务的Swagger文档聚合到Gateway统一访问
 *
 * @author User Profile Team
 * @version 1.4.0
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * User Service API文档分组
     */
    @Bean
    public GroupedOpenApi userServiceApi() {
        return GroupedOpenApi.builder()
                .group("1. User Service")
                .pathsToMatch("/api/users/**", "/api/auth/**")
                .build();
    }

    /**
     * Profile Service API文档分组
     */
    @Bean
    public GroupedOpenApi profileServiceApi() {
        return GroupedOpenApi.builder()
                .group("2. Profile Service")
                .pathsToMatch(
                        "/api/profiles/**",
                        "/api/events/**",
                        "/api/segments/**",
                        "/api/recommendations/**"
                )
                .build();
    }

    /**
     * Tag Service API文档分组
     */
    @Bean
    public GroupedOpenApi tagServiceApi() {
        return GroupedOpenApi.builder()
                .group("3. Tag Service")
                .pathsToMatch("/api/tags/**")
                .build();
    }

    /**
     * Gateway API文档分组 (Actuator endpoints)
     */
    @Bean
    public GroupedOpenApi gatewayApi() {
        return GroupedOpenApi.builder()
                .group("0. Gateway")
                .pathsToMatch("/actuator/**")
                .build();
    }
}
