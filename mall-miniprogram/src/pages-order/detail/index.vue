<template>
  <page-placeholder
    title="订单详情"
    desc="orderApi.getDetail(orderId) 返回 OrderDetailVO"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '展示订单号、下单时间、订单状态、收货信息、备注',
  '展示订单商品明细与金额（商品总额 / 运费 / 优惠 / 实付）',
  '待支付状态提供支付入口，调用 paymentApi.wxpay()',
  '提供取消订单（orderApi.cancel）'
]
</script>
