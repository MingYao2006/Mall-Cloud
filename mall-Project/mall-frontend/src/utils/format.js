// 价格格式化：后端以「元」返回（BigDecimal），这里统一成 ¥ 文案
export function formatPrice(value) {
  if (value == null || value === '') return '¥0'
  const numValue = Number(value)
  if (Number.isNaN(numValue)) return '¥0'
  return '¥' + (Number.isInteger(numValue) ? numValue : numValue.toFixed(2))
}

// 订单状态：0-待支付 1-已支付 2-已发货 3-已完成 4-已取消 5-已退款
export const ORDER_STATUS = {
  0: '待支付',
  1: '已支付',
  2: '已发货',
  3: '已完成',
  4: '已取消',
  5: '已退款'
}

export function orderStatusText(status) {
  return ORDER_STATUS[status] || '未知状态'
}

// 时间格式化：2026-09-23T16:00:00 → 2026-09-23 16:00:00
export function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}
