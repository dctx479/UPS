package com.userprofile.user.client;

import com.userprofile.user.dto.InitialProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Profile Service Feign客户端
 * 用于服务间通信 - P1-2修复
 *
 * @author User Profile Team
 * @version 1.5.0
 */
@FeignClient(
    name = "profile-service",
    path = "/api/profiles",
    fallbackFactory = ProfileClientFallbackFactory.class
)
public interface ProfileClient {

    /**
     * 创建初始用户画像
     *
     * @param request 初始化请求
     */
    @PostMapping("/initialize")
    void initializeProfile(@RequestBody InitialProfileRequest request);
}
