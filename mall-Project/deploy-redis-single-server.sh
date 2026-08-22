#!/bin/bash

# Redis 一主两从架构部署脚本（单机多实例）
# 架构说明：在同一台服务器上启动三个 Redis 实例
# - 主节点（Master）：192.168.152.100:6379 - 负责写操作
# - 从节点1（Slave1）：192.168.152.100:6380 - 负责读操作
# - 从节点2（Slave2）：192.168.152.100:6381 - 负责读操作

echo "========================================="
echo "Redis 一主两从架构部署脚本（单机多实例）"
echo "========================================="

# 配置参数
SERVER_IP="192.168.152.100"
MASTER_PORT="6379"
SLAVE1_PORT="6380"
SLAVE2_PORT="6381"
REDIS_PASSWORD=""  # 如需密码，在此设置

# 配置文件目录
CONFIG_DIR="/etc/redis/one-master-two-slave"
DATA_DIR="/var/lib/redis"
LOG_DIR="/var/log/redis"
PID_DIR="/var/run/redis"

# 检查 Redis 是否已安装
check_redis() {
    if ! command -v redis-server &> /dev/null; then
        echo "错误：Redis 未安装，请先安装 Redis"
        echo "安装命令："
        echo "  Ubuntu/Debian: sudo apt-get install redis-server"
        echo "  CentOS/RHEL: sudo yum install redis"
        exit 1
    fi
    echo "✓ Redis 已安装"
    redis-server --version
}

# 创建必要的目录
create_directories() {
    echo "创建必要的目录..."
    
    sudo mkdir -p $CONFIG_DIR
    sudo mkdir -p $DATA_DIR/6379
    sudo mkdir -p $DATA_DIR/6380
    sudo mkdir -p $DATA_DIR/6381
    sudo mkdir -p $LOG_DIR
    sudo mkdir -p $PID_DIR
    
    # 设置权限
    sudo chown -R redis:redis $DATA_DIR
    sudo chown -R redis:redis $LOG_DIR
    sudo chown -R redis:redis $PID_DIR
    
    echo "✓ 目录创建完成"
}

# 配置主节点（6379）
configure_master() {
    echo "配置主节点（端口 $MASTER_PORT）..."
    
    cat > /tmp/redis-6379.conf <<EOF
# Redis 主节点配置文件
bind 0.0.0.0
port $MASTER_PORT
daemonize yes
pidfile $PID_DIR/redis-6379.pid
logfile $LOG_DIR/redis-6379.log
dir $DATA_DIR/6379

# 持久化配置
appendonly yes
appendfsync everysec
save 900 1
save 300 10
save 60 10000

# 主从复制配置
# 主节点不需要配置 replicaof
replica-serve-stale-data yes
replica-read-only yes

# 密码配置（如需密码，取消注释并设置）
# requirepass $REDIS_PASSWORD

# 内存管理
maxmemory 2gb
maxmemory-policy allkeys-lru

# 连接配置
maxclients 10000
timeout 300

# 日志级别
loglevel notice
EOF

    sudo mv /tmp/redis-6379.conf $CONFIG_DIR/6379.conf
    echo "✓ 主节点配置完成：$CONFIG_DIR/6379.conf"
}

# 配置从节点1（6380）
configure_slave1() {
    echo "配置从节点1（端口 $SLAVE1_PORT）..."
    
    cat > /tmp/redis-6380.conf <<EOF
# Redis 从节点1配置文件
bind 0.0.0.0
port $SLAVE1_PORT
daemonize yes
pidfile $PID_DIR/redis-6380.pid
logfile $LOG_DIR/redis-6380.log
dir $DATA_DIR/6380

# 持久化配置
appendonly yes
appendfsync everysec
save 900 1
save 300 10
save 60 10000

# 主从复制配置 - 指定主节点
replicaof $SERVER_IP $MASTER_PORT
replica-serve-stale-data yes
replica-read-only yes

# 密码配置（如需密码，取消注释并设置）
# requirepass $REDIS_PASSWORD
# masterauth $REDIS_PASSWORD

# 内存管理
maxmemory 2gb
maxmemory-policy allkeys-lru

# 连接配置
maxclients 10000
timeout 300

# 日志级别
loglevel notice
EOF

    sudo mv /tmp/redis-6380.conf $CONFIG_DIR/6380.conf
    echo "✓ 从节点1配置完成：$CONFIG_DIR/6380.conf"
}

