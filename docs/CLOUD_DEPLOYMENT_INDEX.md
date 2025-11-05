# 云服务器部署完整指南索引

本文档提供UPS系统在各大云服务商的部署指南快速索引。

---

## 📚 文档体系

### 通用文档
- **[公网访问完整指南](./PUBLIC_ACCESS_GUIDE.md)** - 详细的技术配置文档，适用于所有云服务商
- **[快速公网配置](./QUICK_PUBLIC_ACCESS.md)** - 一键配置脚本使用说明
- **[部署指南](./DEPLOYMENT.md)** - Docker Compose生产环境部署

### 云服务商专属指南

#### 国内云服务商
- **[腾讯云轻量应用服务器](./TENCENT_CLOUD_GUIDE.md)** - 腾讯云Lighthouse
- **[阿里云ECS](./ALIYUN_GUIDE.md)** - 阿里云弹性计算服务
- **[华为云ECS](./HUAWEI_CLOUD_GUIDE.md)** - 华为云弹性云服务器

#### 国际云服务商
- **[AWS EC2](./AWS_GUIDE.md)** - Amazon Web Services Elastic Compute Cloud
- **[Azure虚拟机](./AZURE_GUIDE.md)** - Microsoft Azure Virtual Machines
- **[Google Cloud Compute Engine](./GCP_GUIDE.md)** - Google Cloud Platform

---

## 🎯 选择指南

### 我应该看哪个文档？

#### 使用腾讯云 → [腾讯云指南](./TENCENT_CLOUD_GUIDE.md)
- ✅ 腾讯云轻量应用服务器
- ✅ 防火墙控制台配置详解
- ✅ WebShell连接方式
- ✅ 完整的故障排查

#### 使用阿里云 → [阿里云指南](./ALIYUN_GUIDE.md)
- ✅ 阿里云ECS云服务器
- ✅ 安全组控制台配置详解
- ✅ 阿里云镜像加速配置
- ✅ Workbench远程连接
- ✅ 成本优化建议

#### 使用华为云 → [华为云指南](./HUAWEI_CLOUD_GUIDE.md)
- ✅ 华为云弹性云服务器
- ✅ 安全组快速模板
- ✅ 弹性公网IP配置
- ✅ CloudShell连接方式

#### 使用AWS → [AWS指南](./AWS_GUIDE.md)
- ✅ Amazon EC2实例创建
- ✅ 安全组和网络配置
- ✅ Elastic IP配置
- ✅ Application Load Balancer
- ✅ Route 53 DNS配置
- ✅ CloudWatch监控

#### 使用Azure → [Azure指南](./AZURE_GUIDE.md)
- ✅ Azure虚拟机创建
- ✅ 网络安全组配置
- ✅ 静态公共IP配置
- ✅ 应用程序网关
- ✅ Azure DNS配置
- ✅ Azure Monitor监控

#### 使用Google Cloud → [GCP指南](./GCP_GUIDE.md)
- ✅ Compute Engine实例创建
- ✅ VPC防火墙规则配置
- ✅ 静态外部IP配置
- ✅ Cloud Load Balancing
- ✅ Cloud DNS配置
- ✅ Cloud Monitoring

#### 使用其他云服务商 → [通用指南](./PUBLIC_ACCESS_GUIDE.md)
- ✅ 详细的Nginx配置
- ✅ Let's Encrypt HTTPS配置
- ✅ 防火墙通用配置方法
- ✅ 性能优化和安全加固

#### 想要快速配置 → [快速配置指南](./QUICK_PUBLIC_ACCESS.md)
- ✅ 一键配置脚本使用方法
- ✅ 手动配置简化步骤
- ✅ 常见问题快速解答

---

## 🚀 快速对比

| 特性 | 腾讯云 | 阿里云 | 华为云 | AWS | Azure | GCP | 通用指南 |
|------|-------|-------|-------|-----|-------|-----|---------|
| 防火墙配置 | 控制台图形化 | 控制台图形化 | 控制台图形化 | 安全组 | NSG | VPC防火墙 | 命令行配置 |
| 连接方式 | WebShell | Workbench | CloudShell | SSH/Session Manager | Bastion/SSH | Cloud Shell/SSH | SSH |
| 镜像加速 | 通用 | 阿里云专属 | 通用 | 通用 | 通用 | 通用 | 通用 |
| 域名服务 | 腾讯云DNS | 阿里云DNS | 华为云DNS | Route 53 | Azure DNS | Cloud DNS | 第三方DNS |
| 负载均衡 | CLB | SLB | ELB | ALB/NLB | App Gateway | Cloud LB | Nginx |
| 监控服务 | 云监控 | 云监控 | CES | CloudWatch | Azure Monitor | Cloud Monitoring | Prometheus |
| 文档完整度 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

