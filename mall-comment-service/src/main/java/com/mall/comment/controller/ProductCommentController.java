package com.mall.comment.controller;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.comment.entity.ProductComment;
import com.mall.comment.service.ProductCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
@Tag(name = "评论服务", description = "商品评论相关接口")
public class ProductCommentController {

    @Autowired
    private ProductCommentService productCommentService;

    @PostMapping
    @Operation(summary = "添加评论")
    public Result<String> addComment(@RequestBody ProductComment comment) {
        productCommentService.addComment(comment);
        return Result.success("评论提交成功，等待审核");
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "获取商品评论列表")
    public Result<List<ProductComment>> getProductComments(@PathVariable Long productId) {
        List<ProductComment> comments = productCommentService.getProductComments(productId);
        return Result.success(comments);
    }

    @PutMapping("/audit/{commentId}")
    @Operation(summary = "审核评论")
    public Result<String> auditComment(@PathVariable Long commentId, @RequestParam Integer status) {
        productCommentService.auditComment(commentId, status);
        return Result.success("审核完成");
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户评论列表")
    public Result<List<ProductComment>> getUserComments(@PathVariable Long userId) {
        List<ProductComment> comments = productCommentService.getUserComments(userId);
        return Result.success(comments);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询评论（管理端）")
    public Result<PageResult> pageComments(@RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Long productId,
                                           @RequestParam(required = false) Long userId,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(productCommentService.pageComments(status, productId, userId, pageNum, pageSize));
    }

    @GetMapping("/count")
    @Operation(summary = "按状态统计评论数量")
    public Result<Map<String, Long>> countByStatus(@RequestParam(required = false) Long productId,
                                                   @RequestParam(required = false) Long userId) {
        return Result.success(productCommentService.countByStatus(productId, userId));
    }
}