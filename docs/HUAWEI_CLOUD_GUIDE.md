# 华为云ECS部署指南

本指南专门针对华为云弹性云服务器(ECS)，提供详细的UPS系统部署和公网访问配置步骤。

---

## 前置准备

### 1. 购买弹性云服务器

**推荐配置**：
- 实例规格：s6.large.2（2核4GB）或更高
- 系统盘：40GB 高IO
- 公网带宽：5Mbps或以上
- 镜像：Ubuntu 22.04 Server 64bit 或 CentOS 7.9 64bit

### 2. 获取服务器信息

登录 [华为云ECS控制台](https://console.huaweicloud.com/ecm) 获取：
- 弹性公网IP
- 实例ID
- root密码（或密钥对）
- 所属区域

---

## 第一步：配置安全组（必须）

⚠️ **重要**：华为云ECS默认只开放22端口，必须配置安全组开放其他端口！

### 控制台配置步骤

1. **进入安全组配置**
   - 登录 [华为云控制台](https://console.huaweicloud.com)
   - 选择【服务列表】→【计算】→【弹性云服务器ECS】
   - 点击左侧【安全组】菜单

2. **找到您的安全组**
   - 找到ECS实例关联的安全组
   - 点击安全组名称进入详情页
   - 点击【入方向规则】标签页

3. **添加入方向规则**

点击【添加规则】，配置以下规则：

**规则1：HTTP访问**
```
协议端口: TCP:80
源地址: 0.0.0.0/0
描述: API HTTP访问
操作: 允许
```

**规则2：HTTPS访问**
```
协议端口: TCP:443
源地址: 0.0.0.0/0
描述: API HTTPS访问
操作: 允许
```

**规则3：API Gateway**
```
协议端口: TCP:8080
源地址: 0.0.0.0/0
描述: API Gateway直接访问
操作: 允许
```

**规则4：SSH访问**
```
协议端口: TCP:22
源地址: 你的IP/32 (或 0.0.0.0/0)
描述: SSH远程管理
操作: 允许
```

4. **保存规则**
   - 点击【确定】保存每条规则
   - 规则立即生效，无需重启

### 快速配置（使用系统模板）

1. 在安全组规则页面，点击【快速添加规则】
2. 选择系统模板：
   - ✅ HTTP(80)
   - ✅ HTTPS(443)
   - ✅ SSH(22)
3. 手动添加自定义规则：8080端口

### 安全建议

✅ **推荐配置**：
- HTTP、HTTPS、8080端口对所有IP开放
- SSH端口限制为特定IP访问（推荐）
- 不要开放数据库端口：3306, 27017, 6379
- 不要开放微服务内部端口：8081-8083

---

## 第二步：连接到服务器

### 方式1：使用华为云远程登录（推荐）

1. 进入ECS控制台
2. 找到您的实例
3. 点击【远程登录】
4. 选择【CloudShell登录】或【VNC登录】
5. 输入root密码

### 方式2：使用SSH客户端

**Windows用户**：
```powershell
ssh root@your-server-ip
# 输入密码
```

**Mac/Linux用户**：
```bash
ssh root@your-server-ip
# 输入密码
```

**使用密钥对登录**：
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

# 2. 安装Docker
curl -fsSL https://get.docker.com | bash

# 3. 配置Docker镜像加速（使用华为云镜像）
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

### 方法1：直接通过弹性公网IP访问

部署完成后，可以直接通过弹性公网IP访问：

```
http://your-server-ip:8080
```

**访问地址**：
- API文档：`http://your-server-ip:8080/swagger-ui.html`
- 健康检查：`http://your-server-ip:8080/actuator/health`

⚠️ **注意**：确保安全组已开放8080端口！

### 方法2：使用域名+HTTPS（生产环境推荐）

#### 步骤1：配置域名

1. **购买域名**（可在华为云购买）

2. **配置DNS解析**
   - 登录 [华为云DNS控制台](https://console.huaweicloud.com/dns)
   - 选择您的域名
   - 点击【添加记录集】
   - 配置A记录：
     ```
     主机记录: api (或 @)
     类型: A-将域名指向IPv4地址
     线路类型: 全网默认
     TTL: 5分钟
     值: 您的ECS弹性公网IP
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

脚本会自动完成：
- ✅ 安装Nginx
- ✅ 配置反向代理
- ✅ 获取Let's Encrypt SSL证书
- ✅ 配置HTTPS
- ✅ 设置证书自动续期

完成后访问：
```
https://api.yourdomain.com
https://api.yourdomain.com/swagger-ui.html
```

---

## 第六步：配置Web界面（可选）

### 使用Nginx托管静态文件

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
```

### 华为云监控

1. 登录ECS控制台
2. 点击实例名称进入详情
3. 查看【监控】标签页
4. 查看CPU、内存、网络等指标

### 配置告警

1. 进入 [云监控服务控制台](https://console.huaweicloud.com/ces)
2. 配置告警规则：
   - CPU使用率 > 80%
   - 内存使用率 > 80%
   - 磁盘使用率 > 80%

---

## 常见问题排查

### 问题1：无法通过公网IP访问

**检查清单**：
1. ✅ 安全组是否开放对应端口
2. ✅ 弹性公网IP是否已绑定
3. ✅ Docker服务是否运行
4. ✅ 端口是否被占用

**排查步骤**：
```bash
# 1. 检查弹性公网IP
华为云控制台 → 弹性公网IP → 查看绑定状态

# 2. 检查安全组
华为云控制台 → 安全组 → 入方向规则 → 检查8080端口

# 3. 检查Docker服务
docker-compose ps

# 4. 检查端口监听
netstat -tlnp | grep 8080

# 5. 测试本地访问
curl http://localhost:8080/actuator/health
```

**解决方法**：
```bash
# 添加安全组规则
华为云控制台 → 安全组 → 入方向规则 → 添加规则 → TCP:8080

# 重启服务
cd ~/UPS
docker-compose restart
```

### 问题2：HTTPS证书获取失败

**可能原因**：
- 域名未正确解析
- 安全组未开放80/443端口
- DNS解析未生效

**解决方法**：
```bash
# 1. 验证域名解析
nslookup api.yourdomain.com

# 2. 检查安全组
确保80和443端口已开放

# 3. 手动重试
sudo certbot --nginx -d api.yourdomain.com --force-renew
```

---

## 性能优化

### 1. 配置华为云内网DNS

```bash
# /etc/resolv.conf
nameserver 100.125.1.250
nameserver 100.125.21.250
```

### 2. Nginx性能优化

```nginx
# /etc/nginx/nginx.conf
worker_processes auto;

events {
    worker_connections 4096;
}

http {
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    keepalive_timeout 65;
}
```

---

## 安全加固

### 1. 限制SSH访问IP

```
华为云控制台 → 安全组 → 入方向规则 →
修改SSH规则 → 源地址改为你的IP/32
```

### 2. 安装Fail2Ban

```bash
sudo apt install fail2ban -y
sudo systemctl start fail2ban
sudo systemctl enable fail2ban
```

---

## 备份和恢复

### 创建云服务器备份

1. 登录ECS控制台
2. 选择您的实例
3. 点击【更多】→【创建镜像】
4. 输入镜像名称，点击【立即创建】

建议：定期创建系统镜像备份

---

## 快速参考

### 常用命令

```bash
# 查看服务状态
docker-compose ps

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f
```

### 访问地址

| 服务 | 公网IP访问 | 域名访问（HTTPS） |
|------|-----------|-----------------|
| API Gateway | `http://IP:8080` | `https://api.domain.com` |
| Swagger UI | `http://IP:8080/swagger-ui.html` | `https://api.domain.com/swagger-ui.html` |

### 华为云控制台链接

- ECS控制台：https://console.huaweicloud.com/ecm
- 安全组配置：https://console.huaweicloud.com/vpc/#/secgroups
- DNS解析：https://console.huaweicloud.com/dns
- 云监控：https://console.huaweicloud.com/ces

---

## 相关资源

- 华为云ECS文档：https://support.huaweicloud.com/ecs/
- 华为云安全组配置：https://support.huaweicloud.com/usermanual-ecs/zh-cn_topic_0030878383.html
- UPS项目地址：https://github.com/dctx479/UPS

---

## 技术支持

如有问题请联系：
- 📧 邮箱：b150w4942@163.com
- 📝 GitHub Issues：https://github.com/dctx479/UPS/issues

---

**更新时间**：2025-11-05
**适用版本**：UPS v1.0
**适用区域**：华为云所有区域
