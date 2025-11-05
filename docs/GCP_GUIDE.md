# Google Cloud Platform (GCP) Compute Engine部署指南

本指南专门针对Google Cloud Platform Compute Engine虚拟机，提供详细的UPS系统部署和公网访问配置步骤。

---

## 前置准备

### 1. 创建GCP账户

- 访问 [Google Cloud](https://cloud.google.com)
- 注册账户（新用户可获得$300免费额度，有效期90天）
- 完成身份验证和信用卡绑定

### 2. 创建项目

1. 登录 [Google Cloud Console](https://console.cloud.google.com)
2. 点击顶部项目选择器
3. 点击【新建项目】
4. 项目名称：UPS-Project
5. 组织：选择您的组织（可选）
6. 点击【创建】

### 3. 选择GCP区域

推荐区域（延迟较低）：
- **台湾**: asia-east1
- **香港**: asia-east2
- **东京**: asia-northeast1
- **首尔**: asia-northeast3
- **新加坡**: asia-southeast1
- **美国西部**: us-west1

---

## 第一步：创建Compute Engine实例

### 方法1：使用Google Cloud Console（图形界面）

#### 1. 进入Compute Engine

1. 打开 [Google Cloud Console](https://console.cloud.google.com)
2. 左侧菜单 → Compute Engine → VM实例
3. 首次使用会提示启用Compute Engine API（点击启用）
4. 点击【创建实例】

#### 2. 配置实例

**名称和区域**：
```
名称: ups-server
区域: asia-east1 (台湾) 或其他首选区域
地区: asia-east1-b (或任意可用区)
```

**机器配置**：

**系列选择**：
```
通用型: E2, N2, N2D, N1
计算优化型: C2, C2D
内存优化型: M1, M2
```

**机器类型**：
```
开发测试环境:
- e2-medium (2 vCPU, 4 GB 内存) - 推荐
- e2-small (2 vCPU, 2 GB 内存) - 最小配置

生产环境:
- n2-standard-2 (2 vCPU, 8 GB 内存)
- n2-standard-4 (4 vCPU, 16 GB 内存)
```

**启动磁盘**：

点击【更改】配置启动磁盘：
```
公共映像:
  操作系统: Ubuntu
  版本: Ubuntu 22.04 LTS (x86/64)

启动磁盘类型:
  开发测试: 标准永久性磁盘
  生产环境: SSD 永久性磁盘 (推荐)

大小(GB):
  最小: 30 GB
  推荐: 40-50 GB
```

点击【选择】

**身份和API访问**：
```
服务账号: Compute Engine default service account
访问权限范围: 允许默认访问权限
```

**防火墙**：
```
✅ 允许HTTP流量
✅ 允许HTTPS流量
```

⚠️ **注意**：这只开放了80和443端口，8080端口需要稍后配置防火墙规则！

**高级选项**（展开）：

**管理 → 元数据**（可选，自动化安装）：

添加启动脚本：
```bash
#!/bin/bash

# 更新系统
apt update -y
apt upgrade -y

# 安装基础工具
apt install -y git curl wget vim htop

# 安装Docker
curl -fsSL https://get.docker.com | bash
systemctl start docker
systemctl enable docker

# 安装Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 配置Docker镜像加速
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://registry.docker-cn.com"
  ]
}
EOF

systemctl daemon-reload
systemctl restart docker
```

**网络 → 网络标记**：
```
网络标记: ups-server
(用于防火墙规则)
```

#### 3. 创建实例

1. 查看右侧定价预估
2. 点击【创建】
3. 等待实例创建（约30-60秒）

### 方法2：使用gcloud命令行（推荐自动化）

```bash
# 安装gcloud CLI (如果未安装)
# https://cloud.google.com/sdk/docs/install

# 初始化配置
gcloud init

# 设置项目
gcloud config set project UPS-PROJECT-ID

# 创建实例
gcloud compute instances create ups-server \
  --zone=asia-east1-b \
  --machine-type=e2-medium \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=40GB \
  --boot-disk-type=pd-standard \
  --tags=ups-server,http-server,https-server \
  --metadata=startup-script='#!/bin/bash
apt update -y && apt upgrade -y
apt install -y git curl wget vim
curl -fsSL https://get.docker.com | bash
systemctl start docker && systemctl enable docker
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose'
```

---

## 第二步：配置防火墙规则

GCP使用VPC防火墙规则控制网络访问。

### 方法1：使用Console配置

1. **进入防火墙规则**
   - 左侧菜单 → VPC网络 → 防火墙
   - 或直接访问：https://console.cloud.google.com/networking/firewalls

2. **创建防火墙规则**

点击【创建防火墙规则】

**允许SSH (22端口)**：
```
名称: allow-ssh
日志: 关闭
网络: default
优先级: 1000
流量方向: 入站
对匹配项执行的操作: 允许
目标: 指定的目标标记
目标标记: ups-server
来源过滤条件: IPv4范围
来源IPv4范围: 0.0.0.0/0 (或您的IP/32，更安全)
协议和端口:
  指定的协议和端口
  ✅ TCP: 22
```

**允许HTTP (80端口)**：
```
名称: allow-http
网络: default
优先级: 1000
流量方向: 入站
对匹配项执行的操作: 允许
目标: 指定的目标标记
目标标记: http-server
来源IPv4范围: 0.0.0.0/0
协议和端口:
  ✅ TCP: 80
```

**允许HTTPS (443端口)**：
```
名称: allow-https
网络: default
优先级: 1000
流量方向: 入站
对匹配项执行的操作: 允许
目标: 指定的目标标记
目标标记: https-server
来源IPv4范围: 0.0.0.0/0
协议和端口:
  ✅ TCP: 443
```

**允许API Gateway (8080端口)**：
```
名称: allow-api-gateway
日志: 关闭
网络: default
优先级: 1000
流量方向: 入站
对匹配项执行的操作: 允许
目标: 指定的目标标记
目标标记: ups-server
来源过滤条件: IPv4范围
来源IPv4范围: 0.0.0.0/0
协议和端口:
  指定的协议和端口
  ✅ TCP: 8080
```

点击【创建】

### 方法2：使用gcloud命令

```bash
# 允许SSH
gcloud compute firewall-rules create allow-ssh \
  --network=default \
  --allow=tcp:22 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=ups-server

# 允许HTTP
gcloud compute firewall-rules create allow-http \
  --network=default \
  --allow=tcp:80 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=http-server

# 允许HTTPS
gcloud compute firewall-rules create allow-https \
  --network=default \
  --allow=tcp:443 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=https-server

# 允许API Gateway
gcloud compute firewall-rules create allow-api-gateway \
  --network=default \
  --allow=tcp:8080 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=ups-server
```

### 必需的防火墙规则汇总

| 规则名称 | 目标标记 | 协议 | 端口 | 来源 | 说明 |
|---------|---------|------|------|------|-----|
| allow-ssh | ups-server | TCP | 22 | 你的IP/32 | SSH管理 |
| allow-http | http-server | TCP | 80 | 0.0.0.0/0 | HTTP访问 |
| allow-https | https-server | TCP | 443 | 0.0.0.0/0 | HTTPS访问 |
| allow-api-gateway | ups-server | TCP | 8080 | 0.0.0.0/0 | API Gateway |

❌ **禁止开放的端口**：
- 3306 (MySQL)
- 27017 (MongoDB)
- 6379 (Redis)
- 8081-8083 (微服务内部端口)

### 安全最佳实践

✅ **推荐配置**：
```
1. SSH仅对特定IP开放
2. 使用SSH密钥认证
3. 启用VPC Flow Logs
4. 使用Cloud Armor防DDoS
5. 定期审查防火墙规则
```

---

## 第三步：连接到实例

### 获取外部IP

```bash
# 使用gcloud
gcloud compute instances describe ups-server \
  --zone=asia-east1-b \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

或在Console查看：Compute Engine → VM实例 → 外部IP列

### 方法1：使用浏览器SSH（最简单）

1. Compute Engine → VM实例
2. 找到ups-server
3. 点击【SSH】按钮
4. 浏览器会打开SSH终端

### 方法2：使用gcloud SSH

```bash
# 自动配置SSH密钥并连接
gcloud compute ssh ups-server --zone=asia-east1-b
```

### 方法3：使用传统SSH

**生成SSH密钥对**：
```bash
# 在本地电脑执行
ssh-keygen -t rsa -f ~/.ssh/gcp-ups-key -C "your-username"
```

**添加公钥到GCP**：
```bash
# 方法1: 使用gcloud
gcloud compute instances add-metadata ups-server \
  --zone=asia-east1-b \
  --metadata-from-file ssh-keys=~/.ssh/gcp-ups-key.pub

# 方法2: 在Console添加
# Compute Engine → 元数据 → SSH密钥 → 添加SSH密钥
```

**SSH连接**：
```bash
# Linux/Mac
ssh -i ~/.ssh/gcp-ups-key your-username@EXTERNAL_IP

# Windows PowerShell
ssh -i C:\path\to\gcp-ups-key your-username@EXTERNAL_IP
```

### 方法4：使用Cloud Shell

1. 点击Console右上角Cloud Shell图标
2. 执行：
```bash
gcloud compute ssh ups-server --zone=asia-east1-b
```

---

## 第四步：安装Docker环境

连接到实例后，执行以下命令：

```bash
# 1. 更新系统
sudo apt update && sudo apt upgrade -y

# 2. 安装Docker
curl -fsSL https://get.docker.com | bash

# 3. 将当前用户添加到docker组
sudo usermod -aG docker $USER

# 4. 启动Docker服务
sudo systemctl start docker
sudo systemctl enable docker

# 5. 验证Docker安装
docker --version
docker info

# 6. 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 7. 验证Docker Compose
docker-compose --version

# 8. 配置Docker镜像加速
sudo mkdir -p /etc/docker
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

# 9. 重新登录以应用docker组权限
exit
# 然后重新连接
```

---

## 第五步：部署UPS系统

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

# 3. 获取外部IP
curl -H "Metadata-Flavor: Google" http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip

# 4. 测试公网访问
curl http://EXTERNAL_IP:8080/actuator/health
```

---

## 第六步：配置公网访问

### 方法1：直接通过外部IP访问

获取外部IP：
```bash
gcloud compute instances describe ups-server \
  --zone=asia-east1-b \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

直接访问：
```
http://EXTERNAL_IP:8080
```

**访问地址**：
- API文档：`http://EXTERNAL_IP:8080/swagger-ui.html`
- 健康检查：`http://EXTERNAL_IP:8080/actuator/health`

⚠️ **确保防火墙规则已开放8080端口！**

### 方法2：使用域名+HTTPS（生产环境推荐）

#### 步骤1：配置静态外部IP

**为什么需要静态IP？**
- VM重启后临时外部IP会变化
- 静态IP保持不变，便于DNS配置

**配置步骤**：

```bash
# 1. 创建静态IP
gcloud compute addresses create ups-static-ip \
  --region=asia-east1

# 2. 查看静态IP地址
gcloud compute addresses describe ups-static-ip \
  --region=asia-east1 \
  --format='get(address)'

# 3. 将静态IP分配给VM
gcloud compute instances delete-access-config ups-server \
  --zone=asia-east1-b \
  --access-config-name="External NAT"

gcloud compute instances add-access-config ups-server \
  --zone=asia-east1-b \
  --access-config-name="External NAT" \
  --address=STATIC_IP_ADDRESS
```

**或使用Console**：
1. VPC网络 → IP地址 → 保留外部静态地址
2. 名称：ups-static-ip
3. 区域：asia-east1
4. 附加到：ups-server
5. 点击【保留】

#### 步骤2：配置DNS

**使用Cloud DNS**：

1. **创建DNS区域**
```bash
gcloud dns managed-zones create ups-zone \
  --dns-name="yourdomain.com." \
  --description="UPS DNS Zone"
```

或在Console：
- 网络服务 → Cloud DNS → 创建区域
- 区域类型：公共
- 区域名称：ups-zone
- DNS名称：yourdomain.com
- 点击【创建】

2. **添加A记录**
```bash
gcloud dns record-sets create api.yourdomain.com. \
  --zone=ups-zone \
  --type=A \
  --ttl=300 \
  --rrdatas=YOUR_STATIC_IP
```

或在Console：
- 选择DNS区域
- 点击【添加记录集】
- DNS名称：api
- 资源记录类型：A
- IPv4地址：您的静态IP
- 点击【创建】

3. **更新域名服务器**
```bash
# 查看Cloud DNS的名称服务器
gcloud dns managed-zones describe ups-zone
```
复制名称服务器，到域名注册商处更新NS记录。

**或使用第三方DNS**：
```
记录类型: A
主机记录: api
记录值: 您的GCP静态IP
TTL: 300-600秒
```

#### 步骤3：验证DNS解析

```bash
# 等待5-30分钟后验证
nslookup api.yourdomain.com
dig api.yourdomain.com

# 应该返回您的静态IP
```

#### 步骤4：使用自动配置脚本

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

## 第七步：配置负载均衡（高可用，可选）

### 使用Cloud Load Balancing

#### 1. 创建实例组

```bash
# 创建实例模板
gcloud compute instance-templates create ups-template \
  --machine-type=e2-medium \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=40GB \
  --tags=ups-server,http-server,https-server

# 创建托管实例组
gcloud compute instance-groups managed create ups-group \
  --base-instance-name=ups \
  --template=ups-template \
  --size=2 \
  --zone=asia-east1-b
```

#### 2. 配置健康检查

```bash
gcloud compute health-checks create http ups-health-check \
  --port=8080 \
  --request-path=/actuator/health \
  --check-interval=30s \
  --timeout=10s \
  --unhealthy-threshold=3 \
  --healthy-threshold=2
```

#### 3. 创建后端服务

```bash
gcloud compute backend-services create ups-backend \
  --protocol=HTTP \
  --health-checks=ups-health-check \
  --global

gcloud compute backend-services add-backend ups-backend \
  --instance-group=ups-group \
  --instance-group-zone=asia-east1-b \
  --global
```

#### 4. 创建URL映射和代理

```bash
# URL映射
gcloud compute url-maps create ups-url-map \
  --default-service=ups-backend

# HTTP代理
gcloud compute target-http-proxies create ups-http-proxy \
  --url-map=ups-url-map

# 转发规则
gcloud compute forwarding-rules create ups-http-rule \
  --global \
  --target-http-proxy=ups-http-proxy \
  --ports=80
```

#### 5. 配置HTTPS（可选）

```bash
# 创建SSL证书
gcloud compute ssl-certificates create ups-ssl-cert \
  --domains=api.yourdomain.com

# HTTPS代理
gcloud compute target-https-proxies create ups-https-proxy \
  --url-map=ups-url-map \
  --ssl-certificates=ups-ssl-cert

# HTTPS转发规则
gcloud compute forwarding-rules create ups-https-rule \
  --global \
  --target-https-proxy=ups-https-proxy \
  --ports=443
```

---

## 监控和维护

### 1. Cloud Monitoring

**查看VM指标**：
1. Compute Engine → VM实例 → 点击实例名
2. 点击【监控】标签
3. 查看：
   - CPU使用率
   - 磁盘读写
   - 网络流量

**创建告警策略**：
```bash
# 使用gcloud创建告警
gcloud alpha monitoring policies create \
  --notification-channels=CHANNEL_ID \
  --display-name="High CPU Alert" \
  --condition-display-name="CPU > 80%" \
  --condition-threshold-value=0.8 \
  --condition-threshold-duration=300s
```

或在Console：
- 监控 → 告警 → 创建政策
- 添加条件：VM实例 → CPU使用率 > 80%
- 配置通知渠道

### 2. Cloud Logging

**查看系统日志**：
```bash
# 使用gcloud
gcloud logging read "resource.type=gce_instance AND resource.labels.instance_id=INSTANCE_ID" \
  --limit=50 \
  --format=json
```

或在Console：
- 日志记录 → 日志浏览器
- 选择资源：VM实例 → ups-server

**Docker日志**：
```bash
# 实时查看
docker-compose logs -f

# 查看特定服务
docker-compose logs -f gateway-service

# 导出到Cloud Logging
docker-compose logs | gcloud logging write ups-logs --severity=INFO
```

### 3. 系统维护

```bash
# 查看系统资源
htop
df -h
free -h

# 清理Docker资源
docker system prune -a

# 更新系统
sudo apt update && sudo apt upgrade -y
```

---

## 常见问题排查

### 问题1：无法通过外部IP访问

**检查清单**：
1. ✅ 防火墙规则是否正确配置
2. ✅ VM是否有外部IP
3. ✅ Docker服务是否运行
4. ✅ 网络标记是否正确

**排查步骤**：
```bash
# 1. 检查外部IP
gcloud compute instances describe ups-server \
  --zone=asia-east1-b \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'

# 2. 检查防火墙规则
gcloud compute firewall-rules list --filter="targetTags:ups-server"

# 3. 检查Docker服务
docker-compose ps

# 4. 检查端口监听
sudo netstat -tlnp | grep 8080

# 5. 测试本地访问
curl http://localhost:8080/actuator/health
```

**解决方法**：
```bash
# 添加防火墙规则
gcloud compute firewall-rules create allow-api-gateway \
  --network=default \
  --allow=tcp:8080 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=ups-server

# 重启服务
cd ~/UPS
docker-compose restart
```

### 问题2：SSH连接失败

**检查SSH防火墙规则**：
```bash
gcloud compute firewall-rules list --filter="allowed.ports:22"
```

**使用浏览器SSH**：
- Compute Engine → VM实例 → SSH按钮

**重置SSH密钥**：
```bash
gcloud compute reset-windows-password ups-server --zone=asia-east1-b
```

### 问题3：磁盘空间不足

**检查磁盘**：
```bash
df -h
du -sh /var/lib/docker
```

**扩展磁盘**：
```bash
# 1. 停止实例
gcloud compute instances stop ups-server --zone=asia-east1-b

# 2. 调整磁盘大小
gcloud compute disks resize ups-server \
  --size=100GB \
  --zone=asia-east1-b

# 3. 启动实例
gcloud compute instances start ups-server --zone=asia-east1-b

# 4. SSH连接后扩展文件系统
sudo growpart /dev/sda 1
sudo resize2fs /dev/sda1
```

### 问题4：HTTPS证书获取失败

**检查DNS解析**：
```bash
nslookup api.yourdomain.com
dig api.yourdomain.com
```

**检查防火墙**：
```bash
# 确保80和443端口开放
gcloud compute firewall-rules list --filter="allowed.ports:(80 OR 443)"
```

**手动重试**：
```bash
sudo certbot --nginx -d api.yourdomain.com --force-renew
```

---

## 性能优化

### 1. 使用抢占式VM（开发环境）

节省高达80%费用：
```bash
gcloud compute instances create ups-preemptible \
  --zone=asia-east1-b \
  --machine-type=e2-medium \
  --preemptible
```

⚠️ **注意**：抢占式VM最多运行24小时，不适合生产环境！

### 2. 选择SSD永久性磁盘

```bash
gcloud compute instances create ups-server \
  --boot-disk-type=pd-ssd \
  --boot-disk-size=40GB
```

性能对比：
- 标准永久性磁盘：~75 IOPS/GB
- SSD永久性磁盘：~30 IOPS/GB
- 极速SSD：~100,000+ IOPS

### 3. 启用网络性能优化

```bash
# 创建VM时启用
gcloud compute instances create ups-server \
  --network-interface=nic-type=GVNIC
```

### 4. Nginx优化

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

    # 缓存
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=100m;

    # 连接保持
    keepalive_timeout 65;
}
```

### 5. 使用Cloud CDN

```bash
# 启用Cloud CDN
gcloud compute backend-services update ups-backend \
  --enable-cdn \
  --global
```

---

## 安全加固

### 1. 使用OS Login

```bash
# 启用OS Login
gcloud compute project-info add-metadata \
  --metadata enable-oslogin=TRUE

# 为VM启用
gcloud compute instances add-metadata ups-server \
  --zone=asia-east1-b \
  --metadata enable-oslogin=TRUE
```

### 2. 启用Shielded VM

```bash
gcloud compute instances create ups-server \
  --shielded-secure-boot \
  --shielded-vtpm \
  --shielded-integrity-monitoring
```

### 3. 使用Secret Manager

```bash
# 创建密钥
echo -n "your-secret-value" | gcloud secrets create jwt-secret \
  --data-file=-

# 授予访问权限
gcloud secrets add-iam-policy-binding jwt-secret \
  --member=serviceAccount:SERVICE_ACCOUNT_EMAIL \
  --role=roles/secretmanager.secretAccessor

# 在应用中访问
gcloud secrets versions access latest --secret="jwt-secret"
```

### 4. 启用Cloud Armor

防DDoS和WAF：
```bash
# 创建安全策略
gcloud compute security-policies create ups-security-policy

# 添加规则
gcloud compute security-policies rules create 1000 \
  --security-policy=ups-security-policy \
  --expression="origin.region_code == 'CN'" \
  --action=allow

# 应用到后端服务
gcloud compute backend-services update ups-backend \
  --security-policy=ups-security-policy \
  --global
```

### 5. 配置VPC防火墙最佳实践

```bash
# 拒绝所有入站流量（默认）
gcloud compute firewall-rules create deny-all-ingress \
  --network=default \
  --action=DENY \
  --rules=all \
  --source-ranges=0.0.0.0/0 \
  --priority=65534

# 仅允许必要端口（优先级更高）
# 已在前面配置
```

---

## 备份和灾难恢复

### 1. 创建快照

**手动快照**：
```bash
# 创建磁盘快照
gcloud compute disks snapshot ups-server \
  --snapshot-names=ups-snapshot-$(date +%Y%m%d) \
  --zone=asia-east1-b
```

**自动快照策略**：
```bash
# 创建快照计划
gcloud compute resource-policies create snapshot-schedule ups-daily-backup \
  --region=asia-east1 \
  --max-retention-days=7 \
  --start-time=02:00 \
  --daily-schedule

# 应用到磁盘
gcloud compute disks add-resource-policies ups-server \
  --resource-policies=ups-daily-backup \
  --zone=asia-east1-b
```

### 2. 创建机器映像

```bash
# 创建机器映像（包含所有磁盘）
gcloud compute machine-images create ups-image-$(date +%Y%m%d) \
  --source-instance=ups-server \
  --source-instance-zone=asia-east1-b

# 从映像创建新实例
gcloud compute instances create ups-server-new \
  --source-machine-image=ups-image-20251105 \
  --zone=asia-east1-b
```

### 3. 跨区域复制

```bash
# 复制快照到其他区域
gcloud compute snapshots create ups-snapshot-dr \
  --source-disk=ups-server \
  --source-disk-zone=asia-east1-b \
  --storage-location=us-west1
```

### 4. 数据库备份

```bash
# 备份MySQL
docker exec ups-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} userservice > backup_$(date +%Y%m%d).sql

# 上传到Cloud Storage
gsutil cp backup_$(date +%Y%m%d).sql gs://your-backup-bucket/mysql/

# 自动化备份脚本
cat > /usr/local/bin/backup-ups.sh << 'EOF'
#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
docker exec ups-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} --all-databases > /tmp/mysql_${TIMESTAMP}.sql
gsutil cp /tmp/mysql_${TIMESTAMP}.sql gs://your-backup-bucket/mysql/
rm /tmp/mysql_${TIMESTAMP}.sql
EOF

