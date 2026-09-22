import { get, post } from '@/utils/request'

/**
 * 商品模块接口
 *
 * 注意：网关白名单仅放行了 product 的 list / search / page / category / status，
 * 商品详情 GET /product/{id} 未在白名单中，当前需要携带 token 才能访问。
 * 若希望游客也能浏览商品详情，需在网关与聚合层白名单补充该规则。
 */
export const productApi = {
  /** 商品列表（免登录） */
  getList: (params) => get('/product/list', params),

  /** 商品详情（当前需登录） */
  getDetail: (id) => get('/product/' + id),

  /** 关键词搜索（免登录） */
  search: (keyword) => get('/product/search', { keyword }),

  /** 按分类查询（免登录） */
  getByCategory: (categoryId) => get('/product/category/' + categoryId),

  /** 分页查询（免登录） */
  getPage: (pageNum = 1, pageSize = 10) => get('/product/page', { pageNum, pageSize }),

  /** 多条件分页查询（免登录） */
  getPageByCondition: (query) => post('/product/page/query', query)
}

export default productApi
