/**
 * 接口兼容适配层
 * ------------------------------------------------------------------
 * 页面的视觉与交互照搬自开源项目 af-mall-web（Vue3 + Vite + Element Plus 电商前台），
 * 但本项目后端（Spring Cloud Alibaba 微服务，网关前缀 /api/c）的接口形态与之不同，
 * 所以这里做一层「形状适配」：把本项目接口包成参考页面期望的数据结构，
 * 页面组件因此可以基本原样复用，只需把 import 从 '@/api' 指向这里。
 */
import request, {
  productApi as rawProduct,
  categoryApi as rawCategory,
  cartApi as rawCart,
  orderApi as rawOrder,
  paymentApi,
  userApi as rawUser,
  couponApi as rawCoupon,
  commentApi as rawComment
} from './index'

/* ==================== 通用工具 ==================== */
const unwrap = (res) => (res && typeof res === 'object' && 'data' in res ? res.data : res)
const num = (v) => (v == null || v === '' ? 0 : Number(v))
const dt = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '')

/* ==================== 商品 ==================== */
function adaptProduct(p) {
  if (!p) return null
  const images = typeof p.subImages === 'string' && p.subImages
    ? p.subImages.split(',').map((s) => s.trim()).filter(Boolean)
    : (Array.isArray(p.subImages) ? p.subImages : [])
  const cover = p.mainImage || images[0] || ''
  return {
    ...p,
    spuId: p.id,
    skuId: p.id,
    spuName: p.name,
    mainImage: cover,
    images: images.length ? images : (cover ? [cover] : []),
    brandName: p.brand || 'Mall-Cloud 严选',
    minPrice: num(p.price),
    price: num(p.price),
    originalPrice: p.originalPrice == null ? null : num(p.originalPrice),
    category1Id: p.categoryId,
    sales: p.sales || 0,
    stock: p.stock == null ? 999 : p.stock,
    // 本项目后端没有 SKU 表：合成一个默认规格，让照搬来的详情页/购物车逻辑继续可用
    skus: [{
      id: p.id,
      skuName: '默认规格',
      price: num(p.price),
      stock: p.stock == null ? 999 : p.stock,
      enable: p.stock == null || p.stock > 0,
      pic: cover
    }]
  }
}

function adaptPage(res) {
  const data = unwrap(res) || {}
  const list = (data.list || data.records || []).map(adaptProduct)
  return {
    records: list,
    list,
    total: num(data.total) || list.length,
    pageNum: data.pageNum || 1,
    pages: data.pages || 1
  }
}

/** 分类：后端返回扁平列表，这里补成树（已有 children 时原样返回） */
function buildTree(list) {
  const nodes = new Map(list.map(c => [c.id, { ...c, children: [] }]))
  const roots = []
  for (const n of nodes.values()) {
    const p = nodes.get(n.parentId)
    if (p) p.children.push(n)
    else roots.push(n)   // parentId=0 或父节点没返回，都当根
  }
  return roots
}

let _categoryCache = null

/** 取某个分类及其所有子分类的 id 集合（本项目分类有 parentId/level 两层结构） */
async function descendantCategoryIds(rootId) {
  if (!_categoryCache) {
    _categoryCache = unwrap(await rawCategory.getList()) || []
  }
  const ids = new Set([Number(rootId)])
  let grew = true
  while (grew) {
    grew = false
    _categoryCache.forEach((c) => {
      if (c.parentId != null && ids.has(Number(c.parentId)) && !ids.has(Number(c.id))) {
        ids.add(Number(c.id))
        grew = true
      }
    })
  }
  return ids
}

export const productApi = {
  /** 参考页面用法：spuPage({ pageNum, pageSize, category1Id, key }) */
  spuPage: async (params = {}) => {
    const {
      pageNum = 1, pageSize = 10,
      category1Id, categoryId, key, keyword, name
    } = params
    const cidRaw = category1Id != null ? category1Id : categoryId
    const cid = cidRaw != null && cidRaw !== '' ? Number(cidRaw) : null
    const kw = String(key || keyword || name || '').trim()

    // 无筛选条件：直接走后端分页读接口（网关只放行 GET /product/page）
    if (cid == null && !kw) {
      return adaptPage(await rawProduct.getPage(pageNum, pageSize))
    }

    // 有筛选条件：商品总量不大，一次取回后在前端做分类（含子分类）/关键词过滤再分页
    const all = (unwrap(await rawProduct.getList()) || []).map(adaptProduct)
    let filtered = all

    if (cid != null) {
      const ids = await descendantCategoryIds(cid)
      filtered = filtered.filter((item) => ids.has(Number(item.categoryId)))
    }

    if (kw) {
      const lower = kw.toLowerCase()
      filtered = filtered.filter((item) =>
        String(item.name || '').toLowerCase().includes(lower) ||
        String(item.brand || '').toLowerCase().includes(lower)
      )
    }

    const start = (pageNum - 1) * pageSize
    const records = filtered.slice(start, start + pageSize)
    return {
      records,
      list: records,
      total: filtered.length,
      pageNum,
      pages: Math.ceil(filtered.length / pageSize) || 1
    }
  },
  spuDetail: async (id) => adaptProduct(unwrap(await rawProduct.getDetail(id))),
  categoryTree: async () => buildTree(unwrap(await rawCategory.getList())),
  categoryList: async () => buildTree(unwrap(await rawCategory.getList()))
}

