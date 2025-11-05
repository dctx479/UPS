#!/bin/bash

# 用户画像系统 - Kubernetes部署脚本

echo "=========================================="
echo "用户画像系统 - Kubernetes 部署"
echo "=========================================="

# 创建命名空间
echo "1. 创建命名空间..."
kubectl apply -f redis.yaml

# 等待命名空间创建完成
sleep 2

# 部署数据库
echo "2. 部署 PostgreSQL..."
kubectl apply -f postgres.yaml

echo "3. 部署 MongoDB..."
kubectl apply -f mongodb.yaml

# 等待数据库就绪
echo "4. 等待数据库就绪..."
kubectl wait --for=condition=ready pod -l app=postgres -n userprofile --timeout=180s
kubectl wait --for=condition=ready pod -l app=mongodb -n userprofile --timeout=180s

# 部署微服务
echo "5. 部署用户服务..."
kubectl apply -f user-service.yaml

# 查看部署状态
echo "6. 查看部署状态..."
kubectl get pods -n userprofile
kubectl get services -n userprofile

echo "=========================================="
echo "部署完成！"
echo "=========================================="
echo ""
echo "查看服务状态:"
echo "  kubectl get all -n userprofile"
echo ""
echo "查看日志:"
echo "  kubectl logs -f deployment/user-service -n userprofile"
echo ""
echo "端口转发访问服务:"
echo "  kubectl port-forward svc/user-service 8081:8081 -n userprofile"
echo ""
