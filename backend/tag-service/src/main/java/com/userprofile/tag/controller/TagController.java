package com.userprofile.tag.controller;

import com.userprofile.common.response.Result;
import com.userprofile.tag.dto.TagDTO;
import com.userprofile.tag.entity.UserTag;
import com.userprofile.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "标签管理", description = "用户标签管理接口")
public class TagController {

    private final TagService tagService;

    /**
     * 创建标签
     */
    @PostMapping
    @Operation(summary = "创建标签", description = "为用户创建新标签")
    public Result<TagDTO> createTag(@Valid @RequestBody TagDTO tagDTO) {
        log.info("创建标签请求: {}", tagDTO);

        UserTag tag = tagDTO.toEntity();
        UserTag created = tagService.createTag(tag);

        return Result.success(TagDTO.fromEntity(created));
    }

    /**
     * 批量创建标签
     */
    @PostMapping("/batch")
    @Operation(summary = "批量创建标签", description = "批量为用户创建标签")
    public Result<List<TagDTO>> createTags(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Valid @RequestBody List<TagDTO> tagDTOs) {

        log.info("批量创建标签: userId={}, count={}", userId, tagDTOs.size());

        List<UserTag> tags = tagDTOs.stream()
                .map(TagDTO::toEntity)
                .collect(Collectors.toList());

        List<UserTag> created = tagService.createTags(userId, tags);

        List<TagDTO> result = created.stream()
                .map(TagDTO::fromEntity)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 更新标签
     */
    @PutMapping("/{tagId}")
    @Operation(summary = "更新标签", description = "更新标签信息")
    public Result<TagDTO> updateTag(
            @Parameter(description = "标签ID") @PathVariable Long tagId,
            @Valid @RequestBody TagDTO tagDTO) {

        log.info("更新标签: tagId={}, data={}", tagId, tagDTO);

        UserTag tagUpdate = tagDTO.toEntity();
        UserTag updated = tagService.updateTag(tagId, tagUpdate);

        return Result.success(TagDTO.fromEntity(updated));
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{tagId}")
    @Operation(summary = "删除标签", description = "删除指定标签")
    public Result<Void> deleteTag(
            @Parameter(description = "标签ID") @PathVariable Long tagId,
            @Parameter(description = "用户ID") @RequestParam Long userId) {

        log.info("删除标签: tagId={}, userId={}", tagId, userId);
        tagService.deleteTag(tagId, userId);

        return Result.success(null);
    }

    /**
     * 删除用户所有标签
     */
    @DeleteMapping("/user/{userId}")
    @Operation(summary = "删除用户所有标签", description = "删除用户的所有标签")
    public Result<Void> deleteUserTags(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        log.info("删除用户所有标签: userId={}", userId);
        tagService.deleteUserAllTags(userId);

        return Result.success(null);
    }

    /**
     * 查询标签
     */
    @GetMapping("/{tagId}")
    @Operation(summary = "查询标签", description = "根据ID查询标签详情")
    public Result<TagDTO> getTag(
            @Parameter(description = "标签ID") @PathVariable Long tagId) {

        return tagService.getTagById(tagId)
                .map(TagDTO::fromEntity)
                .map(Result::success)
                .orElseGet(() -> Result.error(404, "标签不存在"));
    }

    /**
     * 查询用户所有标签
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户标签", description = "查询用户的所有标签")
    public Result<List<TagDTO>> getUserTags(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        log.debug("查询用户标签: userId={}", userId);

        List<UserTag> tags = tagService.getUserTags(userId);
        List<TagDTO> result = tags.stream()
                .map(TagDTO::fromEntity)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 查询用户有效标签
     */
    @GetMapping("/user/{userId}/active")
    @Operation(summary = "查询有效标签", description = "查询用户的有效标签（未过期）")
    public Result<List<TagDTO>> getActiveUserTags(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        List<UserTag> tags = tagService.getActiveUserTags(userId);
        List<TagDTO> result = tags.stream()
                .map(TagDTO::fromEntity)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 查询用户高权重标签
     */
    @GetMapping("/user/{userId}/high-weight")
    @Operation(summary = "查询高权重标签", description = "查询用户的高权重标签（权重>=0.7）")
    public Result<List<TagDTO>> getHighWeightTags(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        List<UserTag> tags = tagService.getHighWeightTags(userId);
        List<TagDTO> result = tags.stream()
                .map(TagDTO::fromEntity)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 根据分类查询标签
     */
    @GetMapping("/user/{userId}/category/{category}")
    @Operation(summary = "按分类查询标签", description = "根据分类查询用户标签")
    public Result<List<TagDTO>> getUserTagsByCategory(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "标签分类") @PathVariable String category) {

        List<UserTag> tags = tagService.getUserTagsByCategory(userId, category);
        List<TagDTO> result = tags.stream()
                .map(TagDTO::fromEntity)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 分页查询用户标签
     */
    @GetMapping("/user/{userId}/page")
    @Operation(summary = "分页查询标签", description = "分页查询用户标签")
    public Result<Page<TagDTO>> getUserTagsPage(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "updateTime") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserTag> tagPage = tagService.getUserTagsPage(userId, pageable);

        Page<TagDTO> result = tagPage.map(TagDTO::fromEntity);

        return Result.success(result);
    }

    /**
     * 统计用户标签数量
     */
    @GetMapping("/user/{userId}/count")
    @Operation(summary = "统计标签数量", description = "统计用户的标签数量")
    public Result<Long> countUserTags(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        long count = tagService.countUserTags(userId);
        return Result.success(count);
    }

    /**
     * 标签去重
     */
    @PostMapping("/user/{userId}/deduplicate")
    @Operation(summary = "标签去重", description = "去除用户的重复标签")
    public Result<Void> deduplicateTags(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        log.info("标签去重: userId={}", userId);
        tagService.deduplicateTags(userId);

        return Result.success(null);
    }

    /**
     * 调整标签权重
     */
    @PostMapping("/adjust-weight")
    @Operation(summary = "调整标签权重", description = "调整标签权重")
    public Result<Void> adjustTagWeight(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "标签名称") @RequestParam String tagName,
            @Parameter(description = "权重变化量") @RequestParam double delta) {

        log.info("调整标签权重: userId={}, tagName={}, delta={}", userId, tagName, delta);
        tagService.adjustTagWeight(userId, tagName, delta);

        return Result.success(null);
    }
}
