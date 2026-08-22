package com.mall.service;

import cn.hutool.core.thread.ThreadException;
import com.mall.common.Result;
import com.mall.entity.Cart;
import com.mall.entity.Product;
import com.mall.entity.vo.CartVO;
import com.mall.feign.ProductServiceFeignClient;
import com.mall.mapper.CartMapper;
import com.mall.mapper.ProductMapper;
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
    private final ProductMapper productMapper;
    @Autowired
    private ProductServiceFeignClient productServiceFeignClient;

    private CartService(CartMapper cartMapper, ProductMapper productMapper) {
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
    }

    @Resource
    private RedissonClient redissonClient;

    /**
     * 添加商品到购物车
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  商品数量
     *                  <p>
     *                  实现流程：
     *                  1. 参数校验（用户ID、商品ID不能为空，数量必须大于0）
     *                  2. 查询商品是否存在且上架状态
     *                  3. 校验库存是否充足
     *                  4. 使用分布式锁防止并发添加同一商品到购物车
     *                  - 锁key: lock:cart:{userId}:{productId}
     *                  - 尝试获取锁（等待3秒，锁持有10秒）
     *                  5. 查询购物车中是否已存在该商品
     *                  6. 如果存在：更新数量 = 原数量 + 新数量
     *                  7. 如果不存在：插入新的购物车记录
     *                  8. 释放锁
     */
    public void addToCart(Long userId, Long productId, Integer quantity) {
//        参数校验（userId、productId 不为空，quantity > 0）
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("商品数量必须大于0");
        }

//        查询商品是否存在且上架状态
        Result<Product> result = productServiceFeignClient.findById(productId);
        Product product = result.getData();
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new RuntimeException("商品已下架，无法添加到购物车");
        }
        // 校验库存是否充足
        if (product.getStock() == null || product.getStock() < quantity) {
            throw new RuntimeException("商品库存不足");
        }

//        查询购物车中是否已存在该商品
        String lockKey = "lock:cart:" + userId + ":" + productId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!tryLock) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
            if (cart != null) {
                //        如果存在：更新数量 = 原数量 + 新数量
                int newQuantity = cart.getQuantity() + quantity;
                cart.setQuantity(newQuantity);
                cart.setUpdateTime(LocalDateTime.now());
                cartMapper.updateById(cart);
            } else {
//        如果不存在：插入新的购物车记录
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
            throw new RuntimeException("获取锁被中断", e);
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
//        参数校验（quantity > 0）
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("商品数量必须大于0");
        }

//        查询购物车记录是否存在
        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (cart == null) {
            throw new RuntimeException("购物车中不存在该商品");
        }

//        校验购物车归属（确保是當前用户的购物车）
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该购物车");
        }

//        更新商品数量和更新时间
        int result = cartMapper.updateQuantity(userId, productId, quantity);

//        校验更新结果（受影响行数 > 0）
        if (result <= 0) {
            throw new RuntimeException("更新购物车数量失败");
        }
    }


    /**
     * 3 删除购物车商品
     */
    public void deleteFromCart(Long userId, Long productId) {
//        参数校验
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }

//        查询购物车记录是否存在
        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (cart == null) {
            throw new RuntimeException("购物车中不存在该商品");
        }

//        校验购物车归属
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该购物车");
        }

//        执行逻辑删除（或物理删除）
        int result = cartMapper.deleteByUserIdAndProductId(userId, productId);

//        校验删除结果
        if (result <= 0) {
            throw new RuntimeException("删除购物车商品失败");
        }
    }

    /**
     * 4 查询用户购物车列表
     */
    public List<CartVO> getCartList(Long userId) {
//        参数校验
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

//        查询用户的所有购物车记录（已通过JOIN关联商品信息）
        List<CartVO> cartList = cartMapper.selectCartListByUserId(userId);

//        计算每项的小计金额
        if (cartList != null && !cartList.isEmpty()) {
            for (CartVO cartVO : cartList) {
                if (cartVO.getProductPrice() != null && cartVO.getQuantity() != null) {
                    BigDecimal totalPrice = cartVO.getProductPrice().multiply(new BigDecimal(cartVO.getQuantity()));
                    cartVO.setTotalPrice(totalPrice);
                }
            }
        }

//        返回购物车列表
        return cartList != null ? cartList : new ArrayList<>();
    }

    /**
     * 5 选中/取消选中商品
     */
    public void updateSelected(Long userId, Long productId, Integer selected) {
//        参数校验（selected 为 0 或 1）
        if (selected == null || (selected != 0 && selected != 1)) {
            throw new RuntimeException("选中状态必须为0或1");
        }
//        查询购物车记录是否存在
        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (cart == null) {
            throw new RuntimeException("购物车中不存在该商品");
        }
//        校验购物车归属
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该购物车");
        }
//        更新选中状态
        int rows = cartMapper.updateSelected(userId, productId, selected);
//        校验更新结果
        if (rows <= 0) {
            throw new RuntimeException("更新选中状态失败");
        }
    }

    /**
     * 6 全选/全不选
     *
     * @return
     */
    public int updateAllSelected(Long userId, Integer selected) {
//        参数校验
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (selected == null || (selected != 0 && selected != 1)) {
            throw new RuntimeException("选中状态必须为0或1");
        }
//        查询用户的所有购物车记录
        List<CartVO> cartListByUserId = cartMapper.selectCartListByUserId(userId);
//        批量更新所有记录的 selected 字段
        int rows = cartMapper.updateAllSelected(userId, selected);
//        校验更新结果
        if (rows <= 0) {
            throw new RuntimeException("更新选中状态失败");
        }
//        返回更新的记录数
        return rows;
    }

    /**
     * 7 获取选中商品列表
     */
    public List<CartVO> getSelectedCartItems(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
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
     * 8 计算选中商品总价
     */
    public BigDecimal calculateTotalPrice(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
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
     * 9 清空购物车
     */
    public int clearCart(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        int rows = cartMapper.deleteByUserId(userId);
        if (rows <= 0) {
            throw new RuntimeException("清空购物车失败");
        }

        return rows;
    }

    /**
     * * 10 合并购物车（登录后）
     * *
     */
    public void mergeCart(Long userId, List<Cart> tempCartList) {
        if (tempCartList == null || tempCartList.isEmpty()) {
            throw new RuntimeException("临时购物车不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
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


