package com.userprofile.tag.repository;

import com.userprofile.tag.entity.UserTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 标签数据访问层
 */
@Repository
public interface TagRepository extends JpaRepository<UserTag, Long> {

    /**
     * 根据用户ID查询所有标签
     */
    List<UserTag> findByUserId(Long userId);

    /**
     * 根据用户ID和标签名称查询
     */
    Optional<UserTag> findByUserIdAndTagName(Long userId, String tagName);

    /**
     * 根据标签分类查询
     */
    List<UserTag> findByCategory(String category);

    /**
     * 根据用户ID和分类查询
     */
    List<UserTag> findByUserIdAndCategory(Long userId, String category);

    /**
     * 查询用户的有效标签
     */
    @Query("SELECT t FROM UserTag t WHERE t.userId = :userId AND t.active = true " +
            "AND (t.expireTime IS NULL OR t.expireTime > :now)")
    List<UserTag> findActiveTagsByUserId(@Param("userId") Long userId,
                                         @Param("now") LocalDateTime now);

    /**
     * 根据标签来源查询
     */
    List<UserTag> findBySource(UserTag.TagSource source);

    /**
     * 查询已过期的标签
     */
    @Query("SELECT t FROM UserTag t WHERE t.expireTime IS NOT NULL AND t.expireTime <= :now")
    List<UserTag> findExpiredTags(@Param("now") LocalDateTime now);

    /**
     * 分页查询标签
     */
    Page<UserTag> findByUserId(Long userId, Pageable pageable);

    /**
     * 统计用户标签数量
     */
    long countByUserId(Long userId);

    /**
     * 统计某个分类的标签数量
     */
    long countByCategory(String category);

    /**
     * 删除用户的所有标签
     */
    void deleteByUserId(Long userId);

    /**
     * 根据权重范围查询标签
     */
    @Query("SELECT t FROM UserTag t WHERE t.userId = :userId AND t.weight >= :minWeight " +
            "AND t.weight <= :maxWeight ORDER BY t.weight DESC")
    List<UserTag> findByUserIdAndWeightBetween(@Param("userId") Long userId,
                                                @Param("minWeight") Double minWeight,
                                                @Param("maxWeight") Double maxWeight);

    /**
     * 查询高权重标签（权重>=0.7）
     */
    @Query("SELECT t FROM UserTag t WHERE t.userId = :userId AND t.weight >= 0.7 " +
            "AND t.active = true ORDER BY t.weight DESC")
    List<UserTag> findHighWeightTags(@Param("userId") Long userId);
}
