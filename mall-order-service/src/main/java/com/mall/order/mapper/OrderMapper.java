package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.Order;
import com.mall.common.vo.SalesTrendItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    // ============================================================
    // BaseMapper<Order> 已自动提供（可安全删除下面的手写版本）：
    //   insert(Order)        → INSERT INTO orders(...)
    //   selectById(Long)     → SELECT * FROM orders WHERE id=? AND is_deleted=0
    //   deleteById(Long)     → UPDATE orders SET is_deleted=1 WHERE id=? (@TableLogic)
    //

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} AND is_deleted = 0")
    Order selectByOrderNo(String orderNo);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserId(Long userId);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND status = #{status} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Order> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("SELECT * FROM orders WHERE is_deleted = 0 ORDER BY create_time DESC")
    List<Order> selectAll();

    // ============================================================
    // 状态流转一律走「条件更新」（乐观锁 / CAS）：把状态判断和写入合并成一条 SQL，
    // 由数据库的行锁保证原子性，返回影响行数=1 才代表本次流转成功。
    //
    // 为什么不能用 selectById + if(status) + updateById：
    //   · 读和写之间有窗口，支付与超时取消并发时互相覆盖
    //     → 用户付了钱订单被置为已取消，而库存已被回补，造成超卖；
    //   · updateById 会把实体所有非 null 字段整行写回，覆盖别人的并发修改。
    //
    // 注意：@TableLogic 只对 MyBatis-Plus 生成的 CRUD 生效，
    //       手写 SQL 必须显式带上 is_deleted = 0。
    // ============================================================

    /**
     * 支付：仅当订单仍为「待支付」时把 0 改成 1。
     * 返回 1 表示本次调用真正完成了支付；返回 0 表示订单已被取消或已支付。
     */
    @Update("UPDATE orders SET status = 1, payment_time = NOW(), update_time = NOW() " +
            "WHERE id = #{id} AND status = 0 AND is_deleted = 0")
    int payIfPending(@Param("id") Long id);

    /**
     * 超时关单：仅当订单仍为「待支付」时取消。
     * 用户恰好在这一刻支付成功时影响行数为 0，取消自动失效，不会覆盖支付结果。
     */
    @Update("UPDATE orders SET status = 4, update_time = NOW() " +
            "WHERE id = #{id} AND status = 0 AND is_deleted = 0")
    int cancelIfPending(@Param("id") Long id);

    /**
     * 用户主动取消：允许「待支付」和「已支付」两种前置状态。
     */
    @Update("UPDATE orders SET status = 4, update_time = NOW() " +
            "WHERE id = #{id} AND status IN (0, 1) AND is_deleted = 0")
    int cancelIfPendingOrPaid(@Param("id") Long id);

    /**
     * 扫表兜底：按 id 游标分批捞出已超时未支付的订单。
     * <p>
     * 只取 id 而不是整行，既省带宽，也从根上避免拿旧快照整行回写。
     * 用 id > #{lastId} 而不是 LIMIT offset，避免深分页越翻越慢。
     */
    @Select("SELECT id FROM orders WHERE status = 0 AND is_deleted = 0 " +
            "AND create_time < #{deadline} AND id > #{lastId} " +
            "ORDER BY id LIMIT #{limit}")
    List<Long> selectTimeoutIds(@Param("deadline") LocalDateTime deadline,
                                @Param("lastId") Long lastId,
                                @Param("limit") int limit);

    // ============================================================
    // 管理端统计：有效订单 = 已支付 / 已发货 / 已完成（status IN (1,2,3)）
    // ============================================================

    @Select("SELECT COUNT(*) FROM orders WHERE is_deleted = 0 AND status IN (1, 2, 3)")
    Long countEffectiveOrders();

    @Select("SELECT COUNT(*) FROM orders WHERE is_deleted = 0")
    Long countAllOrders();

    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM orders WHERE is_deleted = 0 AND status IN (1, 2, 3)")
    BigDecimal sumEffectiveAmount();

    @Select("SELECT COUNT(*) FROM orders WHERE is_deleted = 0 AND create_time >= #{start}")
    Long countSince(@Param("start") LocalDateTime start);

    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM orders WHERE is_deleted = 0 " +
            "AND status IN (1, 2, 3) AND create_time >= #{start}")
    BigDecimal sumEffectiveAmountSince(@Param("start") LocalDateTime start);

    @Select("SELECT COUNT(*) FROM orders WHERE is_deleted = 0 AND status = #{status}")
    Long countByStatus(@Param("status") Integer status);

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS statDate, " +
            "COUNT(*) AS orderCount, IFNULL(SUM(pay_amount), 0) AS salesAmount " +
            "FROM orders WHERE is_deleted = 0 AND status IN (1, 2, 3) AND create_time >= #{start} " +
            "GROUP BY statDate ORDER BY statDate")
    List<SalesTrendItem> selectSalesTrend(@Param("start") LocalDateTime start);
}