chmod +x /usr/local/bin/backup-ups.sh

# 添加到crontab
echo "0 2 * * * /usr/local/bin/backup-ups.sh" | crontab -
```

---

## 成本优化

### 1. 使用承诺使用折扣

- 1年承诺：节省37%
- 3年承诺：节省55%

```bash
# 购买承诺
gcloud compute commitments create ups-commitment \
  --region=asia-east1 \
  --plan=12-month \
  --resources=vcpu=2,memory=4GB
```

### 2. 使用抢占式VM

```bash
gcloud compute instances create ups-preemptible \
  --preemptible \
  --maintenance-policy=TERMINATE
```

节省：60-91%（仅适合开发环境）

### 3. 自动调度

```bash
# 创建Cloud Scheduler任务
# 工作日9:00启动
gcloud scheduler jobs create http start-vm \
  --schedule="0 9 * * 1-5" \
  --uri="https://compute.googleapis.com/compute/v1/projects/PROJECT/zones/ZONE/instances/ups-server/start" \
  --http-method=POST

# 工作日18:00停止
gcloud scheduler jobs create http stop-vm \
  --schedule="0 18 * * 1-5" \
  --uri="https://compute.googleapis.com/compute/v1/projects/PROJECT/zones/ZONE/instances/ups-server/stop" \
  --http-method=POST
