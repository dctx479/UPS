# 用户画像系统 - 使用指南

## 🌐 访问方式

### 方式 1: 通过 Web 界面（推荐）

1. **拉取最新代码**
   ```bash
   cd ~/UPS
   git pull origin main
   ```

2. **访问前端页面**

   在服务器上启动一个简单的 HTTP 服务器：
   ```bash
   cd ~/UPS/frontend
   python3 -m http.server 8000
   ```

   然后在浏览器中访问：
   ```
   http://你的服务器IP:8000
   ```

3. **使用 Web 界面**
   - 📝 注册新用户
   - 🔐 用户登录
   - 👤 查看用户画像
   - 🏷️ 创建和管理标签

### 方式 2: 通过 Swagger UI

直接在浏览器中访问：
```
http://你的服务器IP:8080/swagger-ui.html
```

**Swagger UI 使用步骤：**
1. 点击右上角 "Authorize" 按钮
2. 输入格式：`Bearer YOUR_TOKEN`（先通过 /api/auth/login 获取 token）
3. 点击接口展开详情
4. 点击 "Try it out" 进行测试
5. 填写参数后点击 "Execute" 执行请求

### 方式 3: 使用 API 测试脚本

```bash
cd ~/UPS
chmod +x test-api.sh
./test-api.sh 你的服务器IP
```

脚本会自动执行以下操作：
- ✅ 注册新用户
- ✅ 用户登录
- ✅ 获取用户信息
- ✅ 查看用户画像
- ✅ 创建用户标签
- ✅ 查看用户标签

### 方式 4: 使用 curl 命令

#### 1. 注册用户
```bash
curl -X POST http://你的服务器IP:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123456",
    "email": "test@example.com",
    "name": "测试用户"
  }'
```

#### 2. 登录获取 Token
```bash
curl -X POST http://你的服务器IP:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123456"
  }'
```

保存返回的 `accessToken`，在后续请求中使用。

#### 3. 查看用户信息（需要 Token）
```bash
curl http://你的服务器IP:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 4. 查看用户画像
```bash
curl http://你的服务器IP:8080/api/profiles/user/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 5. 创建标签
```bash
curl -X POST http://你的服务器IP:8080/api/tags \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "tagName": "高价值用户",
    "category": "价值分类",
    "source": "MANUAL",
    "weight": 0.9
  }'
```

#### 6. 查看用户标签
```bash
curl http://你的服务器IP:8080/api/tags/user/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 方式 5: 使用 Postman

1. 下载并安装 Postman: https://www.postman.com/downloads/
2. 创建新的 Collection
3. 添加以下请求：

**环境变量设置：**
- `base_url`: `http://你的服务器IP:8080`
- `token`: 登录后获取的 access token

**请求列表：**
1. POST `/api/users` - 注册用户
2. POST `/api/auth/login` - 用户登录
3. GET `/api/users/:id` - 获取用户信息
4. GET `/api/profiles/user/:userId` - 查看用户画像
5. POST `/api/tags` - 创建标签
6. GET `/api/tags/user/:userId` - 查看用户标签

## 🔗 系统端点

| 服务 | 地址 | 说明 |
|------|------|------|
| **API Gateway** | http://服务器IP:8080 | 统一入口 |
| **Swagger UI** | http://服务器IP:8080/swagger-ui.html | API 文档 |
| **Consul UI** | http://服务器IP:8500 | 服务管理 |
| **User Service** | http://服务器IP:8081 | 用户服务 |
| **Profile Service** | http://服务器IP:8082 | 画像服务 |
| **Tag Service** | http://服务器IP:8083 | 标签服务 |

## 📊 主要功能接口

### 用户管理
- `POST /api/users` - 注册用户
- `POST /api/auth/login` - 用户登录
- `GET /api/users/{id}` - 获取用户信息
- `PUT /api/users/{id}` - 更新用户信息
- `DELETE /api/users/{id}` - 删除用户
- `GET /api/users` - 查询用户列表（分页）

### 用户画像
- `POST /api/profiles/initialize` - 初始化用户画像
- `GET /api/profiles/user/{userId}` - 查看用户画像
- `POST /api/profiles` - 创建/更新用户画像
- `PUT /api/profiles/user/{userId}/recalculate` - 重新计算画像评分
- `GET /api/profiles/user/{userId}/type` - 分析用户类型
- `GET /api/profiles/user/{userId}/tags` - 生成用户标签
- `GET /api/profiles/user/{userId}/strategy` - 推荐营销策略

### 标签管理
- `POST /api/tags` - 创建标签
- `POST /api/tags/batch` - 批量创建标签
- `GET /api/tags/{id}` - 根据ID查询标签
- `GET /api/tags/user/{userId}` - 查询用户所有标签
- `GET /api/tags/category/{category}` - 根据分类查询标签
- `PUT /api/tags/{id}` - 更新标签
- `DELETE /api/tags/{id}` - 删除标签
- `GET /api/tags` - 分页查询标签

## 🛠️ 系统管理

### 查看服务状态
```bash
docker-compose ps
```

### 查看服务日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f user-service
docker-compose logs -f profile-service
docker-compose logs -f tag-service
docker-compose logs -f gateway
```

### 重启服务
```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart user-service
```

### 停止服务
```bash
docker-compose down
```

### 启动服务
```bash
./quick-start.sh
```

## 💡 使用示例

### 完整的用户画像创建流程

1. **注册用户**
   ```bash
   curl -X POST http://localhost:8080/api/users \
     -H "Content-Type: application/json" \
     -d '{"username": "demo", "password": "Demo@123456", "email": "demo@example.com", "name": "示例用户"}'
   ```

2. **登录并获取 Token**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "demo", "password": "Demo@123456"}'
   ```

3. **完善用户画像**
   ```bash
   curl -X POST http://localhost:8080/api/profiles \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "userId": 1,
       "username": "demo",
       "digitalBehavior": {
         "productCategories": ["电子产品", "图书"],
         "infoAcquisitionHabit": "搜索引擎",
         "purchaseDecisionPreference": "品质导向",
         "brandPreferences": ["Apple", "华为"]
       },
       "coreNeeds": {
         "topConcerns": ["品质", "性能"],
         "decisionPainPoint": "价格敏感"
       },
       "valueAssessment": {
         "consumptionLevel": "MEDIUM",
         "preferenceAnalysis": {"quality": 0.8, "price": 0.7}
       },
       "stickiness": {
         "loyaltyScore": 75.0
       }
     }'
   ```

4. **查看用户画像分析**
   ```bash
   # 查看完整画像
   curl http://localhost:8080/api/profiles/user/1 \
     -H "Authorization: Bearer YOUR_TOKEN"

   # 分析用户类型
   curl http://localhost:8080/api/profiles/user/1/type \
     -H "Authorization: Bearer YOUR_TOKEN"

   # 获取营销策略建议
   curl http://localhost:8080/api/profiles/user/1/strategy \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

## 🔧 故障排除

### 问题 1: 无法访问服务
**解决方案：**
```bash
# 检查服务状态
docker-compose ps

# 查看服务日志
docker-compose logs gateway
docker-compose logs user-service
```

### 问题 2: Token 过期
**解决方案：** 重新调用登录接口获取新的 Token

### 问题 3: 服务未注册到 Consul
**解决方案：**
```bash
# 检查 Consul 服务
curl http://localhost:8500/v1/catalog/services

# 重启服务
docker-compose restart user-service profile-service tag-service
```

## 📞 技术支持

如有问题，请联系：
- 邮箱：b150w4942@163.com
- GitHub Issues: https://github.com/dctx479/UPS/issues
