# 服务器公网访问配置指南

## 概述

本指南详细说明如何将User Profiling System部署到云服务器并配置公网访问。

---

## 前置准备

### 1. 服务器要求

| 配置项 | 最低要求 | 推荐配置 |
|-------|---------|---------|
| CPU | 2核 | 4核+ |
| 内存 | 4GB | 8GB+ |
| 磁盘 | 20GB | 50GB+ SSD |
| 带宽 | 1Mbps | 5Mbps+ |
| 操作系统 | Ubuntu 20.04+ / CentOS 7+ | Ubuntu 22.04 LTS |

### 2. 云服务商选择

支持的云服务商：
- 阿里云 ECS
- 腾讯云 CVM
- 华为云 ECS
- AWS EC2
- Azure VM
- Google Cloud Compute Engine

### 3. 必需软件

```bash
# 安装Docker
curl -fsSL https://get.docker.com | bash

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

---

## 快速部署

### 方法1：一键部署脚本（推荐）

```bash
# 1. 连接到服务器
ssh user@your-server-ip

# 2. 克隆项目
git clone https://github.com/dctx479/UPS.git
cd UPS

# 3. 运行快速部署脚本
chmod +x quick-start.sh
./quick-start.sh

# 4. 等待服务启动（约5-10分钟）
docker-compose ps
```

脚本会自动：
- ✅ 生成安全的随机密码
- ✅ 配置环境变量
- ✅ 构建并启动所有服务
- ✅ 显示服务访问地址

### 方法2：手动部署

```bash
# 1. 克隆项目
git clone https://github.com/dctx479/UPS.git
cd UPS

# 2. 配置环境变量
cp .env.example .env

# 3. 生成安全密钥
cat > .env << EOF
JWT_SECRET=$(openssl rand -base64 48 | tr -d "=+/")
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
MYSQL_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
MONGO_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
REDIS_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/")
MYSQL_USER=userservice
MONGO_USERNAME=admin
EOF

# 4. 启动服务
docker-compose up -d

# 5. 查看日志
docker-compose logs -f
```

---

## 网络配置

### 1. 配置安全组/防火墙规则

#### 阿里云/腾讯云安全组配置

登录云控制台 → 安全组 → 添加规则：

| 规则方向 | 端口范围 | 授权对象 | 说明 |
|---------|---------|---------|------|
| 入方向 | 8080 | 0.0.0.0/0 | API Gateway（必需）|
| 入方向 | 8500 | 你的IP/32 | Consul UI（管理用）|
| 入方向 | 22 | 你的IP/32 | SSH（管理用）|
| 入方向 | 80 | 0.0.0.0/0 | HTTP（可选）|
| 入方向 | 443 | 0.0.0.0/0 | HTTPS（推荐）|

⚠️ **安全建议**：
- 数据库端口（3306, 27017, 6379）不要对外开放
- 微服务端口（8081-8083）不要对外开放
- 仅开放Gateway端口8080

#### Linux防火墙配置

**Ubuntu/Debian (ufw)**:
```bash
# 启用防火墙
sudo ufw enable

# 允许SSH
sudo ufw allow 22/tcp

# 允许API Gateway
sudo ufw allow 8080/tcp

# 允许HTTPS（推荐）
sudo ufw allow 443/tcp

# 查看规则
sudo ufw status
```

**CentOS/RHEL (firewalld)**:
```bash
# 启动防火墙
sudo systemctl start firewalld
sudo systemctl enable firewalld

# 允许端口
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --reload

# 查看规则
sudo firewall-cmd --list-all
```

### 2. 服务端口映射

默认端口配置：

| 服务 | 内部端口 | 外部端口 | 公网访问 |
|------|---------|---------|---------|
| API Gateway | 8080 | 8080 | ✅ 是 |
| Consul UI | 8500 | 8500 | ⚠️ 限制IP |
| User Service | 8081 | - | ❌ 否 |
| Profile Service | 8082 | - | ❌ 否 |
| Tag Service | 8083 | - | ❌ 否 |
| MySQL | 3306 | - | ❌ 否 |
| MongoDB | 27017 | - | ❌ 否 |
| Redis | 6379 | - | ❌ 否 |

### 3. 域名配置（可选但推荐）

#### A. 购买域名

从域名注册商购买域名：
- 阿里云万网
- 腾讯云DNSPod
- GoDaddy
- Namecheap

#### B. 配置DNS解析

添加A记录：
```
类型: A
主机记录: api (或 @)
记录值: 你的服务器公网IP
TTL: 600
```

示例：
- `api.yourdomain.com` → `123.45.67.89`
- `ups.yourdomain.com` → `123.45.67.89`

#### C. 验证DNS解析

```bash
# 等待5-10分钟后验证
nslookup api.yourdomain.com