export const categoryApi = {
  tree: async () => buildTree(unwrap(await rawCategory.getList())),
  list: async () => unwrap(await rawCategory.getList()),
  children: async (parentId) => unwrap(await rawCategory.getChildren(parentId))
}

/* ==================== 搜索 ==================== */
export const searchApi = {
  /** 后端 /product/search 返回 List，这里做前端分页，形状对齐参考页面 */
  search: async (params = {}) => {
    const { pageNum = 1, pageSize = 10, key, keyword } = params
    const kw = String(key || keyword || '').trim()
    if (!kw) return { records: [], list: [], total: 0, pageNum: 1, pages: 1 }
    const all = (unwrap(await rawProduct.search(kw)) || []).map(adaptProduct)
    const start = (pageNum - 1) * pageSize
    const records = all.slice(start, start + pageSize)
    return {
      records,
      list: records,
      total: all.length,
      pageNum,
      pages: Math.ceil(all.length / pageSize) || 1
    }
  },
  suggest: async () => []
}

/* ==================== 购物车 ==================== */
function adaptCartItem(c) {
  return {
    id: c.id,
    skuId: c.productId,
    productId: c.productId,
    title: c.productName || '',
    image: c.productImage || '',
    price: num(c.productPrice),
    num: c.quantity || 1,
    quantity: c.quantity || 1,
    selected: c.selected === 1 || c.selected === true,
    stock: c.productStock == null ? 999 : c.productStock,
    totalPrice: num(c.totalPrice)
  }
}

export const cartApi = {
  list: async () => {
    const items = (unwrap(await rawCart.getList()) || []).map(adaptCartItem)
    return {
      items,
      totalPrice: items.filter((i) => i.selected).reduce((s, i) => s + i.price * i.num, 0),
      totalCount: items.reduce((s, i) => s + i.num, 0)
    }
  },
  add: (data) => rawCart.add(data.skuId != null ? data.skuId : data.productId, data.num || 1),
  updateNum: (skuId, n) => rawCart.updateQuantity(skuId, n),
  // 后端的 selected 是 Integer，传布尔值会 500
  select: (skuId, selected) => rawCart.updateSelected(skuId, selected ? 1 : 0),
  selectAll: (selected) => request.put('/cart/selected/all', null, { params: { selected: selected ? 1 : 0 } }),
  remove: (skuId) => rawCart.delete(skuId),
  clear: () => rawCart.clear(),
  count: async () => ((unwrap(await rawCart.getList()) || []).length)
}

/* ==================== 订单 ==================== */
const ORDER_STATUS = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消', 5: '已退款' }

function adaptOrder(o) {
  if (!o) return null
  const items = o.orderItems || o.items || []
  return {
    ...o,
    goodsTitle: items.length ? items[0].productName : (o.goodsTitle || ''),
    goodsPic: items.length ? items[0].productImage : (o.goodsPic || ''),
    goodsNum: items.reduce((s, i) => s + (i.quantity || 0), 0) || o.goodsNum || 0,
    totalAmount: num(o.totalAmount),
    payAmount: num(o.payAmount != null ? o.payAmount : o.totalAmount),
    statusText: o.statusDesc || ORDER_STATUS[o.status] || '未知状态',
    createTime: dt(o.createTime)
  }
}

/** 后端「我的订单」接口不支持 status 参数，筛选在前端做 */
function filterByStatus(list, status) {
  if (status == null || status === '') return list
  return list.filter((o) => Number(o.status) === Number(status))
}

export const orderApi = {
  page: async (params = {}) => {
    const { pageNum = 1, pageSize = 10, status } = params
    const list = filterByStatus((unwrap(await rawOrder.getList()) || []).map(adaptOrder), status)
    const start = (pageNum - 1) * pageSize
    const records = list.slice(start, start + pageSize)
    return { records, list: records, total: list.length, pageNum, pages: Math.ceil(list.length / pageSize) || 1 }
  },
  list: async (params = {}) => filterByStatus((unwrap(await rawOrder.getList()) || []).map(adaptOrder), params.status),
  detail: async (id) => adaptOrder(unwrap(await rawOrder.getDetail(id))),
  create: (data) => rawOrder.create(data),
  cancel: (id) => rawOrder.cancel(id),
  statusList: async () => Object.keys(ORDER_STATUS).map((k) => ({ value: Number(k), label: ORDER_STATUS[k] }))
}

/* ==================== 支付 ==================== */
export const payApi = {
  /** 本项目走支付宝沙箱：后端返回可直接写入页面并提交的支付表单 HTML */
  pay: async ({ orderId, subject }) => unwrap(await paymentApi.alipay(orderId, subject)),
  mock: async () => true,
  status: async (orderId) => unwrap(await rawOrder.getDetail(orderId))
}

/* ==================== 通知（本项目暂无通知服务，占位避免页面报错） ==================== */
export const notifyApi = {
  list: async () => ({ records: [], list: [], total: 0 }),
  unread: async () => 0,
  markRead: async () => true,
  markAllRead: async () => true
}

export const authApi = {
  login: (data) => rawUser.login(data),
  register: (data) => rawUser.register(data),
  loginByCode: (phone, code) => rawUser.loginByCode(phone, code),
  sendCode: (phone) => rawUser.sendCode(phone)
}

export { rawUser as userApi, rawCoupon as couponApi }
export default request

export const commentApi={
  getByProduct:(productId)=>rawComment.getByProduct(productId),
  add:(data)=>rawComment.add(data),
  getByUser:(userId)=>rawComment.getByUser(userId)
}