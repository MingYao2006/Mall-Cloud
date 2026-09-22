<template>
  <page-placeholder
    title="我的订单"
    desc="orderApi.getList() 返回当前登录用户的订单"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '调用 orderApi.getList() 拉取订单列表（OrderVO）',
  '按状态分 Tab：0-待支付 1-已支付 2-已发货 3-已完成 4-已取消 5-已退款',
  '待支付订单提供「去支付」与「取消订单」入口',
  '点击进入订单详情 pages-order/detail/index'
]
</script>