# 或使用dig
dig api.yourdomain.com
```

---

## HTTPS配置（强烈推荐）

### 方法1：使用Nginx反向代理 + Let's Encrypt（推荐）

#### 1. 安装Nginx

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nginx -y

# CentOS/RHEL
sudo yum install nginx -y

# 启动Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

#### 2. 配置Nginx反向代理

创建配置文件：
```bash
sudo nano /etc/nginx/sites-available/ups
```

添加以下内容：
```nginx
server {
    listen 80;
    server_name api.yourdomain.com;  # 替换为你的域名

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

启用配置：
```bash
sudo ln -s /etc/nginx/sites-available/ups /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### 3. 安装Certbot并获取SSL证书

```bash
# Ubuntu/Debian
sudo apt install certbot python3-certbot-nginx -y

# CentOS/RHEL
sudo yum install certbot python3-certbot-nginx -y

# 获取证书（自动配置HTTPS）
sudo certbot --nginx -d api.yourdomain.com

# 测试自动续期
sudo certbot renew --dry-run
```

#### 4. 验证HTTPS

访问 `https://api.yourdomain.com/actuator/health`

应看到：
```json
{
  "status": "UP"
}
```

### 方法2：使用Caddy（自动HTTPS）

#### 1. 安装Caddy

```bash
# Ubuntu/Debian
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update
sudo apt install caddy
```

#### 2. 配置Caddyfile

```bash
sudo nano /etc/caddy/Caddyfile
```

添加内容：
```caddy
api.yourdomain.com {
    reverse_proxy localhost:8080
    encode gzip
    log {
        output file /var/log/caddy/access.log
    }
}
```

#### 3. 启动Caddy

```bash
sudo systemctl start caddy
sudo systemctl enable caddy
```

Caddy会自动获取Let's Encrypt证书并配置HTTPS！

---

## 访问方式

部署完成后，可通过以下方式访问：

### 1. 通过公网IP访问（HTTP）

```
http://your-server-ip:8080
```

**API端点**：
- API文档：`http://your-server-ip:8080/swagger-ui.html`
- 健康检查：`http://your-server-ip:8080/actuator/health`
- 用户注册：`http://your-server-ip:8080/api/users`
- 用户登录：`http://your-server-ip:8080/api/auth/login`

### 2. 通过域名访问（HTTPS，推荐）

```
https://api.yourdomain.com
```

**API端点**：
- API文档：`https://api.yourdomain.com/swagger-ui.html`
- 健康检查：`https://api.yourdomain.com/actuator/health`

### 3. 使用Web界面

#### 方式A：通过Nginx提供静态文件

```bash
# 复制前端文件到Nginx目录
sudo cp -r ~/UPS/frontend /var/www/html/ups

# 配置Nginx
sudo nano /etc/nginx/sites-available/ups-frontend
```

添加：
```nginx
server {
    listen 80;
    server_name ups.yourdomain.com;

    root /var/www/html/ups;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/ups-frontend /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# 配置HTTPS
sudo certbot --nginx -d ups.yourdomain.com
```

访问：`https://ups.yourdomain.com`

#### 方式B：使用Python简单HTTP服务器

```bash
cd ~/UPS/frontend
nohup python3 -m http.server 8000 > /tmp/frontend.log 2>&1 &
```

通过防火墙开放8000端口后访问：
```
http://your-server-ip:8000
```

---

## 测试公网访问

### 1. 基础连通性测试

```bash
# 从本地电脑测试
curl http://your-server-ip:8080/actuator/health

# 期望输出
{"status":"UP"}
```

### 2. API功能测试

使用提供的测试脚本：

```bash
# 下载测试脚本到本地
wget https://raw.githubusercontent.com/dctx479/UPS/main/test-api.sh

# 修改脚本中的服务器地址
chmod +x test-api.sh
./test-api.sh your-server-ip
```

### 3. Web界面测试

1. 在浏览器访问：`http://your-server-ip:8080/swagger-ui.html`
2. 测试用户注册
3. 测试用户登录
4. 测试API调用

---

## 性能优化

### 1. Nginx优化配置

```nginx
# /etc/nginx/nginx.conf
worker_processes auto;
worker_rlimit_nofile 65535;

events {
    worker_connections 4096;
    use epoll;
    multi_accept on;
}

http {
    # 开启gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    # 连接超时
    keepalive_timeout 65;
    client_max_body_size 10M;

    # 缓存配置
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=1g inactive=60m;
}
```

### 2. Docker Compose资源限制

```yaml
# docker-compose.yml
services:
  gateway-service:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 3. 数据库优化

**MySQL**:
```sql
-- my.cnf
[mysqld]
max_connections = 200
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
```

**MongoDB**:
```yaml
# mongod.conf
net:
  maxIncomingConnections: 200
storage:
  wiredTiger:
    engineConfig:
      cacheSizeGB: 2
```

---

## 监控与日志

### 1. 服务监控

```bash
# 查看服务状态
docker-compose ps

# 查看资源使用
docker stats

# 查看服务日志
docker-compose logs -f gateway-service
```

### 2. Nginx访问日志

```bash
# 实时查看访问日志
sudo tail -f /var/log/nginx/access.log

# 分析访问统计
sudo cat /var/log/nginx/access.log | awk '{print $1}' | sort | uniq -c | sort -nr | head -10
```

### 3. 配置日志轮转

```bash
# /etc/logrotate.d/ups
/var/log/nginx/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 nginx nginx
    sharedscripts
    postrotate
        [ -f /var/run/nginx.pid ] && kill -USR1 `cat /var/run/nginx.pid`
    endscript
}
```

---

## 故障排查

### 问题1：无法访问服务

**症状**：浏览器显示"无法访问此网站"

**排查步骤**：
```bash
# 1. 检查服务是否运行
docker-compose ps

