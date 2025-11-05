package com.userprofile.profile.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户事件数据访问接口
 */
@Repository
public interface UserEventRepository extends MongoRepository<UserEvent, String> {

    /**
     * 查询用户的事件列表
     */
    List<UserEvent> findByUserIdOrderByEventTimeDesc(Long userId);

    /**
     * 查询用户指定时间范围内的事件
     */
    List<UserEvent> findByUserIdAndEventTimeBetween(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 查询用户特定类型的事件
     */
    List<UserEvent> findByUserIdAndEventType(Long userId, UserEvent.EventType eventType);

    /**
     * 查询未处理的事件（用于批量计算画像）
     */
    @Query("{'processed': false}")
    List<UserEvent> findUnprocessedEvents();

    /**
     * 查询用户最近N天的���件
     */
    @Query("{'userId': ?0, 'eventTime': {$gte: ?1}}")
    List<UserEvent> findRecentEvents(Long userId, LocalDateTime sinceTime);

    /**
     * 统计用户事件数量
     */
    long countByUserId(Long userId);

    /**
     * 统计用户特定类型事件数量
     */
    long countByUserIdAndEventType(Long userId, UserEvent.EventType eventType);

    /**
     * 分页查询用户事件
     */
    Page<UserEvent> findByUserId(Long userId, Pageable pageable);
}
