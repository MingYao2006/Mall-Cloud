import { get } from '@/utils/request'

/**
 * 搜索模块接口（基于 Elasticsearch）
 *
 * 后端 ProductSearchController 映射在 /search 前缀下；
 * 网关白名单未放行该前缀，当前需登录后访问。
 * 具体查询参数以后端实现为准，这里透传 params。
 */
export const searchApi = {
  /** 商品搜索 */
  searchProducts: (params) => get('/search/products', params),

  /** 按分类搜索 */
  byCategory: (categoryId, params) => get('/search/category/' + categoryId, params),

  /** 品牌聚合 */
  brands: (params) => get('/search/brand', params)
}

export default searchApi
