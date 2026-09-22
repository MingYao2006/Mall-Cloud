import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/c',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 有 body 时用 JSON，无 body 时去掉 Content-Type 避免 415
    // FormData 不上传 JSON Content-Type，让浏览器自动设置 multipart/form-data
    if (config.data && !(config.data instanceof FormData)) {
      config.headers['Content-Type'] = 'application/json'
    } else if (!config.data) {
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
    if (error.response?.status === 403) {
      ElMessage.error('无权限访问该资源')
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
  updateInfo: (data) => request.put('/users/info', data).then(res => {
    if (res.code === 200) {
      localStorage.setItem('userInfo', JSON.stringify(res.data || res));
    }
    return res;
  }),
  updatePassword: (data) => request.put('/users/password', data),
  uploadAvatar: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    // 不手动设置 Content-Type，由浏览器自动生成带 boundary 的 multipart/form-data 请求头
    return request.post('/users/avatar', formData)
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
  getPageByCondition: (params) => request.post('/product/page/query', params)
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
  // 获取购物车列表（userId 由后端从 token 解析，无需前端传入）
  getList: () => request.get('/cart/list'),
  // 添加商品到购物车
  add: (productId, quantity) => request.post('/cart/add', null, { params: { productId, quantity } }),
  // 更新购物车商品数量
  updateQuantity: (productId, quantity) => request.put('/cart/quantity', null, { params: { productId, quantity } }),
  // 更新商品选中状态
  updateSelected: (productId, selected) => request.put('/cart/selected', null, { params: { productId, selected } }),
  // 全选/全不选
  updateAllSelected: (selected) => request.put('/cart/selected/all', null, { params: { selected } }),
  // 删除购物车商品
  delete: (productId) => request.delete('/cart/delete', { params: { productId } }),
  // 清空购物车
  clear: () => request.delete('/cart/clear'),
  // 获取选中商品列表
  getSelectedList: () => request.get('/cart/selected/list'),
  // 获取选中商品总价
  getTotalPrice: () => request.get('/cart/total-price'),
  // 合并购物车
  merge: (cartList) => request.post('/cart/merge', cartList)
}

export const orderApi = {
  create: (data) => request.post('/orders', data),
  getList: (params) => request.get('/orders', { params }),
  getDetail: (id) => request.get(`/orders/${id}`),
  cancel: (id) => request.put(`/orders/${id}/cancel`),
  pay: (id) => request.put(`/orders/${id}/pay`)
}

export const paymentApi = {
  // 支付宝支付 - 返回支付表单 HTML
  alipay: (orderId, subject) => request.post('/payment/alipay/pay', { orderId, subject: subject || '商城订单' })
}

export const seckillApi = {
  // 获取秒杀商品列表
  getList: () => request.get('/seckill/list'),
  // 获取秒杀商品详情
  getDetail: (id) => request.get(`/seckill/${id}`),
  // 执行秒杀（userId 由后端从 token 解析，无需前端传入）
  execute: (seckillId) => request.post(`/seckill/${seckillId}`)
}

export const couponApi = {
  // 获取优惠券列表
  getList: () => request.get('/coupon/list'),
  // 获取优惠券详情
  getDetail: (id) => request.get(`/coupon/detail/${id}`),
  // 秒杀抢券（userId 由后端从 token 解析，无需前端传入）
  seckill: (couponId) => request.post(`/coupon/seckill/${couponId}`),
  // 获取我的优惠券（userId 由后端从 token 解析，无需前端传入）
  getMy: (status) => request.get('/coupon/my', { params: { status } })
}

export default request
