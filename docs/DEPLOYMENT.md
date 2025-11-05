# 部署指南

## Docker Compose部署（推荐）

### 前置要求
- Docker 20.10+
- Docker Compose 2.0+
- 8GB+ 内存
- 20GB+ 磁盘空间

### 快速部署

#### 方式一：自动部署脚本（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/dctx479/UPS.git
cd UPS

# 2. 运行快速启动脚本
chmod +x quick-start.sh
./quick-start.sh
```

快速启动脚本会自动：
- 检查环境依赖
- 生成环境配置文件（.env）
- 拉取基础镜像
- 构建微服务镜像
- 启动所有服务
- 等待服务就绪

#### 方式二：交互式部署脚本

```bash
# 使用交互式部署脚本
chmod +x deploy.sh
./deploy.sh
```

交互式脚本提供以下功能：
1. 部署所有服务（完整堆栈）
2. 仅部署基础设施（Consul, Redis, MongoDB, MySQL）
3. 仅部署微服务
4. 停止所有服务
5. 查看服务日志
6. 检查服务状态
7. 清理容器和数据卷

#### 方式三：手动部署

```bash
# 1. 环境配置
cp .env.example .env
vim .env  # 编辑配置

# 2. 启动服务
docker-compose up -d

# 3. 查看日志
docker-compose logs -f

# 4. 查看服务状态
docker-compose ps
```

### 环境配置

⚠️ **安全警告**：生产环境必须修改所有默认密码和密钥！

**.env 配置项**:
```env
# ⚠️ 以下为示例值，生产环境必须修改！

# MySQL配置
MYSQL_ROOT_PASSWORD=CHANGE_THIS_IN_PRODUCTION
MYSQL_USER=userservice
MYSQL_PASSWORD=CHANGE_THIS_IN_PRODUCTION

# MongoDB配置
MONGO_USERNAME=admin
MONGO_PASSWORD=CHANGE_THIS_IN_PRODUCTION

# Redis配置
REDIS_PASSWORD=CHANGE_THIS_IN_PRODUCTION

# JWT密钥（必须至少32个字符，推荐48个字符）
# 生成方法: openssl rand -base64 48
JWT_SECRET=CHANGE_THIS_JWT_SECRET_KEY_AT_LEAST_32_CHARS_REQUIRED
```

**生成安全密钥**:
```bash
# 生成强随机密码（24字符）
openssl rand -base64 24 | tr -d "=+/"

# 生成JWT密钥（48字符）
openssl rand -base64 48 | tr -d "=+/"
```

**快速部署注意事项**:
- 使用 `quick-start.sh` 会自动生成随机密码
- 生成的密码保存在 `.env` 文件中
- 请务必备份 `.env` 文件！
- 首次部署后会在控制台显示生成的密码

### 健康检查
```bash
# 检查各服务健康状态
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Profile Service
curl http://localhost:8083/actuator/health  # Behavior Service

# 检查Consul服务注册
curl http://localhost:8500/v1/catalog/services
```

### 初始化数据

数据库初始化脚本会自动执行：
- MySQL: `scripts/mysql-init.sql` - 创建表结构和默认管理员用户
- MongoDB: `scripts/mongo-init.js` - 创建集合、索引和示例数据

默认管理员账号：
- 用户名: `admin`
- 密码: `admin123`

### 服务管理

**停止服务**:
```bash
docker-compose stop
```

**重启服务**:
```bash
docker-compose restart
```

**查看日志**:
```bash
# 所有服务
docker-compose logs -f

# 特定服务
docker-compose logs -f user-service
```

**清理数据**:
```bash
# 停止并删除容器和卷
docker-compose down -v
```

---

## Kubernetes部署

### 前置要求
- Kubernetes 1.24+
- kubectl已配置
- Helm 3.0+

### 部署步骤

#### 1. 创建命名空间
```bash
kubectl create namespace user-profile
```

#### 2. 配置Secret
```bash
# 创建数据库密码
kubectl create secret generic db-secrets \
  --from-literal=postgres-password=your_password \
  --from-literal=mongo-password=your_password \
  --from-literal=redis-password=your_password \
  -n user-profile

# 创建JWT密钥
kubectl create secret generic jwt-secret \
  --from-literal=secret-key=your_jwt_secret_key \
  -n user-profile
