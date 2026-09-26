package com.mall.admin.controller;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.entity.ProductComment;
import com.mall.feign.CommentServiceFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论管理控制器（B端）
 * <p>
 * 提供评论审核、评论查看等管理接口。
 */
@Tag(name = "评论管理", description = "评论管理接口（管理端）")
@RestController
@RequestMapping("/comment")
public class AdminCommentController {

    private final CommentServiceFeignClient commentService;

    public AdminCommentController(CommentServiceFeignClient commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "审核评论")
    @PutMapping("/audit/{commentId}")
    public Result<String> auditComment(@PathVariable Long commentId, @RequestParam Integer status) {
        return commentService.auditComment(commentId, status);
    }

    @Operation(summary = "获取商品评论列表")
    @GetMapping("/product/{productId}")
    public Result<List<ProductComment>> getProductComments(@PathVariable Long productId) {
        return commentService.getProductComments(productId);
    }

    @Operation(summary = "获取用户评论列表")
    @GetMapping("/user/{userId}")
    public Result<List<ProductComment>> getUserComments(@PathVariable Long userId) {
        return commentService.getUserComments(userId);
    }

    @Operation(summary = "分页查询评论")
    @GetMapping("/page")
    public Result<PageResult> pageComments(@RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Long productId,
                                           @RequestParam(required = false) Long userId,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        if (status != null) params.put("status", status);
        if (productId != null) params.put("productId", productId);
        if (userId != null) params.put("userId", userId);
        return commentService.pageComments(params);
    }

    @Operation(summary = "按状态统计评论数量")
    @GetMapping("/count")
    public Result<Map<String, Long>> countByStatus(@RequestParam(required = false) Long productId,
                                                   @RequestParam(required = false) Long userId) {
        Map<String, Object> params = new HashMap<>();
        if (productId != null) params.put("productId", productId);
        if (userId != null) params.put("userId", userId);
        return commentService.countByStatus(params);
    }
}
