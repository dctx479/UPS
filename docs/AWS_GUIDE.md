# AWS EC2部署指南

本指南专门针对Amazon Web Services (AWS) EC2云服务器，提供详细的UPS系统部署和公网访问配置步骤。

---

## 前置准备

### 1. 创建AWS账户

- 访问 [AWS官网](https://aws.amazon.com)
- 注册账户（需要信用卡验证）
- 完成身份验证

### 2. 选择AWS区域

推荐区域（延迟较低）：
- **亚太地区（东京）**: ap-northeast-1
- **亚太地区（首尔）**: ap-northeast-2
- **亚太地区（新加坡）**: ap-southeast-1
- **美国西部（俄勒冈）**: us-west-2

---

## 第一步：创建EC2实例

### 1. 进入EC2控制台

1. 登录 [AWS管理控制台](https://console.aws.amazon.com)
2. 搜索并选择【EC2】服务
3. 选择您的首选区域（右上角下拉菜单）

### 2. 启动实例

点击【Launch Instance】（启动实例），配置如下：

#### 基本配置

**名称和标签**：
```
名称: UPS-Server
标签: Environment: Production
```

**应用程序和操作系统映像 (AMI)**：
```
推荐选择:
- Ubuntu Server 22.04 LTS (HVM), SSD Volume Type
- 或 Amazon Linux 2023 AMI
- 架构: 64位 (x86)
```

**实例类型**：
```
开发测试环境:
- t3.medium (2 vCPU, 4 GiB 内存) - 推荐
- t3.small (2 vCPU, 2 GiB 内存) - 最小配置

生产环境:
- t3.large (2 vCPU, 8 GiB 内存)
- m5.large (2 vCPU, 8 GiB 内存)
```

#### 密钥对配置

**创建新密钥对**（首次使用）：
```
密钥对名称: ups-key
密钥对类型: RSA
私钥文件格式: .pem (Linux/Mac) 或 .ppk (Windows/PuTTY)
```

⚠️ **重要**：下载的私钥文件(.pem)请妥善保管，无法重新下载！

#### 网络设置

**VPC和子网**：
```
VPC: 默认VPC (或创建新VPC)
子网: 无偏好设置（自动选择可用区）
自动分配公有IP: 启用
```

**防火墙（安全组）**：

创建新安全组或使用现有安全组：
```
安全组名称: UPS-Security-Group
描述: Security group for UPS application

入站规则:
1. SSH
   - 类型: SSH
   - 协议: TCP
   - 端口范围: 22
   - 源: 我的IP (推荐) 或 0.0.0.0/0

2. HTTP
   - 类型: HTTP
   - 协议: TCP
   - 端口范围: 80
   - 源: 0.0.0.0/0

3. HTTPS
   - 类型: HTTPS
   - 协议: TCP
   - 端口范围: 443
   - 源: 0.0.0.0/0

4. API Gateway (自定义TCP)
   - 类型: 自定义TCP
   - 协议: TCP
   - 端口范围: 8080
   - 源: 0.0.0.0/0
```

#### 存储配置

```
根卷:
- 大小: 30 GiB (最小) - 40 GiB (推荐)
- 卷类型: gp3 (通用型SSD) - 推荐
- IOPS: 3000 (默认)
- 吞吐量: 125 MB/s (默认)
- 启用加密: 推荐启用
```

#### 高级详细信息（可选）

**用户数据**（自动执行脚本）：
```bash
#!/bin/bash
# 更新系统
apt update -y
apt upgrade -y

# 安装基础工具
apt install -y git curl wget vim

# 安装Docker
curl -fsSL https://get.docker.com | bash
systemctl start docker
systemctl enable docker

# 安装Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

### 3. 启动实例

1. 检查配置摘要
2. 点击【Launch Instance】
3. 等待实例状态变为 "Running"（约2-3分钟）

---

## 第二步：配置安全组

如果创建实例时未正确配置，可以修改安全组规则：

### 修改安全组规则

1. **进入安全组**
   - EC2控制台 → 左侧菜单【网络与安全】→【安全组】
   - 找到您的安全组（如 UPS-Security-Group）

2. **编辑入站规则**

   点击【入站规则】→【编辑入站规则】→【添加规则】

**必需的安全规则**：

| 类型 | 协议 | 端口范围 | 源 | 说明 |
|-----|------|---------|----|----|
| SSH | TCP | 22 | 你的IP/32 | SSH远程管理 |
| HTTP | TCP | 80 | 0.0.0.0/0 | HTTP访问 |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS访问 |
| 自定义TCP | TCP | 8080 | 0.0.0.0/0 | API Gateway |

❌ **禁止开放的端口**：
- 3306 (MySQL)
- 27017 (MongoDB)
- 6379 (Redis)
- 8081-8083 (微服务内部端口)

### 安全最佳实践

✅ **推荐配置**：
- SSH端口仅对您的IP开放
- 使用密钥对认证，禁用密码登录
- 定期轮换密钥对
- 启用AWS CloudTrail审计日志

---

## 第三步：连接到EC2实例

### 方法1：使用AWS Session Manager（推荐，无需密钥）

1. 确保实例有正确的IAM角色
2. EC2控制台 → 选择实例 → 点击【连接】
3. 选择【Session Manager】标签
4. 点击【连接】

### 方法2：使用SSH（传统方式）

#### 获取连接信息

1. EC2控制台 → 选择实例
2. 复制【公有IPv4地址】或【公有IPv4 DNS】

#### Linux/Mac连接

```bash
# 设置密钥文件权限
chmod 400 ~/Downloads/ups-key.pem

# 连接到实例
ssh -i ~/Downloads/ups-key.pem ubuntu@your-instance-public-ip

# 或使用公有DNS
ssh -i ~/Downloads/ups-key.pem ubuntu@ec2-xx-xx-xx-xx.compute.amazonaws.com
```

#### Windows连接

**使用PowerShell**：
```powershell
ssh -i C:\path\to\ups-key.pem ubuntu@your-instance-public-ip
```

**使用PuTTY**：
1. 使用PuTTYgen转换.pem为.ppk格式
2. 打开PuTTY
3. Host Name: ubuntu@your-instance-public-ip
4. Connection → SSH → Auth → 选择.ppk文件
5. 点击Open

### 方法3：使用EC2 Instance Connect（浏览器）

1. EC2控制台 → 选择实例 → 点击【连接】
2. 选择【EC2 Instance Connect】标签
3. 用户名: ubuntu
4. 点击【连接】

---

## 第四步：安装Docker环境

连接到实例后，执行以下命令：

```bash
# 1. 更新系统
sudo apt update && sudo apt upgrade -y

# 2. 安装Docker
curl -fsSL https://get.docker.com | bash

# 3. 将当前用户添加到docker组（避免每次使用sudo）
sudo usermod -aG docker ubuntu

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

# 8. 配置Docker镜像加速（可选）
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
# 然后重新SSH连接
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

# 3. 测试公网访问
curl http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080/actuator/health
```

---

## 第六步：配置公网访问

### 方法1：直接通过公网IP访问

获取实例公网IP：
```bash
# 在EC2实例内执行
curl http://169.254.169.254/latest/meta-data/public-ipv4
```

直接访问：
```
http://your-public-ip:8080
```

**访问地址**：
- API文档：`http://your-public-ip:8080/swagger-ui.html`
- 健康检查：`http://your-public-ip:8080/actuator/health`

⚠️ **注意**：确保安全组已开放8080端口！

### 方法2：使用域名+HTTPS（生产环境推荐）

#### 步骤1：配置Elastic IP（弹性IP）

**为什么需要Elastic IP？**
- EC2实例重启后公网IP会变化
- Elastic IP是固定的静态IP地址

**配置步骤**：
1. EC2控制台 → 左侧菜单【网络与安全】→【弹性IP】
2. 点击【分配弹性IP地址】
3. 点击【分配】
4. 选择分配的IP → 点击【操作】→【关联弹性IP地址】
5. 选择您的实例 → 点击【关联】

⚠️ **注意**：未关联到实例的Elastic IP会产生费用！

#### 步骤2：配置DNS解析

**使用Route 53（AWS DNS服务）**：

1. **创建托管区域**
   - 进入Route 53控制台
   - 点击【创建托管区域】
   - 域名: yourdomain.com
   - 类型: 公有托管区域

2. **添加A记录**
   - 点击【创建记录】
   - 记录名称: api (或留空使用根域名)
   - 记录类型: A - 将流量路由到IPv4地址
   - 值: 您的Elastic IP地址
   - TTL: 300秒
   - 点击【创建记录】

3. **更新域名服务器**
   - 复制Route 53提供的4个名称服务器
   - 到域名注册商处更新NS记录

**或使用第三方DNS**：
```
记录类型: A
主机记录: api (或 @)
记录值: 您的Elastic IP
TTL: 300-600秒
```

#### 步骤3：验证DNS解析

```bash
# 等待5-30分钟后验证
nslookup api.yourdomain.com
dig api.yourdomain.com

# 应该返回您的Elastic IP
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

### 使用Application Load Balancer (ALB)

#### 1. 创建目标组

1. EC2控制台 → 【负载均衡】→【目标组】
2. 点击【创建目标组】
3. 配置：
```
目标类型: 实例
目标组名称: UPS-Target-Group
协议: HTTP
端口: 8080
VPC: 选择您的VPC
健康检查路径: /actuator/health
```

4. 注册目标 → 选择您的EC2实例 → 点击【包含为待处理项】
5. 点击【创建目标组】

#### 2. 创建负载均衡器

1. EC2控制台 → 【负载均衡】→【负载均衡器】
2. 点击【创建负载均衡器】
3. 选择【Application Load Balancer】
4. 配置：
```
负载均衡器名称: UPS-ALB
方案: 面向互联网
IP地址类型: IPv4
```

5. 网络映射：
   - 选择您的VPC
   - 选择至少2个可用区

6. 安全组：
   - 创建新安全组或选择现有（开放80, 443端口）

7. 监听器和路由：
```
监听器1:
协议: HTTP
端口: 80
默认操作: 转发到 UPS-Target-Group

监听器2 (可选):
协议: HTTPS
端口: 443
SSL证书: 从ACM选择或上传
默认操作: 转发到 UPS-Target-Group
```

8. 点击【创建负载均衡器】

#### 3. 配置HTTPS（使用AWS Certificate Manager）

1. 进入ACM控制台
2. 点击【请求证书】
3. 选择【请求公有证书】
4. 域名: api.yourdomain.com
5. 验证方法: DNS验证（推荐）
6. 在Route 53中添加验证记录
7. 等待证书颁发
8. 在ALB监听器中选择该证书

---

## 监控和维护

### 1. CloudWatch监控

**查看实例指标**：
1. EC2控制台 → 选择实例 → 【监控】标签
2. 查看：
   - CPU使用率
   - 网络流量
   - 磁盘读写

**设置告警**：
```bash
# 创建CPU告警
aws cloudwatch put-metric-alarm \
  --alarm-name ups-high-cpu \
  --alarm-description "UPS CPU > 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2
```

### 2. 日志管理

**配置CloudWatch Logs**：

```bash
# 安装CloudWatch Agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i -E ./amazon-cloudwatch-agent.deb

# 配置日志收集
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard
```

**查看Docker日志**：
```bash
# 实时查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f gateway-service

# 导出日志到CloudWatch
docker-compose logs gateway-service | aws logs put-log-events ...
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

### 问题1：无法通过公网IP访问

**检查清单**：
1. ✅ 安全组是否开放对应端口
2. ✅ 实例是否有公网IP/Elastic IP
3. ✅ Docker服务是否运行
4. ✅ 端口是否被占用

**排查步骤**：
```bash
# 1. 检查公网IP
curl http://169.254.169.254/latest/meta-data/public-ipv4

# 2. 检查安全组
AWS控制台 → EC2 → 安全组 → 检查入站规则

# 3. 检查Docker服务
docker-compose ps

# 4. 检查端口监听
netstat -tlnp | grep 8080

# 5. 测试本地访问
curl http://localhost:8080/actuator/health

# 6. 检查防火墙（Ubuntu）
sudo ufw status
```

**解决方法**：
```bash
# 添加安全组规则
AWS控制台 → EC2 → 安全组 → 编辑入站规则 → 添加8080端口

# 重启服务
cd ~/UPS
docker-compose restart
```

### 问题2：SSH连接失败

**可能原因**：
- 密钥文件权限不正确
- 安全组未开放22端口
- 使用错误的用户名

**解决方法**：
```bash
# 修复密钥权限
chmod 400 ups-key.pem

# 使用正确的用户名
# Ubuntu AMI: ubuntu
# Amazon Linux: ec2-user
# CentOS: centos

ssh -i ups-key.pem ubuntu@your-public-ip
```

### 问题3：实例状态检查失败

**检查状态**：
```bash
# EC2控制台 → 实例 → 状态检查
# 如果失败，查看系统日志
```

**常见原因**：
- 内存不足
- 磁盘空间不足
- 内核崩溃

**解决方法**：
```bash
# 停止并启动实例（不是重启）
AWS控制台 → 实例 → 实例状态 → 停止 → 启动

# 或使用AWS CLI
aws ec2 stop-instances --instance-ids i-1234567890abcdef0
aws ec2 start-instances --instance-ids i-1234567890abcdef0
```

### 问题4：HTTPS证书获取失败

**检查**：
```bash
# 验证域名解析
nslookup api.yourdomain.com
dig api.yourdomain.com

# 检查安全组
确保80和443端口已开放

# 查看Certbot日志
sudo cat /var/log/letsencrypt/letsencrypt.log
```

**解决**：
```bash
# 手动重试
sudo certbot --nginx -d api.yourdomain.com --force-renew
```

---

## 性能优化

### 1. 使用增强型网络

启用增强型网络可提升网络性能：
```bash
# 检查是否支持
aws ec2 describe-instance-attribute \
  --instance-id i-1234567890abcdef0 \
  --attribute sriovNetSupport

# 启用增强型网络
aws ec2 modify-instance-attribute \
  --instance-id i-1234567890abcdef0 \
  --sriov-net-support simple
```

### 2. 使用EBS优化实例

```bash
# 启用EBS优化
aws ec2 modify-instance-attribute \
  --instance-id i-1234567890abcdef0 \
  --ebs-optimized
```

### 3. Nginx优化

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

### 4. 使用CloudFront CDN（可选）

1. 创建CloudFront分配
2. 源: ALB或EC2公网IP
3. 配置缓存行为
4. 配置自定义SSL证书

---

## 安全加固

### 1. IAM角色最佳实践

创建具有最小权限的IAM角色：
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::your-bucket/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    }
  ]
}
```

### 2. 启用AWS Systems Manager Session Manager

替代SSH的更安全方式：
1. 为EC2实例附加IAM角色（AmazonSSMManagedInstanceCore）
2. 安装SSM Agent（Amazon Linux和Ubuntu 20.04+预装）
3. 通过Systems Manager连接

### 3. 配置网络ACL

额外的网络层安全：
```
入站规则:
规则号: 100, 类型: HTTP (80), 源: 0.0.0.0/0, 允许
规则号: 110, 类型: HTTPS (443), 源: 0.0.0.0/0, 允许
规则号: 120, 类型: SSH (22), 源: 你的IP/32, 允许
规则号: 130, 类型: 自定义TCP (8080), 源: 0.0.0.0/0, 允许

