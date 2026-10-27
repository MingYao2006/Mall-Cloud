package com.mall.seckill.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.seckill.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 安全说明：
 * userId 统一通过 @RequestHeader("X-User-Id") 从网关注入的请求头获取，
 * 不再接收前端明文传入，防止越权操作。
 */
@RestController
@RequestMapping("/seckill")
@Validated
@Tag(name = "秒杀服务", description = "秒杀相关接口")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 测试接口
     */
    @GetMapping("/test")
    @Operation(summary = "测试接口")
    @SentinelResource(value = "seckillTest", blockHandler = "handleTestBlock")
    public Result<String> test() {
        return Result.success("秒杀服务接口正常工作");
    }

    /**
     * 测试接口限流处理
     */
    public Result<String> handleTestBlock(BlockException ex) {
        return Result.fail("当前排队人数过多，请稍后再试");
    }

    /**
     * 秒杀下单接口
     *
     * @SentinelResource 参数说明：
     * - value: 资源名称（在控制台配置规则时使用）
     * - blockHandler: 限流/熔断时的处理方法
     * - fallback: 发生异常时的处理方法
     */
    @PostMapping("/coupon/{couponId}")
    @Operation(summary = "秒杀抢券")
    @SentinelResource(
            value = "seckillCoupon",
            blockHandler = "handleSeckillBlock",
            fallback = "handleSeckillFallback"
    )
    public Result<String> seckillCoupon(
            @RequestHeader("X-User-Id") @NotNull(message = "用户ID不能为空") Long userId,
            @PathVariable @NotNull(message = "优惠券ID不能为空") Long couponId) {
        // 发送消息到 RabbitMQ，返回 messageId 供前端轮询结果
        String messageId = seckillService.sendSeckillMessage(userId, couponId);
        // 直接返回，不等待处理完成
        return Result.success(messageId);
    }

    /**
     * 查询抢券结果（前端用 messageId 轮询）。
     * <p>
     * 三种返回：
     * · status=SUCCESS / FAIL：已有结果，停止轮询；
     * · pending=true：还在排队或处理中，继续轮询；
     * · 结果已过期（超过 5 分钟）：也返回 pending，提示用户去"我的优惠券"确认。
     * <p>
     * 之所以不做成"阻塞等待结果"：秒杀的核心价值就是把同步链路削峰成异步，
     * 入口若阻塞等待，MQ 的意义就没了，而且高并发下会把 Tomcat 线程占满。
     */
    @GetMapping("/result/{messageId}")
    @Operation(summary = "查询抢券结果")
    public Result<Map<String, Object>> getSeckillResult(
            @RequestHeader("X-User-Id") @NotNull(message = "用户ID不能为空") Long userId,
            @PathVariable String messageId) {
        Map<String, Object> result = seckillService.getSeckillResult(messageId);
        if (result == null) {
            // 还没产生结果，或已过 TTL：交给前端继续轮询 / 引导去券列表确认
            Map<String, Object> pending = new HashMap<>();
            pending.put("pending", true);
            pending.put("message", "抢券请求处理中，请稍候");
            return Result.success(pending);
        }
        Map<String, Object> data = new HashMap<>(result);
        data.put("pending", false);
        return Result.success(data);
    }

    @GetMapping("/doSeckill")
    @Operation(summary = "秒杀抢券（GET方式）")
    public Result<String> doSeckill(
            @RequestHeader("X-User-Id") @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam("couponId") @NotNull(message = "优惠券ID不能为空") Long couponId) {
        // 发送消息到 RabbitMQ
        seckillService.sendSeckillMessage(userId, couponId);
        return Result.success("秒杀请求已提交，正在处理中...");
    }

/**
 * 限流处理方法
 * */
    public Result<String> handleSeckillBlock(
            Long userId,
            Long couponId,
            BlockException ex
    ) {
        return Result.fail("当前排队人数过多，请稍后再试");
    }

    /**
     * 异常处理方法
     * */
    public Result<String> handleSeckillFallback(
            Long productId,
            Long userId,
            Throwable ex
    ) {
        return Result.fail("系统繁忙，请稍后再试");
    }
}