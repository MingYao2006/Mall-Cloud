import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAdminStore = defineStore('admin', () => {
  const token = ref(localStorage.getItem('admin_token') || '')

  const getUserInfo = () => {
    const stored = localStorage.getItem('admin_userInfo')
    if (!stored || stored === 'undefined' || stored === 'null') {
      return null
    }
    try {
      return JSON.parse(stored)
    } catch {
      return null
    }
  }
  const userInfo = ref(getUserInfo())

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('admin_token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('admin_userInfo', JSON.stringify(info))
  }

  async function login(loginForm) {
    const { authApi } = await import('@/api')
    const res = await authApi.login(loginForm)
    // 校验是否为管理员
    if (res.data.role !== 1) {
      const { ElMessage } = await import('element-plus')
      ElMessage.error('该账号无管理员权限')
      throw new Error('非管理员账号')
    }
    setToken(res.data.token)
    setUserInfo(res.data)
    return res
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_userInfo')
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    login,
    logout
  }
})
