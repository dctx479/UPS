package com.userprofile.tag.service;

import com.userprofile.common.exception.BusinessException;
import com.userprofile.common.lock.DistributedLock;
import com.userprofile.tag.entity.UserTag;
import com.userprofile.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 标签服务层
 *
 * <p>提供用户标签的完整管理功能，包括CRUD、缓存、定时清理等
 *
 * @author User Profile Team
 * @version 1.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    /**
     * 创建标签
     */
    @Transactional
    @CacheEvict(value = "userTags", key = "#tag.userId")
    public UserTag createTag(UserTag tag) {
        // 检查是否已存在相同标签
        Optional<UserTag> existing = tagRepository.findByUserIdAndTagName(
                tag.getUserId(), tag.getTagName());

        if (existing.isPresent()) {
            // 更新现有标签
            UserTag existingTag = existing.get();
            existingTag.setWeight(tag.getWeight());
            existingTag.setCategory(tag.getCategory());
            existingTag.setSource(tag.getSource());
            existingTag.setExpireTime(tag.getExpireTime());
            existingTag.setActive(true);
            log.info("更新已存在标签: userId={}, tagName={}", tag.getUserId(), tag.getTagName());
            return tagRepository.save(existingTag);
        }

        // 创建新标签
        if (tag.getActive() == null) {
            tag.setActive(true);
        }
        if (tag.getWeight() == null) {
            tag.setWeight(1.0);
        }

        log.info("创建新标签: userId={}, tagName={}, category={}, source={}",
                tag.getUserId(), tag.getTagName(), tag.getCategory(), tag.getSource());

        return tagRepository.save(tag);
    }

    /**
     * 批量创建标签
     */
    @Transactional
    @CacheEvict(value = "userTags", key = "#userId")
    public List<UserTag> createTags(Long userId, List<UserTag> tags) {
        tags.forEach(tag -> {
            tag.setUserId(userId);
            if (tag.getActive() == null) {
                tag.setActive(true);
            }
            if (tag.getWeight() == null) {
                tag.setWeight(1.0);
            }
        });

        log.info("批量创建标签: userId={}, count={}", userId, tags.size());
        return tagRepository.saveAll(tags);
    }

    /**
     * 更新标签
     */
    @Transactional
    @CachePut(value = "userTags", key = "#result.userId")
    public UserTag updateTag(Long tagId, UserTag tagUpdate) {
        UserTag existingTag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException("标签不存在: " + tagId));

        if (tagUpdate.getTagName() != null) {
            existingTag.setTagName(tagUpdate.getTagName());
        }
        if (tagUpdate.getCategory() != null) {
            existingTag.setCategory(tagUpdate.getCategory());
        }
        if (tagUpdate.getWeight() != null) {
            existingTag.setWeight(tagUpdate.getWeight());
        }
        if (tagUpdate.getExpireTime() != null) {
            existingTag.setExpireTime(tagUpdate.getExpireTime());
        }
        if (tagUpdate.getActive() != null) {
            existingTag.setActive(tagUpdate.getActive());
        }

        log.info("更新标签: tagId={}, userId={}", tagId, existingTag.getUserId());
        return tagRepository.save(existingTag);
    }

    /**
     * 删除标签
     */
    @Transactional
    @CacheEvict(value = "userTags", key = "#userId")
    public void deleteTag(Long tagId, Long userId) {
        UserTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException("标签不存在: " + tagId));

        if (!tag.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该标签");
        }

        log.info("删除标签: tagId={}, userId={}", tagId, userId);
        tagRepository.deleteById(tagId);
    }

    /**
     * 删除用户所有标签
     */
    @Transactional
    @CacheEvict(value = "userTags", key = "#userId")
    public void deleteUserAllTags(Long userId) {
        log.info("删除用户所有标签: userId={}", userId);
        tagRepository.deleteByUserId(userId);
    }

    /**
     * 根据ID查询标签
     */
    public Optional<UserTag> getTagById(Long tagId) {
        return tagRepository.findById(tagId);
    }

    /**
     * 查询用户所有标签（带缓存）
     */
    @Cacheable(value = "userTags", key = "#userId")
    public List<UserTag> getUserTags(Long userId) {
        log.debug("查询用户标签: userId={}", userId);
        return tagRepository.findByUserId(userId);
    }

    /**
     * 查询用户有效标签
     */
    public List<UserTag> getActiveUserTags(Long userId) {
        return tagRepository.findActiveTagsByUserId(userId, LocalDateTime.now());
    }

    /**
     * 根据分类查询用户标签
     */
    public List<UserTag> getUserTagsByCategory(Long userId, String category) {
        return tagRepository.findByUserIdAndCategory(userId, category);
    }

    /**
     * 查询高权重标签
     */
    public List<UserTag> getHighWeightTags(Long userId) {
        return tagRepository.findHighWeightTags(userId);
    }

    /**
     * 分页查询用户标签
     */
    public Page<UserTag> getUserTagsPage(Long userId, Pageable pageable) {
        return tagRepository.findByUserId(userId, pageable);
    }

    /**
     * 统计用户标签数量
     */
    public long countUserTags(Long userId) {
        return tagRepository.countByUserId(userId);
    }

    /**
     * 统计分类标签数量
     */
    public long countCategoryTags(String category) {
        return tagRepository.countByCategory(category);
    }

    /**
     * 标签去重
     */
    @Transactional
    @CacheEvict(value = "userTags", key = "#userId")
    public void deduplicateTags(Long userId) {
        List<UserTag> tags = tagRepository.findByUserId(userId);

        // 按标签名分组，保留最新的一个
        tags.stream()
                .collect(java.util.stream.Collectors.groupingBy(UserTag::getTagName))
                .forEach((tagName, tagList) -> {
                    if (tagList.size() > 1) {
                        // 按更新时间排序，保留最新的
                        tagList.sort((t1, t2) -> t2.getUpdateTime().compareTo(t1.getUpdateTime()));
                        for (int i = 1; i < tagList.size(); i++) {
                            tagRepository.deleteById(tagList.get(i).getId());
                        }
                        log.info("去重标签: userId={}, tagName={}, 删除{}个重复", userId, tagName, tagList.size() - 1);
                    }
                });
    }

    /**
     * 定时清理过期标签（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @DistributedLock(key = "tag:cleanup-expired", leaseTime = 1800)
    @Transactional
    public void cleanExpiredTags() {
        List<UserTag> expiredTags = tagRepository.findExpiredTags(LocalDateTime.now());

        if (!expiredTags.isEmpty()) {
            expiredTags.forEach(tag -> {
                tag.setActive(false);
                log.info("标记过期标签: tagId={}, userId={}, tagName={}",
                        tag.getId(), tag.getUserId(), tag.getTagName());
            });

            tagRepository.saveAll(expiredTags);
            log.info("清理过期标签完成，共处理{}个标签", expiredTags.size());
        }
    }

    /**
     * 自动调整标签权重（基于使用频率）
     */
    @Transactional
    public void adjustTagWeight(Long userId, String tagName, double delta) {
        Optional<UserTag> tagOpt = tagRepository.findByUserIdAndTagName(userId, tagName);

        if (tagOpt.isPresent()) {
            UserTag tag = tagOpt.get();
            double newWeight = Math.max(0.0, Math.min(1.0, tag.getWeight() + delta));
            tag.setWeight(newWeight);
            tagRepository.save(tag);

            log.debug("调整标签权重: userId={}, tagName={}, oldWeight={}, newWeight={}",
                    userId, tagName, tag.getWeight(), newWeight);
        }
    }
}
