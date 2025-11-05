# Postman Collection 使用指南

## 📦 包含文件

1. **UserProfile-API-Collection.postman_collection.json** - API测试集合
2. **UserProfile-Environment.postman_environment.json** - 环境配置

## 🚀 快速开始

### 1. 导入Collection和Environment

#### 方式一: 通过Postman UI导入
1. 打开Postman应用
2. 点击左上角 **Import** 按钮
3. 选择 **File** 标签
4. 依次导入以下文件:
   - `UserProfile-API-Collection.postman_collection.json`
   - `UserProfile-Environment.postman_environment.json`

#### 方式二: 拖拽导入
1. 打开Postman应用
2. 直接将两个JSON文件拖入Postman窗口
3. 确认导入

### 2. 配置Environment

1. 点击右上角的环境下拉菜单
2. 选择 **User Profile System - Local**
3. 确认 `base_url` 设置为 `http://localhost:8080`

### 3. 启动后端服务

确保所有服务已启动:

```bash
# Docker Compose方式
cd deployment
docker-compose up -d

# 验证服务状态
docker-compose ps
curl http://localhost:8080/actuator/health
```

### 4. 运行第一个请求

1. 展开Collection: **User Profile System API**
2. 展开文件夹: **1. Authentication (认证)**
3. 点击: **1.1 User Login**
4. 点击右上角蓝色 **Send** 按钮
5. 查看响应,Token会自动保存到环境变量中

✅ **成功标志**: 响应状态200,控制台显示 "✅ Access token saved"

---

## 📚 Collection结构

### 1. Authentication (认证) - 3个请求
- **1.1 User Login** ⭐ 必须首先运行
- 1.2 Refresh Token
- 1.3 Validate Token

### 2. User Management (用户管理) - 6个请求
- 2.1 Create User
- 2.2 Get User by ID
- 2.3 Update User
- 2.4 Get All Users
- 2.5 Get User by Username
- 2.6 Delete User

### 3. User Profile (用户画像) - 3个请求
- 3.1 Get User Profile
- 3.2 Calculate User Profile
- 3.3 Get All Profiles

### 4. Recommendations (推荐) - 1个请求
- 4.1 Get Recommendations

### 5. User Segments (用户分群) - 1个请求
- 5.1 Get User Segments

### 6. Tags (标签管理) - 6个请求
- 6.1 Create Tag
- 6.2 Get Tag by ID
- 6.3 Get All Tags
- 6.4 Get Tags by Category
- 6.5 Update Tag
- 6.6 Delete Tag

### 7. Health Checks (健康检查) - 4个请求
- 7.1 Gateway Health
- 7.2 User Service Health
- 7.3 Profile Service Health
- 7.4 Tag Service Health

**总计**: 7个文件夹, 24个API请求

---

## 🔐 认证机制

### 自动认证流程

1. **首次登录**
   ```
   运行 "1.1 User Login" → 获取Token → 自动保存到环境变量
   ```

2. **后续请求**
   ```
   所有请求自动从环境变量读取Token并添加到Header
   Authorization: Bearer {access_token}
   ```

3. **Token过期处理**
   ```
   收到401响应 → 运行 "1.2 Refresh Token" → 获取新Token
   或重新运行 "1.1 User Login"
   ```

### Token有效期
- **Access Token**: 24小时
- **Refresh Token**: 7天

### 手动设置Token

如果需要手动设置Token:

1. 点击右上角环境下拉菜单旁的"眼睛"图标
2. 找到 `access_token` 变量
3. 点击编辑,粘贴Token值
4. 保存

---

## 🧪 测试脚本说明

每个请求都包含自动化测试脚本:

### 通用测试
```javascript
// 验证响应状态
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// 验证响应结构
pm.test("Response has correct structure", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('success');
    pm.expect(jsonData).to.have.property('data');
});
```

### 特殊功能

#### 1. 自动保存变量
登录后自动保存Token:
```javascript
var jsonData = pm.response.json();
pm.environment.set("access_token", jsonData.data.token);
pm.environment.set("refresh_token", jsonData.data.refreshToken);
```