```

#### 3. 部署基础设施
```bash
# 部署PostgreSQL
helm install postgresql bitnami/postgresql \
  --set auth.existingSecret=db-secrets \
  -n user-profile

# 部署MongoDB
helm install mongodb bitnami/mongodb \
  --set auth.existingSecret=db-secrets \
  -n user-profile

# 部署Redis
helm install redis bitnami/redis \
  --set auth.existingSecret=db-secrets \
  -n user-profile

# 部署Consul
helm install consul hashicorp/consul \
  -n user-profile
```

#### 4. 部署应用服务
```bash
# 应用配置文件在 k8s/ 目录
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/profile-service.yaml
kubectl apply -f k8s/tag-service.yaml
kubectl apply -f k8s/gateway-service.yaml
```

#### 5. 暴露服务
```bash
# 使用Ingress
kubectl apply -f k8s/ingress.yaml

# 或使用LoadBalancer
kubectl expose deployment gateway-service \
  --type=LoadBalancer \
  --port=80 \
  --target-port=8080 \
  -n user-profile
```

### 监控和维护

**查看Pod状态**:
```bash
kubectl get pods -n user-profile
```

**查看日志**:
```bash
kubectl logs -f deployment/user-service -n user-profile
```

**扩容**:
```bash
kubectl scale deployment user-service --replicas=3 -n user-profile
```

---

## 生产环境优化

### 数据库优化

**MySQL索引** (自动创建于 `scripts/mysql-init.sql`):
```sql
-- 用户表索引
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_phone ON users(phone);
CREATE INDEX idx_status ON users(status);
CREATE INDEX idx_create_time ON users(create_time);
```

**MongoDB索引** (自动创建于实体类和 `scripts/mongo-init.js`):
```javascript
// user_profiles 索引
db.user_profiles.createIndex({ userId: 1 }, { unique: true });
db.user_profiles.createIndex({ username: 1 });
db.user_profiles.createIndex({ updateTime: -1 });
db.user_profiles.createIndex({ profileScore: -1 });
db.user_profiles.createIndex({ userId: 1, updateTime: -1 });

// user_behaviors 索引
db.user_behaviors.createIndex({ userId: 1 });
db.user_behaviors.createIndex({ behaviorType: 1 });
db.user_behaviors.createIndex({ timestamp: -1 });
db.user_behaviors.createIndex({ userId: 1, timestamp: -1 });
```

### JVM参数调优

Dockerfile中已配置容器化优化参数：
```bash
# 自动使用容器内存限制
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0

# 优化随机数生成
-Djava.security.egd=file:/dev/./urandom
```

生产环境可通过环境变量覆盖：
```yaml
# docker-compose.yml
environment:
  JAVA_OPTS: "-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Redis配置

docker-compose.yml 中已配置：
```yaml
redis:
  command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD:-redis123}
```

生产环境建议配置：
```conf
# 内存限制
maxmemory 2gb
maxmemory-policy allkeys-lru

# 持久化策略
appendonly yes
appendfsync everysec
```

### 4. 监控配置

**Prometheus监控**:
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'user-profile-services'
    consul_sd_configs:
      - server: 'localhost:8500'
    relabel_configs:
      - source_labels: [__meta_consul_service]
        target_label: service
```

**告警规则**:
```yaml
# alerts.yml
groups:
  - name: service_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
```

---

## 备份策略

### 数据库备份

**MySQL备份** (使用docker-compose服务名):
```bash
# 手动备份
docker exec ups-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} userservice > backup_$(date +%Y%m%d).sql

# 定时备份 (crontab)
0 2 * * * docker exec ups-mysql mysqldump -u root -proot123 userservice > /backup/mysql_$(date +\%Y\%m\%d).sql
```

**MongoDB备份**:
```bash
# 手动备份
docker exec ups-mongodb mongodump --uri="mongodb://admin:admin123@localhost:27017" --db=userprofile --out=/backup/mongo_$(date +%Y%m%d)

# 定时备份 (crontab)
0 2 * * * docker exec ups-mongodb mongodump --uri="mongodb://admin:admin123@localhost:27017" --db=userprofile --out=/backup/mongo_$(date +\%Y\%m\%d)
```

**恢复数据**:
```bash
# MySQL恢复
docker exec -i ups-mysql mysql -u root -proot123 userservice < backup_20250105.sql

