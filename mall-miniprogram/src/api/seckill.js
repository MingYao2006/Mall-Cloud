import { get, post } from '@/utils/request'

/**
 * 秒杀模块接口
 *
 * ⚠ 与 Web 端 api/index.js 不一致：
 * Web 端调用的 /seckill/list、/seckill/{id} 在后端 SeckillController 中并不存在，
 * 实际可用的是下面两个「秒杀抢券」接口（秒杀与优惠券共用一条链路）。
 *
 * userId 由后端从 token 解析（网关注入 X-User-Id 头），前端无需传入。
 */
export const seckillApi = {
  /**
   * 秒杀抢券（推荐使用）
   * userId 由后端从 X-User-Id 请求头解析
   */
  seckillCoupon: (couponId) =>
    post('/seckill/coupon/' + couponId),

  /** 秒杀抢券（GET 方式，用于兜底联调） */
  doSeckill: (couponId) => get('/seckill/doSeckill', { couponId }),

  /** 连通性测试 */
  test: () => get('/seckill/test')
}

export default seckillApi