#### 2. 动态数据生成
创建用户时自动生成唯一用户名:
```javascript
pm.variables.set("timestamp", Date.now());
pm.variables.set("random_email", "test" + Date.now() + "@example.com");
```

#### 3. 错误提示
Token过期时自动提示:
```javascript
if (pm.response.code === 401) {
    console.log('⚠️ Token expired, please run "1.1 User Login" again');
}
```

---

## 🎯 常见使用场景

### 场景1: 完整的用户创建流程

**步骤**:
1. 运行 `1.1 User Login` (获取Token)
2. 运行 `2.1 Create User` (创建用户,自动保存user_id)
3. 运行 `3.2 Calculate User Profile` (计算画像)
4. 运行 `3.1 Get User Profile` (查看画像)
5. 运行 `4.1 Get Recommendations` (获取推荐)

### 场景2: 标签管理测试

**步骤**:
1. 运行 `1.1 User Login`
2. 运行 `6.1 Create Tag` (创建标签,自动保存tag_id)
3. 运行 `6.2 Get Tag by ID` (查看标签)
4. 运行 `6.5 Update Tag` (更新标签)
5. 运行 `6.3 Get All Tags` (查看所有标签)
6. 运行 `6.6 Delete Tag` (删除标签)

### 场景3: 系统健康检查

**步骤**:
1. 运行 `7.1 Gateway Health`
2. 运行 `7.2 User Service Health`
3. 运行 `7.3 Profile Service Health`
4. 运行 `7.4 Tag Service Health`

**预期结果**: 所有服务返回 `{"status":"UP"}`

### 场景4: 完整回归测试

**使用Collection Runner**:
1. 点击Collection右侧的三个点 `...`
2. 选择 **Run collection**
3. 确保选中所有请求
4. 点击 **Run User Profile System API**
5. 等待所有请求执行完成
6. 查看测试报告

**注意**:
- 首先确保 `1.1 User Login` 在最前面执行
- 删除类请求(2.6, 6.6)可能导致后续请求失败,建议单独测试

---

## 🔧 环境变量说明

### 必需变量

| 变量名 | 说明 | 默认值 | 示例 |
|-------|------|-------|------|
| `base_url` | API基础URL | `http://localhost:8080` | `http://192.168.1.100:8080` |

### 自动设置变量

| 变量名 | 说明 | 何时设置 |
|-------|------|----------|
| `access_token` | JWT访问令牌 | 登录/刷新Token后 |
| `refresh_token` | JWT刷新令牌 | 登录/刷新Token后 |
| `test_user_id` | 测试用户ID | 创建用户后 |
| `test_tag_id` | 测试标签ID | 创建标签后 |

### 创建多环境配置

可以为不同环境创建多个Environment:

#### Local环境 (已提供)
```json
{
  "base_url": "http://localhost:8080"
}
```

#### Docker环境
```json
{
  "base_url": "http://host.docker.internal:8080"
}
```

#### Staging环境
```json
{
  "base_url": "https://staging.userprofile.com"
}
```

#### Production环境
```json
{
  "base_url": "https://api.userprofile.com"
}
```

---

## 📊 使用Collection Runner批量测试

### 1. 基础使用

1. 点击Collection名称旁的 `▶ Run` 按钮
2. 选择要运行的请求(默认全选)
3. 选择Environment: **User Profile System - Local**
4. 点击 **Run User Profile System API**

### 2. 高级配置

#### 设置迭代次数
- **Iterations**: 设置为 `10` 可以运行10次完整测试
- 用于压力测试或稳定性测试

#### 设置延迟
- **Delay**: 设置为 `1000` (毫秒) 在每个请求间暂停1秒
- 避免请求过快导致限流

#### 数据驱动测试
1. 准备CSV或JSON数据文件
2. 点击 **Select File** 上传数据文件
3. 在请求中使用变量引用数据: `{{username}}`

**示例CSV** (users.csv):
```csv
username,password,email
user1,Pass@123,user1@example.com
user2,Pass@456,user2@example.com
user3,Pass@789,user3@example.com
```

### 3. 查看测试报告