出站规则:
规则号: 100, 类型: 所有流量, 目标: 0.0.0.0/0, 允许
```

### 4. 启用VPC Flow Logs

记录网络流量：
```bash
aws ec2 create-flow-logs \
  --resource-type VPC \
  --resource-ids vpc-12345678 \
  --traffic-type ALL \
  --log-destination-type cloud-watch-logs \
  --log-group-name /aws/vpc/flowlogs
```

### 5. 启用GuardDuty

AWS威胁检测服务：
1. 进入GuardDuty控制台
2. 点击【开始使用】
3. 启用30天免费试用

---

## 备份和灾难恢复

### 1. 创建AMI镜像

```bash
# 使用AWS CLI
aws ec2 create-image \
  --instance-id i-1234567890abcdef0 \
  --name "UPS-Backup-$(date +%Y%m%d)" \
  --description "UPS System Backup" \
  --no-reboot
```

**或使用控制台**：
1. EC2控制台 → 选择实例
2. 操作 → 映像和模板 → 创建映像

### 2. 配置EBS快照

**自动快照策略**：
1. EC2控制台 → 弹性块存储 → 生命周期管理器
2. 创建快照生命周期策略
3. 配置：
```
资源类型: 卷
目标标签: Name=UPS-Server
计划: 每天 02:00 UTC
保留规则: 保留7个快照
```

### 3. 跨区域复制

```bash
# 复制AMI到其他区域
aws ec2 copy-image \
  --source-region us-west-2 \
  --source-image-id ami-1234567890abcdef0 \
  --region us-east-1 \
  --name "UPS-DR-Image"
