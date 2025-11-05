package com.userprofile.profile.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户事件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventService {

    private final UserEventRepository eventRepository;

    /**
     * 保存单个事件
     */
    public UserEvent saveEvent(UserEvent event) {
        if (event.getEventTime() == null) {
            event.setEventTime(LocalDateTime.now());
        }
        if (event.getProcessed() == null) {
            event.setProcessed(false);
        }
        if (event.getWeight() == null) {
            event.setWeight(getDefaultWeight(event.getEventType()));
        }

        log.info("记录用户事件: userId={}, type={}", event.getUserId(), event.getEventType());
        return eventRepository.save(event);
    }

    /**
     * 批量保存事件
     */
    public List<UserEvent> saveEvents(List<UserEvent> events) {
        events.forEach(event -> {
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }
            if (event.getProcessed() == null) {
                event.setProcessed(false);
            }
            if (event.getWeight() == null) {
                event.setWeight(getDefaultWeight(event.getEventType()));
            }
        });

        return eventRepository.saveAll(events);
    }

    /**
     * 获取用户事件列表
     */
    public List<UserEvent> getUserEvents(Long userId) {
        return eventRepository.findByUserIdOrderByEventTimeDesc(userId);
    }

    /**
     * 获取事件的默认权重
     */
    private double getDefaultWeight(UserEvent.EventType eventType) {
        return switch (eventType) {
            case PAY -> 10.0;              // 支付权重最高
            case PLACE_ORDER -> 8.0;        // 下单
            case ADD_TO_CART -> 5.0;        // 加购
            case COLLECT -> 4.0;            // 收藏
            case SHARE -> 3.0;              // 分享
            case PRODUCT_VIEW -> 2.0;       // 商品浏览
            case CLICK -> 1.5;              // 点击
            case PAGE_VIEW -> 1.0;          // 页面浏览
            default -> 1.0;
        };
    }
}
