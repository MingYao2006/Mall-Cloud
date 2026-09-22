import { get, post, put } from '@/utils/request'

/**
 * 订单模块接口（全部需要登录）
 *
 * 创建订单时 userId 由后端从 token 解析，前端无需传。
 * 收货信息字段：receiverName / receiverPhone / receiverAddress
 */
export const orderApi = {
  /**
   * 创建订单
   * @param {Object} data CreateOrderRequest
   *   - cartItemIds: 购物车项 ID 列表（购物车下单）
   *   - productId / quantity: 商品 ID 与数量（立即购买）
   *   - receiverName / receiverPhone / receiverAddress: 收货信息
   *   - note: 备注
   *   - paymentType: 1-微信 2-支付宝 3-银行卡
   */
  create: (data) => post('/orders', data),

  /** 当前用户订单列表 */
  getList: () => get('/orders'),

  /** 订单详情 */
  getDetail: (orderId) => get('/orders/' + orderId),

  /** 取消订单 */
  cancel: (orderId) => put('/orders/' + orderId + '/cancel'),

  /** 将订单标记为已支付（非真实支付，仅状态流转） */
  pay: (orderId) => put('/orders/' + orderId + '/pay')
}

export default orderApi
