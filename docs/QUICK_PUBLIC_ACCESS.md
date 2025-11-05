# 快速配置公网访问

本文档提供快速配置服务器公网访问的步骤说明。

## 一键配置脚本

我们提供了自动配置脚本，可以快速设置Nginx反向代理和HTTPS。

### 使用方法

```bash
# 1. 确保UPS已部署
cd ~/UPS
./quick-start.sh

# 2. 运行公网访问配置脚本
chmod +x setup-public-access.sh
sudo ./setup-public-access.sh
```

### 脚本功能

配置脚本会自动完成：

1. ✅ 检查Docker和UPS服务状态
2. ✅ 安装并配置Nginx反向代理
3. ✅ 配置HTTPS (Let's Encrypt)
4. ✅ 配置防火墙规则
5. ✅ 设置SSL证书自动续期
6. ✅ 测试公网访问

### 配置选项

#### 选项1：使用域名 + HTTPS (推荐)

```bash
sudo ./setup-public-access.sh

# 按提示输入:
# 1) 使用域名 + HTTPS (推荐)
# 域名: api.example.com
# 邮箱: your@email.com
```

**前置条件**：
- 已购买域名
- 域名已解析到服务器IP
- 服务器80/443端口可访问

**完成后访问**：
```
https://api.example.com
https://api.example.com/swagger-ui.html
```

#### 选项2：仅使用公网IP + HTTP

```bash
sudo ./setup-public-access.sh

# 按提示输入:
# 2) 仅使用公网IP + HTTP
```

**完成后访问**：
```
http://服务器IP
http://服务器IP/swagger-ui.html
```

## 手动配置步骤

如果不使用自动脚本，可以手动配置：

### 1. 安装Nginx

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

### 2. 配置Nginx反向代理

创建配置文件：
```bash
sudo nano /etc/nginx/sites-available/ups
```

添加内容：
```nginx
server {
    listen 80;
    server_name your-domain.com;  # 或使用服务器IP

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置：
```bash
sudo ln -s /etc/nginx/sites-available/ups /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 3. 配置HTTPS (可选但推荐)

```bash
# 安装Certbot
sudo apt install certbot python3-certbot-nginx -y

# 获取SSL证书
sudo certbot --nginx -d your-domain.com

# 测试自动续期
sudo certbot renew --dry-run
```

### 4. 配置防火墙

```bash
# Ubuntu/Debian (ufw)
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# CentOS/RHEL (firewalld)
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 5. 云服务商安全组

登录云控制台，添加入站规则：

| 端口 | 协议 | 源地址 | 说明 |
|-----|------|--------|------|
| 80 | TCP | 0.0.0.0/0 | HTTP |
| 443 | TCP | 0.0.0.0/0 | HTTPS |
| 22 | TCP | 你的IP | SSH |

## 验证访问

### 1. 测试HTTP访问

```bash
curl http://服务器IP/actuator/health
```

应返回：
```json
{"status":"UP"}
```

### 2. 测试HTTPS访问

```bash
curl https://your-domain.com/actuator/health
```

### 3. 浏览器测试

访问：
- Swagger UI: `https://your-domain.com/swagger-ui.html`
- API文档: `https://your-domain.com/v3/api-docs`

## 常见问题

### 问题1：无法访问

**检查步骤**：
```bash
# 1. 检查服务运行
docker-compose ps

# 2. 检查端口监听
sudo netstat -tlnp | grep 8080

# 3. 检查Nginx状态
sudo systemctl status nginx

# 4. 检查防火墙
sudo ufw status
```

### 问题2：HTTPS证书获取失败

**可能原因**：
- 域名未解析到服务器
- 80端口被占用或被防火墙阻止
- DNS解析未生效

**解决方法**：
```bash
# 验证域名解析
nslookup your-domain.com

# 检查80端口
sudo netstat -tlnp | grep :80

# 查看Certbot日志
sudo cat /var/log/letsencrypt/letsencrypt.log
```

### 问题3：502 Bad Gateway

**原因**：Docker服务未运行

**解决方法**：
```bash
cd ~/UPS
docker-compose ps
docker-compose up -d
```

## 监控和维护

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

### 手动续期SSL证书

```bash
sudo certbot renew
sudo systemctl reload nginx
```

## 性能优化

### Nginx优化

编辑 `/etc/nginx/nginx.conf`：
```nginx
worker_processes auto;

events {
    worker_connections 2048;
}

http {
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;

    keepalive_timeout 65;
    client_max_body_size 10M;
}
```

重启Nginx：
```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 安全建议

1. ✅ 使用HTTPS (强制)
2. ✅ 限制SSH访问IP
3. ✅ 安装Fail2Ban防暴力破解
4. ✅ 定期更新系统和软件
5. ✅ 配置日志监控和告警

## 更多信息

详细配置请参考：
- [公网访问完整指南](./PUBLIC_ACCESS_GUIDE.md)
- [安全配置指南](../SECURITY.md)
- [部署指南](./DEPLOYMENT.md)

## 支持

如有问题请联系：
- 邮箱：b150w4942@163.com
- GitHub Issues：https://github.com/dctx479/UPS/issues
