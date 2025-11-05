package com.userprofile.profile.repository;

import com.userprofile.profile.entity.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户画像数据访问接口
 */
@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    /**
     * 根据用户ID查找画像
     */
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * 根据用户名查找画像
     */
    Optional<UserProfile> findByUsername(String username);

    /**
     * 检查用户画像是否存在
     */
    boolean existsByUserId(Long userId);

    /**
     * 删除用户画像
     */
    void deleteByUserId(Long userId);
}
