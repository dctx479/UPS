# 故障排查

## 常见问题

### 服务启动失败

#### 问题1: 端口被占用
```bash
# 错误信息
Port 8080 is already in use

# 解决方案
# 查找占用端口的进程
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Linux/Mac

# 停止占用进程或修改端口
# 修改 application.yml 中的 server.port
```

#### 问题2: 数据库连接失败
```bash
# 错误信息
Unable to connect to database

# 检查数据库服务
docker-compose ps

# 查看数据库日志
docker-compose logs postgres
docker-compose logs mongodb

# 验证连接
docker exec -it postgres psql -U postgres
docker exec -it mongodb mongosh
```

#### 问题3: Consul连接失败
```bash
# 错误信息
Connection refused: localhost:8500

# 检查Consul状态
docker-compose ps consul

# 重启Consul
docker-compose restart consul

# 禁用Consul（临时）
# 在 application.yml 添加:
spring:
  cloud:
    consul:
      discovery:
        enabled: false
```

---

## 服务问题

### Gateway无法访问

**症状**: 访问 http://localhost:8080 返回404

**排查步骤**:
```bash
# 1. 检查Gateway服务状态
docker-compose ps gateway-service

# 2. 查看Gateway日志
docker-compose logs gateway-service

# 3. 检查路由配置
curl http://localhost:8080/actuator/gateway/routes

# 4. 验证后端服务是否注册
curl http://localhost:8500/v1/catalog/services
```

**常见原因**:
- 后端服务未启动
- 服务未注册到Consul
- 路由配置错误

### 用户服务异常

**症状**: 登录失败或创建用户失败

**排查步骤**:
```bash
# 1. 检查日志
docker-compose logs user-service | grep ERROR

# 2. 验证数据库
docker exec -it postgres psql -U postgres -d userprofile_db
\dt  # 查看表
SELECT * FROM users LIMIT 5;

# 3. 测试直接访问
curl http://localhost:8081/actuator/health

# 4. 检查密码策略
# 确保密码至少8位，包含大小写字母、数字、特殊字符
```

### 画像服务异常

**症状**: 画像查询失败或初始化失败

**排查步骤**:
```bash
# 1. 检查MongoDB连接
docker exec -it mongodb mongosh
use userprofile_db
db.user_profiles.find().limit(5)

# 2. 验证索引
db.user_profiles.getIndexes()

# 3. 检查缓存
docker exec -it redis redis-cli
KEYS userProfiles*

# 4. 查看补偿调度器日志
docker-compose logs user-service | grep "补偿"
```

### 标签服务异常

**排查步骤**:
```bash
# 检查标签数据
docker exec -it postgres psql -U postgres -d userprofile_db
SELECT * FROM tags WHERE user_id = 1;

# 测试标签去重
curl -X POST http://localhost:8080/api/tags/deduplicate?userId=1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 性能问题

### 响应慢

**排查步骤**:
```bash
# 1. 查看慢查询日志
docker-compose logs profile-service | grep "slow"

# 2. 检查数据库性能
# PostgreSQL
docker exec -it postgres psql -U postgres
SELECT * FROM pg_stat_activity;

# MongoDB
docker exec -it mongodb mongosh
db.currentOp()

# 3. 检查缓存命中率
docker exec -it redis redis-cli
INFO stats

# 4. 查看JVM内存
docker stats
```

**优化建议**:
- 添加必要的数据库索引
- 增加Redis缓存容量
- 调整JVM堆内存
- 启用查询缓存

### 内存溢出

**症状**: `OutOfMemoryError`

**解决方案**:
```bash
# 1. 增加JVM内存（docker-compose.yml）
environment:
  JAVA_OPTS: "-Xms2g -Xmx4g"

# 2. 分析内存使用
docker exec -it user-service jmap -heap 1

# 3. 生成heap dump
docker exec -it user-service jmap -dump:format=b,file=/tmp/heap.bin 1
```

### CPU占用高

**排查步骤**:
```bash
# 查看进程CPU
docker stats