---

## 📋 配置步骤对比

### 第一步：购买服务器

| 云服务商 | 推荐配置 | 参考价格 |
|---------|---------|---------|
| **腾讯云** | 2核4GB, 5M带宽 | ~75元/月 |
| **阿里云** | ecs.t6-c2m4, 5M带宽 | ~80元/月 |
| **华为云** | s6.large.2, 5M带宽 | ~70元/月 |
| **AWS** | t3.medium, 5Mbps | ~$38/月 (~¥280) |
| **Azure** | B2s, 5Mbps | ~$43/月 (~¥315) |
| **GCP** | e2-medium, 5Mbps | ~$38/月 (~¥280) |

### 第二步：配置防火墙/安全组

所有云服务商都需要开放以下端口：

| 端口 | 协议 | 说明 | 是否必须 |
|-----|------|------|---------|
| 22 | TCP | SSH远程管理 | ✅ 必须 |
| 80 | TCP | HTTP访问 | ✅ 必须 |
| 443 | TCP | HTTPS访问 | ✅ 推荐 |
| 8080 | TCP | API Gateway直接访问 | ✅ 必须 |

❌ **不要开放**：3306, 27017, 6379, 8081-8083

### 第三步：部署系统

**所有云服务商都使用相同的部署命令**：

```bash
# 1. 安装Docker
curl -fsSL https://get.docker.com | bash

# 2. 克隆项目
git clone https://github.com/dctx479/UPS.git
cd UPS

# 3. 一键部署
chmod +x quick-start.sh
./quick-start.sh
```

### 第四步：配置公网访问

**方式1：直接IP访问**（所有云服务商通用）
```
http://服务器公网IP:8080
```

**方式2：域名+HTTPS**（推荐，所有云服务商通用）
```bash
# 使用自动配置脚本
sudo ./setup-public-access.sh
```

---

## 🔍 详细功能对比

### 腾讯云特点
✅ WebShell直接登录，无需SSH客户端
✅ 防火墙规则简单直观
✅ 轻量应用服务器价格优惠
✅ 域名备案流程便捷

### 阿里云特点
✅ Workbench功能强大
✅ 镜像加速服务稳定
✅ 预留实例券节省成本
✅ 云监控功能完善
✅ 生态系统最完整

### 华为云特点
✅ CloudShell便捷登录
✅ 安全组快速模板
✅ 性价比较高
✅ 国内访问速度快

### AWS特点
✅ 全球基础设施最完善
✅ 服务种类最丰富
✅ Savings Plans节省成本
✅ IAM权限管理强大
✅ 成熟的生态系统

### Azure特点
✅ 与Microsoft产品集成好
✅ 企业级功能完善
✅ Hybrid Cloud支持强
✅ Security Center安全防护
✅ 预留实例最高节省72%

### GCP特点
✅ 网络性能优秀
✅ 大数据和AI能力强
✅ 承诺使用折扣灵活
✅ 抢占式VM性价比高
✅ 开源友好

---

## 💡 最佳实践

### 开发测试环境
```
服务器：2核4GB
带宽：3Mbps按量付费
系统：Ubuntu 22.04
成本：~50元/月
```

### 生产环境
```
服务器：4核8GB
带宽：5Mbps固定带宽
系统：Ubuntu 22.04 LTS
域名：已备案
HTTPS：Let's Encrypt
成本：~150元/月
```

### 高可用环境
```
服务器：2台 4核8GB（主备）
负载均衡：SLB/CLB
带宽：10Mbps
数据库：RDS/云数据库
成本：~500元/月
```

---

## 🆘 遇到问题？

### 问题诊断流程

1. **确定使用的云服务商** → 查看对应的专属指南
2. **检查防火墙/安全组** → 确认端口已开放
3. **测试本地访问** → `curl localhost:8080/actuator/health`
4. **测试公网访问** → `curl http://服务器IP:8080/actuator/health`
5. **查看服务日志** → `docker-compose logs -f`

### 常见问题快速链接

