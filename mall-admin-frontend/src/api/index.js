import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/admin',
  timeout: 15000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
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
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_userInfo')
      if (window.location.pathname !== '/login') {
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
    if (error.response?.status === 403) {
      ElMessage.error('无管理员权限，无法访问该资源')
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 管理员登录（走 C 端登录接口，拿到含 role=1 的 Token）
export const authApi = {
  login: (data) => request.post('/users/login', data, { baseURL: '/api/c' }),
  getInfo: () => request.get('/users/info', { baseURL: '/api/c' })
}

export const productApi = {
  getList: () => request.get('/product/list'),
  getDetail: (id) => request.get(`/product/${id}`),
  add: (data) => request.post('/product', data),
  update: (data) => request.put('/product', data),
  delete: (id) => request.delete(`/product/${id}`),
  getPage: (pageNum = 1, pageSize = 10) => request.get('/product/page', { params: { pageNum, pageSize } }),
  getPageByCondition: (params) => request.post('/product/page/query', params),
  search: (keyword) => request.get('/product/search', { params: { keyword } }),
  getByCategory: (categoryId) => request.get(`/product/category/${categoryId}`),
  getByStatus: (status) => request.get(`/product/status/${status}`),
  publish: (id) => request.post(`/product/publish/${id}`),
  unpublish: (id) => request.post(`/product/unpublish/${id}`),
  updateStock: (id, stock) => request.post(`/product/stock/${id}`, null, { params: { stock } }),
  increaseStock: (id, quantity) => request.post(`/product/stock/increase/${id}`, null, { params: { quantity } }),
  decreaseStock: (id, quantity) => request.post(`/product/stock/decrease/${id}`, null, { params: { quantity } }),
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/product/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const categoryApi = {
  getList: () => request.get('/category/list'),
  getDetail: (id) => request.get(`/category/${id}`),
  add: (data) => request.post('/category', data),
  update: (data) => request.put('/category', data),
  delete: (id) => request.delete(`/category/${id}`),
  getChildren: (parentId) => request.get(`/category/children/${parentId}`),
  getByLevel: (level) => request.get(`/category/level/${level}`),
  updateStatus: (id, status) => request.put('/category/status', null, { params: { id, status } })
}

export const commentApi = {
  getProductComments: (productId) => request.get(`/comment/product/${productId}`),
  getUserComments: (userId) => request.get(`/comment/user/${userId}`),
  audit: (commentId, status) => request.put(`/comment/audit/${commentId}`, null, { params: { status } }),
  getPage: (params) => request.get('/comment/page', { params }),
  getCount: (params) => request.get('/comment/count', { params })
}

export const searchApi = {
  importAll: () => request.post('/search/import'),
  searchProducts: (keyword) => request.get('/search/products', { params: { keyword } })
}

export const orderApi = {
  getDetail: (orderId) => request.get(`/orders/${orderId}`),
  getList: (userId) => request.get('/orders', { params: userId ? { userId } : {} }),
  cancel: (orderId) => request.put(`/orders/${orderId}/cancel`)
}

export const userApi = {
  getInfo: () => request.get('/users/info'),
  health: () => request.get('/users/health')
}

export const couponApi = {
  getList: () => request.get('/coupon/list'),
  getDetail: (id) => request.get(`/coupon/detail/${id}`)
}

export default request
