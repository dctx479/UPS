#!/bin/bash

# ====================================
# User Profile System - Deployment Script
# ====================================

set -e

echo "===================================="
echo "User Profile System Deployment"
echo "===================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if docker and docker-compose are installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}Error: docker-compose is not installed${NC}"
    exit 1
fi

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${YELLOW}Warning: .env file not found${NC}"
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo -e "${YELLOW}Please edit .env file with your credentials before proceeding${NC}"
    echo ""
    read -p "Press Enter to continue after editing .env, or Ctrl+C to exit..."
fi

# Function to show menu
show_menu() {
    echo ""
    echo "===================================="
    echo "Deployment Options:"
    echo "===================================="
    echo "1. Deploy all services (full stack)"
    echo "2. Deploy infrastructure only (Consul, Redis, MongoDB, MySQL)"
    echo "3. Deploy microservices only"
    echo "4. Stop all services"
    echo "5. View logs"
    echo "6. Check service status"
    echo "7. Clean up (remove containers and volumes)"
    echo "8. Exit"
    echo "===================================="
}

# Function to deploy all services
deploy_all() {
    echo -e "${GREEN}Deploying all services...${NC}"
    docker-compose up -d
    echo ""
    echo -e "${GREEN}Deployment completed!${NC}"
    show_status
}

# Function to deploy infrastructure only
deploy_infrastructure() {
    echo -e "${GREEN}Deploying infrastructure services...${NC}"
    docker-compose up -d consul redis mongodb mysql
    echo ""
    echo -e "${GREEN}Infrastructure deployment completed!${NC}"
    show_status
}

# Function to deploy microservices only
deploy_microservices() {
    echo -e "${GREEN}Deploying microservices...${NC}"
    docker-compose up -d gateway-service user-service profile-service behavior-service
    echo ""
    echo -e "${GREEN}Microservices deployment completed!${NC}"
    show_status
}

# Function to stop all services
stop_all() {
    echo -e "${YELLOW}Stopping all services...${NC}"
    docker-compose down
    echo -e "${GREEN}All services stopped${NC}"
}

# Function to view logs
view_logs() {
    echo ""
    echo "Which service logs do you want to view?"
    echo "1. All services"
    echo "2. Gateway"
    echo "3. User Service"
    echo "4. Profile Service"
    echo "5. Behavior Service"
    echo "6. Infrastructure (Consul, Redis, MongoDB, MySQL)"
    read -p "Enter choice [1-6]: " log_choice

    case $log_choice in
        1) docker-compose logs -f ;;
        2) docker-compose logs -f gateway-service ;;
        3) docker-compose logs -f user-service ;;
        4) docker-compose logs -f profile-service ;;
        5) docker-compose logs -f behavior-service ;;
        6) docker-compose logs -f consul redis mongodb mysql ;;
        *) echo -e "${RED}Invalid choice${NC}" ;;
    esac
}

# Function to check service status
show_status() {
    echo ""
    echo -e "${GREEN}Service Status:${NC}"
    echo "===================================="
    docker-compose ps
    echo ""
    echo -e "${GREEN}Service Health:${NC}"
    echo "===================================="
    echo "Consul UI: http://localhost:8500"
    echo "Gateway API: http://localhost:8080"
    echo "API Documentation: http://localhost:8080/swagger-ui.html"
    echo ""
}

# Function to clean up
cleanup() {
    echo -e "${RED}WARNING: This will remove all containers and volumes!${NC}"
    read -p "Are you sure? (yes/no): " confirm
    if [ "$confirm" == "yes" ]; then
        echo -e "${YELLOW}Cleaning up...${NC}"
        docker-compose down -v
        echo -e "${GREEN}Cleanup completed${NC}"
    else
        echo "Cleanup cancelled"
    fi
}

# Main menu loop
while true; do
    show_menu
    read -p "Enter choice [1-8]: " choice

    case $choice in
        1) deploy_all ;;
        2) deploy_infrastructure ;;
        3) deploy_microservices ;;
        4) stop_all ;;
        5) view_logs ;;
        6) show_status ;;
        7) cleanup ;;
        8)
            echo "Exiting..."
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            ;;
    esac
done
