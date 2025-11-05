# 腾讯云轻量应用服务器部署指南

本指南专门针对腾讯云轻量应用服务器，提供详细的UPS系统部署和公网访问配置步骤。

---

## 前置准备

### 1. 购买轻量应用服务器

**推荐配置**：
- CPU：2核或以上
- 内存：4GB或以上
- 系统盘：40GB或以上 SSD
- 带宽：5Mbps或以上
- 操作系统：Ubuntu 22.04 LTS（推荐）或 CentOS 7.9

### 2. 获取服务器信息

登录 [腾讯云控制台](https://console.cloud.tencent.com/lighthouse/instance) 获取：
- 公网IP地址
- root密码（或SSH密钥）
- 地域

---

## 第一步：配置防火墙（必须）

⚠️ **重要**：腾讯云轻量应用服务器默认自带防火墙，必须在控制台配置！

### 控制台配置步骤

1. **登录控制台**
   - 访问：https://console.cloud.tencent.com/lighthouse/instance
   - 选择您的服务器实例

2. **打开防火墙配置**
   - 点击服务器名称进入详情页
   - 点击顶部的"防火墙"选项卡
   - 点击"添加规则"按钮

3. **添加必需端口规则**

| 应用类型 | 协议 | 端口 | 策略 | 说明 |
|---------|------|------|------|------|
| HTTP | TCP | 80 | 允许 | API Gateway HTTP访问 |
| HTTPS | TCP | 443 | 允许 | API Gateway HTTPS访问 |
| 自定义 | TCP | 8080 | 允许 | API Gateway直接访问 |
| Linux登录 | TCP | 22 | 允许 | SSH远程管理 |

**操作示例**：

```
规则 1:
应用类型: HTTP
协议: TCP
端口: 80
策略: 允许
备注: API HTTP访问

规则 2:
应用类型: HTTPS
协议: TCP
端口: 443
策略: 允许
备注: API HTTPS访问

规则 3:
应用类型: 自定义
协议: TCP
端口: 8080
策略: 允许
备注: API Gateway

规则 4:
应用类型: Linux登录
协议: TCP
端口: 22
策略: 允许
备注: SSH管理
```

4. **保存配置**
   - 点击"确定"保存每条规则
   - 规则立即生效，无需重启服务器

### 安全建议

- ✅ 仅开放必需端口
- ✅ 数据库端口（3306, 27017, 6379）不要对外开放
- ✅ 微服务端口（8081-8083）不要对外开放
- ✅ 22端口可以限制为特定IP访问（推荐）

---

## 第二步：连接到服务器

### 方式1：使用腾讯云自带的登录功能（推荐）

1. 进入轻量应用服务器控制台
2. 找到您的服务器
3. 点击"登录"按钮
4. 选择"使用 WebShell 登录"
5. 输入root密码

### 方式2：使用SSH客户端

**Windows用户（使用PuTTY或Windows Terminal）**：
```powershell
ssh root@your-server-ip
# 输入密码
```

**Mac/Linux用户**：
```bash
ssh root@your-server-ip
# 输入密码
```

---

## 第三步：安装Docker环境

连接到服务器后，执行以下命令：

```bash
# 1. 更新系统
apt update && apt upgrade -y   # Ubuntu/Debian
# 或
yum update -y                   # CentOS

# 2. 安装Docker
curl -fsSL https://get.docker.com | bash

# 3. 启动Docker服务
systemctl start docker
systemctl enable docker

# 4. 验证Docker安装
docker --version

# 5. 安装Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 6. 验证Docker Compose安装
docker-compose --version
```

---

## 第四步：部署UPS系统

### 方法1：使用一键部署脚本（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/dctx479/UPS.git
cd UPS

# 2. 运行快速部署脚本
chmod +x quick-start.sh
./quick-start.sh

# 等待5-10分钟，所有服务会自动启动
```

### 方法2：手动部署

```bash
# 1. 克隆项目
git clone https://github.com/dctx479/UPS.git
cd UPS

# 2. 配置环境变量
cp .env.example .env

# 3. 生成安全密钥
cat > .env << 'EOF'
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

# 5. 查看服务状态
docker-compose ps

# 6. 查看启动日志
docker-compose logs -f
```

### 验证部署

```bash
# 1. 检查所有服务是否启动
docker-compose ps

# 应该看到所有服务状态为 "Up"

# 2. 测试本地访问
curl http://localhost:8080/actuator/health

# 应返回: {"status":"UP"}
```

---

## 第五步：配置公网访问

### 方法1：直接通过IP访问（简单快速）

部署完成后，可以直接通过公网IP访问：

```
http://your-server-ip:8080
```

**访问地址**：
- API文档：`http://your-server-ip:8080/swagger-ui.html`
- 健康检查：`http://your-server-ip:8080/actuator/health`
- Web界面：`http://your-server-ip:8000`（需要先启动前端）

### 方法2：使用域名+HTTPS（推荐）

#### 步骤1：配置域名

1. **购买域名**（可以在腾讯云购买）
2. **添加DNS解析**
   - 登录腾讯云 [DNS解析控制台](https://console.cloud.tencent.com/cns)
   - 添加A记录：
     ```
     主机记录: api (或 @)
     记录类型: A
     记录值: 你的服务器公网IP
     TTL: 600
     ```

3. **验证DNS解析**
   ```bash
   nslookup api.yourdomain.com
   ```

#### 步骤2：使用自动配置脚本

```bash
cd ~/UPS

# 运行公网访问配置脚本
chmod +x setup-public-access.sh
sudo ./setup-public-access.sh

# 按提示输入:
# 1) 使用域名 + HTTPS (推荐)
# 域名: api.yourdomain.com
# 邮箱: your@email.com
```

脚本会自动：
- ✅ 安装Nginx
- ✅ 配置反向代理
- ✅ 获取Let's Encrypt SSL证书
- ✅ 配置HTTPS
- ✅ 设置自动续期

完成后访问：
```
https://api.yourdomain.com
https://api.yourdomain.com/swagger-ui.html
```

---

## 第六步：配置Web界面（可选）

### 方法1：使用Nginx提供静态文件

```bash
# 1. 复制前端文件
sudo mkdir -p /var/www/html/ups
sudo cp -r ~/UPS/frontend/* /var/www/html/ups/

# 2. 创建Nginx配置
sudo nano /etc/nginx/sites-available/ups-frontend
```

添加内容：
```nginx
server {
    listen 80;
    server_name ups.yourdomain.com;

    root /var/www/html/ups;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }

    # API代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

```bash
# 3. 启用配置
sudo ln -s /etc/nginx/sites-available/ups-frontend /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# 4. 配置HTTPS
sudo certbot --nginx -d ups.yourdomain.com
```

访问：`https://ups.yourdomain.com`

### 方法2：使用简单HTTP服务器

```bash
cd ~/UPS/frontend
nohup python3 -m http.server 8000 > /tmp/frontend.log 2>&1 &
```

**记得在腾讯云防火墙开放8000端口！**

访问：`http://your-server-ip:8000`

---

## 第七步：测试访问

### 1. 基础测试

```bash
# 测试健康检查
curl http://your-server-ip:8080/actuator/health

# 测试HTTPS（如果配置）
curl https://api.yourdomain.com/actuator/health
```

### 2. API测试

```bash
# 下载测试脚本
cd ~/UPS
chmod +x test-api.sh

# 运行测试
./test-api.sh your-server-ip
```

### 3. 浏览器测试

在本地电脑浏览器访问：
- Swagger UI: `http://your-server-ip:8080/swagger-ui.html`
- 或: `https://api.yourdomain.com/swagger-ui.html`

测试功能：
1. 用户注册
2. 用户登录
3. 获取Token
4. 调用API

---

## 监控和维护

### 查看服务状态

```bash
# 查看Docker容器状态
docker-compose ps

# 查看资源使用情况
docker stats

# 查看服务日志
docker-compose logs -f gateway-service
docker-compose logs -f user-service
```

### 查看Nginx日志

```bash
# 访问日志
sudo tail -f /var/log/nginx/access.log

# 错误日志
sudo tail -f /var/log/nginx/error.log
```

### 查看SSL证书状态

```bash
sudo certbot certificates
```

---

## 常见问题排查

### 问题1：无法通过公网IP访问

**检查清单**：
1. ✅ 腾讯云防火墙是否开放8080端口
2. ✅ Docker服务是否运行：`docker-compose ps`
3. ✅ 端口是否监听：`netstat -tlnp | grep 8080`

**解决方法**：
```bash
# 重新配置防火墙
进入腾讯云控制台 → 防火墙 → 添加规则 → 端口8080 → 允许

# 重启服务
cd ~/UPS
docker-compose restart
```

### 问题2：HTTPS证书获取失败

**可能原因**：
- 域名未正确解析到服务器IP
- 防火墙未开放80/443端口
- DNS解析未生效（需等待10-30分钟）

**解决方法**：
```bash
# 验证域名解析
nslookup api.yourdomain.com

# 验证防火墙
确保腾讯云控制台已开放80和443端口

# 手动重试获取证书
sudo certbot --nginx -d api.yourdomain.com
```

### 问题3：服务启动失败

**检查日志**：
```bash
docker-compose logs gateway-service | grep -i error
docker-compose logs user-service | grep -i error
```

**常见原因**：
- JWT密钥未配置或长度不足
- 数据库连接失败
- 内存不足

**解决方法**：
```bash
# 检查.env文件
cat .env

# 重新生成配置
cd ~/UPS
./quick-start.sh
```

---

## 性能优化

### 1. Docker资源限制

编辑 `docker-compose.yml`：
```yaml
services:
  gateway-service:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
```

### 2. Nginx缓存配置

```nginx
# /etc/nginx/nginx.conf
http {
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=100m;
    proxy_cache_key "$scheme$request_method$host$request_uri";
}
```

### 3. 启用Gzip压缩

```nginx
# /etc/nginx/nginx.conf
http {
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript;
}
```

---

## 安全建议

### 1. 修改SSH端口（可选）

```bash
# 编辑SSH配置
sudo nano /etc/ssh/sshd_config

# 修改端口
Port 2222

# 重启SSH
sudo systemctl restart sshd
```

**记得在腾讯云防火墙开放新端口！**

### 2. 安装Fail2Ban

```bash
# 安装
sudo apt install fail2ban -y

# 启动
sudo systemctl start fail2ban
sudo systemctl enable fail2ban
```

### 3. 配置HTTPS强制跳转

Nginx会自动配置（如果使用certbot）

---

## 备份和恢复

### 备份数据库

```bash
# 备份MySQL
docker exec ups-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} userservice > backup_$(date +%Y%m%d).sql

# 备份MongoDB
docker exec ups-mongodb mongodump --uri="mongodb://admin:${MONGO_PASSWORD}@localhost:27017" --db=userprofile --out=/backup
```

### 恢复数据库

```bash
# 恢复MySQL
docker exec -i ups-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} userservice < backup_20250105.sql

# 恢复MongoDB
docker exec ups-mongodb mongorestore --uri="mongodb://admin:${MONGO_PASSWORD}@localhost:27017" --db=userprofile /backup/userprofile
```

---

## 升级系统

```bash
cd ~/UPS

# 拉取最新代码
git pull origin main

# 重新构建和启动
docker-compose down
docker-compose build
docker-compose up -d

# 查看状态
docker-compose ps
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

# 查看资源使用
docker stats

# 查看防火墙规则
进入腾讯云控制台查看
```

### 访问地址

| 服务 | 直接IP访问 | 域名访问（HTTPS） |
|------|-----------|-----------------|
| API Gateway | `http://IP:8080` | `https://api.domain.com` |
| Swagger UI | `http://IP:8080/swagger-ui.html` | `https://api.domain.com/swagger-ui.html` |
| Web界面 | `http://IP:8000` | `https://ups.domain.com` |
| Consul | `http://IP:8500` | 不建议公网访问 |

---

## 相关链接

- 腾讯云控制台：https://console.cloud.tencent.com/lighthouse
- 腾讯云文档：https://cloud.tencent.com/document/product/1207
- UPS项目地址：https://github.com/dctx479/UPS

---

## 技术支持

如有问题请联系：
- 📧 邮箱：b150w4942@163.com
- 📝 GitHub Issues：https://github.com/dctx479/UPS/issues

---

**更新时间**：2025-11-05
**适用版本**：UPS v1.0
