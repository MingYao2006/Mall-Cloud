import { post } from '@/utils/request'

/**
 * 支付模块接口
 *
 * 现状：后端仅有支付宝沙箱「电脑网站支付」，返回一段 HTML 表单，
 * 小程序无法直接提交该表单，因此小程序端需要新增微信支付（JSAPI）。
 */
export const paymentApi = {
  /** 支付宝支付（返回 HTML 表单，仅 H5 端可用，小程序端不可用） */
  alipay: (orderId, subject) => post('/payment/alipay/pay', { orderId, subject: subject || '商城订单' }),

  /**
   * 微信支付下单（待后端实现）
   *
   * 后端契约：POST /api/c/payment/wxpay/pay
   * 请求体：{ orderId }
   * 返回 data：小程序 wx.requestPayment 所需参数
   *   { timeStamp, nonceStr, package, signType, paySign }
   *
   * 同时需要新增回调地址 POST /api/payment/wxpay/notify，
   * 并加入网关 AuthGlobalFilter.PUBLIC_WHITE_LIST。
   */
  wxpay: (orderId) => post('/payment/wxpay/pay', { orderId })
}

export default paymentApi
