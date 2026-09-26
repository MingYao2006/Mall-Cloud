package com.mall.feign;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.ProductComment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评论服务 Feign 客户端（管理端用）
 * <p>
 * 管理端聚合服务通过此客户端调用 mall-comment-service 微服务，
 * 主要用于评论审核管理。
 */
@FeignClient(
        name = "mall-comment-service",
        contextId = "commentServiceFeignClient",
        path = "/comment"
)
public interface CommentServiceFeignClient {

    @PostMapping
    Result<String> addComment(@RequestBody ProductComment comment);

    @GetMapping("/product/{productId}")
    Result<List<ProductComment>> getProductComments(@PathVariable("productId") Long productId);

    @PutMapping("/audit/{commentId}")
    Result<String> auditComment(@PathVariable("commentId") Long commentId,
                                @RequestParam("status") Integer status);

    @GetMapping("/user/{userId}")
    Result<List<ProductComment>> getUserComments(@PathVariable("userId") Long userId);

    @GetMapping("/page")
    Result<PageResult> pageComments(@RequestParam Map<String, Object> params);

    @GetMapping("/count")
    Result<Map<String, Long>> countByStatus(@RequestParam Map<String, Object> params);
}
