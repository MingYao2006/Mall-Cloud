#!/bin/bash

# Redis 一主两从架构部署脚本
# 架构说明：
# - 主节点（Master）：192.168.152.100:6379 - 负责写操作
# - 从节点1（Slave1）：192.168.152.101:6379 - 负责读操作
# - 从节点2（Slave2）：192.168.152.102:6379 - 负责读操作

echo "========================================="
echo "Redis 一主两从架构部署脚本"
echo "========================================="

# 配置参数
MASTER_IP="192.168.152.100"
MASTER_PORT="6379"
SLAVE1_IP="192.168.152.101"
SLAVE1_PORT="6379"
SLAVE2_IP="192.168.152.102"
SLAVE2_PORT="6379"
REDIS_PASSWORD=""  # 如需密码，在此设置

# 检查 Redis 是否已安装
check_redis() {
    if ! command -v redis-server &> /dev/null; then
        echo "错误：Redis 未安装，请先安装 Redis"
        exit 1
    fi
    echo "✓ Redis 已安装"
}

# 创建配置文件目录
create_config_dir() {
    echo "创建配置文件目录..."
    sudo mkdir -p /etc/redis
    sudo mkdir -p /var/log/redis
    sudo mkdir -p /data/redis
    echo "✓ 配置文件目录创建完成"
}

# 配置主节点
configure_master() {
    echo "配置主节点（$MASTER_IP:$MASTER_PORT）..."
    
    cat > /tmp/redis-master.conf <<EOF
# Redis 主节点配置文件
bind 0.0.0.0
port $MASTER_PORT
daemonize yes
pidfile /var/run/redis/redis-master.pid
logfile /var/log/redis/redis-master.log
dir /data/redis

# 持久化配置
appendonly yes
appendfsync everysec
save 900 1
save 300 10
save 60 10000

# 密码配置（如需密码，取消注释并设置）
# requirepass $REDIS_PASSWORD

# 主从复制配置
# 主节点不需要配置 replicaof

# 最大内存配置（根据实际情况调整）
maxmemory 2gb
maxmemory-policy allkeys-lru

# 连接配置
maxclients 10000
timeout 300

# 日志级别
loglevel notice
EOF

    sudo mv /tmp/redis-master.conf /etc/redis/redis-master.conf
    echo "✓ 主节点配置完成"
}

# 配置从节点1
configure_slave1() {
    echo "配置从节点1（$SLAVE1_IP:$SLAVE1_PORT）..."
    
    cat > /tmp/redis-slave1.conf <<EOF
# Redis 从节点1配置文件
bind 0.0.0.0
port $SLAVE1_PORT
daemonize yes
pidfile /var/run/redis/redis-slave1.pid
logfile /var/log/redis/redis-slave1.log
dir /data/redis

# 持久化配置
appendonly yes
appendfsync everysec
save 900 1
save 300 10
save 60 10000

# 密码配置（如需密码，取消注释并设置）
# requirepass $REDIS_PASSWORD
# masterauth $REDIS_PASSWORD

# 主从复制配置 - 指定主节点
replicaof $MASTER_IP $MASTER_PORT

# 从节点只读
replica-read-only yes

# 最大内存配置（根据实际情况调整）
maxmemory 2gb
maxmemory-policy allkeys-lru

# 连接配置
maxclients 10000
timeout 300

# 日志级别
loglevel notice
EOF

    sudo mv /tmp/redis-slave1.conf /etc/redis/redis-slave1.conf
    echo "✓ 从节点1配置完成"
}

# 配置从节点2
configure_slave2() {
    echo "配置从节点2（$SLAVE2_IP:$SLAVE2_PORT）..."
    
    cat > /tmp/redis-slave2.conf <<EOF
# Redis 从节点2配置文件
bind 0.0.0.0
port $SLAVE2_PORT
daemonize yes
pidfile /var/run/redis/redis-slave2.pid
logfile /var/log/redis/redis-slave2.log
dir /data/redis

# 持久化配置
appendonly yes
appendfsync everysec
save 900 1
save 300 10
save 60 10000

# 密码配置（如需密码，取消注释并设置）
# requirepass $REDIS_PASSWORD
# masterauth $REDIS_PASSWORD

# 主从复制配置 - 指定主节点
replicaof $MASTER_IP $MASTER_PORT

# 从节点只读
replica-read-only yes

# 最大内存配置（根据实际情况调整）
maxmemory 2gb
maxmemory-policy allkeys-lru

# 连接配置
maxclients 10000
timeout 300

# 日志级别
loglevel notice
EOF

    sudo mv /tmp/redis-slave2.conf /etc/redis/redis-slave2.conf
    echo "✓ 从节点2配置完成"
}

# 启动 Redis 服务
start_redis() {
    echo "启动 Redis 服务..."
    
    # 启动主节点
    if [ -f /etc/redis/redis-master.conf ]; then
        redis-server /etc/redis/redis-master.conf
        echo "✓ 主节点已启动"
    fi
    
    # 启动从节点1
    if [ -f /etc/redis/redis-slave1.conf ]; then
        redis-server /etc/redis/redis-slave1.conf
        echo "✓ 从节点1已启动"
    fi
    
    # 启动从节点2
    if [ -f /etc/redis/redis-slave2.conf ]; then
        redis-server /etc/redis/redis-slave2.conf
        echo "✓ 从节点2已启动"
    fi
    
    sleep 2
}

# 验证主从复制
verify_replication() {
    echo "验证主从复制状态..."
    
    echo "主节点信息："
    redis-cli -h $MASTER_IP -p $MASTER_PORT INFO replication
    
    echo -e "\n从节点1信息："
    redis-cli -h $SLAVE1_IP -p $SLAVE1_PORT INFO replication
    
    echo -e "\n从节点2信息："
    redis-cli -h $SLAVE2_IP -p $SLAVE2_PORT INFO replication
}

# 测试主从同步
test_replication() {
    echo "测试主从同步..."
    
    # 在主节点写入数据
    echo "在主节点写入测试数据..."
    redis-cli -h $MASTER_IP -p $MASTER_PORT SET test_key "Hello Master-Slave"
    
    sleep 1
    
    # 从从节点读取数据
    echo "从节点1读取："
    redis-cli -h $SLAVE1_IP -p $SLAVE1_PORT GET test_key
    
    echo "从节点2读取："
    redis-cli -h $SLAVE2_IP -p $SLAVE2_PORT GET test_key
    
    # 清理测试数据
    redis-cli -h $MASTER_IP -p $MASTER_PORT DEL test_key
    echo "✓ 主从同步测试完成"
}

# 主菜单
main() {
    check_redis
    create_config_dir
    
    echo -e "\n请选择要执行的操作："
    echo "1. 配置主节点"
    echo "2. 配置从节点1"
    echo "3. 配置从节点2"
    echo "4. 启动所有节点"
    echo "5. 验证主从复制"
    echo "6. 测试主从同步"
    echo "7. 一键部署（执行所有步骤）"
    echo "0. 退出"
    
    read -p "请输入选项: " choice
    
    case $choice in
        1) configure_master ;;
        2) configure_slave1 ;;
        3) configure_slave2 ;;
        4) start_redis ;;
        5) verify_replication ;;
        6) test_replication ;;
        7) 
            configure_master
            configure_slave1
            configure_slave2
            start_redis
            verify_replication
            test_replication
            ;;
        0) exit 0 ;;
        *) echo "无效选项" ;;
    esac
}

# 执行主函数
main
