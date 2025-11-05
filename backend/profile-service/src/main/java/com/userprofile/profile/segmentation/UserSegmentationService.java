package com.userprofile.profile.segmentation;

import com.userprofile.profile.analytics.UserBehaviorAnalytics;
import com.userprofile.profile.entity.UserProfile;
import com.userprofile.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户分群服务
 * 提供智能分群和圈选功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSegmentationService {

    private final UserProfileRepository profileRepository;
    private final UserBehaviorAnalytics behaviorAnalytics;
    private final UserSegmentRepository segmentRepository;

    /**
     * 创建用户分群
     */
    public UserSegment createSegment(UserSegment segment) {
        segment.setCreateTime(LocalDateTime.now());
        segment.setUpdateTime(LocalDateTime.now());
        segment.setActive(true);

        // 如果是动态分群，立即执行筛选
        if (segment.getType() == UserSegment.SegmentType.DYNAMIC) {
            List<Long> userIds = executeSegmentation(segment.getConditions());
            segment.setUserIds(userIds);
            segment.setUserCount(userIds.size());
        }

        log.info("创建用户分群: {}, 用户数: {}", segment.getName(), segment.getUserCount());
        return segmentRepository.save(segment);
    }

    /**
     * 执行分群筛选
     * 根据条件筛选符合的用户
     */
    public List<Long> executeSegmentation(List<UserSegment.SegmentCondition> conditions) {
        List<UserProfile> allProfiles = profileRepository.findAll();

        return allProfiles.stream()
                .filter(profile -> matchesConditions(profile, conditions))
                .map(UserProfile::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * 判断用户画像是否匹配条件
     */
    private boolean matchesConditions(UserProfile profile, List<UserSegment.SegmentCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        boolean result = true;
        String currentLogic = "AND";

        for (UserSegment.SegmentCondition condition : conditions) {
            boolean conditionMet = evaluateCondition(profile, condition);

            if ("OR".equals(currentLogic)) {
                result = result || conditionMet;
            } else {  // AND
                result = result && conditionMet;
            }

            currentLogic = condition.getLogic() != null ? condition.getLogic() : "AND";
        }

        return result;
    }

    /**
     * 评估单个条件
     */
    private boolean evaluateCondition(UserProfile profile, UserSegment.SegmentCondition condition) {
        Object fieldValue = getFieldValue(profile, condition.getField());

        if (fieldValue == null) {
            return condition.getOperator() == UserSegment.SegmentCondition.Operator.IS_NULL;
        }

        return switch (condition.getOperator()) {
            case EQUALS -> fieldValue.equals(condition.getValue());
            case NOT_EQUALS -> !fieldValue.equals(condition.getValue());
            case GREATER_THAN -> compareNumeric(fieldValue, condition.getValue()) > 0;
            case LESS_THAN -> compareNumeric(fieldValue, condition.getValue()) < 0;
            case GREATER_OR_EQUAL -> compareNumeric(fieldValue, condition.getValue()) >= 0;
            case LESS_OR_EQUAL -> compareNumeric(fieldValue, condition.getValue()) <= 0;
            case CONTAINS -> fieldValue.toString().contains(condition.getValue().toString());
            case NOT_CONTAINS -> !fieldValue.toString().contains(condition.getValue().toString());
            case IS_NOT_NULL -> true;
            default -> false;
        };
    }

    /**
     * 获取字段值
     */
    private Object getFieldValue(UserProfile profile, String field) {
        return switch (field) {
            case "profileScore" -> profile.getProfileScore();
            case "userId" -> profile.getUserId();
            case "username" -> profile.getUsername();
            // 可以根据需要添加更多字段
            default -> null;
        };
    }

    /**
     * 数值比较
     */
    private int compareNumeric(Object value1, Object value2) {
        double d1 = ((Number) value1).doubleValue();
        double d2 = ((Number) value2).doubleValue();
        return Double.compare(d1, d2);
    }

    /**
     * 基于RFM模型自动分群
     */
    public Map<String, UserSegment> segmentByRFM() {
        log.info("开始执行RFM自动分群...");

        Map<String, List<Long>> rfmGroups = new HashMap<>();
        rfmGroups.put("重要价值客户", new ArrayList<>());
        rfmGroups.put("重要发展客户", new ArrayList<>());
        rfmGroups.put("重要保持客户", new ArrayList<>());
        rfmGroups.put("一般客户", new ArrayList<>());
        rfmGroups.put("低价值客户", new ArrayList<>());

        List<UserProfile> allProfiles = profileRepository.findAll();

        for (UserProfile profile : allProfiles) {
            try {
                Map<String, Object> rfm = behaviorAnalytics.calculateRFM(profile.getUserId());
                String level = (String) rfm.get("level");
                rfmGroups.get(level).add(profile.getUserId());
            } catch (Exception e) {
                log.error("计算用户{}的RFM失败", profile.getUserId(), e);
            }
        }

        // 创建分群
        Map<String, UserSegment> segments = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : rfmGroups.entrySet()) {
            UserSegment segment = new UserSegment();
            segment.setName("RFM-" + entry.getKey());
            segment.setDescription("基于RFM模型自动分群: " + entry.getKey());
            segment.setType(UserSegment.SegmentType.RFM);
            segment.setUserIds(entry.getValue());
            segment.setUserCount(entry.getValue().size());
            segment.setCreateTime(LocalDateTime.now());
            segment.setUpdateTime(LocalDateTime.now());
            segment.setActive(true);

            segments.put(entry.getKey(), segmentRepository.save(segment));
        }

        log.info("RFM分群完成，共创建{}个分群", segments.size());
        return segments;
    }

    /**
     * 基于画像评分自动分群
     */
    public Map<String, UserSegment> segmentByProfileScore() {
        log.info("开始执行画像评分自动分群...");

        Map<String, List<Long>> scoreGroups = new HashMap<>();
        scoreGroups.put("高价值用户(80+)", new ArrayList<>());
        scoreGroups.put("活跃用户(60-79)", new ArrayList<>());
        scoreGroups.put("潜力用户(40-59)", new ArrayList<>());
        scoreGroups.put("普通用户(20-39)", new ArrayList<>());
        scoreGroups.put("低活跃用户(<20)", new ArrayList<>());

        List<UserProfile> allProfiles = profileRepository.findAll();

        for (UserProfile profile : allProfiles) {
            double score = profile.getProfileScore() != null ? profile.getProfileScore() : 0;
            String group = score >= 80 ? "高价值用户(80+)" :
                          score >= 60 ? "活跃用户(60-79)" :
                          score >= 40 ? "潜力用户(40-59)" :
                          score >= 20 ? "普通用户(20-39)" : "低活跃用户(<20)";
            scoreGroups.get(group).add(profile.getUserId());
        }

        // 创��分群
        Map<String, UserSegment> segments = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : scoreGroups.entrySet()) {
            UserSegment segment = new UserSegment();
            segment.setName("评分-" + entry.getKey());
            segment.setDescription("基于画像评分自动分群: " + entry.getKey());
            segment.setType(UserSegment.SegmentType.DYNAMIC);
            segment.setUserIds(entry.getValue());
            segment.setUserCount(entry.getValue().size());
            segment.setCreateTime(LocalDateTime.now());
            segment.setUpdateTime(LocalDateTime.now());
            segment.setActive(true);

            segments.put(entry.getKey(), segmentRepository.save(segment));
        }

        log.info("评分分群完成，共创建{}个分群", segments.size());
        return segments;
    }

    /**
     * 基于流失风险自动分群
     */
    public Map<String, UserSegment> segmentByChurnRisk() {
        log.info("开始执行流失风险自动分群...");

        Map<String, List<Long>> riskGroups = new HashMap<>();
        riskGroups.put("高流失风险", new ArrayList<>());
        riskGroups.put("中流失风险", new ArrayList<>());
        riskGroups.put("低流失风险", new ArrayList<>());

        List<UserProfile> allProfiles = profileRepository.findAll();

        for (UserProfile profile : allProfiles) {
            try {
                Map<String, Object> risk = behaviorAnalytics.predictChurnRisk(profile.getUserId());
                String riskLevel = (String) risk.get("risk");
                riskGroups.get(riskLevel + "流失风险").add(profile.getUserId());
            } catch (Exception e) {
                log.error("分析用户{}的流失风险失败", profile.getUserId(), e);
            }
        }

        // 创建分群
        Map<String, UserSegment> segments = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : riskGroups.entrySet()) {
            UserSegment segment = new UserSegment();
            segment.setName("流失风险-" + entry.getKey());
            segment.setDescription("基于流失风险预测自动分群: " + entry.getKey());
            segment.setType(UserSegment.SegmentType.BEHAVIOR);
            segment.setUserIds(entry.getValue());
            segment.setUserCount(entry.getValue().size());
            segment.setCreateTime(LocalDateTime.now());
            segment.setUpdateTime(LocalDateTime.now());
            segment.setActive(true);

            segments.put(entry.getKey(), segmentRepository.save(segment));
        }

        log.info("流失风险分群完成，共创建{}个分群", segments.size());
        return segments;
    }

    /**
     * 更新动态分群
     */
    public void refreshDynamicSegments() {
        log.info("开始刷新动态分群...");

        List<UserSegment> dynamicSegments = segmentRepository.findByTypeAndActive(
                UserSegment.SegmentType.DYNAMIC, true
        );

        for (UserSegment segment : dynamicSegments) {
            try {
                List<Long> newUserIds = executeSegmentation(segment.getConditions());
                segment.setUserIds(newUserIds);
                segment.setUserCount(newUserIds.size());
                segment.setUpdateTime(LocalDateTime.now());
                segmentRepository.save(segment);

                log.info("分群{}已更新，当前用户数: {}", segment.getName(), newUserIds.size());
            } catch (Exception e) {
                log.error("更新分群{}失败", segment.getName(), e);
            }
        }

        log.info("动态分群刷新完成");
    }

    /**
     * 获取所有分群
     */
    public List<UserSegment> getAllSegments() {
        return segmentRepository.findAll();
    }

    /**
     * 获取分群详情
     */
    public UserSegment getSegmentById(String id) {
        return segmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分群不存在"));
    }
}
