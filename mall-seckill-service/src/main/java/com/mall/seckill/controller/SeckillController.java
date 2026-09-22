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
        // 发送消息到 RabbitMQ
        seckillService.sendSeckillMessage(userId, couponId);
        // 直接返回，不等待处理完成
        return Result.success("秒杀请求已提交，正在处理中...");
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