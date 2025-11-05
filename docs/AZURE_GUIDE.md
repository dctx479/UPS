# Azure虚拟机部署指南

本指南专门针对Microsoft Azure虚拟机(VM)，提供详细的UPS系统部署和公网访问配置步骤。

---

## 前置准备

### 1. 创建Azure账户

- 访问 [Azure官网](https://azure.microsoft.com)
- 注册账户（新用户可获得$200免费额度）
- 完成身份验证

### 2. 选择Azure区域

推荐区域（国内延迟较低）：
- **中国东部**: chinaeast
- **中国北部**: chinanorth
- **东亚**: eastasia（香港）
- **东南亚**: southeastasia（新加坡）
- **日本东部**: japaneast

---

## 第一步：创建虚拟机

### 1. 进入Azure门户

1. 登录 [Azure门户](https://portal.azure.com)
2. 点击【创建资源】
3. 搜索并选择【虚拟机】
4. 点击【创建】

### 2. 基础配置

#### 项目详细信息

**订阅**：
```
选择您的Azure订阅
```

**资源组**：
```
创建新资源组: UPS-ResourceGroup
或选择现有资源组
```

#### 实例详细信息

**虚拟机名称**：
```
ups-server
```

**区域**：
```
开发测试: 东南亚 (成本较低)
生产环境: 中国东部/北部 (国内访问快)
```

**可用性选项**：
```
开发测试: 无需基础结构冗余
生产环境: 可用性区域 (高可用)
```

**安全类型**：
```
标准 (推荐)
```

**映像**：
```
推荐选择:
- Ubuntu Server 22.04 LTS - x64 Gen2
- Ubuntu Server 20.04 LTS - x64 Gen2
```

**VM体系结构**：
```
x64
```

**大小**：
```
开发测试环境:
- Standard_B2s (2 vCPU, 4 GiB 内存) - 推荐
- Standard_B1ms (1 vCPU, 2 GiB 内存) - 最小配置

生产环境:
- Standard_D2s_v3 (2 vCPU, 8 GiB 内存)
- Standard_D4s_v3 (4 vCPU, 16 GiB 内存)
```

点击【查看所有大小】可浏览更多选项。

#### 管理员账户

**身份验证类型**：

**选项1：SSH公钥（推荐）**
```
用户名: azureuser
SSH公钥源: 生成新密钥对
密钥对名称: ups-key
```

**选项2：密码**
```
用户名: azureuser
密码: 复杂密码（大小写+数字+特殊字符，12位以上）
确认密码: 再次输入
```

#### 入站端口规则

**公共入站端口**：
```
选择: 允许所选端口
```

**选择入站端口**：
```
✅ HTTP (80)
✅ HTTPS (443)
✅ SSH (22)
```

⚠️ **注意**：8080端口需要稍后在网络安全组中手动添加！

### 3. 磁盘配置

点击【下一步: 磁盘】

**OS磁盘类型**：
```
开发测试: 标准SSD (E10) - 成本效益高
生产环境: 高级SSD (P10) - 性能最佳
```

**OS磁盘大小**：
```
默认: 30 GiB (最小)
推荐: 64 GiB 或更大
```

**加密类型**：
```
(默认) 使用平台托管密钥加密
```

**数据磁盘**（可选）：
```
如需额外存储，点击【创建并附加新磁盘】
大小: 根据需求选择
类型: 标准SSD或高级SSD
```

### 4. 网络配置

点击【下一步: 网络】

**虚拟网络**：
```
(新) ups-vnet
地址空间: 10.0.0.0/16
```

**子网**：
```
(新) default (10.0.0.0/24)
```

**公共IP**：
```
(新) ups-server-ip
SKU: 标准
分配: 静态 (推荐，避免IP变化)
```

**NIC网络安全组**：
```
基本 (简单配置)
或
高级 (更精细控制)
```

**公共入站端口**：
```
允许所选端口
```

**选择入站端口**：
```
HTTP (80)
HTTPS (443)
SSH (22)
```

**是否要删除虚拟机时删除公共IP和NIC**：
```
✅ 勾选（资源清理）
```

**负载均衡**（高可用，可选）：
```
开发测试: 无
生产环境: Azure负载均衡器
```

### 5. 管理配置（可选）

点击【下一步: 管理】

**Azure AD**：
```
使用Azure AD登录: 关闭（或根据需要启用）
```

**自动关闭**（开发环境省钱）：
```
启用自动关闭: 开启
关闭时间: 23:00 (北京时间)
时区: (UTC+08:00) 北京，重庆，香港，乌鲁木齐
通知: 启用（关闭前发送通知）
```

**备份**（生产环境推荐）：
```
启用备份: 是
恢复服务保管库: 创建新保管库
备份策略: 每日备份
```

**监视**：
```
启动诊断: 启用（推荐）
来宾OS诊断: 关闭（可选）
```

### 6. 高级配置（可选）

点击【下一步: 高级】

**用户数据**（自动化安装脚本）：
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

# 将用户添加到docker组
usermod -aG docker azureuser
```

### 7. 标记（可选）

点击【下一步: 标记】

添加标记便于资源管理：
```
Environment: Production
Project: UPS
Owner: YourName
```

### 8. 审阅和创建

1. 点击【审阅 + 创建】
2. 等待验证通过
3. 查看配置摘要
4. 点击【创建】

如果选择了SSH密钥，会提示下载私钥：
⚠️ **重要**：立即下载并保存私钥文件！无法重新下载！

部署通常需要3-5分钟。

---

## 第二步：配置网络安全组

虚拟机创建后，需要添加8080端口规则。

### 方法1：通过虚拟机配置

1. **进入虚拟机**
   - Azure门户 → 虚拟机 → 选择 ups-server

2. **配置网络**
   - 点击左侧菜单【网络】或【网络设置】
   - 点击【添加入站端口规则】

3. **添加8080端口**
```
源: Any
源端口范围: *
目标: Any
服务: 自定义
目标端口范围: 8080
协议: TCP
操作: 允许
优先级: 310
名称: Port_8080
```

4. 点击【添加】

### 方法2：通过网络安全组

1. **找到网络安全组**
   - Azure门户 → 网络安全组
   - 找到与VM关联的NSG（通常名为 ups-server-nsg）

2. **编辑入站规则**
   - 点击左侧【入站安全规则】
   - 点击【+ 添加】

3. **配置规则**

**API Gateway (8080)**：
```
源: Any
源端口范围: *
目标: Any
目标端口范围: 8080
协议: TCP
操作: 允许
优先级: 310
名称: AllowHTTP8080
```

### 必需的安全规则汇总

| 优先级 | 名称 | 端口 | 协议 | 源 | 操作 |
|-------|------|------|------|----|----|
| 300 | AllowSSH | 22 | TCP | 你的IP | 允许 |
| 310 | AllowHTTP | 80 | TCP | Any | 允许 |
| 320 | AllowHTTPS | 443 | TCP | Any | 允许 |
| 330 | AllowGateway | 8080 | TCP | Any | 允许 |

❌ **禁止开放的端口**：
- 3306 (MySQL)
- 27017 (MongoDB)
- 6379 (Redis)
- 8081-8083 (微服务内部端口)

### 安全最佳实践

✅ **推荐配置**：
```
1. SSH端口仅对特定IP开放
2. 使用SSH密钥而非密码
3. 启用Just-In-Time VM访问
4. 定期审查NSG规则
5. 使用Azure Defender for Cloud
```

---

## 第三步：连接到虚拟机

### 获取连接信息

1. Azure门户 → 虚拟机 → ups-server
2. 查看【概述】页面
3. 复制【公共IP地址】

### 方法1：使用Azure Cloud Shell（推荐）

1. 点击Azure门户右上角的Cloud Shell图标 (>_)
2. 选择【Bash】
3. 连接：
```bash
ssh azureuser@your-public-ip
```

### 方法2：使用本地SSH

#### Linux/Mac

```bash
# 设置私钥权限
chmod 400 ~/Downloads/ups-key.pem

# 连接
ssh -i ~/Downloads/ups-key.pem azureuser@your-public-ip
```

#### Windows PowerShell

```powershell
# 使用SSH密钥
ssh -i C:\path\to\ups-key.pem azureuser@your-public-ip

# 或使用密码
ssh azureuser@your-public-ip
```

#### Windows使用PuTTY

1. 使用PuTTYgen转换.pem为.ppk
2. 配置PuTTY连接
3. 加载私钥文件

### 方法3：使用Azure Bastion（最安全）

Azure Bastion提供无需暴露SSH端口的安全连接：

1. 创建Bastion（需要额外费用）
2. 虚拟机页面 → 点击【连接】→【Bastion】
3. 输入用户名和密码/SSH密钥
4. 点击【连接】

---

## 第四步：安装Docker环境

连接到虚拟机后，执行以下命令：

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

# 3. 测试公网访问（在本地电脑）
curl http://your-public-ip:8080/actuator/health
```

---

## 第六步：配置公网访问

### 方法1：直接通过公网IP访问

```
http://your-public-ip:8080
```

**访问地址**：
- API文档：`http://your-public-ip:8080/swagger-ui.html`
- 健康检查：`http://your-public-ip:8080/actuator/health`

⚠️ **确保网络安全组已开放8080端口！**

### 方法2：使用域名+HTTPS（生产环境推荐）

#### 步骤1：配置静态公网IP（如未配置）

1. Azure门户 → 虚拟机 → ups-server → 网络设置
2. 点击公共IP地址
3. 配置 → 分配：选择【静态】
4. 点击【保存】

#### 步骤2：配置DNS

**使用Azure DNS**：

1. **创建DNS区域**
   - Azure门户 → 搜索【DNS区域】
   - 点击【+ 创建】
   - 资源组: UPS-ResourceGroup
   - 名称: yourdomain.com
   - 点击【审阅 + 创建】

2. **添加A记录**
   - 进入创建的DNS区域
   - 点击【+ 记录集】
   - 配置：
```
名称: api (或留空使用根域名)
类型: A
TTL: 300
TTL单位: 秒
IP地址: 您的虚拟机公网IP
```
   - 点击【确定】

3. **更新域名服务器**
   - 查看DNS区域的【名称服务器】
   - 复制4个名称服务器地址
   - 到域名注册商处更新NS记录

**或使用第三方DNS**：
```
记录类型: A
主机记录: api
记录值: 您的Azure公网IP
TTL: 300-600秒
```

#### 步骤3：验证DNS解析

```bash
# 等待5-30分钟后验证
nslookup api.yourdomain.com
dig api.yourdomain.com

# 应该返回您的公网IP
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

## 第七步：配置应用程序网关（高可用，可选）

Azure应用程序网关提供7层负载均衡和WAF功能。

### 1. 创建应用程序网关

1. Azure门户 → 创建资源 → 应用程序网关
2. 基本配置：
```
资源组: UPS-ResourceGroup
应用程序网关名称: ups-appgw
区域: 与VM相同区域
层: 标准V2 或 WAF V2
```

3. 前端配置：
```
前端IP类型: 公共
公共IP地址: 新建 ups-appgw-pip
```

4. 后端配置：
```
后端池名称: ups-backend-pool
添加后端目标: 虚拟机 ups-server
```

5. 路由规则：
```
规则名称: ups-routing-rule
侦听器:
  - 侦听器名称: ups-listener
  - 前端IP: 公共
  - 协议: HTTP (稍后配置HTTPS)
  - 端口: 80
后端目标:
  - 后端池: ups-backend-pool
  - HTTP设置: 创建新设置
    - 名称: ups-http-settings
    - 后端端口: 8080
    - 协议: HTTP
```

### 2. 配置HTTPS

1. 上传SSL证书或使用托管证书
2. 修改侦听器协议为HTTPS
3. 配置HTTP到HTTPS重定向

---

## 监控和维护

### 1. Azure Monitor

**查看虚拟机指标**：
1. 虚拟机页面 → 监视 → 指标
2. 查看：
   - CPU百分比
   - 网络进出流量
   - 磁盘读写

**创建警报规则**：
1. 虚拟机页面 → 监视 → 警报
2. 点击【+ 创建警报规则】
3. 配置条件：
```
信号: Percentage CPU
条件: 大于
阈值: 80
聚合粒度: 5分钟
```

### 2. 日志管理

**启用诊断设置**：
```bash
# 安装Azure Monitor Agent
wget https://aka.ms/dependencyagentlinux -O InstallDependencyAgent-Linux64.bin
sudo sh InstallDependencyAgent-Linux64.bin
```

**查看Docker日志**：
```bash
# 实时查看
docker-compose logs -f

# 查看特定服务
docker-compose logs -f gateway-service

# 导出日志
docker-compose logs > ups-logs.txt
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

### 4. 使用Azure Log Analytics

1. 创建Log Analytics工作区
2. 虚拟机 → 监视 → 日志
3. 启用VM Insights
4. 编写KQL查询分析日志

---

## 常见问题排查

### 问题1：无法通过公网IP访问

**检查清单**：
1. ✅ 网络安全组是否开放8080端口
2. ✅ 虚拟机是否有公网IP
3. ✅ Docker服务是否运行
4. ✅ 端口是否被占用

**排查步骤**：
```bash
# 1. 检查公网IP
curl ifconfig.me

# 2. 检查NSG规则
Azure门户 → 网络安全组 → 入站安全规则

# 3. 检查Docker服务
docker-compose ps

# 4. 检查端口监听
netstat -tlnp | grep 8080

# 5. 测试本地访问
curl http://localhost:8080/actuator/health
```

**解决方法**：
```bash
# 添加NSG规则
Azure门户 → 网络安全组 → 添加入站规则 → 8080端口

# 重启服务
cd ~/UPS
docker-compose restart
```

### 问题2：SSH连接失败

**可能原因**：
- 密钥权限不正确
- NSG未开放22端口
- 使用错误的用户名

**解决方法**：
```bash
# 修复密钥权限
chmod 400 ups-key.pem

# 使用正确的用户名 (azureuser)
ssh -i ups-key.pem azureuser@your-public-ip

# 或使用Azure Serial Console
Azure门户 → 虚拟机 → 支持 + 疑难解答 → 串行控制台
```

### 问题3：磁盘空间不足

**检查磁盘**：
```bash
df -h
du -sh /var/lib/docker
```

**扩展OS磁盘**：
1. Azure门户 → 虚拟机 → 磁盘
2. 选择OS磁盘 → 大小+性能
3. 选择更大的磁盘大小
4. 点击【调整大小】
5. VM内执行：
```bash
# 扩展分区
sudo growpart /dev/sda 1
sudo resize2fs /dev/sda1
```

### 问题4：性能问题

**VM大小不足**：
1. 停止虚拟机
2. 虚拟机 → 大小
3. 选择更大的VM大小
4. 点击【调整大小】
5. 启动虚拟机

**临时性能提升**（B系列VM）：
- B系列VM可以突增CPU性能
- 监控CPU积分使用情况

---

## 性能优化

### 1. 选择合适的VM大小

**负载特征分析**：
```
CPU密集: F系列、Fsv2系列
内存密集: E系列、Esv3系列
平衡型: D系列、Dsv3系列
突发工作负载: B系列
```

### 2. 使用高级SSD

```
性能对比:
标准HDD: ~500 IOPS
标准SSD: ~6,000 IOPS
高级SSD: ~7,500+ IOPS
超级磁盘: ~160,000 IOPS
```

### 3. 启用加速网络

**支持的VM系列**：D/DSv2、D/DSv3、E/ESv3、F/FS、FSv2、Ms/Mms

**启用方法**：
```bash
# 使用Azure CLI
az network nic update \
  --name ups-serverVMNic \
  --resource-group UPS-ResourceGroup \
  --accelerated-networking true
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

### 5. 使用Azure CDN（可选）

1. 创建CDN配置文件
2. 添加CDN终结点
3. 源类型: 自定义源
4. 源主机名: 您的应用程序网关或公网IP

---

## 安全加固

### 1. 启用Azure Security Center

1. Azure门户 → Security Center
2. 升级到标准层（30天免费试用）
3. 查看安全建议
4. 应用安全建议

### 2. 启用Just-In-Time VM访问

1. Security Center → Just-in-time VM access
2. 选择ups-server
3. 点击【在VM上启用JIT】
4. 配置允许的端口和时间窗口

### 3. 使用Azure Key Vault

存储敏感信息：
```bash
# 创建Key Vault
az keyvault create \
  --name ups-keyvault \
  --resource-group UPS-ResourceGroup \
  --location eastasia

# 存储密钥
az keyvault secret set \
  --vault-name ups-keyvault \
  --name jwt-secret \
  --value "your-secret-value"
```

### 4. 启用磁盘加密

```bash
# 使用Azure Disk Encryption
az vm encryption enable \
  --resource-group UPS-ResourceGroup \
  --name ups-server \
  --disk-encryption-keyvault ups-keyvault
```

### 5. 配置NSG最佳实践

```
优先级规则:
100: 允许特定IP的SSH (最高优先级)
200: 拒绝所有SSH
300: 允许HTTP
310: 允许HTTPS
320: 允许8080
65000: 拒绝所有入站流量 (默认)
```

---

## 备份和灾难恢复

### 1. 配置Azure Backup

1. **启用备份**
   - 虚拟机 → 备份
   - 恢复服务保管库: 创建新保管库
   - 备份策略: 默认策略（每日备份）
   - 点击【启用备份】

2. **立即备份**
   - 虚拟机 → 备份
   - 点击【立即备份】
   - 保留备份至: 选择日期

3. **还原VM**
   - 虚拟机 → 备份 → 备份项
   - 选择恢复点
   - 点击【还原VM】

### 2. 创建VM映像

```bash
# 使用Azure CLI
# 1. 解除分配VM
az vm deallocate \
  --resource-group UPS-ResourceGroup \
  --name ups-server

# 2. 创建映像
az image create \
  --resource-group UPS-ResourceGroup \
  --name ups-image-$(date +%Y%m%d) \
  --source ups-server

# 3. 从映像创建新VM
az vm create \
  --resource-group UPS-ResourceGroup \
  --name ups-server-new \
  --image ups-image-20251105
```

### 3. 跨区域复制

```bash
# 使用Azure Site Recovery
# 1. 创建恢复服务保管库（目标区域）
# 2. 配置复制设置
# 3. 启用复制
# 4. 测试故障转移
```

### 4. 数据库备份

```bash
# 备份MySQL
docker exec ups-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} userservice > backup_$(date +%Y%m%d).sql

# 上传到Azure Blob Storage
az storage blob upload \
  --account-name yourstorageaccount \
  --container-name backups \
  --name mysql/backup_$(date +%Y%m%d).sql \
  --file backup_$(date +%Y%m%d).sql

# 自动化备份脚本
cat > /usr/local/bin/backup-ups.sh << 'EOF'
#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
docker exec ups-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} --all-databases > /tmp/mysql_${TIMESTAMP}.sql
az storage blob upload --account-name yourstorageaccount --container-name backups --name mysql/mysql_${TIMESTAMP}.sql --file /tmp/mysql_${TIMESTAMP}.sql
rm /tmp/mysql_${TIMESTAMP}.sql
EOF

