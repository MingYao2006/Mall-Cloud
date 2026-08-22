#!/bin/bash

# Redis 哨兵集群部署脚本
# 用途：快速部署 Redis 哨兵集群（一主两从三哨兵）

echo "========================================="
echo "Redis 哨兵集群部署脚本"
echo "========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Docker 是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}错误：Docker 未安装，请先安装 Docker${NC}"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}错误：Docker Compose 未安装，请先安装 Docker Compose${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker 环境检查通过${NC}"
}

# 创建配置文件目录
create_directories() {
    echo ""
    echo "创建配置文件目录..."
    mkdir -p redis-config-sentinel/master
    mkdir -p redis-config-sentinel/slave1
    mkdir -p redis-config-sentinel/slave2
    mkdir -p redis-config-sentinel/sentinel1
    mkdir -p redis-config-sentinel/sentinel2
    mkdir -p redis-config-sentinel/sentinel3
    echo -e "${GREEN}✓ 配置文件目录创建完成${NC}"
}

# 启动 Redis 哨兵集群
start_cluster() {
    echo ""
    echo "启动 Redis 哨兵集群..."
    docker-compose -f docker-redis-sentinel.yml up -d
    
    echo ""
    echo -e "${GREEN}✓ Redis 哨兵集群启动完成${NC}"
}

# 等待服务就绪
wait_for_services() {
    echo ""
    echo "等待服务启动..."
    sleep 5
    
    # 检查主节点
    echo "检查主节点状态..."
    docker exec redis-master redis-cli ping
    
    # 检查从节点1
    echo "检查从节点1状态..."
    docker exec redis-slave1 redis-cli ping
    
    # 检查从节点2
    echo "检查从节点2状态..."
    docker exec redis-slave2 redis-cli ping
    
    # 检查哨兵节点
    echo "检查哨兵节点状态..."
    docker exec redis-sentinel1 redis-cli -p 26379 ping
    docker exec redis-sentinel2 redis-cli -p 26379 ping
    docker exec redis-sentinel3 redis-cli -p 26379 ping
    
    echo -e "${GREEN}✓ 所有服务启动成功${NC}"
}

# 显示集群信息
show_cluster_info() {
    echo ""
    echo "========================================="
    echo "Redis 哨兵集群信息"
    echo "========================================="
    echo ""
    echo "主节点："
    echo "  - 地址：localhost:6379"
    echo "  - 容器：redis-master"
    echo ""
    echo "从节点："
    echo "  - 从节点1：localhost:6380 (容器：redis-slave1)"
    echo "  - 从节点2：localhost:6381 (容器：redis-slave2)"
    echo ""
    echo "哨兵节点："
    echo "  - 哨兵1：localhost:26379 (容器：redis-sentinel1)"
    echo "  - 哨兵2：localhost:26380 (容器：redis-sentinel2)"
    echo "  - 哨兵3：localhost:26381 (容器：redis-sentinel3)"
    echo ""
    echo "管理工具："
    echo "  - Redis Commander：http://localhost:8081"
    echo ""
    echo "========================================="
    echo ""
    echo -e "${YELLOW}常用命令：${NC}"
    echo "查看集群状态："
    echo "  docker exec redis-sentinel1 redis-cli -p 26379 sentinel master mymaster"
    echo ""
    echo "查看从节点信息："
    echo "  docker exec redis-sentinel1 redis-cli -p 26379 sentinel replicas mymaster"
    echo ""
    echo "查看哨兵信息："
    echo "  docker exec redis-sentinel1 redis-cli -p 26379 sentinel sentinels mymaster"
    echo ""
    echo "测试故障转移："
    echo "  docker exec redis-sentinel1 redis-cli -p 26379 sentinel failover mymaster"
    echo ""
    echo "停止集群："
    echo "  docker-compose -f docker-redis-sentinel.yml down"
    echo ""
    echo "查看日志："
    echo "  docker-compose -f docker-redis-sentinel.yml logs -f"
    echo ""
}

# 主流程
main() {
    check_docker
    create_directories
    start_cluster
    wait_for_services
    show_cluster_info
    
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}Redis 哨兵集群部署完成！${NC}"
    echo -e "${GREEN}=========================================${NC}"
}

# 执行主流程
main