#!/bin/bash

# Redis 集群管理脚本

COMPOSE_FILE="docker-compose.yml"

case "$1" in
    start)
        echo "启动 Redis 集群..."
        docker-compose -f $COMPOSE_FILE up -d
        echo "Redis 集群已启动"
        ;;
    
    stop)
        echo "停止 Redis 集群..."
        docker-compose -f $COMPOSE_FILE down
        echo "Redis 集群已停止"
        ;;
    
    restart)
        echo "重启 Redis 集群..."
        docker-compose -f $COMPOSE_FILE restart
        echo "Redis 集群已重启"
        ;;
    
    status)
        echo "Redis 集群状态："
        docker-compose -f $COMPOSE_FILE ps
        ;;
    
    logs)
        docker-compose -f $COMPOSE_FILE logs -f
        ;;
    
    cli-master)
        docker exec -it redis-master redis-cli
        ;;
    
    cli-slave1)
        docker exec -it redis-slave-1 redis-cli
        ;;
    
    cli-slave2)
        docker exec -it redis-slave-2 redis-cli
        ;;
    
    backup)
        BACKUP_DIR="./backup"
        mkdir -p $BACKUP_DIR
        DATE=$(date +%Y%m%d_%H%M%S)
        
        echo "触发 RDB 快照..."
        docker exec redis-master redis-cli BGSAVE
        sleep 5
        
        echo "备份 RDB 文件..."
        docker cp redis-master:/data/dump.rdb $BACKUP_DIR/dump-$DATE.rdb
        
        echo "备份完成: dump-$DATE.rdb"
        ;;
    
    info)
        echo "主节点信息："
        docker exec redis-master redis-cli info replication
        echo ""
        echo "从节点 1 信息："
        docker exec redis-slave-1 redis-cli info replication
        echo ""
        echo "从节点 2 信息："
        docker exec redis-slave-2 redis-cli info replication
        ;;
    
    *)
        echo "用法: $0 {start|stop|restart|status|logs|cli-master|cli-slave1|cli-slave2|backup|info}"
        exit 1
        ;;
esac
