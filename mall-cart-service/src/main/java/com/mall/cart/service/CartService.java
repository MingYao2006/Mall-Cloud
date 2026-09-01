package com.mall.cart.service;

import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.cart.entity.Cart;
import com.mall.cart.entity.vo.CartVO;
import com.mall.cart.feign.ProductServiceFeignClient;
import com.mall.cart.mapper.CartMapper;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CartService {

    private final CartMapper cartMapper;

    @Autowired
    private ProductServiceFeignClient productServiceFeignClient;

    private CartService(CartMapper cartMapper) {
        this.cartMapper = cartMapper;
    }

    @Resource
    private RedissonClient redissonClient;

    /**
     * 添加商品到购物车
     */
    public void addToCart(Long userId, Long productId, Integer quantity) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (productId == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(400, "商品数量必须大于0");
        }

        Result<?> result = productServiceFeignClient.findById(productId);
        if (result == null || result.getData() == null) {
            throw new BusinessException(400, "商品不存在");
        }

        String lockKey = "lock:cart:" + userId + ":" + productId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!tryLock) {
                throw new BusinessException(500, "系统繁忙，请稍后重试");
            }
            Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
            if (cart != null) {
                int newQuantity = cart.getQuantity() + quantity;
                cart.setQuantity(newQuantity);
                cart.setUpdateTime(LocalDateTime.now());
                cartMapper.updateById(cart);
            } else {
                Cart newCart = new Cart();
                newCart.setProductId(productId);
                newCart.setUpdateTime(LocalDateTime.now());
                newCart.setCreateTime(LocalDateTime.now());
                newCart.setUserId(userId);
                newCart.setQuantity(quantity);
                newCart.setSelected(1);
                cartMapper.insert(newCart);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "获取锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 2 更新购物车商品数量
     */
    public void updateCartQuantity(Long userId, Long productId, Integer quantity) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (productId == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(400, "商品数量必须大于0");
        }

        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (cart == null) {
            throw new BusinessException(400, "购物车中不存在该商品");
        }

        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该购物车");
        }

        int result = cartMapper.updateQuantity(userId, productId, quantity);

        if (result <= 0) {
            throw new BusinessException(500, "更新购物车数量失败");
        }
    }

    /**
     * 3 删除购物车商品
     */
    public void deleteFromCart(Long userId, Long productId) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (productId == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }

        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (cart == null) {
            throw new BusinessException(400, "购物车中不存在该商品");
        }

        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该购物车");
        }

        int result = cartMapper.deleteByUserIdAndProductId(userId, productId);

        if (result <= 0) {
            throw new BusinessException(500, "删除购物车商品失败");
        }
    }

    /**
     * 4 查询用户购物车列表
     */
    public List<CartVO> getCartList(Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }

        List<CartVO> cartList = cartMapper.selectCartListByUserId(userId);

        if (cartList != null && !cartList.isEmpty()) {
            for (CartVO cartVO : cartList) {
                if (cartVO.getProductPrice() != null && cartVO.getQuantity() != null) {
                    BigDecimal totalPrice = cartVO.getProductPrice().multiply(new BigDecimal(cartVO.getQuantity()));
                    cartVO.setTotalPrice(totalPrice);
                }
            }
        }

        return cartList != null ? cartList : new ArrayList<>();
    }

    /**
     * 5 选中/取消选中商品
     */
    public void updateCartSelected(Long userId, Long productId, Boolean selected) {
        Integer selectedInt = selected ? 1 : 0;
        
        if (selectedInt == null || (selectedInt != 0 && selectedInt != 1)) {
            throw new BusinessException(400, "选中状态必须为0或1");
        }
        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (cart == null) {
            throw new BusinessException(400, "购物车中不存在该商品");
        }
        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该购物车");
        }
        int rows = cartMapper.updateSelected(userId, productId, selectedInt);
        if (rows <= 0) {
            throw new BusinessException(500, "更新选中状态失败");
        }
    }

    /**
     * 6 全选/全不选
     */
    public void updateAllSelected(Long userId, Boolean selected) {
        Integer selectedInt = selected ? 1 : 0;
        
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (selectedInt == null || (selectedInt != 0 && selectedInt != 1)) {
            throw new BusinessException(400, "选中状态必须为0或1");
        }
        int rows = cartMapper.updateAllSelected(userId, selectedInt);
        if (rows <= 0) {
            throw new BusinessException(500, "更新选中状态失败");
        }
    }

    /**
     * 7 批量删除购物车商品
     */
    public void batchDeleteFromCart(Long userId, List<Long> productIds) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException(400, "商品ID列表不能为空");
        }

        for (Long productId : productIds) {
            Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
            if (cart != null && cart.getUserId().equals(userId)) {
                cartMapper.deleteByUserIdAndProductId(userId, productId);
            }
        }
    }

    /**
     * 8 计算选中商品总价
     */
    public BigDecimal calculateTotalPrice(Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }

        List<CartVO> selectedCartItems = getSelectedCartItems(userId);

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (CartVO cartVO : selectedCartItems) {
            if (cartVO.getTotalPrice() != null) {
                totalPrice = totalPrice.add(cartVO.getTotalPrice());
            }
        }

        return totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 获取选中商品列表
     */
    public List<CartVO> getSelectedCartItems(Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }

        List<CartVO> selectedCartItems = cartMapper.selectSelectedCartItems(userId);

        if (selectedCartItems != null && !selectedCartItems.isEmpty()) {
            for (CartVO cartVO : selectedCartItems) {
                if (cartVO.getProductPrice() != null && cartVO.getQuantity() != null) {
                    BigDecimal itemTotalPrice = cartVO.getProductPrice().multiply(new BigDecimal(cartVO.getQuantity()));
                    cartVO.setTotalPrice(itemTotalPrice);
                }
            }
        }

        return selectedCartItems != null ? selectedCartItems : new ArrayList<>();
    }

    /**
     * 9 清空购物车
     */
    public int clearCart(Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }

        int rows = cartMapper.deleteByUserId(userId);
        if (rows <= 0) {
            throw new BusinessException(500, "清空购物车失败");
        }

        return rows;
    }

    /**
     * 10 合并购物车（登录后）
     */
    public void mergeCart(Long userId, List<Cart> tempCartList) {
        if (tempCartList == null || tempCartList.isEmpty()) {
            throw new BusinessException(400, "临时购物车不能为空");
        }
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }

        for (Cart tempCart : tempCartList) {
            Long productId = tempCart.getProductId();
            Integer quantity = tempCart.getQuantity();

            if (productId == null || quantity == null || quantity <= 0) {
                continue;
            }

            Cart existCart = cartMapper.selectByUserIdAndProductId(userId, productId);

            if (existCart != null) {
                int newQuantity = existCart.getQuantity() + quantity;
                cartMapper.updateQuantity(userId, productId, newQuantity);
            } else {
                tempCart.setUserId(userId);
                tempCart.setSelected(1);
                tempCart.setCreateTime(LocalDateTime.now());
                tempCart.setUpdateTime(LocalDateTime.now());
                cartMapper.insert(tempCart);
            }
        }
    }
}