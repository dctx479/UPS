package com.userprofile.profile.segmentation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户分群数据访问接口
 */
@Repository
public interface UserSegmentRepository extends MongoRepository<UserSegment, String> {

    /**
     * 根据类型和状态查询分群
     */
    List<UserSegment> findByTypeAndActive(UserSegment.SegmentType type, Boolean active);

    /**
     * 根据创建人查询分群
     */
    List<UserSegment> findByCreator(String creator);

    /**
     * 根据名称查询分群
     */
    UserSegment findByName(String name);

    /**
     * 查询所有启用的分群
     */
    List<UserSegment> findByActive(Boolean active);
}