# 查看线程栈
docker exec -it user-service jstack 1 > thread_dump.txt

# 分析慢接口
curl http://localhost:8080/actuator/metrics/http.server.requests
```

---

## 数据问题

### 数据不一致

**症状**: 用户存在但画像不存在

**解决方案**:
```bash
# 检查补偿调度器日志
docker-compose logs user-service | grep "补偿"

# 手动触发补偿（重启服务）
docker-compose restart user-service

# 查询缺失的画像
# 在user-service容器中执行检查逻辑
```

### 缓存不一致

**症状**: 更新数据后仍返回旧数据

**解决方案**:
```bash
# 清除特定缓存
docker exec -it redis redis-cli
DEL userProfiles::1

# 清除所有缓存
FLUSHALL

# 重启服务刷新缓存
docker-compose restart profile-service
```

---

## 限流问题

### 频繁429错误

**症状**: `429 Too Many Requests`

**解决方案**:
```bash
# 1. 检查当前限流配置
# gateway-service/application.yml
redis-rate-limiter:
  replenishRate: 100  # 增加此值
  burstCapacity: 200

# 2. 查看Redis限流key
docker exec -it redis redis-cli
KEYS request_rate_limiter*

# 3. 重启Gateway应用新配置
docker-compose restart gateway-service
```

---

## 日志分析

### 查看错误日志
```bash
# 所有错误
docker-compose logs | grep ERROR

# 特定服务错误
docker-compose logs user-service | grep ERROR

# 实时跟踪
docker-compose logs -f --tail=100 user-service
```

### 审计日志查询
```bash
# 连接数据库
docker exec -it postgres psql -U postgres -d userprofile_db

# 查询审计日志
SELECT * FROM audit_logs
WHERE action = 'USER_CREATED'
ORDER BY timestamp DESC
LIMIT 10;

# 查询特定用户操作
SELECT * FROM audit_logs
WHERE user_id = 1
ORDER BY timestamp DESC;
```

---

## 网络问题

### 服务间无法通信

**排查步骤**:
```bash
# 1. 检查Docker网络
docker network ls
docker network inspect 构建用户画像_default

# 2. 测试服务间连接
docker exec -it gateway-service ping user-service
docker exec -it gateway-service curl http://user-service:8081/actuator/health

# 3. 检查Consul服务发现
curl http://localhost:8500/v1/health/service/user-service
```

### CORS跨域问题

**症状**: 前端请求被浏览器拦截

**解决方案**:
```yaml
# gateway-service/application.yml
globalcors:
  corsConfigurations:
    '[/**]':
      allowedOrigins:
        - "http://localhost:3000"
        - "https://yourdomain.com"
      allowedMethods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
```

---

## 调试技巧

### 启用DEBUG日志
```yaml
# application.yml
logging:
  level:
    com.userprofile: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

### 远程调试
```yaml
# docker-compose.yml
environment:
  JAVA_OPTS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ports:
  - "5005:5005"  # 调试端口
```

### 性能分析
```bash
# 启用Spring Boot Actuator端点
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
curl http://localhost:8080/actuator/health
```

---

## 获取帮助

### 查看系统信息
```bash
# Java版本
docker exec -it user-service java -version

# Spring Boot版本
docker exec -it user-service cat /app/BOOT-INF/classes/application.yml | grep version

# 系统资源
docker stats --no-stream
```

### 导出诊断信息
```bash
# 导出所有日志
docker-compose logs > system_logs_$(date +%Y%m%d).log

# 导出数据库结构
docker exec -it postgres pg_dump -U postgres -s userprofile_db > schema.sql

# 导出配置
tar -czf config_$(date +%Y%m%d).tar.gz backend/*/src/main/resources/
```

### 联系支持

收集以下信息:
1. 错误日志
2. 系统配置
3. 重现步骤
4. 环境信息（OS、Docker版本等）
