package com.userprofile.user.client;

import com.userprofile.user.dto.InitialProfileRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * ProfileClient降级工厂
 * 当profile-service不可用时提供降级处理
 *
 * @author User Profile Team
 * @version 1.5.0
 */
@Slf4j
@Component
public class ProfileClientFallbackFactory implements FallbackFactory<ProfileClient> {

    @Override
    public ProfileClient create(Throwable cause) {
        return new ProfileClient() {
            @Override
            public void initializeProfile(InitialProfileRequest request) {
                log.error("调用profile-service初始化画像失败,userId={}, 错误: {}",
                        request.getUserId(), cause.getMessage(), cause);
                // 降级处理:记录失败,可以后续通过补偿机制重试
                // 不抛出异常,避免影响用户注册流程
            }
        };
    }
}
