package com.userprofile.user.service;

import com.userprofile.common.exception.BusinessException;
import com.userprofile.common.response.PageResult;
import com.userprofile.user.dto.UserDTO;
import com.userprofile.user.entity.User;
import com.userprofile.user.event.UserCreatedEvent;
import com.userprofile.user.event.UserUpdatedEvent;
import com.userprofile.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建用户
     */
    @Transactional
    public User createUser(UserDTO userDTO) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 检查密码
        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }

        User user = new User();
        BeanUtils.copyProperties(userDTO, user);

        // 加密密码
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setStatus(1);

        User savedUser = userRepository.save(user);

        // 发布用户创建事件
        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getName(),
                savedUser.getEmail()
        );
        eventPublisher.publishEvent(event);
        log.info("发布用户创建事件: userId={}, username={}", savedUser.getId(), savedUser.getUsername());

        return savedUser;
    }

    /**
     * 更新用户
     */
    @Transactional
    public User updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 如果用户名发生变化,检查新用户名是否已被占用
        if (!user.getUsername().equals(userDTO.getUsername())) {
            if (userRepository.existsByUsername(userDTO.getUsername())) {
                throw new BusinessException("用户名已存在");
            }
        }

        BeanUtils.copyProperties(userDTO, user, "password"); // 排除密码字段
        user.setId(id);

        // 如果提供了新密码,则更新密码
        if (userDTO.getPassword() != null && !userDTO.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        User updatedUser = userRepository.save(user);

        // 发布用户更新事件
        UserUpdatedEvent event = new UserUpdatedEvent(updatedUser.getId(), updatedUser.getUsername());
        eventPublisher.publishEvent(event);
        log.info("发布用户更新事件: userId={}, username={}", updatedUser.getId(), updatedUser.getUsername());

        return updatedUser;
    }

    /**
     * 根据ID查询用户
     * 添加sync=true防止缓存击穿(P2-4修复)
     */
    @Cacheable(value = "users", key = "#id", sync = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 根据用户名查询用户
     * 添加sync=true防止缓存击穿(P2-4修复)
     */
    @Cacheable(value = "users", key = "'username:' + #username", sync = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 分页查询用户列表
     */
    public PageResult<User> getUserList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<User> userPage = userRepository.findAll(pageRequest);

        return new PageResult<>(
                userPage.getContent(),
                userPage.getTotalElements(),
                (long) page,
                (long) size
        );
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    /**
     * 获取所有用户 (已废弃,请使用getUserList分页查询)
     * @deprecated 请使用 {@link #getUserList(int, int)} 进行分页查询
     */
    @Deprecated
    public List<User> getAllUsers() {
        log.warn("调用了废弃的getAllUsers()方法,建议使用getUserList(page, size)进行分页查询");
        // 限制返回最近100条,避免数据量过大
        PageRequest pageRequest = PageRequest.of(0, 100);
        return userRepository.findAll(pageRequest).getContent();
    }
}
