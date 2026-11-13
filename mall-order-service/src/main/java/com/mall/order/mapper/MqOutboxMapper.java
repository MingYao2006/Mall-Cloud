package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.MqOutbox;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MqOutboxMapper extends BaseMapper<MqOutbox> {

    /**
     * 幂等入队：唯一索引 (biz_key, exchange_name) 冲突时静默忽略。
     * 用 INSERT IGNORE 而不是先查后插，避免"查不到→并发插入→重复"的窗口。
     */
    @Insert("INSERT IGNORE INTO mq_outbox " +
            "(biz_key, exchange_name, routing_key, payload, status, retry_count, next_retry_time) " +
            "VALUES (#{bizKey}, #{exchangeName}, #{routingKey}, #{payload}, 0, 0, NOW())")
    int insertIgnore(@Param("bizKey") String bizKey,
                     @Param("exchangeName") String exchangeName,
                     @Param("routingKey") String routingKey,
                     @Param("payload") String payload);

    /**
     * 投递任务扫描：捞出到期待投递的记录。
     * 走 idx_status_retry (status, next_retry_time)。
     */
    @Select("SELECT * FROM mq_outbox WHERE status = 0 AND next_retry_time <= #{now} " +
            "ORDER BY id LIMIT #{limit}")
    List<MqOutbox> selectPending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 投递成功：置 SENT。带上 status = 0 前置条件，避免覆盖已被标记失败的记录。
     */
    @Update("UPDATE mq_outbox SET status = 1, last_error = NULL, update_time = NOW() " +
            "WHERE id = #{id} AND status = 0")
    int markSent(@Param("id") Long id);

    /**
     * 投递失败：retry_count+1，并按指数退避推迟下次重试。
     * <p>
     * 超过 maxRetry 后置为 STATUS_FAILED（=2），退出扫描范围，等人工介入——
     * 无限重试会把投递任务变成"每轮都在打同一个坏消息"，反而拖垮正常消息的投递。
     * <p>
     * ⚠️ 赋值顺序不能调换：MySQL 的 UPDATE 按 SET 子句从左到右求值，
     * 后面的表达式读到的是已更新的值。status 必须写在 retry_count 之前，
     * 这样它用的是"本次之前的"重试次数；否则 `retry_count + 1` 会被加两次，导致提前一轮判失败。
     */
    @Update("UPDATE mq_outbox SET " +
            "status = IF(retry_count + 1 >= #{maxRetry}, 2, 0), " +
            "retry_count = retry_count + 1, " +
            "next_retry_time = #{nextRetryTime}, last_error = #{error}, update_time = NOW() " +
            "WHERE id = #{id} AND status = 0")
    int markRetry(@Param("id") Long id,
                  @Param("maxRetry") int maxRetry,
                  @Param("nextRetryTime") LocalDateTime nextRetryTime,
                  @Param("error") String error);

    /**
     * 消息被 Broker 退回（到了交换机但路由不到任何队列）。
     * <p>
     * 这种情况 Broker 的 confirm 是 ack（它确实收到了），所以投递任务可能已经置了 SENT；
     * 但消息没有任何队列会收到，必须重置回待投递并重试。
     * 因此这里的 status 条件包含 1，允许把刚被误标为 SENT 的记录拉回 INIT。
     */
    @Update("UPDATE mq_outbox SET status = 0, retry_count = retry_count + 1, " +
            "next_retry_time = #{nextRetryTime}, last_error = #{error}, update_time = NOW() " +
            "WHERE id = #{id} AND status IN (0, 1)")
    int markReturned(@Param("id") Long id,
                     @Param("nextRetryTime") LocalDateTime nextRetryTime,
                     @Param("error") String error);
}
