import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  
  // 安全解析 userInfo，避免 JSON.parse 错误
  const getUserInfo = () => {
    const stored = localStorage.getItem('userInfo')
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
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  async function login(loginForm) {
    const { userApi } = await import('@/api')
    const res = await userApi.login(loginForm)
    setToken(res.data.token)
    setUserInfo(res.data)
    return res
  }

  async function logout() {
    const { userApi } = await import('@/api')
    await userApi.logout()
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function updateUserInfo(data) {
    const { userApi } = await import('@/api')
    const res = await userApi.updateInfo(data)
    // 更新成功后，刷新本地用户信息
    const infoRes = await userApi.getInfo()
    setUserInfo(infoRes.data)
    return res
  }

  async function updateAvatar(file) {
    const { userApi } = await import('@/api')
    const res = await userApi.uploadAvatar(file)
    // 更新成功后，刷新本地用户信息
    const infoRes = await userApi.getInfo()
    setUserInfo(infoRes.data)
    return res
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    login,
    logout,
    updateUserInfo,
    updateAvatar
  }
})
