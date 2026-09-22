import { get, post, put, del } from '@/utils/request'

/**
 * 购物车模块接口（全部需要登录）
 *
 * userId 由后端从 token 解析（网关注入 X-User-Id 头），
 * 前端无需传入 userId 参数。
 */
export const cartApi = {
  /** 购物车列表 */
  getList: () => get('/cart/list'),

  /** 加入购物车 */
  add: (productId, quantity = 1) => post('/cart/add', null, { params: { productId, quantity } }),

  /** 修改数量 */
  updateQuantity: (productId, quantity) => put('/cart/quantity', null, { params: { productId, quantity } }),

  /** 修改单个商品选中状态 */
  updateSelected: (productId, selected) => put('/cart/selected', null, { params: { productId, selected } }),

  /** 全选 / 全不选 */
  updateAllSelected: (selected) => put('/cart/selected/all', null, { params: { selected } }),

  /** 删除单个商品 */
  remove: (productId) => del('/cart/delete', { productId }),

  /** 清空购物车 */
  clear: () => del('/cart/clear'),

  /** 已选中的商品列表 */
  getSelectedList: () => get('/cart/selected/list'),

  /** 已选中商品总价 */
  getTotalPrice: () => get('/cart/total-price'),

  /** 合并购物车（登录后合并本地临时购物车） */
  merge: (cartList) => post('/cart/merge', cartList)
}

export default cartApi
