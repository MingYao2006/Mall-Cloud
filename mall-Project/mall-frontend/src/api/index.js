import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 有 body 时用 JSON，无 body 时去掉 Content-Type 避免 415
    if (config.data) {
      config.headers['Content-Type'] = 'application/json'
    } else {
      delete config.headers['Content-Type']
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export const userApi = {
  login: (data) => request.post('/users/login', data),
  register: (data) => request.post('/users/register', data),
  logout: () => request.post('/users/logout'),
  getInfo: () => request.get('/users/info'),
  updateInfo: (data) => request.put('/users/info', data),
  updatePassword: (data) => request.put('/users/password', data),
  uploadAvatar: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/users/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  // 发送验证码
  sendCode: (phone) => request.post('/users/code', null, { params: { phone } }),
  // 验证码登录
  loginByCode: (phone, code) => request.post('/users/login/code', null, { params: { phone, code } })
}

export const productApi = {
  // 获取商品列表
  getList: (params) => request.get('/product/list', { params }),
  // 获取商品详情
  getDetail: (id) => request.get(`/product/${id}`),
  // 搜索商品
  search: (keyword) => request.get('/product/search', { params: { keyword } }),
  // 按分类查询商品
  getByCategory: (categoryId) => request.get(`/product/category/${categoryId}`),
  // 分页查询商品
  getPage: (pageNum = 1, pageSize = 10) => request.get('/product/page', { params: { pageNum, pageSize } }),
  // 条件组合分页查询
  getPageByCondition: (params) => request.get('/product/page/query', { params })
}

export const categoryApi = {
  // 获取所有分类
  getList: () => request.get('/category/list'),
  // 获取分类详情
  getDetail: (id) => request.get(`/category/${id}`),
  // 获取子分类
  getChildren: (parentId) => request.get(`/category/children/${parentId}`),
  // 按层级获取分类
  getByLevel: (level) => request.get(`/category/level/${level}`)
}

export const cartApi = {
  // 获取购物车列表（需要传入userId）
  getList: (userId) => request.get('/cart/list', { params: { userId } }),
  // 添加商品到购物车
  add: (userId, productId, quantity) => request.post('/cart/add', null, { params: { userId, productId, quantity } }),
  // 更新购物车商品数量
  updateQuantity: (userId, productId, quantity) => request.put('/cart/quantity', null, { params: { userId, productId, quantity } }),
  // 更新商品选中状态
  updateSelected: (userId, productId, selected) => request.put('/cart/selected', null, { params: { userId, productId, selected } }),
  // 全选/全不选
  updateAllSelected: (userId, selected) => request.put('/cart/selected/all', null, { params: { userId, selected } }),
  // 删除购物车商品
  delete: (userId, productId) => request.delete('/cart/delete', { params: { userId, productId } }),
  // 清空购物车
  clear: (userId) => request.delete('/cart/clear', { params: { userId } }),
  // 获取选中商品列表
  getSelectedList: (userId) => request.get('/cart/selected/list', { params: { userId } }),
  // 获取选中商品总价
  getTotalPrice: (userId) => request.get('/cart/total-price', { params: { userId } }),
  // 合并购物车
  merge: (userId, cartList) => request.post('/cart/merge', cartList, { params: { userId } })
}

export const orderApi = {
  create: (data) => request.post('/orders', data),
  getList: (params) => request.get('/orders', { params }),
  getDetail: (id) => request.get(`/orders/${id}`),
  cancel: (id) => request.put(`/orders/${id}/cancel`),
  pay: (id) => request.put(`/orders/${id}/pay`)
}

export const couponApi = {
  // 获取优惠券列表
  getList: () => request.get('/coupon/list'),
  // 获取优惠券详情
  getDetail: (id) => request.get(`/coupon/detail/${id}`),
  // 秒杀抢券
  seckill: (couponId, userId) => request.post(`/coupon/seckill/${couponId}`, null, { params: { userId } }),
  // 获取我的优惠券
  getMy: (userId, status) => request.get('/coupon/my', { params: { userId, status } })
}

export default request
