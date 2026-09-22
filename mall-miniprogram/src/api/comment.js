import { get, post } from '@/utils/request'

/**
 * 评论模块接口
 * 审核相关接口已迁移至 B 端（mall-project-admin），C 端仅保留发表与查询。
 */
export const commentApi = {
  /** 发表评论（需登录） */
  add: (data) => post('/comment', data),

  /** 商品评论列表（当前需登录，未在网关白名单中） */
  getByProduct: (productId) => get('/comment/product/' + productId),

  /** 用户评论列表（需登录） */
  getByUser: (userId) => get('/comment/user/' + userId)
}

export default commentApi