chmod +x /usr/local/bin/backup-ups.sh

# 添加到crontab
echo "0 2 * * * /usr/local/bin/backup-ups.sh" | crontab -
```

---

## 成本优化

### 1. 使用预留实例

- 承诺1年或3年使用
- 节省高达72%费用
- 适合生产环境

### 2. 使用Spot VM（开发环境）

```bash
# 创建Spot VM（可节省90%）
az vm create \
  --resource-group UPS-ResourceGroup \
  --name ups-spot \
  --image UbuntuLTS \
  --priority Spot \
  --max-price 0.05 \
  --eviction-policy Deallocate
```

⚠️ **注意**：Spot VM可能被驱逐，不适合生产环境！

### 3. 自动启停

**使用Azure Automation**：
```powershell
# 停止VM (工作日18:00)
Stop-AzVM -ResourceGroupName "UPS-ResourceGroup" -Name "ups-server" -Force

# 启动VM (工作日09:00)
Start-AzVM -ResourceGroupName "UPS-ResourceGroup" -Name "ups-server"
```

### 4. 调整VM大小

定期审查使用情况：
- CPU < 20% → 考虑降级
- CPU > 80% → 考虑升级

### 5. 使用成本管理工具

1. Azure门户 → 成本管理 + 计费
2. 查看成本分析
3. 设置预算
4. 配置成本警报

### 定价估算

**开发环境** (B2s):
```
VM (B2s): ~$30/月
磁盘 (64GB 标准SSD): ~$5/月
公网IP (静态): ~$3/月
出站流量: ~$5/月
总计: ~$43/月
```

**生产环境** (D2s_v3 + App Gateway):
```
VM (D2s_v3): ~$70/月
磁盘 (128GB 高级SSD): ~$20/月
应用程序网关: ~$130/月
公网IP: ~$3/月
出站流量: ~$10/月
总计: ~$233/月
```

---

## 快速参考

### 常用命令

```bash
# 查看VM信息
az vm show --resource-group UPS-ResourceGroup --name ups-server

