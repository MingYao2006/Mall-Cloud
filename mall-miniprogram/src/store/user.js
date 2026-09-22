import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { userApi } from '@/api/user'
import * as auth from '@/utils/auth'

/**
 * 标记「微信授权阶段」的错误
 *
 * 后端业务错误已由 utils/request.js 统一 toast，
 * 页面只需对微信授权本身的失败额外提示，避免重复弹窗。
 */
function markAuthError(message) {
  const error = new Error(message)
  error.wxAuthFailed = true
  return error
}

/**
 * 用户状态
 *
 * 与 Web 端 mall-frontend/src/stores/user.js 保持一致的状态语义，
 * 差异在于存储介质由 localStorage 换成 uni.getStorageSync（小程序无 localStorage）。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null)

  const isLogin = computed(() => !!token.value)
  const userId = computed(() => {
    const info = userInfo.value
    return info ? info.id || info.userId || null : null
  })

  /** 应用启动时从本地存储恢复登录态 */
  function restore() {
    token.value = auth.getToken()
    userInfo.value = auth.getStoredUserInfo()
  }

  function setToken(value) {
    token.value = value || ''
    auth.setToken(token.value)
  }

  function setUserInfo(info) {
    userInfo.value = info || null
    auth.setStoredUserInfo(userInfo.value)
  }

  function clear() {
    token.value = ''
    userInfo.value = null
    auth.clearAuth()
  }

  /** 登录接口返回结构：data = { token, ...用户信息 } */
  function applyLoginResult(data) {
    const payload = data || {}
    setToken(payload.token)
    setUserInfo(payload)
  }

  /** 账号密码登录 */
  async function login(form) {
    const res = await userApi.login(form)
    applyLoginResult(res.data)
    return res
  }

  /** 验证码登录 */
  async function loginByCode(phone, code) {
    const res = await userApi.loginByCode(phone, code)
    applyLoginResult(res.data)
    return res
  }

  /**
   * 微信小程序登录
   *
   * 1. uni.login 取临时 code
   * 2. 调后端 /users/wx-login 换 token（后端接口待实现，见开发计划 P0-1）
   */
  async function wxLogin(profile = {}) {
    let loginRes
    try {
      loginRes = await new Promise((resolve, reject) => {
        uni.login({ provider: 'weixin', success: resolve, fail: reject })
      })
    } catch (e) {
      throw markAuthError('微信授权失败，请重试')
    }
    if (!loginRes || !loginRes.code) {
      throw markAuthError('未获取到微信登录凭证')
    }
    const res = await userApi.wxLogin({
      code: loginRes.code,
      nickname: profile.nickname || '',
      avatarUrl: profile.avatarUrl || ''
    })
    applyLoginResult(res.data)
    return res
  }

  /** 退出登录 */
  async function logout() {
    try {
      await userApi.logout()
    } catch (e) {
      // 退出接口失败不阻塞本地清理
    }
    clear()
  }

  /** 拉取最新用户信息 */
  async function refreshUserInfo() {
    const res = await userApi.getInfo()
    const info = res.data
    // 保留原有 token，避免被用户信息覆盖
    setUserInfo(Object.assign({}, userInfo.value || {}, info))
    return res
  }

  /** 更新用户资料 */
  async function updateUserInfo(data) {
    const res = await userApi.updateInfo(data)
    await refreshUserInfo()
    return res
  }

  /** 上传头像并刷新本地用户信息 */
  async function updateAvatar(filePath) {
    const res = await userApi.uploadAvatar(filePath)
    await refreshUserInfo()
    return res
  }

  /** 登录守卫：未登录则跳转登录页并返回 false */
  function ensureLogin() {
    if (isLogin.value) {
      return true
    }
    uni.navigateTo({ url: '/pages-user/login/index' })
    return false
  }

  return {
    token,
    userInfo,
    isLogin,
    userId,
    restore,
    setToken,
    setUserInfo,
    clear,
    login,
    loginByCode,
    wxLogin,
    logout,
    refreshUserInfo,
    updateUserInfo,
    updateAvatar,
    ensureLogin
  }
})
