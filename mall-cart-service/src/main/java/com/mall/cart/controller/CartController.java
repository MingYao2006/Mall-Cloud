package com.mall.cart.controller;

import com.mall.common.Result;
import com.mall.cart.entity.Cart;
import com.mall.cart.entity.vo.CartVO;
import com.mall.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 安全说明：
 * userId 统一通过 @RequestHeader("X-User-Id") 从网关注入的请求头获取，
 * 不再接收前端明文传入，防止越权操作他人购物车。
 */
@Tag(name = "购物车服务", description = "购物车相关接口")
@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService){
        this.cartService=cartService;
    }

    /**
     * 1 添加商品到购物车
     */
    @Operation(summary = "添加商品到购物车")
    @PostMapping("/add")
    public Result<Void> addToCart(@RequestHeader("X-User-Id") Long userId,
                                   @RequestParam Long productId,
                                   @RequestParam Integer quantity) {
        cartService.addToCart(userId, productId, quantity);
        return Result.success();
    }

    /**
     * 2 更新购物车商品数量
     */
    @Operation(summary = "更新购物车商品数量")
    @PutMapping("/quantity")
    public Result<Void> updateCartQuantity(@RequestHeader("X-User-Id") Long userId,
                                           @RequestParam Long productId,
                                           @RequestParam Integer quantity) {
        cartService.updateCartQuantity(userId, productId, quantity);
        return Result.success();
    }

    /**
     * 3 删除购物车商品
     */
    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/delete")
    public Result<Void> deleteFromCart(@RequestHeader("X-User-Id") Long userId,
                                        @RequestParam Long productId) {
        cartService.deleteFromCart(userId, productId);
        return Result.success();
    }

    /**
     * 4 查询用户购物车列表
     */
    @Operation(summary = "查询用户购物车列表")
    @GetMapping("/list")
    public Result<List<CartVO>> getCartList(@RequestHeader("X-User-Id") Long userId) {
        List<CartVO> cartList = cartService.getCartList(userId);
        return Result.success(cartList);
    }

    /**
     * 5 选中/取消选中商品
     */
    @Operation(summary = "选中或取消选中商品")
    @PutMapping("/select")
    public Result<Void> updateCartSelected(@RequestHeader("X-User-Id") Long userId,
                                           @RequestParam Long productId,
                                           @RequestParam Boolean selected) {
        cartService.updateCartSelected(userId, productId, selected);
        return Result.success();
    }

    /**
     * 6 全选/取消全选
     */
    @Operation(summary = "全选或取消全选")
    @PutMapping("/select-all")
    public Result<Void> updateAllSelected(@RequestHeader("X-User-Id") Long userId,
                                          @RequestParam Boolean selected) {
        cartService.updateAllSelected(userId, selected);
        return Result.success();
    }

    /**
     * 7 批量删除购物车商品
     */
    @Operation(summary = "批量删除购物车商品")
    @DeleteMapping("/batch-delete")
    public Result<Void> batchDeleteFromCart(@RequestHeader("X-User-Id") Long userId,
                                            @RequestBody List<Long> productIds) {
        cartService.batchDeleteFromCart(userId, productIds);
        return Result.success();
    }

    /**
     * 8 计算选中商品总价
     */
    @Operation(summary = "计算选中商品总价")
    @GetMapping("/total-price")
    public Result<BigDecimal> calculateTotalPrice(@RequestHeader("X-User-Id") Long userId) {
        BigDecimal totalPrice = cartService.calculateTotalPrice(userId);
        return Result.success(totalPrice);
    }

    /**
     * 9 清空购物车
     */
    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Integer> clearCart(@RequestHeader("X-User-Id") Long userId) {
        int rows = cartService.clearCart(userId);
        return Result.success(rows);
    }

    /**
     * 10 合并购物车（登录后）
     */
    @Operation(summary = "合并购物车")
    @PostMapping("/merge")
    public Result<Void> mergeCart(@RequestHeader("X-User-Id") Long userId,
                                  @RequestBody List<Cart> tempCartList) {
        cartService.mergeCart(userId, tempCartList);
        return Result.success();
    }
}
