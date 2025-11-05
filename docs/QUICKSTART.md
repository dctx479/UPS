# 快速开始

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ 可用内存

## 一键启动

```bash
# 1. 进入项目目录
cd 构建用户画像

# 2. 启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | API网关 |
| User Service | 8081 | 用户服务 |
| Profile Service | 8082 | 画像服务 |
| Tag Service | 8083 | 标签服务 |
| Consul | 8500 | 服务注册中心 |
| PostgreSQL | 5432 | 用户数据库 |
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
