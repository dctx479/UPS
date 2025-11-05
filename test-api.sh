#!/bin/bash

# User Profiling System API 测试脚本
# 使用方法: ./test-api.sh <服务器IP>

SERVER_IP=${1:-localhost}
BASE_URL="http://${SERVER_IP}:8080"

echo "=========================================="
echo "  User Profiling System API 测试"
echo "  服务器: $BASE_URL"
echo "=========================================="
echo ""

# 1. 注册用户
echo "[1/6] 注册新用户..."
REGISTER_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "apitest",
    "password": "Test@123456",
    "email": "apitest@example.com",
    "name": "API测试用户"
  }')

echo "注册响应: $REGISTER_RESPONSE"
USER_ID=$(echo $REGISTER_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
echo "用户ID: $USER_ID"
echo ""

# 等待用户创建完成
sleep 2

# 2. 用户登录
echo "[2/6] 用户登录..."
LOGIN_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "apitest",
    "password": "Test@123456"
  }')

echo "登录响应: $LOGIN_RESPONSE"
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | grep -o ':[^"]*' | sed 's/://; s/"//g')
echo "访问令牌: ${TOKEN:0:50}..."
echo ""

if [ -z "$TOKEN" ]; then
    echo "❌ 登录失败，无法获取 Token"
    exit 1
fi

# 3. 获取用户信息
echo "[3/6] 获取用户信息..."
curl -s ${BASE_URL}/api/users/${USER_ID} \
  -H "Authorization: Bearer ${TOKEN}" | jq '.'
echo ""

# 4. 查看用户画像（可能还未初始化）
echo "[4/6] 查看用户画像..."
PROFILE_RESPONSE=$(curl -s ${BASE_URL}/api/profiles/user/${USER_ID} \
  -H "Authorization: Bearer ${TOKEN}")
echo $PROFILE_RESPONSE | jq '.'
echo ""

# 5. 创建用户标签
echo "[5/6] 创建用户标签..."
curl -s -X POST ${BASE_URL}/api/tags \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": ${USER_ID},
    \"tagName\": \"测试标签\",
    \"category\": \"测试分类\",
    \"source\": \"MANUAL\",
    \"weight\": 0.8
  }" | jq '.'
echo ""

# 6. 查看用户标签
echo "[6/6] 查看用户标签..."
curl -s ${BASE_URL}/api/tags/user/${USER_ID} \
  -H "Authorization: Bearer ${TOKEN}" | jq '.'
echo ""

echo "=========================================="
echo "  ✅ API 测试完成"
echo "=========================================="
echo ""
echo "其他可用接口："
echo "  • Swagger UI:  ${BASE_URL}/swagger-ui.html"
echo "  • Consul UI:   http://${SERVER_IP}:8500"
echo "  • 健康检查:    ${BASE_URL}/actuator/health"
echo ""
