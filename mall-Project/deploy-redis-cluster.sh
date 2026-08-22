#!/bin/bash

# Redis Cluster 集群部署脚本
# 适用于 6 台虚拟机环境
# node01: 192.168.152.100 (主节点1)
# node02: 192.168.152.101 (主节点2)
# node03: 192.168.152.102 (主节点3)
# node04: 192.168.152.103 (从节点1)
# node05: 192.168.152.104 (从节点2)
# node06: 192.168.152.105 (从节点3)

echo "========================================="
echo "Redis Cluster 集群部署脚本"
echo "========================================="

# 配置变量
REDIS_PORT=7001
REDIS_PASSWORD=""  # 如需密码，在此设置
CLUSTER_IPS="192.168.152.100 192.168.152.101 192.168.152.102 192.168.152.103 192.168.152.104 192.168.152.105"

# 检查 Redis 是否已安装
check_redis() {
    if ! command -v redis-server &> /dev/null; then
        echo "错误: Redis 未安装，请先安装 Redis"
        echo "安装命令: yum install -y redis (CentOS) 或 apt-get install -y redis (Ubuntu)"
        exit 1
    fi
    echo "✓ Redis 已安装"
}

# 创建配置文件
create_config() {
    local ip=$1
    local port=$2
    local config_file="/etc/redis/redis-${port}.conf"
    
    echo "创建配置文件: ${config_file}"
    
    cat > ${config_file} <<EOF
# Redis Cluster 节点配置
port ${port}
bind 0.0.0.0
daemonize yes
cluster-enabled yes
cluster-config-file nodes-${port}.conf
cluster-node-timeout 5000
cluster-announce-ip ${ip}
cluster-announce-port ${port}
cluster-announce-bus-port $((port + 10000))
appendonly yes
appendfsync everysec
logfile /var/log/redis/redis-${port}.log
pidfile /var/run/redis/redis-${port}.pid
dir /data/redis/${port}
$( [ -n "$REDIS_PASSWORD" ] && echo "requirepass $REDIS_PASSWORD" && echo "masterauth $REDIS_PASSWORD" )
EOF

    # 创建必要目录
    mkdir -p /var/log/redis
    mkdir -p /var/run/redis
    mkdir -p /data/redis/${port}
    
    echo "✓ 配置文件创建完成: ${config_file}"
}

# 启动 Redis 节点
start_node() {
    local port=$1
    local config_file="/etc/redis/redis-${port}.conf"
    
    echo "启动 Redis 节点: ${port}"
    redis-server ${config_file}
    
    sleep 2
    
    if [ -f "/var/run/redis/redis-${port}.pid" ]; then
        echo "✓ Redis 节点 ${port} 启动成功"
    else
        echo "✗ Redis 节点 ${port} 启动失败"
        exit 1
    fi
}

# 检查节点状态
check_node() {
    local ip=$1
    local port=$2
    
    echo "检查节点: ${ip}:${port}"
    redis-cli -h ${ip} -p ${port} ping
}

# 创建集群
create_cluster() {
    echo "========================================="
    echo "创建 Redis Cluster 集群"
    echo "========================================="
    
    # 构建集群创建命令
    local cluster_nodes=""
    for ip in ${CLUSTER_IPS}; do
        cluster_nodes="${cluster_nodes} ${ip}:${REDIS_PORT}"
    done
    
    echo "集群节点: ${cluster_nodes}"
    
    # 创建集群（3主3从）
    redis-cli --cluster create ${cluster_nodes} --cluster-replicas 1
    
    echo "✓ 集群创建完成"
}

# 验证集群
verify_cluster() {
    echo "========================================="
    echo "验证集群状态"
    echo "========================================="
    
    # 连接到第一个节点
    redis-cli -c -h 192.168.152.100 -p ${REDIS_PORT} <<EOF
cluster info
cluster nodes
cluster slots
EOF
}

# 主函数
main() {
    echo "请选择操作："
    echo "1) 在当前节点安装并启动 Redis"
    echo "2) 创建集群（在 node01 执行）"
    echo "3) 验证集群状态"
    echo "4) 完整部署（包含以上所有步骤）"
    echo ""
    read -p "请输入选项 (1-4): " choice
    
    case $choice in
        1)
            check_redis
            echo ""
            read -p "请输入当前节点 IP: " node_ip
            create_config ${node_ip} ${REDIS_PORT}
            start_node ${REDIS_PORT}
            ;;
        2)
            create_cluster
            ;;
        3)
            verify_cluster
            ;;
        4)
            check_redis
            echo ""
            echo "注意：此选项需要在每台机器上分别执行"
            read -p "请输入当前节点 IP: " node_ip
            create_config ${node_ip} ${REDIS_PORT}
            start_node ${REDIS_PORT}
            
            echo ""
            read -p "是否在 node01 上创建集群？(y/n): " create_cluster_choice
            if [ "$create_cluster_choice" = "y" ]; then
                create_cluster
                verify_cluster
            fi
            ;;
        *)
            echo "无效选项"
            exit 1
            ;;
    esac
}

# 执行主函数
main
