import { BASE_URL, ORIGIN } from '@/config'
import { clearAuth, getToken } from './auth'

const TIMEOUT = 10000

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

/** 拼接查询参数 */
function buildQuery(params) {
  if (!params) {
    return ''
  }
  const parts = Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map((key) => encodeURIComponent(key) + '=' + encodeURIComponent(params[key]))
  return parts.length ? '?' + parts.join('&') : ''
}

/** 拼接完整请求地址 */
function buildUrl(url) {
  if (/^https?:\/\//.test(url)) {
    return url
  }
  return BASE_URL + url
}

/** 登录失效：清空本地态并跳转登录页 */
function redirectToLogin() {
  clearAuth()
  const pages = getCurrentPages()
  const currentRoute = pages.length ? pages[pages.length - 1].route || '' : ''
  if (currentRoute.indexOf('pages-user/login') !== -1) {
    return
  }
  toast('登录已过期，请重新登录')
  uni.navigateTo({ url: '/pages-user/login/index' })
}

/**
 * 统一请求方法
 *
 * 与 Web 端 axios 封装保持一致：
 * 1. 自动注入 Authorization: Bearer <token>
 * 2. 统一解包后端 Result 结构 { code, message, data }
 * 3. 401 清理登录态并跳转登录页，403 提示无权限
 * 4. 业务失败统一 toast 并以 reject 抛出
 *
 * @param {Object}  options
 * @param {string}  options.url       相对 BASE_URL 的路径，或完整 http(s) 地址
 * @param {string}  [options.method]  请求方法，默认 GET
 * @param {Object}  [options.data]    请求体（POST/PUT/DELETE）
 * @param {Object}  [options.params]  查询参数
 * @param {Object}  [options.header]  自定义请求头
 * @param {boolean} [options.showError] 业务失败是否自动 toast，默认 true
 * @returns {Promise<Object>} 后端 Result 对象（含 code / message / data）
 */
export function request(options) {
  const {
    url,
    method = 'GET',
    data,
    params,
    header = {},
    showError = true
  } = options || {}

  const token = getToken()
  const finalUrl = buildUrl(url) + buildQuery(params)

  const finalHeader = Object.assign({}, header)
  if (data !== undefined && data !== null) {
    finalHeader['Content-Type'] = 'application/json'
  }
  if (token) {
    finalHeader.Authorization = 'Bearer ' + token
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: finalUrl,
      method: method.toUpperCase(),
      data,
      header: finalHeader,
      timeout: TIMEOUT,
      success: (res) => {
        // 网关鉴权失败
        if (res.statusCode === 401) {
          redirectToLogin()
          reject(new Error('登录已过期'))
          return
        }
        if (res.statusCode === 403) {
          toast('无权限访问该资源')
          reject(new Error('无权限访问该资源'))
          return
        }
        if (res.statusCode !== 200) {
          if (showError) {
            toast('请求失败（HTTP ' + res.statusCode + '）')
          }
          reject(new Error('HTTP ' + res.statusCode))
          return
        }

        const body = res.data
        // 非 Result 结构（例如支付表单等纯文本响应）直接返回
        if (!body || typeof body !== 'object' || typeof body.code === 'undefined') {
          resolve(body)
          return
        }
        if (body.code !== 200) {
          if (showError) {
            toast(body.message || '请求失败')
          }
          reject(new Error(body.message || '请求失败'))
          return
        }
        resolve(body)
      },
      fail: (err) => {
        if (showError) {
          toast(err && err.errMsg ? '网络异常：' + err.errMsg : '网络异常，请稍后重试')
        }
        reject(err)
      }
    })
  })
}

export function get(url, params, options) {
  return request(Object.assign({ url, method: 'GET', params }, options))
}

export function post(url, data, options) {
  return request(Object.assign({ url, method: 'POST', data }, options))
}

export function put(url, data, options) {
  return request(Object.assign({ url, method: 'PUT', data }, options))
}

export function del(url, params, options) {
  return request(Object.assign({ url, method: 'DELETE', params }, options))
}

/**
 * 文件上传（小程序端替代 Web 端的 axios + FormData 方案）
 *
 * @param {string} url          相对 BASE_URL 的路径
 * @param {string} filePath     uni.chooseImage 返回的本地临时路径
 * @param {Object} [formData]   额外表单字段
 * @param {string} [name]       文件字段名，默认 file
 */
export function upload(url, filePath, formData = {}, name = 'file') {
  const token = getToken()
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: buildUrl(url),
      filePath,
      name,
      formData,
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success: (res) => {
        if (res.statusCode === 401) {
          redirectToLogin()
          reject(new Error('登录已过期'))
          return
        }
        let body = res.data
        try {
          body = JSON.parse(res.data)
        } catch (e) {
          // 保持原始字符串
        }
        if (body && typeof body === 'object' && typeof body.code !== 'undefined') {
          if (body.code !== 200) {
            toast(body.message || '上传失败')
            reject(new Error(body.message || '上传失败'))
            return
          }
        }
        resolve(body)
      },
      fail: (err) => {
        toast('上传失败，请重试')
        reject(err)
      }
    })
  })
}

/**
 * 图片地址补全
 *
 * 后端用户头像返回的是相对路径（如 /uploads/avatar/xxx.jpg），
 * 小程序 <image src> 无法直接渲染，此处统一补全为完整地址。
 */
export function resolveFileUrl(path) {
  if (!path) {
    return ''
  }
  if (/^https?:\/\//.test(path) || path.indexOf('data:') === 0) {
    return path
  }
  return ORIGIN + (path.charAt(0) === '/' ? path : '/' + path)
}

export default request
