# 快速开始

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ 可用内存

## 方式一：自动部署（推荐）

```bash
# 克隆项目
git clone https://github.com/dctx479/UPS.git
cd UPS

# 运行快速启动脚本
chmod +x quick-start.sh
./quick-start.sh
```

脚本会自动完成：
1. 环境检查
2. 配置初始化
3. 镜像拉取和构建
4. 服务启动
5. 健康检查

## 方式二：手动部署

```bash
# 1. 克隆项目
git clone https://github.com/dctx479/UPS.git
cd UPS

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 文件,修改数据库密码等配置

# 3. 启动所有服务
docker-compose up -d

# 4. 查看服务状态
docker-compose ps
```

## 方式三：交互式部署

```bash
# 使用交互式部署脚本
chmod +x deploy.sh
./deploy.sh
```

部署脚本提供以下选项:
1. 部署所有服务
2. 仅部署基础设施
3. 仅部署微服务
4. 停止所有服务
5. 查看日志
6. 检查服务状态
7. 清理容器和数据卷

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | API网关 |
| User Service | 8081 | 用户服务 |
| Profile Service | 8082 | 画像服务 |
| Tag Service | 8083 | 标签服务 |
| Consul | 8500 | 服务注册中心 |
| MySQL | 3306 | 用户数据库 |
| MongoDB | 27017 | 画像数据库 |
| Redis | 6379 | 缓存 |

## 快速测试

### 1. 创建用户

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123456",
    "name": "测试用户",
    "email": "test@example.com"
  }'
```

### 2. 用户登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123456"
  }'
```

### 3. 查看画像

```bash
# 使用返回的 token
curl http://localhost:8080/api/profiles/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 管理界面

- **Consul UI**: http://localhost:8500
- **Swagger API**: http://localhost:8080/swagger-ui.html

## 停止服务

```bash
docker-compose down
```

## 下一步

- 查看 [系统架构](./ARCHITECTURE.md) 了解设计
- 查看 [API文档](./API.md) 了解接口详情
- 查看 [部署指南](./DEPLOYMENT.md) 部署到生产