| 问题 | 腾讯云 | 阿里云 | 华为云 | AWS | Azure | GCP | 通用 |
|-----|-------|-------|-------|-----|-------|-----|-----|
| 无法访问服务 | [排查](./TENCENT_CLOUD_GUIDE.md#问题1无法通过公网ip访问) | [排查](./ALIYUN_GUIDE.md#问题1无法通过公网ip访问) | [排查](./HUAWEI_CLOUD_GUIDE.md#问题1无法通过公网ip访问) | [排查](./AWS_GUIDE.md#问题1无法通过公网ip访问) | [排查](./AZURE_GUIDE.md#问题1无法通过公网ip访问) | [排查](./GCP_GUIDE.md#问题1无法通过外部ip访问) | [排查](./PUBLIC_ACCESS_GUIDE.md#问题1无法访问服务) |
| HTTPS证书失败 | [解决](./TENCENT_CLOUD_GUIDE.md#问题2https证书获取失败) | [解决](./ALIYUN_GUIDE.md#问题2https证书获取失败) | [解决](./HUAWEI_CLOUD_GUIDE.md#问题2https证书获取失败) | [解决](./AWS_GUIDE.md#问题4https证书获取失败) | [解决](./AZURE_GUIDE.md#问题4https证书获取失败) | [解决](./GCP_GUIDE.md#问题4https证书获取失败) | [解决](./PUBLIC_ACCESS_GUIDE.md#问题2ssl证书问题) |
| 服务启动失败 | [解决](./TENCENT_CLOUD_GUIDE.md#问题3服务启动失败) | [解决](./ALIYUN_GUIDE.md#问题3容器启动失败) | [解决](./HUAWEI_CLOUD_GUIDE.md#常见问题排查) | [解决](./AWS_GUIDE.md#问题3实例状态检查失败) | [解决](./AZURE_GUIDE.md#问题3磁盘空间不足) | [解决](./GCP_GUIDE.md#问题3磁盘空间不足) | [解决](./PUBLIC_ACCESS_GUIDE.md#问题3服务启动失败) |

---

## 🛠️ 自动化工具

### setup-public-access.sh

**一键配置脚本**，支持所有云服务商：

```bash
cd ~/UPS
sudo ./setup-public-access.sh

# 选项1：使用域名 + HTTPS (推荐)
# 选项2：仅使用公网IP + HTTP
```

**功能**：
- ✅ 自动安装Nginx
- ✅ 自动配置反向代理
- ✅ 自动获取SSL证书
- ✅ 自动配置防火墙
- ✅ 自动测试访问

详细使用说明：[快速配置指南](./QUICK_PUBLIC_ACCESS.md)

---

## 📞 技术支持

### 获取帮助

1. **查看文档**：根据云服务商选择对应指南
2. **搜索Issues**：https://github.com/dctx479/UPS/issues
3. **提交Issue**：详细描述问题（云服务商、系统版本、错误日志）
4. **邮件联系**：b150w4942@163.com

### 提供信息

提问时请提供：
- [ ] 使用的云服务商
- [ ] 服务器配置信息
- [ ] 操作系统版本
- [ ] 错误日志/截图
- [ ] 已尝试的解决方法

---

## 📚 相关资源

### 官方文档
- 腾讯云：https://cloud.tencent.com/document/product/1207
- 阿里云：https://help.aliyun.com/product/25365.html
- 华为云：https://support.huaweicloud.com/ecs/
- AWS：https://docs.aws.amazon.com/ec2
- Azure：https://docs.microsoft.com/azure/virtual-machines
- Google Cloud：https://cloud.google.com/compute/docs

### UPS项目
- GitHub：https://github.com/dctx479/UPS
- 文档：https://github.com/dctx479/UPS/tree/main/docs

---

## 🔄 更新日志

| 日期 | 更新内容 |
|------|---------|
| 2025-11-05 | 创建云服务商部署指南索引 |
| 2025-11-05 | 新增腾讯云专属指南 |
| 2025-11-05 | 新增阿里云专属指南 |
| 2025-11-05 | 新增华为云专属指南 |
| 2025-11-05 | 新增一键配置脚本 |
| 2025-11-05 | 新增AWS EC2专属指南 |
| 2025-11-05 | 新增Azure虚拟机专属指南 |
| 2025-11-05 | 新增Google Cloud Compute Engine专属指南 |

---

**最后更新**：2025-11-05
**文档版本**：v2.0
**适用系统**：UPS v1.0
