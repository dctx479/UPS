# 阿里云ECS部署指南

本指南专门针对阿里云ECS云服务器，提供详细的UPS系统部署和公网访问配置步骤。

---

## 前置准备

### 1. 购买ECS云服务器

**推荐配置**：
- 实例规格：ecs.t6-c2m4（2核4GB）或更高
- 系统盘：40GB ESSD云盘
- 公网带宽：5Mbps或以上（按使用流量计费）
- 镜像：Ubuntu 22.04 64位或CentOS 7.9 64位

### 2. 获取服务器信息

登录 [阿里云ECS控制台](https://ecs.console.aliyun.com) 获取：
- 公网IP地址
- 实例ID
- root密码（或SSH密钥）
- 所属地域和可用区

---

## 第一步：配置安全组（必须）

⚠️ **重要**：阿里云ECS默认所有入网端口关闭，必须配置安全组规则！

### 控制台配置步骤

#### 方法1：通过实例配置（推荐）

1. **进入ECS控制台**
   - 访问：https://ecs.console.aliyun.com
   - 选择您的地域

2. **找到实例**
   - 点击左侧菜单【实例与镜像】→【实例】
   - 找到您的ECS实例，点击实例ID

3. **进入安全组配置**
   - 在实例详情页，点击【安全组】标签页
   - 点击安全组ID进入安全组详情
   - 点击【配置规则】→【手动添加】

4. **添加入方向规则**

点击【手动添加】，配置以下规则：

**规则1：HTTP访问**
```
授权策略: 允许
优先级: 1
协议类型: TCP
端口范围: 80/80
授权对象: 0.0.0.0/0
描述: API HTTP访问
```

**规则2：HTTPS访问**
```
授权策略: 允许
优先级: 1
协议类型: TCP
端口范围: 443/443
授权对象: 0.0.0.0/0
描述: API HTTPS访问
```

**规则3：API Gateway**
```
授权策略: 允许
优先级: 1
协议类型: TCP
端口范围: 8080/8080
授权对象: 0.0.0.0/0
描述: API Gateway直接访问
```

**规则4：SSH访问**
```
授权策略: 允许
优先级: 1
协议类型: TCP
端口范围: 22/22
授权对象: 你的IP/32 (或 0.0.0.0/0)
描述: SSH远程管理
```

5. **保存规则**
   - 点击【保存】
   - 规则立即生效，无需重启

#### 方法2：快速添加（简化操作）

1. 进入安全组配置页面
2. 点击【快速添加】
3. 选择以下预设规则：
   - ✅ HTTP(80)
   - ✅ HTTPS(443)
   - ✅ SSH(22)
4. 手动添加自定义规则：8080端口

### 安全建议

✅ **推荐配置**：
- HTTP(80)、HTTPS(443)、8080端口对所有IP开放
- SSH(22)端口限制为特定IP访问
- 不要开放数据库端口：3306, 27017, 6379
- 不要开放微服务端口：8081, 8082, 8083

❌ **禁止配置**：
- 不要使用 0.0.0.0/0 开放所有端口
- 不要开放数据库端口到公网
- 不要禁用防火墙

---

## 第二步：连接到服务器

### 方式1：使用阿里云工作台（推荐）

1. 进入ECS控制台
2. 找到您的实例
3. 点击【远程连接】→【通过Workbench远程连接】
4. 选择认证方式：
   - 密码认证：输入root密码
   - 密钥对认证：选择已绑定的密钥

### 方式2：使用SSH客户端

**Windows用户**：
```powershell
# 使用PowerShell或Windows Terminal
ssh root@your-server-ip

# 首次连接会提示接受指纹，输入yes
# 输入root密码
```

**Mac/Linux用户**：
```bash
ssh root@your-server-ip
# 输入密码
```

**使用密钥登录**：
```bash
ssh -i /path/to/your-key.pem root@your-server-ip
```

---

## 第三步：安装Docker环境

连接到服务器后，执行以下命令：

```bash
# 1. 更新系统
apt update && apt upgrade -y   # Ubuntu/Debian
# 或
yum update -y                   # CentOS

# 2. 安装Docker（使用阿里云镜像加速）
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun

# 3. 配置Docker镜像加速器
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://registry.docker-cn.com"
  ]
}
EOF

# 4. 启动Docker服务
sudo systemctl daemon-reload
sudo systemctl restart docker
sudo systemctl enable docker

# 5. 验证安装
docker --version
docker info

# 6. 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 7. 验证Docker Compose
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

### 方法1：直接通过公网IP访问（快速测试）

部署完成后，可以直接通过公网IP访问：

```
http://your-server-ip:8080
```

**访问地址**：
- API文档：`http://your-server-ip:8080/swagger-ui.html`
- 健康检查：`http://your-server-ip:8080/actuator/health`

⚠️ **注意**：确保安全组已开放8080端口！

### 方法2：使用域名+HTTPS（生产环境推荐）

#### 步骤1：准备域名

1. **购买域名**
   - 可在阿里云万网购买：https://wanwang.aliyun.com
   - 或使用已有域名

2. **配置DNS解析**
   - 登录 [阿里云DNS控制台](https://dns.console.aliyun.com)
   - 选择您的域名，点击【解析设置】
   - 添加记录：
     ```
     记录类型: A
     主机记录: api (或 @)
     记录值: 您的ECS公网IP
     TTL: 10分钟
     ```

3. **验证DNS解析**
   ```bash
   # 等待5-10分钟后验证
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

脚本会自动完成：
- ✅ 安装Nginx
- ✅ 配置反向代理
- ✅ 获取Let's Encrypt SSL证书
- ✅ 配置HTTPS重定向
- ✅ 设置证书自动续期

完成后访问：
```
https://api.yourdomain.com
https://api.yourdomain.com/swagger-ui.html
```

---

## 第六步：配置Web界面（可选）

### 方法1：使用Nginx托管静态文件

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

    # 修改API服务器地址
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
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

### 方法2：使用Python简单服务器

```bash
cd ~/UPS/frontend

# 后台运行
nohup python3 -m http.server 8000 > /tmp/frontend.log 2>&1 &
```

**记得在安全组开放8000端口！**

访问：`http://your-server-ip:8000`

---

## 第七步：测试和验证

### 1. 基础连通性测试

```bash
# 从服务器本地测试
curl http://localhost:8080/actuator/health

# 从本地电脑测试公网访问
curl http://your-server-ip:8080/actuator/health

# 测试HTTPS（如果配置）
curl https://api.yourdomain.com/actuator/health
```

### 2. API功能测试

```bash
cd ~/UPS
chmod +x test-api.sh
./test-api.sh your-server-ip
```

### 3. 浏览器测试

在本地电脑浏览器访问：
- Swagger UI: `http://your-server-ip:8080/swagger-ui.html`
- 或: `https://api.yourdomain.com/swagger-ui.html`

测试功能：
1. ✅ 用户注册
2. ✅ 用户登录
3. ✅ 获取Token
4. ✅ 调用API接口

---

## 监控和维护

### 查看服务状态

```bash
# Docker容器状态
docker-compose ps

# 资源使用情况
docker stats

# 服务日志
docker-compose logs -f gateway-service
docker-compose logs -f user-service
docker-compose logs -f profile-service
```

### 阿里云监控

1. 登录ECS控制台
2. 点击实例ID进入详情
3. 查看【监控】标签页
4. 查看CPU、内存、网络等指标

### 配置告警

1. 进入 [云监控控制台](https://cloudmonitor.console.aliyun.com)
2. 配置告警规则：
   - CPU使用率 > 80%
   - 内存使用率 > 80%
   - 磁盘使用率 > 80%
   - 网络带宽使用率 > 80%

---

## 常见问题排查

### 问题1：无法通过公网IP访问

**检查清单**：
1. ✅ 安全组是否开放对应端口
2. ✅ Docker服务是否运行
3. ✅ 端口是否被占用
4. ✅ 服务器防火墙是否开放

**排查步骤**：
```bash
# 1. 检查安全组配置
登录ECS控制台 → 安全组 → 配置规则 → 检查入方向规则

# 2. 检查Docker服务
docker-compose ps

# 3. 检查端口监听
netstat -tlnp | grep 8080

# 4. 检查服务器防火墙（Ubuntu）
sudo ufw status

# 5. 测试本地访问
curl http://localhost:8080/actuator/health
```

**解决方法**：
```bash
# 添加安全组规则
阿里云控制台 → ECS → 安全组 → 配置规则 → 添加8080端口

# 重启服务
cd ~/UPS
docker-compose restart
```

### 问题2：HTTPS证书获取失败

**可能原因**：
- 域名未正确解析到ECS公网IP
- 安全组未开放80/443端口
- DNS解析未生效（需等待10-30分钟）
- Nginx配置错误

**解决方法**：
```bash
# 1. 验证域名解析
nslookup api.yourdomain.com
dig api.yourdomain.com

# 2. 检查安全组
确保80和443端口已开放

# 3. 检查Nginx配置
sudo nginx -t

# 4. 查看Certbot日志
sudo cat /var/log/letsencrypt/letsencrypt.log

# 5. 手动重试
sudo certbot --nginx -d api.yourdomain.com --force-renew
```

### 问题3：容器启动失败

**检查日志**：
```bash
docker-compose logs gateway-service | grep -i error
docker-compose logs user-service | grep -i error
```

**常见原因**：
- JWT密钥未配置
- 内存不足
- 端口被占用
- 数据库连接失败

**解决方法**：
```bash
# 检查配置文件
cat .env

# 检查系统资源
free -h
df -h

# 重新部署
cd ~/UPS
docker-compose down
./quick-start.sh
```

### 问题4：访问速度慢

**优化建议**：

1. **配置阿里云内网DNS**
```bash
# /etc/resolv.conf
nameserver 100.100.2.136
nameserver 100.100.2.138
```

2. **启用Nginx缓存**
```nginx
# /etc/nginx/nginx.conf
http {
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=100m;
}
```

3. **升级带宽**
   - 登录ECS控制台
   - 升级公网带宽到10Mbps或更高

---

## 性能优化

### 1. Docker镜像加速

```bash
# 配置阿里云镜像加速器
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://registry.docker-cn.com"
  ]
}
EOF

sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 2. Nginx性能优化

```nginx
# /etc/nginx/nginx.conf
worker_processes auto;
worker_rlimit_nofile 65535;

events {
    worker_connections 4096;
    use epoll;
}

http {
    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript;

    # 连接保持
    keepalive_timeout 65;
    keepalive_requests 100;
}
```

### 3. 数据库优化

```bash
# 调整Docker容器资源限制
# docker-compose.yml
services:
  mysql:
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
```

---

## 安全加固

### 1. 配置安全组规则最佳实践

```
入方向规则：
✅ HTTP(80) - 0.0.0.0/0
✅ HTTPS(443) - 0.0.0.0/0
✅ SSH(22) - 你的IP/32 （限制IP访问）
❌ 禁止开放 3306, 27017, 6379 等数据库端口
❌ 禁止开放 8081, 8082, 8083 等内部服务端口
```

### 2. 修改SSH默认端口

```bash
# 编辑SSH配置
sudo nano /etc/ssh/sshd_config

# 修改端口
Port 2222

# 重启SSH
sudo systemctl restart sshd
```

**记得在安全组开放新端口，关闭22端口！**

### 3. 安装Fail2Ban

```bash
# 安装
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
```

```bash
# 启动
sudo systemctl start fail2ban
sudo systemctl enable fail2ban
```

### 4. 启用阿里云安全中心

1. 登录 [安全中心控制台](https://yundun.console.aliyun.com)
2. 开启基础版（免费）
3. 配置漏洞扫描
4. 配置安全告警

---

## 备份和恢复

### 1. 创建ECS快照

1. 登录ECS控制台
2. 点击实例ID进入详情
3. 点击【快照】→【创建快照】
4. 输入快照名称，点击【确定】

建议：
- 每天自动创建快照
- 保留最近7天的快照

### 2. 备份数据库

```bash
# 备份MySQL
docker exec ups-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} userservice > backup_$(date +%Y%m%d).sql

# 上传到OSS（可选）
ossutil cp backup_$(date +%Y%m%d).sql oss://your-bucket/backups/
```

---

## 成本优化

### 1. 使用按量付费

- 开发测试环境使用按量付费
- 晚上和周末关闭实例
- 每月可节省60-80%费用

### 2. 购买预留实例券

- 生产环境使用预留实例券
- 1年期可节省30%
- 3年期可节省50%

### 3. 合理配置带宽

- 按使用流量计费（适合访问量小）
- 按固定带宽计费（适合访问量大）

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
```

### 访问地址

| 服务 | 公网IP访问 | 域名访问（HTTPS） |
|------|-----------|-----------------|
| API Gateway | `http://IP:8080` | `https://api.domain.com` |
| Swagger UI | `http://IP:8080/swagger-ui.html` | `https://api.domain.com/swagger-ui.html` |
| Web界面 | `http://IP:8000` | `https://ups.domain.com` |

### 阿里云控制台链接

- ECS控制台：https://ecs.console.aliyun.com
- 安全组配置：https://ecs.console.aliyun.com/securityGroup
- DNS解析：https://dns.console.aliyun.com
- 云监控：https://cloudmonitor.console.aliyun.com

---

## 相关资源

- 阿里云ECS文档：https://help.aliyun.com/product/25365.html
- 阿里云安全组配置：https://help.aliyun.com/document_detail/25471.html
- UPS项目地址：https://github.com/dctx479/UPS

---

## 技术支持

如有问题请联系：
- 📧 邮箱：b150w4942@163.com
- 📝 GitHub Issues：https://github.com/dctx479/UPS/issues

---

**更新时间**：2025-11-05
**适用版本**：UPS v1.0
**适用地域**：阿里云所有地域