# 获取公网IP
az vm show -d --resource-group UPS-ResourceGroup --name ups-server --query publicIps -o tsv

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

| 服务 | 直接IP访问 | 应用程序网关 | 域名访问（HTTPS） |
|------|-----------|------------|------------------|
| API Gateway | `http://IP:8080` | `http://appgw-ip` | `https://api.domain.com` |
| Swagger UI | `http://IP:8080/swagger-ui.html` | `http://appgw-ip/swagger-ui.html` | `https://api.domain.com/swagger-ui.html` |

### Azure门户链接

- 虚拟机: https://portal.azure.com/#blade/HubsExtension/BrowseResource/resourceType/Microsoft.Compute%2FVirtualMachines
- 网络安全组: https://portal.azure.com/#blade/HubsExtension/BrowseResource/resourceType/Microsoft.Network%2FNetworkSecurityGroups
- DNS区域: https://portal.azure.com/#blade/HubsExtension/BrowseResource/resourceType/Microsoft.Network%2FdnsZones
- 监视: https://portal.azure.com/#blade/Microsoft_Azure_Monitoring/AzureMonitoringBrowseBlade

---

## 相关资源

### Azure官方文档
- 虚拟机文档: https://docs.microsoft.com/azure/virtual-machines
- 网络安全组: https://docs.microsoft.com/azure/virtual-network/network-security-groups-overview
- 应用程序网关: https://docs.microsoft.com/azure/application-gateway

### UPS项目
- GitHub: https://github.com/dctx479/UPS
- 文档: https://github.com/dctx479/UPS/tree/main/docs

### 学习资源
- Azure免费账户: https://azure.microsoft.com/free
- Microsoft Learn: https://docs.microsoft.com/learn/azure
- Azure架构中心: https://docs.microsoft.com/azure/architecture

---

## 技术支持

如有问题请联系：
- 📧 邮箱：b150w4942@163.com
- 📝 GitHub Issues：https://github.com/dctx479/UPS/issues

---

**更新时间**：2025-11-05
**适用版本**：UPS v1.0
**适用区域**：Azure全球所有区域（含Azure中国）