```

### 4. 使用自定义机器类型

精确匹配资源需求：
```bash
gcloud compute instances create ups-custom \
  --custom-cpu=2 \
  --custom-memory=3GB
```

### 定价估算

**开发环境** (e2-medium + 抢占式):
```
VM (e2-medium preemptible): ~$8/月
磁盘 (40GB 标准): ~$1.7/月
外部IP (临时): $0
出站流量: ~$5/月
总计: ~$15/月
```

**生产环境** (n2-standard-2 + 负载均衡):
```
VM (n2-standard-2): ~$70/月
磁盘 (50GB SSD): ~$8.5/月
静态IP: ~$7/月
负载均衡: ~$18/月
Cloud CDN: ~$5/月
出站流量: ~$10/月
总计: ~$118/月
```

---

## 快速参考

### 常用命令

```bash
# 查看实例列表
gcloud compute instances list

# 查看外部IP
gcloud compute instances describe ups-server \
  --zone=asia-east1-b \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'

# SSH连接
gcloud compute ssh ups-server --zone=asia-east1-b

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart
```

### 访问地址

| 服务 | 直接IP访问 | 负载均衡 | 域名访问（HTTPS） |
|------|-----------|---------|------------------|
| API Gateway | `http://IP:8080` | `http://lb-ip` | `https://api.domain.com` |
| Swagger UI | `http://IP:8080/swagger-ui.html` | `http://lb-ip/swagger-ui.html` | `https://api.domain.com/swagger-ui.html` |

### GCP Console链接

- Compute Engine: https://console.cloud.google.com/compute
- VPC防火墙: https://console.cloud.google.com/networking/firewalls
- Cloud DNS: https://console.cloud.google.com/net-services/dns
- Cloud Monitoring: https://console.cloud.google.com/monitoring
- Cloud Logging: https://console.cloud.google.com/logs

---

## 相关资源

### GCP官方文档
- Compute Engine: https://cloud.google.com/compute/docs
- VPC防火墙规则: https://cloud.google.com/vpc/docs/firewalls
- 负载均衡: https://cloud.google.com/load-balancing/docs

### UPS项目
- GitHub: https://github.com/dctx479/UPS
- 文档: https://github.com/dctx479/UPS/tree/main/docs

### 学习资源
- GCP免费套餐: https://cloud.google.com/free
- Google Cloud Skills Boost: https://www.cloudskillsboost.google
- GCP架构中心: https://cloud.google.com/architecture

---

## 技术支持

如有问题请联系：
- 📧 邮箱：b150w4942@163.com
- 📝 GitHub Issues：https://github.com/dctx479/UPS/issues

---

**更新时间**：2025-11-05
**适用版本**：UPS v1.0
**适用区域**：GCP全球所有区域
