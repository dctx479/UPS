#!/bin/bash

# ====================================
# UPS 公网访问配置脚本
# 自动配置Nginx反向代理和HTTPS
# ====================================

set -e

echo "=========================================="
echo "  UPS 公网访问配置向导"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}错误: 请使用root权限运行此脚本${NC}"
    echo "使用方法: sudo ./setup-public-access.sh"
    exit 1
fi

# 检查操作系统
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
else
    echo -e "${RED}无法检测操作系统${NC}"
    exit 1
fi

echo -e "${GREEN}检测到操作系统: $OS${NC}"
echo ""

# 步骤1: 询问配置方式
echo "请选择配置方式:"
echo "1) 使用域名 + HTTPS (推荐)"
echo "2) 仅使用公网IP + HTTP"
echo ""
read -p "请输入选项 [1-2]: " CONFIG_TYPE

if [ "$CONFIG_TYPE" = "1" ]; then
    # 域名配置
    read -p "请输入您的域名 (例如: api.example.com): " DOMAIN_NAME

    if [ -z "$DOMAIN_NAME" ]; then
        echo -e "${RED}错误: 域名不能为空${NC}"
        exit 1
    fi

    # 验证域名格式
    if ! [[ "$DOMAIN_NAME" =~ ^[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\.[a-zA-Z]{2,}$ ]]; then
        echo -e "${YELLOW}警告: 域名格式可能不正确${NC}"
        read -p "是否继续? [y/N]: " CONTINUE
        if [ "$CONTINUE" != "y" ] && [ "$CONTINUE" != "Y" ]; then
            exit 1
        fi
    fi

    read -p "请输入您的邮箱 (用于SSL证书): " EMAIL

    if [ -z "$EMAIL" ]; then
        echo -e "${RED}错误: 邮箱不能为空${NC}"
        exit 1
    fi

    USE_HTTPS=true
else
    USE_HTTPS=false
fi

# 步骤2: 检查Docker服务
echo ""
echo "=========================================="
echo "[1/6] 检查Docker服务"
echo "=========================================="

if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker未安装${NC}"
    echo "请先安装Docker: curl -fsSL https://get.docker.com | bash"
    exit 1
fi

if ! docker ps &> /dev/null; then
    echo -e "${RED}Docker服务未运行${NC}"
    echo "启动Docker服务..."
    systemctl start docker
    systemctl enable docker
fi

echo -e "${GREEN}✓ Docker服务正常${NC}"

# 检查UPS服务
if ! docker ps | grep -q "ups-gateway"; then
    echo -e "${YELLOW}警告: UPS服务未运行${NC}"
    read -p "是否现在启动UPS服务? [Y/n]: " START_UPS
    if [ "$START_UPS" != "n" ] && [ "$START_UPS" != "N" ]; then
        cd ~/UPS 2>/dev/null || cd /root/UPS 2>/dev/null || {
            echo -e "${RED}找不到UPS目录${NC}"
            echo "请先部署UPS系统"
            exit 1
        }
        docker-compose up -d
        echo "等待服务启动..."
        sleep 30
    fi
fi

# 步骤3: 安装Nginx
echo ""
echo "=========================================="
echo "[2/6] 安装Nginx"
echo "=========================================="

if command -v nginx &> /dev/null; then
    echo -e "${GREEN}✓ Nginx已安装${NC}"
else
    echo "正在安装Nginx..."
    if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        apt update
        apt install nginx -y
    elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ]; then
        yum install nginx -y
    else
        echo -e "${RED}不支持的操作系统: $OS${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Nginx安装完成${NC}"
fi

# 启动Nginx
systemctl start nginx
systemctl enable nginx

# 步骤4: 配置Nginx
echo ""
echo "=========================================="
echo "[3/6] 配置Nginx反向代理"
echo "=========================================="

if [ "$USE_HTTPS" = true ]; then
    # HTTPS配置
    NGINX_CONFIG="/etc/nginx/sites-available/ups"

    cat > $NGINX_CONFIG << EOF
server {
    listen 80;
    server_name $DOMAIN_NAME;

    # 临时允许HTTP用于Let's Encrypt验证
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    location / {
        return 301 https://\$server_name\$request_uri;
    }
}

