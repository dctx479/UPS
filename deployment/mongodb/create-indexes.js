// MongoDB索引创建脚本 (JavaScript版本)
// 直接在mongo shell中执行: mongo mongodb://localhost:27017/userprofile create-indexes.js

print("=== MongoDB索引创建脚本 ===");
print("数据库: userprofile");
print("");

// 切换到userprofile数据库
use userprofile;

print("=== 1. userProfiles 集合索引 ===");

// userId唯一索引
db.userProfiles.createIndex(
  { userId: 1 },
  {
    unique: true,
    name: "idx_user_id",
    background: true
  }
);
print("✓ 创建userId唯一索引");

// createTime降序索引
db.userProfiles.createIndex(
  { createTime: -1 },
  {
    name: "idx_create_time",
    background: true
  }
);
print("✓ 创建createTime索引");

// updateTime降序索引
db.userProfiles.createIndex(
  { updateTime: -1 },
  {
    name: "idx_update_time",
    background: true
  }
);
print("✓ 创建updateTime索引");

// 复合索引: userId + updateTime
db.userProfiles.createIndex(
  { userId: 1, updateTime: -1 },
  {
    name: "idx_userId_updateTime",
    background: true
  }
);
print("✓ 创建userId+updateTime复合索引");

print("");
print("=== 2. userEvents 集合索引 ===");

// userId + eventTime复合索引 (最常用)
db.userEvents.createIndex(
  { userId: 1, eventTime: -1 },
  {
    name: "idx_userId_eventTime",
    background: true
  }
);
print("✓ 创建userId+eventTime复合索引");

// eventType索引
db.userEvents.createIndex(
  { eventType: 1 },
  {
    name: "idx_event_type",
    background: true
  }
);
print("✓ 创建eventType索引");

// eventTime降序索引
db.userEvents.createIndex(
  { eventTime: -1 },
  {
    name: "idx_event_time",
    background: true
  }
);
print("✓ 创建eventTime索引");

// eventType + eventTime复合索引
db.userEvents.createIndex(
  { eventType: 1, eventTime: -1 },
  {
    name: "idx_eventType_eventTime",
    background: true
  }
);
print("✓ 创建eventType+eventTime复合索引");

print("");
print("=== 3. userSegments 集合索引 ===");

// userId索引
db.userSegments.createIndex(
  { userId: 1 },
  {
    name: "idx_user_id",
    background: true
  }
);
print("✓ 创建userId索引");

// segmentName索引
db.userSegments.createIndex(
  { segmentName: 1 },
  {
    name: "idx_segment_name",
    background: true
  }
);
print("✓ 创建segmentName索引");

// createTime降序索引
db.userSegments.createIndex(
  { createTime: -1 },
  {
    name: "idx_create_time",
    background: true
  }
);
print("✓ 创建createTime索引");

// segmentName + userId唯一复合索引
db.userSegments.createIndex(
  { segmentName: 1, userId: 1 },
  {
    unique: true,
    name: "idx_segmentName_userId",
    background: true
  }
);
print("✓ 创建segmentName+userId唯一复合索引");

print("");
print("=== 4. recommendations 集合索引 ===");

// userId + createTime复合索引
db.recommendations.createIndex(
  { userId: 1, createTime: -1 },
  {
    name: "idx_userId_createTime",
    background: true
  }
);
print("✓ 创建userId+createTime复合索引");

// recommendationType索引
db.recommendations.createIndex(
  { recommendationType: 1 },
  {
    name: "idx_recommendation_type",
    background: true
  }
);
print("✓ 创建recommendationType索引");

// score降序索引
db.recommendations.createIndex(
  { score: -1 },
  {
    name: "idx_score",
    background: true
  }
);
print("✓ 创建score索引");

print("");
print("=== 5. 查看所有索引 ===");

print("");
print("--- userProfiles索引 ---");
printjson(db.userProfiles.getIndexes());

print("");
print("--- userEvents索引 ---");
printjson(db.userEvents.getIndexes());

print("");
print("--- userSegments索引 ---");
printjson(db.userSegments.getIndexes());

print("");
print("--- recommendations索引 ---");
printjson(db.recommendations.getIndexes());

print("");
print("=== 索引大小统计 ===");

var stats = {
  userProfiles: db.userProfiles.stats().indexSizes,
  userEvents: db.userEvents.stats().indexSizes,
  userSegments: db.userSegments.stats().indexSizes,
  recommendations: db.recommendations.stats().indexSizes
};

printjson(stats);

print("");
print("✅ 所有索引创建完成!");
print("");
print("使用说明:");
print("1. 索引后台创建(background: true)不会阻塞数据库操作");
print("2. 唯一索引会自动删除重复数据,请提前备份");
print("3. 验证索引: db.collection.getIndexes()");
print("4. 删除索引: db.collection.dropIndex('索引名称')");
