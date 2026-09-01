package com.mall.order.service;

import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.order.entity.Cart;
import com.mall.order.entity.Item.OrderItem;
import com.mall.order.entity.Order;
import com.mall.order.entity.Product;
import com.mall.order.entity.dto.CreateOrderRequest;
import com.mall.order.entity.vo.OrderDetailVO;
import com.mall.order.entity.vo.OrderVO;
import com.mall.order.feign.ProductServiceFeignClient;
import com.mall.order.mapper.CartMapper;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.ProductMapper;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mall.common.RedisService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CartMapper cartMapper;
    //    引入redisson锁
    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ProductServiceFeignClient productServiceFeignClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    private static final String ORDER_CACHE_KEY = "order:";
    private static final Long CACHE_TIMEOUT = 30L;

    private static final long ORDER_TIMEOUT_MINUTES = 30;

    public void sendSeckillMessage() {

    }

    /**
     * 创建订单
     * 支持两种方式：
     * 1. 从购物车创建订单（cartItemIds 不为空）
     * 2. 直接购买单个商品（productId 不为空）
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 1. 参数校验
        validateRequest(request);

        // 2. 准备订单商品列表
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            // 从购物车创建订单
            orderItems = createOrderItemsFromCart(request.getCartItemIds());
        } else if (request.getProductId() != null) {
            // 直接购买单个商品
            OrderItem item = createOrderItemFromProduct(
                    request.getProductId(),
                    request.getQuantity() != null ? request.getQuantity() : 1
            );
            orderItems.add(item);
        } else {
            throw new BusinessException(400, "请选择商品或购物车项");
        }

        // 3. 计算总金额
        for (OrderItem item : orderItems) {
            totalAmount = totalAmount.add(item.getTotalPrice());
        }

        // 4. 生成订单号
        String orderNo = generateOrderNo();

        // 5. 创建订单主表
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount); // 暂无优惠，实付=总额
        order.setFreightAmount(BigDecimal.ZERO); // 暂无运费
        order.setDiscountAmount(BigDecimal.ZERO); // 暂无优惠
        order.setPaymentType(request.getPaymentType() != null ? request.getPaymentType() : 1);
        order.setStatus(0); // 待支付
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        order.setNote(request.getNote());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setIsDeleted(0);

        orderMapper.insert(order);

        // 6. 创建订单详情
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
        }
        orderItemMapper.batchInsert(orderItems);

        // 7. 扣减库存
        for (OrderItem item : orderItems) {
            decreaseStock(item.getProductId(), item.getQuantity());
        }

        // 8. 清理购物车（如果是从购物车下单）
        if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            cartMapper.deleteByIds(request.getCartItemIds());
        }

        sendDelayMessages(order.getId());

        return order;
    }

    /**
     * 发送订单超时延迟消息
     */
    private void sendDelayMessages(Long orderId) {
        try {
            rabbitTemplate.convertAndSend(
                    "order.delay.exchange",
                    "order.delay.key",
                    orderId,
                    message -> {
                        message.getMessageProperties().setExpiration(String.valueOf(TimeUnit.MINUTES.toMillis(ORDER_TIMEOUT_MINUTES)));
                        return message;
                    }
            );
        } catch (Exception e) {
            System.err.println("发送订单超时消息失败: " + e.getMessage());
        }

    }

    /**
     * 参数校验
     */
    private void validateRequest(CreateOrderRequest request) {
        if (request.getUserId() == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (request.getReceiverName() == null || request.getReceiverName().trim().isEmpty()) {
            throw new BusinessException(400, "收货人姓名不能为空");
        }
        if (request.getReceiverPhone() == null || request.getReceiverPhone().trim().isEmpty()) {
            throw new BusinessException(400, "收货人电话不能为空");
        }
        String phone = request.getReceiverPhone().trim();
        if (!isValidPhone(phone)) {
            throw new BusinessException(400, "收货人电话格式错误");
        }
        if (request.getReceiverAddress() == null || request.getReceiverAddress().trim().isEmpty()) {
            throw new BusinessException(400, "收货地址不能为空");
        }
    }

    /**
     * 从购物车创建订单项列表
     */
    private List<OrderItem> createOrderItemsFromCart(List<Long> cartItemIds) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (Long cartItemId : cartItemIds) {
            Cart cart = cartMapper.selectById(cartItemId);
            if (cart == null) {
                throw new BusinessException(400, "购物车项不存在：" + cartItemId);
            }

            //调用openfeign服务
            Result<Product> result = productServiceFeignClient.findById(cart.getId());
            Product product = result.getData();

            if (product == null) {
                throw new BusinessException(400, "商品不存在：" + cart.getProductId());
            }

            // 检查商品是否上架
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException(400, "商品【" + product.getName() + "】已下架");
            }

            // 检查库存
            if (product.getStock() < cart.getQuantity()) {
                throw new BusinessException(400, "商品【" + product.getName() + "】库存不足，当前库存：" + product.getStock());
            }

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setProductPrice(product.getPrice());
            item.setQuantity(cart.getQuantity());
            item.setTotalPrice(product.getPrice().multiply(new BigDecimal(cart.getQuantity())));

            orderItems.add(item);
        }

        return orderItems;
    }

    /**
     * 直接购买单个商品 - 创建订单项
     */
    private OrderItem createOrderItemFromProduct(Long productId, Integer quantity) {
        // 1. 查询商品信息
        Result<Product> result = productServiceFeignClient.findById(productId);
        Product product = result.getData();
        if (product == null) {
            throw new BusinessException(400, "商品不存在：" + productId);
        }

        // 2. 检查商品状态
        if (product.getStatus() != 1) {
            throw new BusinessException(400, "商品已下架");
        }

        // 3. 检查库存
        if (product.getStock() < quantity) {
            throw new BusinessException(400, "商品【" + product.getName() + "】库存不足，当前库存：" + product.getStock());
        }

        // 4. 创建订单项
        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductImage(product.getMainImage());
        item.setProductPrice(product.getPrice());
        item.setQuantity(quantity);
        item.setTotalPrice(product.getPrice().multiply(new BigDecimal(quantity)));

        return item;
    }

    /**
     * 生成订单号
     * 格式：时间戳 + 用户ID后4位 + 4位随机数
     */
    private String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String timestamp = LocalDateTime.now().format(formatter);
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return timestamp + uuidPart;
    }

    /**
     * 扣减库存（原子操作，防止并发超卖）
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     *                  <p>
     *                  实现流程：
     *                  1. 使用分布式锁防止并发扣减库存
     *                  - 锁key: lock:product:stock:{productId}
     *                  - 尝试获取锁（等待3秒，锁持有10秒）
     *                  2. 执行库存扣减（使用乐观锁机制）
     *                  3. 如果扣减失败（库存不足），抛出异常
     *                  4. 释放锁
     */
    private void decreaseStock(Long productId, Integer quantity) {
        String lockKey = "lock:product:stock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!tryLock) {
                throw new BusinessException(500, "系统繁忙,请稍后重试");
            }

            Result<String> result = productServiceFeignClient.decreaseStock(productId, quantity);
            if (result == null || result.getCode() == null || result.getCode() != 200) {
                throw new BusinessException(500, "库存扣减失败：" + (result != null ? result.getMessage() : "服务无响应"));
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
     * 根据用户ID查询订单列表（带缓存击穿防护）
     *
     * @param userId 用户ID
     * @return 订单列表VO对象
     * <p>
     * 实现流程（7层防护）：
     * 第1层：参数校验
     * 第2层：空值缓存检查（防止缓存穿透）
     * 第3层：缓存获取
     * 第4层：互斥锁防护（防止缓存击穿）
     * 第5层：数据库查询
     * 第6层：缓存写入（含空值缓存）
     * 第7层：异常降级处理
     */
    public List<OrderVO> getOrdersByUserId(Long userId) {
        // 第1层：参数校验
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }

        String cacheKey = ORDER_CACHE_KEY + "list:" + userId;
        String nullKey = ORDER_CACHE_KEY + "null:" + userId;
        String lockKey = "lock:order:list:" + userId;

        try {
            // 第2层：空值缓存检查（防止缓存穿透）
            if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
                return new ArrayList<>(); // 返回空列表而不是抛异常
            }

            // 第3层：缓存获取
            Object cached = redisService.get(cacheKey);
            if (cached != null) {
                return (List<OrderVO>) cached;
            }

            // 第4层：互斥锁防护（防止缓存击穿）
            RLock lock = redissonClient.getLock(lockKey);
            int maxRetries = 3;

            for (int i = 0; i < maxRetries; i++) {
                try {
                    boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
                    if (!locked) {
                        // 获取锁失败，等待后重试
                        try {
                            Thread.sleep(100 * (i + 1));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }

                    try {
                        // 双重检查：获取锁后再次检查缓存
                        cached = redisService.get(cacheKey);
                        if (cached != null) {
                            return (List<OrderVO>) cached;
                        }

                        // 第5层：数据库查询
                        List<Order> orders = orderMapper.selectByUserId(userId);

                        if (orders == null || orders.isEmpty()) {
                            // 第6层：缓存空值防止缓存穿透（TTL=5分钟）
                            try {
                                redisService.set(nullKey, "null", 5, TimeUnit.MINUTES);
                            } catch (Exception e) {
                                System.err.println("缓存空值失败: " + e.getMessage());
                            }
                            return new ArrayList<>(); // 返回空列表
                        }

                        // 转换为VO列表
                        List<OrderVO> orderVOs = new ArrayList<>();
                        for (Order order : orders) {
                            OrderVO vo = convertToOrderVO(order);
                            orderVOs.add(vo);
                        }

                        // 第6层：缓存写入
                        try {
                            redisService.set(cacheKey, orderVOs, CACHE_TIMEOUT, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            System.err.println("缓存写入失败: " + e.getMessage());
                        }

                        return orderVOs;

                    } finally {
                        // 释放锁
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(500, "获取锁被中断");
                }
            }

            // 第7层：重试失败后的降级处理
            System.err.println("缓存击穿防护失败，降级到数据库查询");
            List<Order> orders = orderMapper.selectByUserId(userId);
            if (orders == null || orders.isEmpty()) {
                return new ArrayList<>();
            }
            List<OrderVO> orderVOs = new ArrayList<>();
            for (Order order : orders) {
                OrderVO vo = convertToOrderVO(order);
                orderVOs.add(vo);
            }
            return orderVOs;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // 第7层：Redis异常降级处理
            System.err.println("Redis不可用，降级到数据库: " + e.getMessage());
            List<Order> orders = orderMapper.selectByUserId(userId);
            if (orders == null || orders.isEmpty()) {
                return new ArrayList<>();
            }
            List<OrderVO> orderVOs = new ArrayList<>();
            for (Order order : orders) {
                OrderVO vo = convertToOrderVO(order);
                orderVOs.add(vo);
            }
            return orderVOs;
        }
    }

    /**
     * 根据订单ID查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情VO对象
     * <p>
     * 实现流程：
     * 1. 参数校验（订单ID不能为空）
     * 2. 根据订单ID查询订单主表信息
     * 3. 验证订单是否存在
     * 4. 查询订单详情列表（订单项）
     * 5. 组装返回对象，包含订单基本信息和订单项列表
     * <p>
     * todo 缓存穿透： 恶意用户频繁请求不存在的订单ID
     */
    public OrderDetailVO getOrderByOrderId(Long orderId) {
        // 1. 参数校验  第1层：参数校验
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(400, "订单id不存在");
        }
        String cacheKey = "select:order:" + orderId;
        String nullKey = "select:order:null:" + orderId;
        try {
            // 第2层：检查空值缓存
            if (redisTemplate.hasKey(nullKey)) {
                throw new BusinessException(404, "数据不存在");
            }
            // 尝试从缓存获取
            Object cached = redisService.get(cacheKey);

            if (cached != null) {
                return (OrderDetailVO) cached;
            }
        } catch (Exception e) {
            System.err.println("Redis失败，降级到数据库: " + e.getMessage());
        }

        // 缓存未命中时，使用互斥锁防止缓存击穿
        String lockKey = "lock:order:cache:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);

        // 最大重试次数
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!tryLock) {
                    // 获取锁失败，短暂等待后重试
                    Thread.sleep(100 * (i + 1)); // 递增等待时间
                    continue;
                }

                try {
                    // 双重检查：获取锁后再次检查缓存
                    // 第2层：检查空值缓存
                    if (redisTemplate.hasKey(nullKey)) {
                        throw new BusinessException(404, "数据不存在");
                    }
                    // 尝试从缓存获取
                    Object cached = redisService.get(cacheKey);
                    if (cached != null) {
                        return (OrderDetailVO) cached;
                    }

                    // 第3层：查询数据库
                    // 2. 查询订单主表
                    Order order = orderMapper.selectById(orderId);
                    // 3. 验证订单是否存在
                    if (order == null) {
                        // 第4层：缓存空值（TTL=5分钟）
                        try {
                            redisService.set(nullKey, "null", 5, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            // 忽略
                        }
                        throw new BusinessException(404, "订单不存在");
                    }
                    // 4. 查询订单详情列表
                    List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
                    // 5. 组装返回对象
                    OrderDetailVO result = convertToOrderDetailVO(order, orderItems);

                    // 写入缓存
                    try {
                        redisService.set(cacheKey, result, 5, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        System.err.println("缓存写入失败: " + e.getMessage());
                    }

                    return result;
                } finally {
                    // 确保锁被释放
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(500, "获取锁被中断");
            } catch (RuntimeException e) {
                // RuntimeException直接抛出（如"订单不存在"）
                throw e;
            } catch (Exception e) {
                // 其他异常记录日志，继续重试
                System.err.println("缓存查询失败: " + e.getMessage());
                if (i == maxRetries - 1) {
                    // 最后一次重试失败，降级直接查数据库
                    Order order = orderMapper.selectById(orderId);
                    if (order == null) {
                        throw new BusinessException(404, "订单不存在");
                    }
                    List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
                    return convertToOrderDetailVO(order, orderItems);
                }
            }
        }

        // 所有重试都失败，降级直接查数据库
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        return convertToOrderDetailVO(order, orderItems);
    }

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     *                <p>
     *                实现流程：
     *                1. 参数校验（订单ID不能为空）
     *                2. 查询订单信息
     *                3. 验证订单是否存在
     *                4. 验证订单状态（只有待支付状态才能取消）
     *                5. 查询订单详情列表，获取商品ID和数量
     *                6. 恢复商品库存（遍历订单项，将数量加回库存）
     *                7. 更新订单状态为已取消（状态值：4）
     *                8. 记录更新时间
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        // 1. 参数校验
        if (orderId == null) {
            throw new BusinessException(400, "订单id不存在");
        }
        // 2. 查询订单信息
        Order order = orderMapper.selectById(orderId);
        // 3. 验证订单是否存在
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 4. 验证订单状态（只有待支付或已支付状态才能取消）
        Integer status = order.getStatus();
        if (status != 0 && status != 1) {
            throw new BusinessException(400, "订单未处于待支付或已支付状态，当前状态为：" + getStatusText(status));
        }
        // 5. 查询订单详情列表
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        if (orderItems == null || orderItems.isEmpty()) {
            throw new BusinessException(400, "订单中没有商品");
        }
        // 6.todo 恢复商品库存
        for (OrderItem item : orderItems) {
            decreaseStockForCancel(item.getProductId(), item.getQuantity());
        }
        // 7. 更新订单状态为已取消
        order.setStatus(4);
        // 8. 记录更新时间
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        sendOrderCancelledMessage(order.getId(), orderItems);
    }

    /**
     * 发送订单取消消息，异步恢复资源
     */
    private void sendOrderCancelledMessage(Long orderId, List<OrderItem> orderItems) {
        try {
            Map<String,Object> message=new HashMap<>();
            message.put("orderId", orderId);
            List<Map<String, Object>> orderItemList = new ArrayList<>();
            for (OrderItem item : orderItems) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productId", item.getProductId());
                itemMap.put("quantity", item.getQuantity());
                orderItemList.add(itemMap);
            }
            message.put("orderitem", orderItemList);
            rabbitTemplate.convertAndSend(
                    "order.cancel.resource.exchange",  // 交换器名称
                    "order.cancel.resource.key",       // 路由键
                    message                             // 消息内容
            );
            System.out.println("订单取消消息已发送，订单ID: " + orderId);
        } catch (Exception e) {
            System.err.println("发送订单取消消息失败: " + e.getMessage());
        }
    }

    /**
     * 取消订单时恢复库存（原子操作，防止并发问题）
     * todo 互斥锁 ：防止取消订单恢复库存时与下单扣库存冲突
     */
    private void decreaseStockForCancel(Long productId, Integer quantity) {
        String redisKey = "lock:product:stock:" + productId;
        RLock lock = redissonClient.getLock(redisKey);
        try {
            boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!tryLock) {
                throw new BusinessException(500, "系统繁忙，请稍后重试");
            }
//            int affectedRows = productMapper.increaseStock(productId, quantity);
            Result<String> result = productServiceFeignClient.increaseStock(productId, quantity);
            if (result == null || result.getCode() == null || result.getCode() != 200) {
                throw new BusinessException(500, "库存增加失败：" + (result != null ? result.getMessage() : "服务无响应"));
            }
//            if (affectedRows == 0) {
//                throw new BusinessException(500, "恢复库存失败，商品ID：" + productId);
//            }
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
     * 获取订单状态文本描述
     *
     * @param status 订单状态值
     * @return 状态文本描述
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:
                return "待支付";
            case 1:
                return "已支付";
            case 2:
                return "已发货";
            case 3:
                return "已完成";
            case 4:
                return "已取消";
            case 5:
                return "已退款";
            default:
                return "未知状态(" + status + ")";
        }
    }

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     *                <p>
     *                实现流程：
     *                1. 参数校验（订单ID不能为空）
     *                2. 查询订单信息
     *                3. 验证订单是否存在
     *                4. 验证订单状态（只有待支付状态才能支付）
     *                5. 更新订单状态为待发货（状态值：1）
     *                6. 记录支付时间
     *                7. 记录更新时间
     */
    @Transactional
    public void payOrder(Long orderId) {
        // 1. 参数校验
        if (orderId == null) {
            throw new BusinessException(400, "订单id不存在");
        }
        // 2. 查询订单信息
        Order order = orderMapper.selectById(orderId);
        // 3. 验证订单是否存在
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 4. 验证订单状态（只有待支付状态才能支付）
        Integer status = order.getStatus();
        if (status != 0) {
            throw new BusinessException(400, "订单未处于待支付状态");
        }
        // 5. 更新订单状态为待发货
        order.setStatus(1);
        // 6. 记录支付时间
        order.setPaymentTime(LocalDateTime.now());
        // 7. 记录更新时间
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        sendPaymentMessage(order);
    }


    /**
     * 支付成功消息发送器
     */
    private void sendPaymentMessage(Order order) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("orderId", order.getId());
            message.put("userId", order.getUserId());
            message.put("orderNo", order.getOrderNo());
            message.put("payAmount", order.getPayAmount());
            message.put("paymentTime", LocalDateTime.now());

            rabbitTemplate.convertAndSend(
                    "payment.success.exchange",
                    "payment.success.key",
                    message
            );

            System.out.println("支付成功消息已发送，订单ID: " + order.getId());
        } catch (Exception e) {
            System.err.println("发送支付成功消息失败: " + e.getMessage());
        }
    }


    /**
     * 将Order和OrderItem转换为OrderDetailVO
     */
    private OrderDetailVO convertToOrderDetailVO(Order order, List<OrderItem> orderItems) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPaymentType(order.getPaymentType());
        vo.setStatus(order.getStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setNote(order.getNote());
        vo.setCreateTime(order.getCreateTime());

        // 设置描述字段
        vo.setPaymentTypeDesc(getPaymentTypeDesc(order.getPaymentType()));
        vo.setStatusDesc(getStatusDesc(order.getStatus()));

        // 转换订单项
        List<OrderDetailVO.OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : orderItems) {
            OrderDetailVO.OrderItemVO itemVO = new OrderDetailVO.OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductImage(item.getProductImage());
            itemVO.setProductPrice(item.getProductPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setTotalPrice(item.getTotalPrice());
            itemVOs.add(itemVO);
        }
        vo.setOrderItems(itemVOs);

        return vo;
    }

    /**
     * 获取支付方式描述
     */
    private String getPaymentTypeDesc(Integer paymentType) {
        if (paymentType == null) return "未知";
        switch (paymentType) {
            case 1:
                return "微信";
            case 2:
                return "支付宝";
            case 3:
                return "银行卡";
            default:
                return "未知";
        }
    }

    /**
     * 获取订单状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:
                return "待支付";
            case 1:
                return "已支付";
            case 2:
                return "已发货";
            case 3:
                return "已完成";
            case 4:
                return "已取消";
            case 5:
                return "已退款";
            default:
                return "未知";
        }
    }

    /**
     * 将Order转换为OrderVO
     */
    private OrderVO convertToOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setCreateTime(order.getCreateTime());

        // 设置状态描述
        vo.setStatusDesc(getStatusDesc(order.getStatus()));

        // 查询订单项列表
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
        List<OrderVO.OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : orderItems) {
            OrderVO.OrderItemVO itemVO = new OrderVO.OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductImage(item.getProductImage());
            itemVO.setProductPrice(item.getProductPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setTotalPrice(item.getTotalPrice());
            itemVOs.add(itemVO);
        }
        vo.setOrderItems(itemVOs);

        return vo;
    }

    private static boolean isValidPhone(String phone) {
        String regex = "^(1[3-9]\\d{9}|(0\\d{2,3}-?)?\\d{7,8})$";
        return phone.matches(regex);
    }
}

