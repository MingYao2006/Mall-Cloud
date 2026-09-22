import { get, post, put, upload } from '@/utils/request'

/**
 * 用户模块接口
 *
 * 对应后端：网关 /api/c/users/** → mall-Project UserController → mall-user-service
 * 其中 login / register / code / login/code 在网关与聚合层均为免登录白名单。
 */
export const userApi = {
  /** 账号密码登录 */
  login: (data) => post('/users/login', data),

  /** 注册 */
  register: (data) => post('/users/register', data),

  /** 退出登录 */
  logout: () => post('/users/logout'),

  /** 获取当前登录用户信息（需登录） */
  getInfo: () => get('/users/info'),

  /** 更新用户资料（需登录） */
  updateInfo: (data) => put('/users/info', data),

  /** 修改密码（需登录） */
  updatePassword: (data) => put('/users/password', data),

  /** 上传头像（需登录），filePath 为 uni.chooseImage 返回的临时路径 */
  uploadAvatar: (filePath) => upload('/users/avatar', filePath),

  /** 发送短信验证码 */
  sendCode: (phone) => post('/users/code', null, { params: { phone } }),

  /** 验证码登录 */
  loginByCode: (phone, code) => post('/users/login/code', null, { params: { phone, code } }),

  /**
   * 微信小程序登录（待后端实现）
   *
   * 后端契约：POST /api/c/users/wx-login
   * 请求体：{ code, nickname, avatarUrl }
   * 返回：与账号密码登录一致，data 中包含 token 与用户信息
   *
   * 实现要点见《小程序开发计划.md》第五章 P0-1：
   * 需在 mall-user-service 调用 jscode2session 换取 openid，
   * 并在网关 C_WHITE_LIST 与聚合层 JwtInterceptor.WHITE_LIST 中加入该路径。
   */
  wxLogin: (data) => post('/users/wx-login', data)
}

export default userApi
