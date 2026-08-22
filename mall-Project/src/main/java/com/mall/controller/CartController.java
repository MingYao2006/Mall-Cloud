package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.Cart;
import com.mall.entity.vo.CartVO;
import com.mall.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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
    @PostMapping("/add")
    public Result<Void> addToCart(@RequestParam Long userId,
                                   @RequestParam Long productId,
                                   @RequestParam Integer quantity) {
        cartService.addToCart(userId, productId, quantity);
        return Result.success();
    }

    /**
     * 2 更新购物车商品数量
     */
    @PutMapping("/quantity")
    public Result<Void> updateCartQuantity(@RequestParam Long userId,
                                           @RequestParam Long productId,
                                           @RequestParam Integer quantity) {
        cartService.updateCartQuantity(userId, productId, quantity);
        return Result.success();
    }

    /**
     * 3 删除购物车商品
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteFromCart(@RequestParam Long userId,
                                        @RequestParam Long productId) {
        cartService.deleteFromCart(userId, productId);
        return Result.success();
    }

    /**
     * 4 查询用户购物车列表
     */
    @GetMapping("/list")
    public Result<List<CartVO>> getCartList(@RequestParam Long userId) {
        List<CartVO> cartList = cartService.getCartList(userId);
        return Result.success(cartList);
    }

    /**
     * 5 选中/取消选中商品
     */
    @PutMapping("/selected")
    public Result<Void> updateSelected(@RequestParam Long userId,
                                       @RequestParam Long productId,
                                       @RequestParam Integer selected) {
        cartService.updateSelected(userId, productId, selected);
        return Result.success();
    }

    /**
     * 6 全选/全不选
     */
    @PutMapping("/selected/all")
    public Result<Integer> updateAllSelected(@RequestParam Long userId,
                                             @RequestParam Integer selected) {
        int rows = cartService.updateAllSelected(userId, selected);
        return Result.success(rows);
    }

    /**
     * 7 获取选中商品列表
     */
    @GetMapping("/selected/list")
    public Result<List<CartVO>> getSelectedCartItems(@RequestParam Long userId) {
        List<CartVO> selectedItems = cartService.getSelectedCartItems(userId);
        return Result.success(selectedItems);
    }

    /**
     * 8 计算选中商品总价
     */
    @GetMapping("/total-price")
    public Result<BigDecimal> calculateTotalPrice(@RequestParam Long userId) {
        BigDecimal totalPrice = cartService.calculateTotalPrice(userId);
        return Result.success(totalPrice);
    }

    /**
     * 9 清空购物车
     */
    @DeleteMapping("/clear")
    public Result<Integer> clearCart(@RequestParam Long userId) {
        int rows = cartService.clearCart(userId);
        return Result.success(rows);
    }

    /**
     * 10 合并购物车（登录后）
     */
    @PostMapping("/merge")
    public Result<Void> mergeCart(@RequestParam Long userId,
                                  @RequestBody List<Cart> tempCartList) {
        cartService.mergeCart(userId, tempCartList);
        return Result.success();
    }
}