# MongoDB恢复
docker exec ups-mongodb mongorestore --uri="mongodb://admin:admin123@localhost:27017" --db=userprofile /backup/mongo_20250105/userprofile
```

### 配置备份
```bash
# 备份配置文件
tar -czf config_backup_$(date +%Y%m%d).tar.gz \
  backend/*/src/main/resources/application.yml \
  docker-compose.yml \
  .env
```

---

## 安全加固

### 1. JWT密钥安全

⚠️ **关键安全配置**：

JWT密钥必须通过环境变量配置，系统启动时会验证：
- 密钥不能为空
- 密钥长度至少32个字符（256位）
- 如果未配置或长度不足，系统将拒绝启动

**配置方法**：
```bash
# 方式1：通过.env文件
JWT_SECRET=$(openssl rand -base64 48 | tr -d "=+/")

# 方式2：通过环境变量
export JWT_SECRET="your-strong-secret-key-at-least-32-characters"

# 方式3：在docker-compose.yml中配置
environment:
  - JWT_SECRET=${JWT_SECRET}
```

**验证配置**：
系统启动日志会显示JWT配置状态，如果看到以下错误，请检查配置：
- "JWT密钥未配置！" - 需要设置jwt.secret环境变量
- "JWT密钥长度不足！" - 密钥至少需要32个字符

### 2. 网络隔离
- 数据库不对外暴露
- 仅Gateway暴露公网
- 使用防火墙规则

### 3. 密码安全策略

⚠️ **强制安全要求**：

**密码强度要求**：
- 所有默认密码必须修改
- 最小长度：16个字符
- 必须包含：大小写字母、数字、特殊字符
- 定期轮换密码（建议每90天）

**数据库密码管理**：
```bash
# 生产环境禁止使用以下弱密码：
# ❌ root123, admin123, userservice123, redis123

# 使用强随机密码（推荐）:
# ✅
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
MONGO_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
REDIS_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
```

**密钥存储建议**：
- 开发环境：使用 `.env` 文件（加入 .gitignore）
- 测试环境：使用环境变量
- 生产环境：使用密钥管理服务（Vault, AWS Secrets Manager, Azure Key Vault）

### 4. SSL/TLS
```yaml
# Gateway配置HTTPS
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
```

### 5. 限流配置
根据实际业务调整限流参数:
```yaml
redis-rate-limiter:
  replenishRate: 100  # 调整为合适的值
  burstCapacity: 200
```

### 6. 安全检查清单

部署前请确认：

- [ ] 所有默认密码已修改
- [ ] JWT密钥长度≥32个字符
- [ ] .env文件已加入.gitignore
- [ ] 数据库仅内网访问
- [ ] Redis已配置密码
- [ ] 已配置HTTPS（生产环境）
- [ ] 已配置防火墙规则
- [ ] 已配置日志审计
- [ ] 已配置监控告警
- [ ] 已配置定期备份

---

## 故障恢复

### 服务重启
```bash
# Docker Compose - 重启单个服务
docker-compose restart user-service

# Docker Compose - 重启所有服务
docker-compose restart

# Docker Compose - 重新构建并重启
docker-compose up -d --build user-service
```

### 数据恢复
```bash
# MySQL恢复
docker exec -i ups-mysql mysql -u root -proot123 userservice < backup_20250105.sql

# MongoDB恢复
docker exec ups-mongodb mongorestore --uri="mongodb://admin:admin123@localhost:27017" --db=userprofile /backup/mongo_20250105/userprofile
```

---

## 性能指标

生产环境建议监控以下指标:

- **响应时间**: P95 < 200ms, P99 < 500ms
- **吞吐量**: > 1000 QPS
- **错误率**: < 0.1%
- **可用性**: > 99.9%
- **CPU使用率**: < 70%
- **内存使用率**: < 80%
- **数据库连接池**: < 80%占用

---

## 扩容建议

| 指标 | 扩容触发条件 | 建议操作 |
|------|-------------|----------|
| CPU | > 70% 持续5分钟 | 增加副本 |
| 内存 | > 80% | 增加副本或内存 |
| QPS | > 设计容量80% | 增加副本 |
| 响应时间 | P99 > 1s | 增加副本或优化 |
| 数据库连接 | > 80%占用 | 增加连接池或副本 |