```

### 4. 数据库备份

```bash
# 备份MySQL
docker exec ups-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} userservice > backup_$(date +%Y%m%d).sql

# 上传到S3
aws s3 cp backup_$(date +%Y%m%d).sql s3://your-backup-bucket/mysql/

# 自动化备份脚本
cat > /usr/local/bin/backup-ups.sh << 'EOF'
#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
docker exec ups-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} --all-databases > /tmp/mysql_${TIMESTAMP}.sql
aws s3 cp /tmp/mysql_${TIMESTAMP}.sql s3://your-backup-bucket/mysql/
rm /tmp/mysql_${TIMESTAMP}.sql
EOF

chmod +x /usr/local/bin/backup-ups.sh

# 添加到crontab (每天凌晨2点)
echo "0 2 * * * /usr/local/bin/backup-ups.sh" | crontab -
```

---

## 成本优化

### 1. 使用Savings Plans

- 承诺1年或3年使用量
- 节省高达72%费用
- 灵活性高于预留实例

### 2. 使用Spot实例（开发环境）

```bash
# 请求Spot实例
aws ec2 request-spot-instances \
  --spot-price "0.05" \
  --instance-count 1 \
  --type "one-time" \
  --launch-specification file://specification.json
```

⚠️ **注意**：Spot实例可能被中断，不适合生产环境！

### 3. 右侧调整实例大小

定期审查CloudWatch指标，选择合适的实例类型：
- CPU平均使用率 < 40% → 考虑降级
- CPU经常 > 80% → 考虑升级

### 4. 使用Auto Scaling

根据负载自动调整实例数量：
```bash
# 创建启动模板
aws ec2 create-launch-template \
  --launch-template-name ups-template \
  --version-description "UPS Launch Template"

