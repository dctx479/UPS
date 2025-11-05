package com.userprofile.profile.segmentation;

import com.userprofile.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户分群控制器
 */
@Tag(name = "用户分群", description = "用户智能分群和圈选功能")
@RestController
@RequestMapping("/api/segments")
@RequiredArgsConstructor
public class UserSegmentController {

    private final UserSegmentationService segmentationService;

    @Operation(summary = "创建用户分群")
    @PostMapping
    public Result<UserSegment> createSegment(@RequestBody UserSegment segment) {
        UserSegment created = segmentationService.createSegment(segment);
        return Result.success("分群创建成功", created);
    }

    @Operation(summary = "获取所有分群")
    @GetMapping
    public Result<List<UserSegment>> getAllSegments() {
        List<UserSegment> segments = segmentationService.getAllSegments();
        return Result.success(segments);
    }

    @Operation(summary = "获取分群详情")
    @GetMapping("/{id}")
    public Result<UserSegment> getSegment(@PathVariable String id) {
        UserSegment segment = segmentationService.getSegmentById(id);
        return Result.success(segment);
    }

    @Operation(summary = "执行自定义筛选")
    @PostMapping("/filter")
    public Result<List<Long>> filterUsers(@RequestBody List<UserSegment.SegmentCondition> conditions) {
        List<Long> userIds = segmentationService.executeSegmentation(conditions);
        return Result.success(userIds);
    }

    @Operation(summary = "RFM自动分群")
    @PostMapping("/auto/rfm")
    public Result<Map<String, UserSegment>> autoSegmentByRFM() {
        Map<String, UserSegment> segments = segmentationService.segmentByRFM();
        return Result.success("RFM分群完成", segments);
    }

    @Operation(summary = "画像评分自动分群")
    @PostMapping("/auto/score")
    public Result<Map<String, UserSegment>> autoSegmentByScore() {
        Map<String, UserSegment> segments = segmentationService.segmentByProfileScore();
        return Result.success("评分分群完成", segments);
    }

    @Operation(summary = "流失风险自动分群")
    @PostMapping("/auto/churn")
    public Result<Map<String, UserSegment>> autoSegmentByChurn() {
        Map<String, UserSegment> segments = segmentationService.segmentByChurnRisk();
        return Result.success("流失风险分群完成", segments);
    }

    @Operation(summary = "刷新动态分群")
    @PutMapping("/refresh")
    public Result<Void> refreshSegments() {
        segmentationService.refreshDynamicSegments();
        return Result.success("分群已刷新", null);
    }
}