server {
    listen 80;
    server_name $DOMAIN_NAME;

    client_max_body_size 10M;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
EOF

else
    # HTTP配置
    SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')
    NGINX_CONFIG="/etc/nginx/sites-available/ups"

    cat > $NGINX_CONFIG << EOF
server {
    listen 80;
    server_name $SERVER_IP;

    client_max_body_size 10M;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
EOF
fi

# 创建软链接
if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
    ln -sf $NGINX_CONFIG /etc/nginx/sites-enabled/ups
    # 删除默认配置
    rm -f /etc/nginx/sites-enabled/default
else
    # CentOS/RHEL
    mkdir -p /etc/nginx/conf.d
    ln -sf $NGINX_CONFIG /etc/nginx/conf.d/ups.conf
fi

# 测试Nginx配置
if nginx -t; then
    echo -e "${GREEN}✓ Nginx配置正确${NC}"
    systemctl reload nginx
else
    echo -e "${RED}✗ Nginx配置错误${NC}"
    exit 1
fi

# 步骤5: 配置HTTPS
if [ "$USE_HTTPS" = true ]; then
    echo ""
    echo "=========================================="
    echo "[4/6] 配置HTTPS证书"
    echo "=========================================="

    # 安装Certbot
    if ! command -v certbot &> /dev/null; then
        echo "正在安装Certbot..."
        if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
            apt install certbot python3-certbot-nginx -y
        elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ]; then
            yum install certbot python3-certbot-nginx -y
        fi
        echo -e "${GREEN}✓ Certbot安装完成${NC}"
    else
        echo -e "${GREEN}✓ Certbot已安装${NC}"
    fi

    # 获取SSL证书
    echo ""
    echo "正在获取SSL证书..."
    echo -e "${YELLOW}注意: 请确保域名已正确解析到此服务器!${NC}"
    echo ""

    certbot --nginx -d $DOMAIN_NAME --non-interactive --agree-tos --email $EMAIL --redirect

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ SSL证书获取成功${NC}"

        # 配置自动续期
        echo "配置证书自动续期..."
        (crontab -l 2>/dev/null; echo "0 3 * * * certbot renew --quiet --post-hook 'systemctl reload nginx'") | crontab -
        echo -e "${GREEN}✓ 自动续期已配置${NC}"
    else
        echo -e "${RED}✗ SSL证书获取失败${NC}"
        echo "可能的原因:"
        echo "1. 域名未正确解析到此服务器"
        echo "2. 防火墙阻止了80/443端口"
        echo "3. Nginx配置错误"
        echo ""
        echo "您仍可以使用HTTP访问系统"
    fi
fi

# 步骤6: 配置防火墙
echo ""
echo "=========================================="
echo "[5/6] 配置防火墙"
echo "=========================================="

if command -v ufw &> /dev/null; then
    echo "配置UFW防火墙..."
    ufw --force enable
    ufw allow 22/tcp
    ufw allow 80/tcp
    if [ "$USE_HTTPS" = true ]; then
        ufw allow 443/tcp
    fi
    ufw status
    echo -e "${GREEN}✓ UFW防火墙配置完成${NC}"
elif command -v firewall-cmd &> /dev/null; then
    echo "配置firewalld防火墙..."
    systemctl start firewalld
    systemctl enable firewalld
    firewall-cmd --permanent --add-service=ssh
    firewall-cmd --permanent --add-service=http
    if [ "$USE_HTTPS" = true ]; then
        firewall-cmd --permanent --add-service=https
    fi
    firewall-cmd --reload
    firewall-cmd --list-all
    echo -e "${GREEN}✓ firewalld防火墙配置完成${NC}"
else
    echo -e "${YELLOW}警告: 未检测到防火墙，请手动配置${NC}"
fi

# 步骤7: 测试访问
echo ""
echo "=========================================="
echo "[6/6] 测试访问"
echo "=========================================="

sleep 3

if [ "$USE_HTTPS" = true ]; then
    TEST_URL="https://$DOMAIN_NAME/actuator/health"
else
    SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')
    TEST_URL="http://$SERVER_IP/actuator/health"
fi

echo "测试URL: $TEST_URL"
if curl -s -f -k "$TEST_URL" > /dev/null; then
    echo -e "${GREEN}✓ 服务访问正常${NC}"
else
    echo -e "${YELLOW}⚠ 无法访问服务，请检查配置${NC}"
fi

# 显示结果
echo ""
echo "=========================================="
echo "  ✅ 配置完成!"
echo "=========================================="
echo ""

if [ "$USE_HTTPS" = true ]; then
    echo -e "${GREEN}您的系统已配置HTTPS公网访问${NC}"
    echo ""
    echo "访问地址:"
    echo "  • API Gateway:    https://$DOMAIN_NAME"
    echo "  • Swagger UI:     https://$DOMAIN_NAME/swagger-ui.html"
    echo "  • 健康检查:       https://$DOMAIN_NAME/actuator/health"
    echo ""
    echo "SSL证书:"
    echo "  • 颁发者: Let's Encrypt"
    echo "  • 有效期: 90天"
    echo "  • 自动续期: 已配置"
else
    SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')
    echo -e "${GREEN}您的系统已配置HTTP公网访问${NC}"
    echo ""
    echo "访问地址:"
    echo "  • API Gateway:    http://$SERVER_IP"
    echo "  • Swagger UI:     http://$SERVER_IP/swagger-ui.html"
    echo "  • 健康检查:       http://$SERVER_IP/actuator/health"
    echo ""
    echo -e "${YELLOW}建议: 配置域名和HTTPS以提高安全性${NC}"
fi

echo ""
echo "下一步:"
echo "  1. 测试API访问: curl $TEST_URL"
echo "  2. 查看Nginx日志: tail -f /var/log/nginx/access.log"
echo "  3. 查看服务日志: docker-compose logs -f"
echo ""

if [ "$USE_HTTPS" = true ]; then
    echo "注意事项:"
    echo "  • SSL证书将在到期前自动续期"
    echo "  • 定期检查证书状态: certbot certificates"
    echo ""
fi

echo "文档参考:"
echo "  • 公网访问配置: docs/PUBLIC_ACCESS_GUIDE.md"
echo "  • 安全配置指南: SECURITY.md"
echo "  • 部署指南: docs/DEPLOYMENT.md"
echo ""
echo "=========================================="