# 创建Auto Scaling组
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name ups-asg \
  --launch-template LaunchTemplateName=ups-template \
  --min-size 1 \
  --max-size 5 \
  --desired-capacity 2
```

### 5. 定时启停实例（开发环境）

```bash
# 创建Lambda函数自动启停
# 工作日 9:00 启动，18:00 停止
# 周末全天停止
# 每月节省约 60-70% 费用
```

---

## 快速参考

### 常用命令

```bash
# 查看实例信息
aws ec2 describe-instances --instance-ids i-1234567890abcdef0

# 获取公网IP
curl http://169.254.169.254/latest/meta-data/public-ipv4

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 查看资源使用
docker stats
htop
```

### 访问地址

| 服务 | 直接IP访问 | ELB访问 | 域名访问（HTTPS） |
|------|-----------|---------|------------------|
| API Gateway | `http://IP:8080` | `http://elb-dns-name` | `https://api.domain.com` |
| Swagger UI | `http://IP:8080/swagger-ui.html` | `http://elb-dns-name/swagger-ui.html` | `https://api.domain.com/swagger-ui.html` |

### AWS控制台链接

- EC2控制台: https://console.aws.amazon.com/ec2
- 安全组: https://console.aws.amazon.com/ec2/#SecurityGroups
- 负载均衡器: https://console.aws.amazon.com/ec2/#LoadBalancers
- Route 53: https://console.aws.amazon.com/route53
- CloudWatch: https://console.aws.amazon.com/cloudwatch
- IAM: https://console.aws.amazon.com/iam

