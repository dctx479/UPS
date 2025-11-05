# 安全配置指南

## 概述

本文档详细说明User Profiling System的安全配置要求和最佳实践。

⚠️ **重要**：生产环境部署前必须完成所有安全配置！

---

## 关键安全问题修复记录

### 已修复的高危问题（v1.0）

| 问题类型 | 严重级别 | 状态 | 修复说明 |
|---------|---------|------|---------|
| JWT密钥使用默认值 | 🔴 高危 | ✅ 已修复 | 移除默认密钥，强制环境变量配置 |
| 数据库密码硬编码 | 🔴 高危 | ✅ 已修复 | 使用环境变量+自动生成随机密码 |
| Redis未配置密码 | 🔴 高危 | ✅ 已修复 | 已配置requirepass参数 |
| 使用废弃Date API | 🟡 中危 | ✅ 已修复 | 替换为java.time包 |
| 配置文件不一致 | 🟡 中危 | ✅ 已修复 | 删除冗余的backend/docker-compose.yml |

---

## JWT安全配置

### 1. 配置要求

**强制要求**：
- JWT密钥不能为空
- 最小长度：32个字符（256位）
- 推荐长度：48个字符（384位）

系统启动时会自动验证JWT配置，如果不符合要求将拒绝启动。

### 2. 配置方法

#### 方法1：使用quick-start.sh（推荐）

```bash
./quick-start.sh
```

脚本会自动生成48字符的强随机JWT密钥。

#### 方法2：手动配置

```bash
# 生成JWT密钥
JWT_SECRET=$(openssl rand -base64 48 | tr -d "=+/")

# 写入.env文件
echo "JWT_SECRET=${JWT_SECRET}" >> .env
```

#### 方法3：环境变量

```bash
export JWT_SECRET="your-strong-secret-key-at-least-32-characters-long"
docker-compose up -d
```

### 3. 验证配置

启动服务后检查日志：

```bash
docker-compose logs gateway-service | grep JWT
```

**正常输出**：服务正常启动
**错误输出**：
- "JWT密钥未配置！" → 需要设置环境变量
- "JWT密钥长度不足！" → 密钥至少32个字符

---

## 数据库安全配置

### 1. MySQL安全配置

#### 密码配置

⚠️ **禁止使用弱密码**：`root123`, `admin123`, `userservice123`

```bash
# 生成强随机密码（24字符）
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
MYSQL_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
```

#### 网络安全

```yaml
# docker-compose.yml
mysql:
  ports:
    - "127.0.0.1:3306:3306"  # 仅本地访问
```

#### 用户权限

```sql
-- 删除匿名用户
DELETE FROM mysql.user WHERE User='';

-- 删除测试数据库
DROP DATABASE IF EXISTS test;

-- 限制root用户远程访问
DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1');

-- 刷新权限
FLUSH PRIVILEGES;
```

### 2. MongoDB安全配置

#### 密码配置

```bash
# 生成强随机密码
MONGO_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
```

#### 启用认证

MongoDB配置已默认启用认证：
```yaml
mongodb:
  environment:
    - MONGO_INITDB_ROOT_USERNAME=admin
    - MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD}
```

#### 网络安全

```yaml
mongodb:
  ports:
    - "127.0.0.1:27017:27017"  # 仅本地访问
```

### 3. Redis安全配置

#### 密码配置

```bash
# 生成强随机密码
REDIS_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
```

#### 启用密码认证

Redis已配置requirepass参数：
```yaml
redis:
  command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
```

#### 禁用危险命令

```conf
# redis.conf (生产环境建议)
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command CONFIG ""
rename-command KEYS ""
```

---

## 网络安全

### 1. 端口暴露策略

**生产环境配置**：

```yaml
# 仅暴露Gateway到公网
gateway-service:
  ports:
    - "8080:8080"  # 对外服务

# 数据库仅内网访问
mysql:
  ports:
    - "127.0.0.1:3306:3306"

mongodb:
  ports:
    - "127.0.0.1:27017:27017"

redis:
  ports:
    - "127.0.0.1:6379:6379"
```

### 2. 防火墙规则

```bash
# 允许Gateway端口
sudo ufw allow 8080/tcp

# 拒绝直接访问数据库
sudo ufw deny 3306/tcp
sudo ufw deny 27017/tcp
sudo ufw deny 6379/tcp

# 启用防火墙
sudo ufw enable
```

### 3. Docker网络隔离

