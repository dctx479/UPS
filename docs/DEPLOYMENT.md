# 部署指南

## Docker Compose部署（推荐）

### 前置要求
- Docker 20.10+
- Docker Compose 2.0+
- 8GB+ 内存
- 20GB+ 磁盘空间

### 部署步骤

#### 1. 环境配置
```bash
# 创建环境变量文件
cp .env.example .env

# 编辑配置（可选）
vim .env
```

**.env 配置项**:
```env
# 数据库配置
POSTGRES_PASSWORD=your_strong_password
MONGO_ROOT_PASSWORD=your_strong_password
REDIS_PASSWORD=your_strong_password

# JWT密钥（生产环境必须修改）
JWT_SECRET=your_jwt_secret_key_at_least_32_chars

# 允许的跨域来源
ALLOWED_ORIGINS=https://yourdomain.com
```

#### 2. 启动服务
```bash
# 构建并启动
docker-compose up -d

# 查看日志
docker-compose logs -f

# 查看服务状态
docker-compose ps
```

#### 3. 健康检查
```bash
# 检查各服务健康状态
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

# 检查Consul服务注册
curl http://localhost:8500/v1/catalog/services
```

#### 4. 初始化数据
```bash
# 创建管理员用户
docker exec -it user-service bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin@123456",
    "name": "系统管理员",
    "email": "admin@example.com"
  }'
```

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

### 1. 数据库优化

**PostgreSQL**:
```sql
-- 创建索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_tags_user_id ON tags(user_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
```

**MongoDB**:
```javascript
// 索引会自动创建，也可手动创建
db.user_profiles.createIndex({ userId: 1 }, { unique: true });
db.user_profiles.createIndex({ username: 1 });
db.user_profiles.createIndex({ updateTime: -1 });
db.user_profiles.createIndex({ profileScore: -1 });
```

### 2. JVM参数调优

```bash
# user-service
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# profile-service
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 3. Redis配置

```conf
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru
save ""  # 禁用RDB（使用AOF）
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

**PostgreSQL**:
```bash
# 每日备份
0 2 * * * docker exec postgres pg_dump -U postgres userprofile_db > /backup/postgres_$(date +\%Y\%m\%d).sql
```

**MongoDB**:
```bash
# 每日备份
0 2 * * * docker exec mongodb mongodump --db userprofile_db --out /backup/mongo_$(date +\%Y\%m\%d)
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

### 1. 网络隔离
- 数据库不对外暴露
- 仅Gateway暴露公网
- 使用防火墙规则

### 2. 密码安全
- 所有默认密码必须修改
- 使用强密码（16位+混合字符）
- 定期轮换密码

### 3. SSL/TLS
```yaml
# Gateway配置HTTPS
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
```

### 4. 限流配置
根据实际业务调整限流参数:
```yaml
redis-rate-limiter:
  replenishRate: 100  # 调整为合适的值
  burstCapacity: 200
```

---

## 故障恢复

### 服务重启
```bash
# Docker Compose
docker-compose restart user-service

# Kubernetes
kubectl rollout restart deployment/user-service -n user-profile
```

### 数据恢复
```bash
# PostgreSQL
docker exec -i postgres psql -U postgres userprofile_db < backup.sql

# MongoDB
docker exec -i mongodb mongorestore --db userprofile_db /backup/mongo_20250105
```

### 回滚部署
```bash
# Kubernetes
kubectl rollout undo deployment/user-service -n user-profile
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
