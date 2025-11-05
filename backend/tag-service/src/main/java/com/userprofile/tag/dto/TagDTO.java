package com.userprofile.tag.dto;

import com.userprofile.tag.entity.UserTag;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO {

    private Long id;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "标签名称不能为空")
    @Size(min = 1, max = 100, message = "标签名称长度必须在1-100之间")
    private String tagName;

    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    @NotNull(message = "标签来源不能为空")
    private UserTag.TagSource source;

    @DecimalMin(value = "0.0", message = "权重不能小于0")
    @DecimalMax(value = "1.0", message = "权重不能大于1")
    private Double weight;

    private LocalDateTime expireTime;

    private Boolean active;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * DTO转Entity
     */
    public UserTag toEntity() {
        UserTag tag = new UserTag();
        tag.setId(this.id);
        tag.setUserId(this.userId);
        tag.setTagName(this.tagName);
        tag.setCategory(this.category);
        tag.setSource(this.source);
        tag.setWeight(this.weight != null ? this.weight : 1.0);
        tag.setExpireTime(this.expireTime);
        tag.setActive(this.active != null ? this.active : true);
        return tag;
    }

    /**
     * Entity转DTO
     */
    public static TagDTO fromEntity(UserTag tag) {
        return TagDTO.builder()
                .id(tag.getId())
                .userId(tag.getUserId())
                .tagName(tag.getTagName())
                .category(tag.getCategory())
                .source(tag.getSource())
                .weight(tag.getWeight())
                .expireTime(tag.getExpireTime())
                .active(tag.getActive())
                .createTime(tag.getCreateTime())
                .updateTime(tag.getUpdateTime())
                .build();
    }
}
