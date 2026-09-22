/**
 * 全局环境配置
 *
 * 开发环境：微信开发者工具需勾选「详情 → 本地设置 → 不校验合法域名」
 * 生产环境：域名必须为 HTTPS 且已在微信公众平台配置为 request 合法域名
 */

// 后端网关地址（开发）
const DEV_ORIGIN = 'http://localhost:9000'

// 后端网关地址（生产，部署前替换为真实域名）
const PROD_ORIGIN = 'https://your-domain.com'

const IS_PROD = process.env.NODE_ENV === 'production'

export const ORIGIN = IS_PROD ? PROD_ORIGIN : DEV_ORIGIN

// C 端接口前缀（网关 C 端路由）
let apiBase = ORIGIN + '/api/c'

// #ifdef H5
// H5 开发环境走 Vite 代理，避免跨域
if (!IS_PROD) {
  apiBase = '/api/c'
}
// #endif

export const BASE_URL = apiBase

// 本地存储 key
export const TOKEN_KEY = 'mall_token'
export const USER_INFO_KEY = 'mall_user_info'