运行完成后可以查看:
- **通过/失败的测试数量**
- **每个请求的响应时间**
- **测试覆盖率**
- **失败的断言详情**

可以导出HTML报告:
1. 点击 **Export Results**
2. 选择保存位置
3. 在浏览器中打开HTML文件

---

## 🐛 常见问题排查

### 问题1: 请求返回404

**原因**: 服务未启动或URL配置错误

**解决方案**:
```bash
# 1. 验证服务状态
docker-compose ps

# 2. 测试连接
curl http://localhost:8080/actuator/health

# 3. 检查Environment中的base_url配置
```

### 问题2: 请求返回401 Unauthorized

**原因**: Token未设置或已过期

**解决方案**:
1. 重新运行 `1.1 User Login`
2. 检查环境变量中是否有 `access_token`
3. 确认请求Header包含: `Authorization: Bearer {token}`

### 问题3: 请求返回CORS错误

**原因**: Gateway CORS配置未包含Postman

**解决方案**:
```bash
# 设置环境变量允许所有来源(仅开发环境)
export ALLOWED_ORIGINS="*"

# 或在docker-compose.yml中配置
gateway-service:
  environment:
    - ALLOWED_ORIGINS=*
```

### 问题4: 请求返回500 Internal Server Error

**原因**: 后端服务异常

**解决方案**:
```bash
# 1. 查看服务日志
docker-compose logs gateway-service --tail=50
docker-compose logs user-service --tail=50

# 2. 检查数据库连接
docker exec -it postgres psql -U userprofile -c "SELECT 1;"

# 3. 检查Redis连接
docker exec -it redis redis-cli PING
```

### 问题5: 创建用户失败 - 用户名已存在

**原因**: 使用了重复的用户名

**解决方案**:
- Pre-request脚本会自动生成唯一用户名(包含时间戳)
- 如果手动修改了请求体,确保使用唯一的username

### 问题6: Tests标签显示失败

**原因**: 响应格式不符合预期

**解决方案**:
1. 点击请求下方的 **Tests** 标签查看失败的测试
2. 点击 **Test Results** 查看详细错误信息
3. 对比实际响应和期望响应
4. 修改测试脚本或修复后端Bug

---

## 📝 编写自定义测试

### 基础断言

```javascript
// 状态码断言
pm.test("Status code is 200", () => {
    pm.response.to.have.status(200);
});

// 响应时间断言
pm.test("Response time is less than 500ms", () => {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

// 响应体断言
pm.test("Response has user data", () => {
    const jsonData = pm.response.json();
    pm.expect(jsonData.data).to.have.property('username');
    pm.expect(jsonData.data.username).to.be.a('string');
    pm.expect(jsonData.data.id).to.be.a('number');
});
```

### 高级断言

```javascript
// 数组断言
pm.test("Returns non-empty array", () => {
    const jsonData = pm.response.json();
    pm.expect(jsonData.data).to.be.an('array');
    pm.expect(jsonData.data.length).to.be.above(0);
});

// 数值范围断言
pm.test("User age is valid", () => {
    const jsonData = pm.response.json();
    pm.expect(jsonData.data.age).to.be.within(0, 120);
});

// 字符串匹配断言
pm.test("Email format is correct", () => {
    const jsonData = pm.response.json();
    pm.expect(jsonData.data.email).to.match(/^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/);
});
```

### 环境变量操作

```javascript
// 设置变量
pm.environment.set("variable_name", "value");

// 获取变量
const value = pm.environment.get("variable_name");

// 删除变量
pm.environment.unset("variable_name");

// 清空所有变量
pm.environment.clear();
```

---

## 🔗 与其他工具集成

### 1. Newman (命令行运行)

Newman是Postman的命令行工具,可用于CI/CD集成。

#### 安装Newman
```bash
npm install -g newman
```

#### 运行Collection
```bash
# 基础运行
newman run UserProfile-API-Collection.postman_collection.json \
  -e UserProfile-Environment.postman_environment.json

# 生成HTML报告
npm install -g newman-reporter-html
newman run UserProfile-API-Collection.postman_collection.json \
  -e UserProfile-Environment.postman_environment.json \
  -r html --reporter-html-export report.html

# 指定迭代次数
newman run UserProfile-API-Collection.postman_collection.json \
  -e UserProfile-Environment.postman_environment.json \
  -n 10

# 设置延迟(毫秒)
newman run UserProfile-API-Collection.postman_collection.json \
  -e UserProfile-Environment.postman_environment.json \
  --delay-request 1000
```

