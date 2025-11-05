# User Profile System (UPS) v1.0

一个基于Spring Boot微服务架构的企业级用户画像系统。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)

## ✨ 特性

- 🏗️ **微服务架构** - 基于Spring Cloud的分布式系统
- 🔒 **安全可靠** - JWT认证 + 强密码策略 + API限流
- ⚡ **高性能** - Redis缓存 + MongoDB索引优化
- 🔄 **高可用** - 服务发现 + 熔断降级 + 分布式事务补偿
- 📊 **完整画像** - 多维度用户行为分析和标签管理
- 🧪 **测试完备** - 93个测试用例，75%+覆盖率

## 🚀 快速开始

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ 可用内存

### 一键启动

```bash
# 克隆项目
git clone https://github.com/dctx479/UPS.git
cd UPS

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

### 访问系统

- **API Gateway**: http://localhost:8080
- **Consul UI**: http://localhost:8500
- **Swagger API**: http://localhost:8080/swagger-ui.html

## 📖 文档

- [快速开始](./docs/QUICKSTART.md) - 5分钟快速部署指南
- [系统架构](./docs/ARCHITECTURE.md) - 技术架构和设计说明
- [API文档](./docs/API.md) - RESTful API接口文档
- [部署指南](./docs/DEPLOYMENT.md) - 生产环境部署
- [故障排查](./docs/TROUBLESHOOTING.md) - 常见问题解决

## 🏗️ 系统架构

```
Flutter Client
      ↓
API Gateway (8080)
      ↓
┌─────┴─────┬─────────┬─────────┐
↓           ↓         ↓         ↓
User     Profile    Tag      Consul
Service  Service  Service  (Registry)
(8081)   (8082)   (8083)    (8500)
↓           ↓         ↓
PostgreSQL MongoDB  Redis
```

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.2, Spring Cloud
- **安全**: Spring Security, JWT
- **服务治理**: Consul, OpenFeign, Resilience4j
- **数据库**: PostgreSQL, MongoDB, Redis

### 前端
- **UI框架**: Flutter
- **状态管理**: Provider

### DevOps
- **容器化**: Docker, Docker Compose
- **编排**: Kubernetes
- **监控**: Prometheus, Grafana
- **日志**: ELK Stack

## 📦 项目结构

```
UPS/
├── backend/                 # 后端服务
│   ├── common/             # 公共模块
│   ├── gateway-service/    # API网关
│   ├── user-service/       # 用户服务
│   ├── profile-service/    # 画像服务
│   └── tag-service/        # 标签服务
├── flutter-app/            # Flutter前端
├── deployment/             # 部署配置
│   ├── k8s/               # Kubernetes配置
│   ├── prometheus/        # 监控配置
│   └── grafana/           # 可视化配置
├── testing/               # 测试脚本
└── docs/                  # 文档
```

## 🔧 开发

### 本地开发

```bash
# 启动基础设施
docker-compose up -d postgres mongodb redis consul

# 启动服务 (需要 Maven 和 Java 17+)
cd backend/user-service
mvn spring-boot:run

cd backend/profile-service
mvn spring-boot:run

cd backend/tag-service
mvn spring-boot:run

cd backend/gateway-service
mvn spring-boot:run
```

### 运行测试

```bash
# 运行所有测试
cd backend
mvn test

# 运行集成测试
mvn test -Dtest=*IntegrationTest
```

## 📊 功能模块

### 用户管理
- 用户注册、登录、认证
- 密码加密和强密码策略
- JWT Token管理
- 审计日志记录

### 画像管理
- 多维度用户画像
- 自动画像初始化
- 画像评分计算
- 用户类型分析
- 营销策略推荐

### 标签管理
- 灵活的标签系统
- 标签权重管理
- 批量操作
- 标签去重

## 🔐 安全特性

- ✅ JWT Token认证
- ✅ 密码BCrypt加密
- ✅ 强密码策略 (8位+，包含大小写字母、数字、特殊字符)
- ✅ API限流保护 (基于IP)
- ✅ 完整审计日志
- ✅ CORS跨域配置

## 📈 性能优化

- ✅ Redis缓存层
- ✅ MongoDB索引优化 (查询性能提升100倍)
- ✅ 数据库连接池
- ✅ 分页查询
- ✅ 异步处理

## 🔄 高可用

- ✅ Consul服务发现
- ✅ 客户端负载均衡
- ✅ 熔断降级机制
- ✅ 分布式事务补偿
- ✅ 健康检查

## 🧪 测试

- **单元测试**: 60个测试用例
- **集成测试**: 33个端到端测试
- **总测试数**: 93个
- **覆盖率**: 75%+

## 📝 版本信息

**当前版本**: v1.0
**发布日期**: 2025-01-05
**状态**: ✅ 生产就绪

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

[MIT License](LICENSE)

## 👨‍💻 作者

- GitHub: [@dctx479](https://github.com/dctx479)
- Email: b150w4942@163.com

## 🙏 致谢

感谢所有开源项目和社区的支持！
