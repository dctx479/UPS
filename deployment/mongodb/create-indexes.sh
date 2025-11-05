#!/bin/bash

# MongoDB索引创建脚本
# 用于优化查询性能

echo "=== MongoDB索引创建脚本 ==="
echo "数据库: userprofile"
echo ""

# MongoDB连接信息
MONGO_HOST=${MONGO_HOST:-localhost}
MONGO_PORT=${MONGO_PORT:-27017}
MONGO_DB="userprofile"

echo "连接到: mongodb://$MONGO_HOST:$MONGO_PORT/$MONGO_DB"
echo ""

# 执行索引创建
mongo mongodb://$MONGO_HOST:$MONGO_PORT/$MONGO_DB <<EOF

print("=== 1. userProfiles 集合索引 ===");

// userId唯一索引
db.userProfiles.createIndex(
  { userId: 1 },
  {
    unique: true,
    name: "idx_user_id"
  }
);
print("✓ 创建userId唯一索引");

// createTime降序索引 (用于按时间查询)
db.userProfiles.createIndex(
  { createTime: -1 },
  { name: "idx_create_time" }
);
print("✓ 创建createTime索引");

// updateTime降序索引
db.userProfiles.createIndex(
  { updateTime: -1 },
  { name: "idx_update_time" }
);
print("✓ 创建updateTime索引");

// 复合索引: userId + updateTime (用于查询特定用户的最新画像)
db.userProfiles.createIndex(
  { userId: 1, updateTime: -1 },
  { name: "idx_userId_updateTime" }
);
print("✓ 创建userId+updateTime复合索引");

print("");
print("=== 2. userEvents 集合索引 ===");

// userId + eventTime复合索引 (最常用查询)
db.userEvents.createIndex(
  { userId: 1, eventTime: -1 },
  { name: "idx_userId_eventTime" }
);
print("✓ 创建userId+eventTime复合索引");

// eventType索引 (用于按事件类型查询)
db.userEvents.createIndex(
  { eventType: 1 },
  { name: "idx_event_type" }
);
print("✓ 创建eventType索引");

// eventTime降序索引 (用于按时间范围查询)
db.userEvents.createIndex(
  { eventTime: -1 },
  { name: "idx_event_time" }
);
print("✓ 创建eventTime索引");

// 复合索引: eventType + eventTime (用于查询特定类型的事件)
db.userEvents.createIndex(
  { eventType: 1, eventTime: -1 },
  { name: "idx_eventType_eventTime" }
);
print("✓ 创建eventType+eventTime复合索引");

print("");
print("=== 3. userSegments 集合索引 ===");

// userId索引
db.userSegments.createIndex(
  { userId: 1 },
  { name: "idx_user_id" }
);
print("✓ 创建userId索引");

// segmentName索引 (用于按分群名称查询)
db.userSegments.createIndex(
  { segmentName: 1 },
  { name: "idx_segment_name" }
);
print("✓ 创建segmentName索引");

// createTime降序索引
db.userSegments.createIndex(
  { createTime: -1 },
  { name: "idx_create_time" }
);
print("✓ 创建createTime索引");

// 复合索引: segmentName + userId (用于查询分群中的用户)
db.userSegments.createIndex(
  { segmentName: 1, userId: 1 },
  {
    unique: true,
    name: "idx_segmentName_userId"
  }
);
print("✓ 创建segmentName+userId唯一复合索引");

print("");
print("=== 4. recommendations 集合索引 ===");

// userId + createTime复合索引
db.recommendations.createIndex(
  { userId: 1, createTime: -1 },
  { name: "idx_userId_createTime" }
);
print("✓ 创建userId+createTime复合索引");

// recommendationType索引
db.recommendations.createIndex(
  { recommendationType: 1 },
  { name: "idx_recommendation_type" }
);
print("✓ 创建recommendationType索引");

// score降序索引 (用于按推荐分数排序)
db.recommendations.createIndex(
  { score: -1 },
  { name: "idx_score" }
);
print("✓ 创建score索引");

print("");
print("=== 5. 查看所有索引 ===");

print("");
print("--- userProfiles ---");
printjson(db.userProfiles.getIndexes());

print("");
print("--- userEvents ---");
printjson(db.userEvents.getIndexes());

print("");
print("--- userSegments ---");
printjson(db.userSegments.getIndexes());

print("");
print("--- recommendations ---");
printjson(db.recommendations.getIndexes());

print("");
print("=== 索引统计信息 ===");

print("");
print("--- userProfiles ---");
printjson(db.userProfiles.stats().indexSizes);

print("");
print("--- userEvents ---");
printjson(db.userEvents.stats().indexSizes);

print("");
print("--- userSegments ---");
printjson(db.userSegments.stats().indexSizes);

print("");
print("--- recommendations ---");
printjson(db.recommendations.stats().indexSizes);

print("");
print("✅ 所有索引创建完成!");

EOF

echo ""
echo "=== 完成 ==="