# 2. 检查端口是否监听
sudo netstat -tlnp | grep 8080

# 3. 检查防火墙规则
sudo ufw status  # Ubuntu
sudo firewall-cmd --list-all  # CentOS

# 4. 检查云服务商安全组
# 登录云控制台检查

# 5. 测试本地访问
curl localhost:8080/actuator/health

# 6. 检查Nginx状态（如果使用）
sudo systemctl status nginx
sudo nginx -t
```

### 问题2：SSL证书问题

**症状**：HTTPS访问显示证书错误

**排查步骤**：
```bash
# 1. 检查证书状态
sudo certbot certificates

# 2. 测试证书
openssl s_client -connect api.yourdomain.com:443 -servername api.yourdomain.com

# 3. 手动续期
sudo certbot renew

# 4. 重启Nginx
sudo systemctl restart nginx
```

### 问题3：服务启动失败

**症状**：docker-compose ps显示服务退出

**排查步骤**：
```bash
# 1. 查看服务日志
docker-compose logs gateway-service

# 2. 检查环境变量
cat .env

# 3. 检查JWT配置
docker-compose logs gateway-service | grep JWT

# 4. 检查数据库连接
docker-compose logs user-service | grep -i error

# 5. 重启服务
docker-compose restart gateway-service
```

### 问题4：API响应慢

**症状**：API响应时间超过3秒

**排查步骤**：
```bash
# 1. 查看系统资源
top
htop
docker stats

# 2. 查看数据库性能
docker exec ups-mysql mysqladmin -uroot -p${MYSQL_ROOT_PASSWORD} processlist

# 3. 查看Redis状态
docker exec ups-redis redis-cli info stats

# 4. 检查Nginx日志
sudo tail -f /var/log/nginx/access.log

# 5. 优化数据库索引（参考DEPLOYMENT.md）
```

---

## 安全加固

### 1. 修改默认SSH端口

```bash
# 编辑SSH配置
sudo nano /etc/ssh/sshd_config

# 修改端口
Port 2222

# 重启SSH
sudo systemctl restart sshd

# 更新防火墙规则
sudo ufw allow 2222/tcp
sudo ufw delete allow 22/tcp
```

### 2. 配置Fail2Ban防止暴力破解

```bash
# 安装Fail2Ban
sudo apt install fail2ban -y

# 配置
sudo nano /etc/fail2ban/jail.local
```

添加：
```ini
[sshd]
enabled = true
port = 2222
maxretry = 3
bantime = 3600

[nginx-limit-req]
enabled = true
filter = nginx-limit-req
logpath = /var/log/nginx/error.log
maxretry = 5
bantime = 600
```

```bash
# 启动Fail2Ban
sudo systemctl start fail2ban
sudo systemctl enable fail2ban

# 查看状态
sudo fail2ban-client status
```

### 3. 配置HTTPS强制跳转

Nginx配置：
```nginx
server {
    listen 80;
    server_name api.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    # SSL证书配置
    ssl_certificate /etc/letsencrypt/live/api.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.yourdomain.com/privkey.pem;

    # SSL安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000" always;

    location / {
        proxy_pass http://localhost:8080;
        # ... 其他配置
    }
}
```

---

## 自动化部署

### 使用CI/CD自动部署

**GitHub Actions示例**：

创建 `.github/workflows/deploy.yml`:
```yaml
name: Deploy to Server

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to server
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.SERVER_IP }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd ~/UPS
            git pull origin main
            docker-compose down
            docker-compose build
            docker-compose up -d
```

---

## 快速参考

### 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 停止服务
docker-compose down

# 更新系统
git pull origin main
docker-compose up -d --build

# 备份数据库
docker exec ups-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} userservice > backup.sql

# 查看系统资源
docker stats
htop
```

### 访问地址速查

| 服务 | HTTP访问 | HTTPS访问（域名） |
|------|---------|-----------------|
| API文档 | `http://IP:8080/swagger-ui.html` | `https://api.domain.com/swagger-ui.html` |
| 健康检查 | `http://IP:8080/actuator/health` | `https://api.domain.com/actuator/health` |
| Web界面 | `http://IP:8000` | `https://ups.domain.com` |
| Consul | `http://IP:8500` | 不建议公网暴露 |

---

## 联系支持

遇到问题？
- 📧 邮箱：b150w4942@163.com
- 📝 GitHub Issues：https://github.com/dctx479/UPS/issues
- 📖 详细文档：查看 `docs/DEPLOYMENT.md`

---

**最后更新**：2025-11-05
