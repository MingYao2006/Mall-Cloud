import { get } from '@/utils/request'

/**
 * 分类模块接口
 *
 * 网关白名单放行：list / children / level；
 * 分类详情 GET /category/{id} 未放行，当前需要登录。
 */
export const categoryApi = {
  /** 全部分类（免登录） */
  getList: () => get('/category/list'),

  /** 分类详情（当前需登录） */
  getDetail: (id) => get('/category/' + id),

  /** 子分类（免登录） */
  getChildren: (parentId) => get('/category/children/' + parentId),

  /** 按层级查询（免登录） */
  getByLevel: (level) => get('/category/level/' + level)
}

export default categoryApi
