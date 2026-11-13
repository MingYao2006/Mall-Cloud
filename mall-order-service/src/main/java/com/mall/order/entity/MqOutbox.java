package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表记录（Transactional Outbox）。
 * <p>
 * 核心思想：把"发 MQ"这个不可回滚的外部副作用，降级成"同事务内插一行本地记录"。
 * 业务事务提交成功后，由 {@link com.mall.order.task.OutboxSendTask} 轮询投递，
 * 从而消除"订单已落库、消息没发出、进程就崩了"这个窗口。
 */
@Data
@TableName("mq_outbox")
public class MqOutbox {

    /** 待投递 */
    public static final int STATUS_INIT = 0;
    /** 已投递（收到 Broker 的 publisher confirm） */
    public static final int STATUS_SENT = 1;
    /** 超过重试上限，需人工介入 */
    public static final int STATUS_FAILED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务幂等键，与 exchange 组成唯一索引，防止同一事件重复入队 */
    private String bizKey;

    private String exchangeName;

    private String routingKey;

    /** 消息体 JSON。存字符串而不是对象，避免消费端反序列化时依赖生产端的类结构 */
    private String payload;

    /** 0-待投递 1-已投递 2-已失败 */
    private Integer status;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private String lastError;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
