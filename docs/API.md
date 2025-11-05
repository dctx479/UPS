# API 文档

所有API通过Gateway访问: `http://localhost:8080`

## 认证接口

### 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123456"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "name": "管理员"
    }
  }
}
```

### Token刷新
```http
POST /api/auth/refresh
Authorization: Bearer {token}
```

### 登出
```http
POST /api/auth/logout
Authorization: Bearer {token}
```

---

## 用户接口

### 创建用户
```http
POST /api/users
Content-Type: application/json

{
  "username": "testuser",
  "password": "Test@123456",
  "name": "测试用户",
  "email": "test@example.com",
  "phone": "13800138000",
  "age": 25
}
```

**密码要求**:
- 至少8位
- 包含大写字母、小写字母、数字、特殊字符

### 查询用户
```http
GET /api/users/{id}
Authorization: Bearer {token}
```

### 分页查询用户
```http
GET /api/users?page=1&size=10
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "items": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

### 更新用户
```http
PUT /api/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "新名称",
  "email": "new@example.com"
}
```

### 删除用户
```http
DELETE /api/users/{id}
Authorization: Bearer {token}
```

---

## 画像接口

### 创建/更新画像
```http
POST /api/profiles
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": 1,
  "username": "testuser",
  "digitalBehavior": {
    "productCategories": ["电子产品", "图书"],
    "infoAcquisitionHabit": "社交媒体",
    "purchaseDecisionPreference": "价格优先",
    "brandPreferences": ["Apple", "华为"]
  },
  "coreNeeds": {
    "topConcerns": ["价格", "质量", "服务"],
    "decisionPainPoint": "选择困难"
  },
  "valueAssessment": {
    "profileQuality": "GOOD",
    "consumptionLevel": "MEDIUM",
    "avgOrderValue": 299.99,
    "feedingMethod": "主动购买",
    "teachability": "HIGH"
  },
  "stickiness": {
    "loyaltyScore": 75.5,
    "painPoint": "物流慢"
  }
}
```

### 查询画像
```http
GET /api/profiles/{userId}
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": "...",
    "userId": 1,
    "username": "testuser",
    "profileScore": 85.6,
    "digitalBehavior": {...},
    "coreNeeds": {...},
    "valueAssessment": {...},
    "stickinessAndLoyalty": {...},
    "createTime": "2025-01-05T10:00:00",
    "updateTime": "2025-01-05T11:00:00"
  }
}
```

### 分页查询画像
```http
GET /api/profiles?page=1&size=10
Authorization: Bearer {token}
```

### 分析用户类型
```http
GET /api/profiles/{userId}/analyze-type
Authorization: Bearer {token}
```

### 生成用户标签
```http
GET /api/profiles/{userId}/generate-tags
Authorization: Bearer {token}
```

### 推荐营销策略
```http
GET /api/profiles/{userId}/recommend-strategy
Authorization: Bearer {token}
```

### 重新计算评分
```http
POST /api/profiles/{userId}/recalculate-score
Authorization: Bearer {token}
```

### 删除画像
```http
DELETE /api/profiles/{userId}
Authorization: Bearer {token}
```

---

## 标签接口

### 创建标签
```http
POST /api/tags
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": 1,
  "tagName": "高价值客户",
  "tagValue": "VIP",
  "category": "价值分类",
  "weight": 90.0
}
```

### 批量创建标签
```http
POST /api/tags/batch
Authorization: Bearer {token}
Content-Type: application/json

[
  {
    "userId": 1,
    "tagName": "高频购买",
    "category": "行为分类",
    "weight": 85.0
  },
  {...}
]
```

### 查询标签
```http
GET /api/tags/{id}
Authorization: Bearer {token}
```

### 根据用户ID查询
```http
GET /api/tags/user/{userId}
Authorization: Bearer {token}
```

### 根据分类查询
```http
GET /api/tags/category/{category}
Authorization: Bearer {token}
```

### 查询高权重标签
```http
GET /api/tags/high-weight?threshold=80&limit=10
Authorization: Bearer {token}
```

### 分页查询
```http
GET /api/tags?page=1&size=10
Authorization: Bearer {token}
```

### 更新标签
```http
PUT /api/tags/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "tagValue": "新值",
  "weight": 95.0
}
```

### 调整权重
```http
PATCH /api/tags/{id}/weight?adjustment=10
Authorization: Bearer {token}
```

### 标签去重
```http
POST /api/tags/deduplicate?userId={userId}
Authorization: Bearer {token}
```

### 统计标签数量
```http
GET /api/tags/count?userId={userId}
Authorization: Bearer {token}
```

### 删除标签
```http
DELETE /api/tags/{id}
Authorization: Bearer {token}
```

### 删除用户所有标签
```http
DELETE /api/tags/user/{userId}
Authorization: Bearer {token}
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如用户名重复） |
| 429 | 请求过于频繁（限流） |
| 500 | 服务器错误 |
| 503 | 服务不可用（熔断） |

## 通用响应格式

**成功**:
```json
{
  "code": 200,
  "message": "success",
  "data": {...}
}
```

**失败**:
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

## Swagger文档

访问完整的API文档: http://localhost:8080/swagger-ui.html
