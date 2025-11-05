#!/bin/bash

# ====================================
# Quick Start Script
# Deploy User Profiling System in 5 minutes
# ====================================

set -e

echo "=========================================="
echo "  User Profiling System - Quick Start"
echo "=========================================="
echo ""

# Check prerequisites
echo "[1/5] Checking prerequisites..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose is not installed. Please install docker-compose first."
    exit 1
fi

echo "✅ Docker and docker-compose are installed"
echo ""

# Setup environment file
echo "[2/5] Setting up environment..."
if [ ! -f .env ]; then
    echo "⚠️  Creating .env file with secure random passwords..."

    # Generate strong random passwords
    MYSQL_ROOT_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-24)
    MYSQL_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-24)
    MONGO_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-24)
    REDIS_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-24)
    JWT_SECRET=$(openssl rand -base64 48 | tr -d "=+/" | cut -c1-48)

    cat > .env << EOF
# ⚠️ AUTO-GENERATED CREDENTIALS - SAVE THESE SECURELY!
# Database Credentials
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
MYSQL_USER=userservice
MYSQL_PASSWORD=${MYSQL_PASSWORD}

MONGO_USERNAME=admin
MONGO_PASSWORD=${MONGO_PASSWORD}

REDIS_PASSWORD=${REDIS_PASSWORD}

# JWT Secret (48 characters)
JWT_SECRET=${JWT_SECRET}
EOF

    echo "✅ Environment file created with secure random passwords"
    echo "⚠️  IMPORTANT: Credentials saved to .env file"
    echo "   Please backup this file securely!"
else
    echo "✅ Environment file already exists"
fi
echo ""

# Pull images
echo "[3/5] Pulling Docker images..."
docker-compose pull consul redis mongodb mysql
echo "✅ Images pulled successfully"
echo ""

# Build services
echo "[4/5] Building microservices..."
echo "This may take 5-10 minutes on first run..."
docker-compose build
echo "✅ Build completed"
echo ""

# Start services
echo "[5/5] Starting all services..."
docker-compose up -d
echo ""

# Wait for services to be healthy
echo "Waiting for services to be ready..."
sleep 30

# Show status
echo ""
echo "=========================================="
echo "  ✅ Deployment Successful!"
echo "=========================================="
echo ""
echo "Service URLs:"
echo "  • API Gateway:    http://localhost:8080"
echo "  • Consul UI:      http://localhost:8500"
echo "  • User Service:   http://localhost:8081"
echo "  • Profile Service: http://localhost:8082"
echo "  • Behavior Service: http://localhost:8083"
echo ""
echo "Service Status:"
docker-compose ps
echo ""
echo "Next Steps:"
echo "  1. Check service health: docker-compose ps"
echo "  2. View logs: docker-compose logs -f"
echo "  3. Access API documentation: http://localhost:8080/swagger-ui.html"
echo ""
echo "To stop all services: docker-compose down"
echo "=========================================="