# 配置从节点2（6381）
configure_slave2() {
    echo "配置从节点2（端口 $SLAVE2_PORT）..."
    
    cat > /tmp/redis-6381.conf <<EOF
# Redis 从节点2配置文件
bind 0.0.0.0
port $SLAVE2_PORT
daemonize yes
pidfile $PID_DIR/redis-6381.pid
logfile $LOG_DIR/redis-6381.log
dir $DATA_DIR/6381

# 持久化配置
appendonly yes
appendfsync everysec
save 900 1
save 300 10
save 60 10000

# 主从复制配置 - 指定主节点
replicaof $SERVER_IP $MASTER_PORT
replica-serve-stale-data yes
replica-read-only yes

# 密码配置（如需密码，取消注释并设置）
# requirepass $REDIS_PASSWORD
# masterauth $REDIS_PASSWORD

# 内存管理
maxmemory 2gb
maxmemory-policy allkeys-lru

# 连接配置
maxclients 10000
timeout 300

# 日志级别
loglevel notice
EOF

    sudo mv /tmp/redis-6381.conf $CONFIG_DIR/6381.conf
    echo "✓ 从节点2配置完成：$CONFIG_DIR/6381.conf"
}

# 启动所有 Redis 实例
start_redis() {
    echo "启动 Redis 实例..."
    
    # 检查是否已经运行
    if [ -f $PID_DIR/redis-6379.pid ]; then
        echo "警告：主节点可能已在运行"
    fi
    
    # 启动主节点
    sudo redis-server $CONFIG_DIR/6379.conf
    sleep 1
    echo "✓ 主节点已启动（端口 $MASTER_PORT）"
    
    # 启动从节点1
    sudo redis-server $CONFIG_DIR/6380.conf
    sleep 1
    echo "✓ 从节点1已启动（端口 $SLAVE1_PORT）"
    
    # 启动从节点2
    sudo redis-server $CONFIG_DIR/6381.conf
    sleep 1
    echo "✓ 从节点2已启动（端口 $SLAVE2_PORT）"
    
    echo "等待主从同步..."
    sleep 2
}

# 停止所有 Redis 实例
stop_redis() {
    echo "停止 Redis 实例..."
    
    redis-cli -p $MASTER_PORT SHUTDOWN NOSAVE 2>/dev/null
    redis-cli -p $SLAVE1_PORT SHUTDOWN NOSAVE 2>/dev/null
    redis-cli -p $SLAVE2_PORT SHUTDOWN NOSAVE 2>/dev/null
    
    echo "✓ 所有 Redis 实例已停止"
}

# 验证主从复制
verify_replication() {
    echo "验证主从复制状态..."
    
    echo -e "\n========== 主节点信息 =========="
    redis-cli -h $SERVER_IP -p $MASTER_PORT INFO replication
    
    echo -e "\n========== 从节点1信息 =========="
    redis-cli -h $SERVER_IP -p $SLAVE1_PORT INFO replication
    
    echo -e "\n========== 从节点2信息 =========="
    redis-cli -h $SERVER_IP -p $SLAVE2_PORT INFO replication
}

# 测试主从同步
test_replication() {
    echo "测试主从同步..."
    
    # 在主节点写入数据
    echo "在主节点写入测试数据..."
    redis-cli -h $SERVER_IP -p $MASTER_PORT SET test_master_slave "Hello Redis Master-Slave"
    
    sleep 1
    
    # 从从节点读取数据
    echo -e "\n从节点1读取："
    redis-cli -h $SERVER_IP -p $SLAVE1_PORT GET test_master_slave
    
    echo -e "\n从节点2读取："
    redis-cli -h $SERVER_IP -p $SLAVE2_PORT GET test_master_slave
    
    # 清理测试数据
    redis-cli -h $SERVER_IP -p $MASTER_PORT DEL test_master_slave
    echo -e "\n✓ 主从同步测试完成"
}

# 查看进程状态
check_status() {
    echo "查看 Redis 进程状态..."
    ps aux | grep redis-server | grep -v grep
}

# 主菜单
main() {
    check_redis
    create_directories
    
    echo -e "\n请选择要执行的操作："
    echo "1. 配置主节点（6379）"
    echo "2. 配置从节点1（6380）"
    echo "3. 配置从节点2（6381）"
    echo "4. 启动所有节点"
    echo "5. 停止所有节点"
    echo "6. 验证主从复制"
    echo "7. 测试主从同步"
    echo "8. 查看进程状态"
    echo "9. 一键部署（执行所有配置和启动）"
    echo "0. 退出"
    
    read -p "请输入选项: " choice
    
    case $choice in
        1) configure_master ;;
        2) configure_slave1 ;;
        3) configure_slave2 ;;
        4) start_redis ;;
        5) stop_redis ;;
        6) verify_replication ;;
        7) test_replication ;;
        8) check_status ;;
        9) 
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