### 定价估算

**开发环境** (t3.medium):
```
EC2实例: ~$30/月
EBS存储 (30GB): ~$3/月
数据传输: ~$5/月
弹性IP: $0 (关联时)
总计: ~$38/月
```

**生产环境** (t3.large + ALB):
```
EC2实例: ~$60/月
EBS存储 (40GB): ~$4/月
ALB: ~$16/月
数据传输: ~$10/月
Route 53: ~$1/月
总计: ~$91/月
```

---

## 相关资源

### AWS官方文档
- EC2用户指南: https://docs.aws.amazon.com/ec2
- 安全组配置: https://docs.aws.amazon.com/vpc/latest/userguide/VPC_SecurityGroups.html
- ELB文档: https://docs.aws.amazon.com/elasticloadbalancing

### UPS项目
- GitHub: https://github.com/dctx479/UPS
- 文档: https://github.com/dctx479/UPS/tree/main/docs

### 学习资源
- AWS免费套餐: https://aws.amazon.com/free
- AWS培训: https://aws.amazon.com/training
- AWS架构中心: https://aws.amazon.com/architecture

---

## 技术支持

如有问题请联系：
- 📧 邮箱：b150w4942@163.com
- 📝 GitHub Issues：https://github.com/dctx479/UPS/issues

---

**更新时间**：2025-11-05
**适用版本**：UPS v1.0
**适用区域**：AWS全球所有区域