```yaml
networks:
  ups-network:
    driver: bridge
    internal: false  # Gateway需要对外

  db-network:
    driver: bridge
    internal: true   # 数据库内网隔离
```

---

## SSL/TLS配置

### 1. Gateway HTTPS配置

```yaml
# application-prod.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
    key-alias: gateway
```

### 2. 生成SSL证书

```bash
# 生成自签名证书（开发环境）
keytool -genkeypair -alias gateway \
  -keyalg RSA -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 365

# 生产环境使用Let's Encrypt
certbot certonly --standalone -d yourdomain.com
```

### 3. 数据库SSL连接

**MySQL**:
```yaml
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/userservice?useSSL=true&requireSSL=true
```

**MongoDB**:
```yaml
SPRING_DATA_MONGODB_URI: mongodb://admin:password@mongodb:27017/userprofile?ssl=true
```

---

## 密钥管理最佳实践

### 1. 环境隔离

| 环境 | 密钥管理方式 | 说明 |
|------|------------|------|
| 开发环境 | .env文件 | 加入.gitignore，不提交到Git |
| 测试环境 | 环境变量 | CI/CD系统管理 |
| 生产环境 | 密钥管理服务 | Vault/AWS Secrets Manager/Azure Key Vault |

### 2. 密钥轮换策略

**轮换周期**：
- JWT密钥：每180天
- 数据库密码：每90天
- Redis密码：每90天
- SSL证书：每365天（或使用自动续期）

**轮换流程**：
1. 生成新密钥
2. 配置双密钥并存（过渡期）
3. 更新所有服务配置
4. 滚动重启服务
5. 验证新密钥生效
6. 撤销旧密钥

### 3. 密钥备份

```bash
# 备份.env文件（加密存储）
gpg --encrypt --recipient your@email.com .env

# 恢复
gpg --decrypt .env.gpg > .env
```

---

## 审计与监控

### 1. 日志审计

记录以下安全事件：
- 用户登录/登出
- JWT令牌生成/验证失败
- 数据库连接失败
- 异常API调用
- 权限拒绝事件

```yaml
# application.yml
logging:
  level:
    com.userprofile.common.security: INFO
    org.springframework.security: WARN
```

### 2. 安全监控指标

- JWT验证失败率
- 登录失败次数
- 异常API调用频率
- 数据库连接池使用率

### 3. 告警配置

```yaml
# Prometheus告警规则
- alert: HighLoginFailureRate
  expr: rate(login_failures_total[5m]) > 10
  for: 5m
  labels:
    severity: warning

- alert: JWTValidationFailures
  expr: rate(jwt_validation_failures_total[5m]) > 5
  for: 5m
  labels:
    severity: critical
```

---

## 安全检查清单

### 部署前检查

- [ ] JWT密钥已配置（≥32字符）
- [ ] 所有数据库密码已修改（≥16字符）
- [ ] Redis已配置密码
- [ ] .env文件已加入.gitignore
- [ ] 数据库端口仅内网访问
- [ ] 已配置HTTPS（生产环境）
- [ ] 已配置防火墙规则
- [ ] 已禁用危险的Redis命令
- [ ] 已删除数据库匿名用户
- [ ] 已配置日志审计
- [ ] 已配置监控告警
- [ ] 已配置定期备份
- [ ] 已测试密钥轮换流程

### 定期安全检查（每月）

- [ ] 检查日志中的异常登录
- [ ] 检查JWT验证失败记录
- [ ] 检查数据库连接失败记录
- [ ] 检查SSL证书有效期
- [ ] 检查系统漏洞更新
- [ ] 检查依赖包安全漏洞
- [ ] 检查防火墙规则
- [ ] 测试备份恢复流程

---

## 应急响应

### 密钥泄露应急流程

**JWT密钥泄露**：
1. 立即生成新密钥
2. 更新所有服务配置
3. 滚动重启所有服务
4. 强制所有用户重新登录
5. 审计日志，查找异常使用

**数据库密码泄露**：
1. 立即修改数据库密码
2. 更新服务配置
3. 重启相关服务
4. 审计数据库访问日志
5. 检查数据是否被篡改

**Redis密码泄露**：
1. 立即修改Redis密码
2. 更新服务配置
3. 重启相关服务
4. 清空缓存数据
5. 审计Redis访问日志

---

## 联系方式

如发现安全漏洞，请联系：
- 邮箱：b150w4942@163.com
- 标题：[SECURITY] User Profiling System 安全漏洞报告

我们会在24小时内响应安全报告。
