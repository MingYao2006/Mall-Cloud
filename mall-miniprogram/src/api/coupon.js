import { get, post } from '@/utils/request'

/**
 * 优惠券模块接口
 *
 * userId 由后端从 token 解析（网关注入 X-User-Id 头），前端无需传入。
 */
export const couponApi = {
  /** 可领取的优惠券列表（免登录） */
  getList: () => get('/coupon/list'),

  /** 优惠券详情 */
  getDetail: (couponId) => get('/coupon/detail/' + couponId),

  /** 抢券（userId 由后端从 token 解析） */
  seckill: (couponId) => post('/coupon/seckill/' + couponId),

  /** 我的优惠券（userId 由后端从 token 解析） */
  getMy: (status) => get('/coupon/my', { status })
}

export default couponApi
