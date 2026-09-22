import { TOKEN_KEY, USER_INFO_KEY } from '@/config'

/** 读取本地 token */
export function getToken() {
  try {
    return uni.getStorageSync(TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

/** 写入本地 token */
export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token || '')
}

/** 读取本地用户信息（容错解析，对应 Web 端 stores/user.js 的安全解析） */
export function getStoredUserInfo() {
  try {
    const raw = uni.getStorageSync(USER_INFO_KEY)
    if (!raw || raw === 'undefined' || raw === 'null') {
      return null
    }
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (e) {
    return null
  }
}

/** 写入本地用户信息 */
export function setStoredUserInfo(info) {
  if (!info) {
    uni.removeStorageSync(USER_INFO_KEY)
    return
  }
  uni.setStorageSync(USER_INFO_KEY, JSON.stringify(info))
}

/** 清空登录态 */
export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_INFO_KEY)
}

/** 是否已登录 */
export function isLogin() {
  return !!getToken()
}
