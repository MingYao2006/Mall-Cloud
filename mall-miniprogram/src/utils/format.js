/** 金额格式化：保留两位小数 */
export function formatPrice(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) {
    return '0.00'
  }
  return num.toFixed(2)
}

/** 截断过长文本 */
export function ellipsis(text, max = 20) {
  if (!text) {
    return ''
  }
  const str = String(text)
  return str.length > max ? str.slice(0, max) + '...' : str
}

/** 时间戳/时间字符串格式化为 YYYY-MM-DD HH:mm */
export function formatTime(value) {
  if (!value) {
    return ''
  }
  const date = value instanceof Date ? value : new Date(String(value).replace(/-/g, '/'))
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  const pad = (n) => String(n).padStart(2, '0')
  return (
    date.getFullYear() +
    '-' +
    pad(date.getMonth() + 1) +
    '-' +
    pad(date.getDate()) +
    ' ' +
    pad(date.getHours()) +
    ':' +
    pad(date.getMinutes())
  )
}

/** 订单状态文案：0-待支付 1-已支付 2-已发货 3-已完成 4-已取消 5-已退款 */
export const ORDER_STATUS_TEXT = {
  0: '待支付',
  1: '已支付',
  2: '已发货',
  3: '已完成',
  4: '已取消',
  5: '已退款'
}

export function orderStatusText(status) {
  return ORDER_STATUS_TEXT[status] || '未知状态'
}
