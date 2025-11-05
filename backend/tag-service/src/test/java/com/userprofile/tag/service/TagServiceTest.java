package com.userprofile.tag.service;

import com.userprofile.common.exception.BusinessException;
import com.userprofile.tag.entity.UserTag;
import com.userprofile.tag.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * TagService单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagService测试")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private UserTag testTag;

    @BeforeEach
    void setUp() {
        testTag = new UserTag();
        testTag.setId(1L);
        testTag.setUserId(100L);
        testTag.setTagName("科技爱好者");
        testTag.setCategory("兴趣爱好");
        testTag.setWeight(0.8);
        testTag.setSource("系统自动");
        testTag.setActive(true);
        testTag.setExpireTime(LocalDateTime.now().plusMonths(1));
        testTag.setCreateTime(LocalDateTime.now());
        testTag.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建新标签 - 成功")
    void createTag_New_Success() {
        // Given
        when(tagRepository.findByUserIdAndTagName(100L, "科技爱好者")).thenReturn(Optional.empty());
        when(tagRepository.save(any(UserTag.class))).thenReturn(testTag);

        // When
        UserTag result = tagService.createTag(testTag);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTagName()).isEqualTo("科技爱好者");
        assertThat(result.getActive()).isTrue();

        verify(tagRepository, times(1)).findByUserIdAndTagName(100L, "科技爱好者");
        verify(tagRepository, times(1)).save(any(UserTag.class));
    }

    @Test
    @DisplayName("创建标签 - 已存在则更新")
    void createTag_Existing_Update() {
        // Given
        UserTag existingTag = new UserTag();
        existingTag.setId(1L);
        existingTag.setUserId(100L);
        existingTag.setTagName("科技爱好者");
        existingTag.setWeight(0.5);

        UserTag newTag = new UserTag();
        newTag.setUserId(100L);
        newTag.setTagName("科技爱好者");
        newTag.setWeight(0.9);
        newTag.setCategory("更新分类");

        when(tagRepository.findByUserIdAndTagName(100L, "科技爱好者"))
                .thenReturn(Optional.of(existingTag));
        when(tagRepository.save(any(UserTag.class))).thenReturn(existingTag);

        // When
        UserTag result = tagService.createTag(newTag);

        // Then
        ArgumentCaptor<UserTag> captor = ArgumentCaptor.forClass(UserTag.class);
        verify(tagRepository).save(captor.capture());

        UserTag savedTag = captor.getValue();
        assertThat(savedTag.getWeight()).isEqualTo(0.9); // 权重已更新
        assertThat(savedTag.getCategory()).isEqualTo("更新分类");
        assertThat(savedTag.getActive()).isTrue();
    }

    @Test
    @DisplayName("批量创建标签 - 成功")
    void createTags_Batch_Success() {
        // Given
        List<UserTag> tags = Arrays.asList(
                createTag(null, 100L, "标签1", "分类A", 0.8),
                createTag(null, 100L, "标签2", "分类B", 0.6),
                createTag(null, 100L, "标签3", "分类A", 0.9)
        );

        when(tagRepository.saveAll(anyList())).thenReturn(tags);

        // When
        List<UserTag> result = tagService.createTags(100L, tags);

        // Then
        assertThat(result).hasSize(3);
        verify(tagRepository, times(1)).saveAll(anyList());

        // 验证所有标签的userId都被设置
        ArgumentCaptor<List<UserTag>> captor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(captor.capture());
        List<UserTag> savedTags = captor.getValue();
        assertThat(savedTags).allMatch(tag -> tag.getUserId().equals(100L));
        assertThat(savedTags).allMatch(tag -> tag.getActive() != null && tag.getActive());
    }

    @Test
    @DisplayName("更新标签 - 成功")
    void updateTag_Success() {
        // Given
        UserTag updateData = new UserTag();
        updateData.setTagName("更新后的标签");
        updateData.setWeight(0.95);

        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        when(tagRepository.save(any(UserTag.class))).thenReturn(testTag);

        // When
        UserTag result = tagService.updateTag(1L, updateData);

        // Then
        ArgumentCaptor<UserTag> captor = ArgumentCaptor.forClass(UserTag.class);
        verify(tagRepository).save(captor.capture());

        UserTag savedTag = captor.getValue();
        assertThat(savedTag.getTagName()).isEqualTo("更新后的标签");
        assertThat(savedTag.getWeight()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("更新标签 - 标签不存在抛出异常")
    void updateTag_NotFound_ThrowException() {
        // Given
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tagService.updateTag(999L, new UserTag()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签不存在");
    }

    @Test
    @DisplayName("删除标签 - 成功")
    void deleteTag_Success() {
        // Given
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        doNothing().when(tagRepository).deleteById(1L);

        // When
        tagService.deleteTag(1L, 100L);

        // Then
        verify(tagRepository, times(1)).findById(1L);
        verify(tagRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("删除标签 - 无权限抛出异常")
    void deleteTag_Unauthorized_ThrowException() {
        // Given
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));

        // When & Then - 尝试用其他用户ID删除
        assertThatThrownBy(() -> tagService.deleteTag(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权删除该标签");

        verify(tagRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除用户所有标签 - 成功")
    void deleteUserAllTags_Success() {
        // Given
        doNothing().when(tagRepository).deleteByUserId(100L);

        // When
        tagService.deleteUserAllTags(100L);

        // Then
        verify(tagRepository, times(1)).deleteByUserId(100L);
    }

    @Test
    @DisplayName("根据ID查询标签 - 成功")
    void getTagById_Found_Success() {
        // Given
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));

        // When
        Optional<UserTag> result = tagService.getTagById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTagName()).isEqualTo("科技爱好者");
    }

    @Test
    @DisplayName("查询用户所有标签 - 成功")
    void getUserTags_Success() {
        // Given
        List<UserTag> tags = Arrays.asList(
                createTag(1L, 100L, "标签1", "分类A", 0.8),
                createTag(2L, 100L, "标签2", "分类B", 0.6),
                createTag(3L, 100L, "标签3", "分类A", 0.9)
        );
        when(tagRepository.findByUserId(100L)).thenReturn(tags);

        // When
        List<UserTag> result = tagService.getUserTags(100L);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(tag -> tag.getUserId().equals(100L));
        verify(tagRepository, times(1)).findByUserId(100L);
    }

    @Test
    @DisplayName("查询用户有效标签 - 成功")
    void getActiveUserTags_Success() {
        // Given
        List<UserTag> activeTags = Arrays.asList(
                createTag(1L, 100L, "有效标签1", "分类A", 0.8),
                createTag(2L, 100L, "有效标签2", "分类B", 0.9)
        );
        when(tagRepository.findActiveTagsByUserId(eq(100L), any(LocalDateTime.class)))
                .thenReturn(activeTags);

        // When
        List<UserTag> result = tagService.getActiveUserTags(100L);

        // Then
        assertThat(result).hasSize(2);
        verify(tagRepository, times(1)).findActiveTagsByUserId(eq(100L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("根据分类查询标签 - 成功")
    void getUserTagsByCategory_Success() {
        // Given
        List<UserTag> categoryTags = Arrays.asList(
                createTag(1L, 100L, "标签1", "兴趣爱好", 0.8),
                createTag(2L, 100L, "标签2", "兴趣爱好", 0.7)
        );
        when(tagRepository.findByUserIdAndCategory(100L, "兴趣爱好")).thenReturn(categoryTags);

        // When
        List<UserTag> result = tagService.getUserTagsByCategory(100L, "兴趣爱好");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(tag -> tag.getCategory().equals("兴趣爱好"));
    }

    @Test
    @DisplayName("查询高权重标签 - 成功")
    void getHighWeightTags_Success() {
        // Given
        List<UserTag> highWeightTags = Arrays.asList(
                createTag(1L, 100L, "高权重标签1", "分类A", 0.9),
                createTag(2L, 100L, "高权重标签2", "分类B", 0.95)
        );
        when(tagRepository.findHighWeightTags(100L)).thenReturn(highWeightTags);

        // When
        List<UserTag> result = tagService.getHighWeightTags(100L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(tag -> tag.getWeight() >= 0.9);
    }

    @Test
    @DisplayName("分页查询标签 - 成功")
    void getUserTagsPage_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<UserTag> tags = Arrays.asList(testTag);
        Page<UserTag> page = new PageImpl<>(tags, pageable, 1);

        when(tagRepository.findByUserId(100L, pageable)).thenReturn(page);

        // When
        Page<UserTag> result = tagService.getUserTagsPage(100L, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("统计用户标签数量 - 成功")
    void countUserTags_Success() {
        // Given
        when(tagRepository.countByUserId(100L)).thenReturn(15L);

        // When
        long count = tagService.countUserTags(100L);

        // Then
        assertThat(count).isEqualTo(15L);
    }

    @Test
    @DisplayName("统计分类标签数量 - 成功")
    void countCategoryTags_Success() {
        // Given
        when(tagRepository.countByCategory("兴趣爱好")).thenReturn(25L);

        // When
        long count = tagService.countCategoryTags("兴趣爱好");

        // Then
        assertThat(count).isEqualTo(25L);
    }

    @Test
    @DisplayName("标签去重 - 成功")
    void deduplicateTags_Success() {
        // Given - 3个重复的标签
        UserTag tag1 = createTag(1L, 100L, "重复标签", "分类A", 0.8);
        tag1.setUpdateTime(LocalDateTime.now().minusDays(2));

        UserTag tag2 = createTag(2L, 100L, "重复标签", "分类A", 0.7);
        tag2.setUpdateTime(LocalDateTime.now().minusDays(1));

        UserTag tag3 = createTag(3L, 100L, "重复标签", "分类A", 0.9);
        tag3.setUpdateTime(LocalDateTime.now()); // 最新的

        UserTag tag4 = createTag(4L, 100L, "唯一标签", "分类B", 0.6);

        List<UserTag> tags = Arrays.asList(tag1, tag2, tag3, tag4);
        when(tagRepository.findByUserId(100L)).thenReturn(tags);
        doNothing().when(tagRepository).deleteById(anyLong());

        // When
        tagService.deduplicateTags(100L);

        // Then
        // 应该删除tag1和tag2,保留最新的tag3和唯一的tag4
        verify(tagRepository, times(1)).deleteById(1L); // tag1被删除
        verify(tagRepository, times(1)).deleteById(2L); // tag2被删除
        verify(tagRepository, never()).deleteById(3L);  // tag3保留
        verify(tagRepository, never()).deleteById(4L);  // tag4保留
    }

    @Test
    @DisplayName("调整标签权重 - 成功")
    void adjustTagWeight_Success() {
        // Given
        when(tagRepository.findByUserIdAndTagName(100L, "科技爱好者"))
                .thenReturn(Optional.of(testTag));
        when(tagRepository.save(any(UserTag.class))).thenReturn(testTag);

        // When - 增加0.1
        tagService.adjustTagWeight(100L, "科技爱好者", 0.1);

        // Then
        ArgumentCaptor<UserTag> captor = ArgumentCaptor.forClass(UserTag.class);
        verify(tagRepository).save(captor.capture());

        UserTag savedTag = captor.getValue();
        assertThat(savedTag.getWeight()).isEqualTo(0.9); // 0.8 + 0.1
    }

    @Test
    @DisplayName("调整标签权重 - 不超过上限")
    void adjustTagWeight_NotExceedMax() {
        // Given
        testTag.setWeight(0.95);
        when(tagRepository.findByUserIdAndTagName(100L, "科技爱好者"))
                .thenReturn(Optional.of(testTag));
        when(tagRepository.save(any(UserTag.class))).thenReturn(testTag);

        // When - 尝试增加0.2,但不应超过1.0
        tagService.adjustTagWeight(100L, "科技爱好者", 0.2);

        // Then
        ArgumentCaptor<UserTag> captor = ArgumentCaptor.forClass(UserTag.class);
        verify(tagRepository).save(captor.capture());

        UserTag savedTag = captor.getValue();
        assertThat(savedTag.getWeight()).isEqualTo(1.0); // 上限是1.0
    }

    @Test
    @DisplayName("调整标签权重 - 不低于下限")
    void adjustTagWeight_NotBelowMin() {
        // Given
        testTag.setWeight(0.05);
        when(tagRepository.findByUserIdAndTagName(100L, "科技爱好者"))
                .thenReturn(Optional.of(testTag));
        when(tagRepository.save(any(UserTag.class))).thenReturn(testTag);

        // When - 尝试减少0.2,但不应低于0.0
        tagService.adjustTagWeight(100L, "科技爱好者", -0.2);

        // Then
        ArgumentCaptor<UserTag> captor = ArgumentCaptor.forClass(UserTag.class);
        verify(tagRepository).save(captor.capture());

        UserTag savedTag = captor.getValue();
        assertThat(savedTag.getWeight()).isEqualTo(0.0); // 下限是0.0
    }

    // 辅助方法
    private UserTag createTag(Long id, Long userId, String tagName, String category, Double weight) {
        UserTag tag = new UserTag();
        tag.setId(id);
        tag.setUserId(userId);
        tag.setTagName(tagName);
        tag.setCategory(category);
        tag.setWeight(weight);
        tag.setActive(true);
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        return tag;
    }
}
