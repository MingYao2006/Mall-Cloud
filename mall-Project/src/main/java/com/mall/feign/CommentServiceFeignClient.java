package com.mall.feign;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.ProductComment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 评论服务 Feign 客户端
 * <p>
 * 与商品、搜索、秒杀等功能的整合架构保持一致：
 * 聚合服务（mall-Project）不再直接实现评论逻辑，统一通过 Feign 调用 mall-comment-service 微服务。
 */
@FeignClient(
        name = "mall-comment-service",
        contextId = "commentServiceFeignClient",
        path = "/comment"
)
public interface CommentServiceFeignClient {

    /**
     * 添加评论
     * POST /comment
     */
    @PostMapping
    Result<String> addComment(@RequestBody ProductComment comment);

    /**
     * 获取商品评论列表
     * GET /comment/product/{productId}
     */
    @GetMapping("/product/{productId}")
    Result<List<ProductComment>> getProductComments(@PathVariable("productId") Long productId);

    /**
     * 审核评论
     * PUT /comment/audit/{commentId}?status=1
     */
    @PutMapping("/audit/{commentId}")
    Result<String> auditComment(@PathVariable("commentId") Long commentId,
                                @RequestParam("status") Integer status);

    /**
     * 获取用户评论列表
     * GET /comment/user/{userId}
     */
    @GetMapping("/user/{userId}")
    Result<List<ProductComment>> getUserComments(@PathVariable("userId") Long userId);

    @GetMapping("/page")
    Result<PageResult> pageComments(@RequestParam Map<String, Object> params);

    @GetMapping("/count")
    Result<Map<String, Long>> countByStatus(@RequestParam Map<String, Object> params);
}
