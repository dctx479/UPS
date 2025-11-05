package com.userprofile.tag.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userprofile.tag.dto.TagDTO;
import com.userprofile.tag.entity.UserTag;
import com.userprofile.tag.repository.TagRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 标签服务集成测试
 * P2-1修复: 端到端业务流程测试
 *
 * @author User Profile Team
 * @version 1.6.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("标签服务集成测试")
class TagServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagRepository tagRepository;

    private static final Long TEST_USER_ID = 2000L;
    private static Long createdTagId;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        if (createdTagId == null) {
            tagRepository.deleteByUserId(TEST_USER_ID);
        }
    }

    @Test
    @Order(1)
    @DisplayName("集成测试 1: 创建标签")
    void testCreateTagFlow() throws Exception {
        // 1. 准备标签数据
        TagDTO tagDTO = new TagDTO();
        tagDTO.setUserId(TEST_USER_ID);
        tagDTO.setTagName("高价值用户");
        tagDTO.setTagCategory("价值分���");
        tagDTO.setTagValue("HIGH_VALUE");
        tagDTO.setWeight(90.0);
        tagDTO.setDescription("消费能力强,忠诚度高");

        // 2. 发送创建请求
        MvcResult result = mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.tagName").value("高价值用户"))
                .andExpect(jsonPath("$.data.weight").value(90.0))
                .andReturn();

        // 3. 提取创建的标签ID
        String responseBody = result.getResponse().getContentAsString();
        createdTagId = objectMapper.readTree(responseBody)
                .path("data")
                .path("id")
                .asLong();

        assertThat(createdTagId).isNotNull().isPositive();

        // 4. 验证标签已保存
        UserTag savedTag = tagRepository.findById(createdTagId).orElse(null);
        assertThat(savedTag).isNotNull();
        assertThat(savedTag.getTagName()).isEqualTo("高价值用户");
    }

    @Test
    @Order(2)
    @DisplayName("集成测试 2: 批量创建标签")
    void testBatchCreateTagsFlow() throws Exception {
        // 1. 准备多个标签
        TagDTO tag1 = new TagDTO();
        tag1.setUserId(TEST_USER_ID);
        tag1.setTagName("猫粮爱好者");
        tag1.setTagCategory("兴趣偏好");
        tag1.setWeight(80.0);

        TagDTO tag2 = new TagDTO();
        tag2.setUserId(TEST_USER_ID);
        tag2.setTagName("价格敏感");
        tag2.setTagCategory("消费行为");
        tag2.setWeight(70.0);

        List<TagDTO> tagList = Arrays.asList(tag1, tag2);

        // 2. 发送批量创建请求
        mockMvc.perform(post("/api/tags/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 3. 验证标签已保存
        List<UserTag> userTags = tagRepository.findByUserId(TEST_USER_ID);
        assertThat(userTags.size()).isGreaterThanOrEqualTo(3); // 包括之前创建的
    }

    @Test
    @Order(3)
    @DisplayName("集成测试 3: 根据ID查询标签")
    void testGetTagByIdFlow() throws Exception {
        mockMvc.perform(get("/api/tags/" + createdTagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(createdTagId))
                .andExpect(jsonPath("$.data.tagName").value("高价值用户"));
    }

    @Test
    @Order(4)
    @DisplayName("集成测试 4: 根据用户ID查询标签")
    void testGetTagsByUserIdFlow() throws Exception {
        mockMvc.perform(get("/api/tags/user/" + TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @Order(5)
    @DisplayName("集成测试 5: 根据分类查询标签")
    void testGetTagsByCategoryFlow() throws Exception {
        mockMvc.perform(get("/api/tags/category/价值分类"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("集成测试 6: 查询高权重标签")
    void testGetHighWeightTagsFlow() throws Exception {
        mockMvc.perform(get("/api/tags/high-weight")
                        .param("minWeight", "75"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(7)
    @DisplayName("集成测试 7: 分页查询标签")
    void testGetTagsWithPaginationFlow() throws Exception {
        mockMvc.perform(get("/api/tags")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    @Order(8)
    @DisplayName("集成测试 8: 更新标签")
    void testUpdateTagFlow() throws Exception {
        // 1. 准备更新数据
        TagDTO updateDTO = new TagDTO();
        updateDTO.setWeight(95.0);
        updateDTO.setDescription("超级高价值用户,重点维护");

        // 2. 发送更新请求
        mockMvc.perform(put("/api/tags/" + createdTagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.weight").value(95.0))
                .andExpect(jsonPath("$.data.description").value("超级高价值用户,重点维护"));

        // 3. 验证更新已保存
        UserTag updatedTag = tagRepository.findById(createdTagId).orElse(null);
        assertThat(updatedTag).isNotNull();
        assertThat(updatedTag.getWeight()).isEqualTo(95.0);
    }

    @Test
    @Order(9)
    @DisplayName("集成测试 9: 调整标签权重")
    void testAdjustTagWeightFlow() throws Exception {
        mockMvc.perform(patch("/api/tags/" + createdTagId + "/weight")
                        .param("adjustment", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.weight").value(100.0)); // 95 + 5 = 100

        // 验证权重已调整
        UserTag adjustedTag = tagRepository.findById(createdTagId).orElse(null);
        assertThat(adjustedTag).isNotNull();
        assertThat(adjustedTag.getWeight()).isEqualTo(100.0);
    }

    @Test
    @Order(10)
    @DisplayName("集成测试 10: 标签去重")
    void testDeduplicateTagsFlow() throws Exception {
        // 1. 创建重复标签
        TagDTO duplicateTag = new TagDTO();
        duplicateTag.setUserId(TEST_USER_ID);
        duplicateTag.setTagName("高价值用户");
        duplicateTag.setTagCategory("价值分类");
        duplicateTag.setWeight(85.0);

        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateTag)));

        // 2. 执行去重
        mockMvc.perform(post("/api/tags/user/" + TEST_USER_ID + "/deduplicate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 3. 验证重复标签���删除
        List<UserTag> userTags = tagRepository.findByUserId(TEST_USER_ID);
        long duplicateCount = userTags.stream()
                .filter(tag -> "高价值用户".equals(tag.getTagName()))
                .count();
        assertThat(duplicateCount).isEqualTo(1); // 只保留一个
    }

    @Test
    @Order(11)
    @DisplayName("集成测试 11: 统计用户标签数量")
    void testCountUserTagsFlow() throws Exception {
        mockMvc.perform(get("/api/tags/user/" + TEST_USER_ID + "/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @Order(12)
    @DisplayName("集成测试 12: 删除单个标签")
    void testDeleteTagFlow() throws Exception {
        mockMvc.perform(delete("/api/tags/" + createdTagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证标签已删除
        boolean exists = tagRepository.existsById(createdTagId);
        assertThat(exists).isFalse();
    }

    @Test
    @Order(13)
    @DisplayName("集成测试 13: 删除用户所有标签")
    void testDeleteAllUserTagsFlow() throws Exception {
        mockMvc.perform(delete("/api/tags/user/" + TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证所有标签已删除
        List<UserTag> userTags = tagRepository.findByUserId(TEST_USER_ID);
        assertThat(userTags).isEmpty();
    }
}
