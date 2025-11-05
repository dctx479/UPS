package com.userprofile.user.repository;

import com.userprofile.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据电话号码查找用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 查找最近创建的用户（用于补偿检查）
     * P1-7修复: 支持分布式事务补偿机制
     */
    @Query("SELECT u FROM User u WHERE u.createTime > :since ORDER BY u.createTime DESC")
    List<User> findRecentUsers(LocalDateTime since);
}
