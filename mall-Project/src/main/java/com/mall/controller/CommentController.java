package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.ProductComment;
import com.mall.feign.CommentServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论服务 Controller - C端聚合服务（客户端版）
 * <p>
 * 提供客户端发表评论、查看评论等接口。
 * 评论审核接口已迁移至 mall-project-admin。
 */
@RestController
@RequestMapping("/comment")
@Tag(name = "评论服务", description = "商品评论接口（客户端聚合服务代理）")
public class CommentController {

    private final CommentServiceFeignClient commentService;

    public CommentController(CommentServiceFeignClient commentService) {
        this.commentService = commentService;
    }

    /**
     * 添加评论
     * POST /comment
     */
    @PostMapping
    @Operation(summary = "添加评论")
    public Result<String> addComment(@RequestBody ProductComment comment) {
        return commentService.addComment(comment);
    }

    /**
     * 获取商品评论列表
     * GET /comment/product/{productId}
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "获取商品评论列表")
    public Result<List<ProductComment>> getProductComments(@PathVariable Long productId) {
        return commentService.getProductComments(productId);
    }

    /**
     * 获取用户评论列表
     * GET /comment/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户评论列表")
    public Result<List<ProductComment>> getUserComments(@PathVariable Long userId) {
        return commentService.getUserComments(userId);
    }

    // ===== 以下管理端接口已迁移至 mall-project-admin =====
    // PUT /comment/audit/{commentId} → AdminCommentController.auditComment
}