### 2. CI/CD集成 (GitHub Actions)

创建 `.github/workflows/api-test.yml`:

```yaml
name: API Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  api-tests:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v3

    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'

    - name: Install Newman
      run: npm install -g newman newman-reporter-html

    - name: Start services
      run: |
        cd deployment
        docker-compose up -d
        sleep 30  # 等待服务启动

    - name: Run API tests
      run: |
        newman run testing/postman/UserProfile-API-Collection.postman_collection.json \
          -e testing/postman/UserProfile-Environment.postman_environment.json \
          -r html,cli \
          --reporter-html-export newman-report.html

    - name: Upload test report
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: newman-report
        path: newman-report.html

    - name: Stop services
      if: always()
      run: |
        cd deployment
        docker-compose down
```

### 3. Jenkins集成

```groovy
pipeline {
    agent any

    stages {
        stage('Start Services') {
            steps {
                sh 'cd deployment && docker-compose up -d'
                sh 'sleep 30'
            }
        }

        stage('Run API Tests') {
            steps {
                sh '''
                    newman run testing/postman/UserProfile-API-Collection.postman_collection.json \
                      -e testing/postman/UserProfile-Environment.postman_environment.json \
                      -r html,junit \
                      --reporter-html-export newman-report.html \
                      --reporter-junit-export newman-report.xml
                '''
            }
        }

        stage('Publish Results') {
            steps {
                junit 'newman-report.xml'
                publishHTML([
                    reportDir: '.',
                    reportFiles: 'newman-report.html',
                    reportName: 'API Test Report'
                ])
            }
        }
    }

    post {
        always {
            sh 'cd deployment && docker-compose down'
        }
    }
}
```

---

## 🎓 最佳实践

### 1. 命名规范
- 请求名称: 使用 "编号 + 描述" 格式 (如: `1.1 User Login`)
- 文件夹: 使用中英文双语 (如: `1. Authentication (认证)`)
- 变量名: 使用小写+下划线 (如: `test_user_id`)

### 2. 测试脚本组织
```javascript
// 1. 基础验证(必须)
pm.test("Status code is 200", () => { /* ... */ });

// 2. 业务逻辑验证
pm.test("User data is complete", () => { /* ... */ });

// 3. 副作用操作(保存变量等)
pm.environment.set("user_id", jsonData.data.id);

// 4. 日志输出
console.log("✅ User created:", jsonData.data.id);
```

### 3. 环境变量管理
- 敏感信息(Token)设置为 `secret` 类型
- 使用不同Environment管理多环境配置
- 定期清理无用变量

### 4. 文档注释
- 每个请求添加详细的Description
- 说明必填/可选参数
- 提供响应示例

### 5. 错误处理
```javascript
// 在Collection级别添加通用错误处理
pm.test("No server error", () => {
    pm.response.to.not.have.status(500);
});

// 记录失败详情
if (pm.response.code >= 400) {
    console.error('Request failed:', {
        url: pm.request.url.toString(),
        status: pm.response.code,
        body: pm.response.json()
    });
}
```

---

## 📖 参考资源

- **Postman官方文档**: https://learning.postman.com/docs/
- **Newman文档**: https://github.com/postmanlabs/newman
- **Chai断言库**: https://www.chaijs.com/api/bdd/
- **项目API文档**: http://localhost:8080/swagger-ui.html

---

## 📞 技术支持

遇到问题?

1. 查看本文档的 [常见问题排查](#-常见问题排查) 章节
2. 查看 [故障排查指南](../../docs/TROUBLESHOOTING_GUIDE.md)
3. 查看服务日志: `docker-compose logs <service-name>`
4. 联系技术支持: support@userprofile.com

---

**版本**: v1.0
**最后更新**: 2024-01-02
**维护者**: 用户画像系统团队
