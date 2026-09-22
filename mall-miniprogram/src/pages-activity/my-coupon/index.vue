<template>
  <page-placeholder
    title="我的优惠券"
    desc="couponApi.getMy(status)"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '按状态筛选：未使用 / 已使用 / 已过期',
  '展示券面额、使用门槛、有效期',
  '下单时选择可用券（当前 CreateOrderRequest 尚无 couponId 字段，需后端补充）'
]
</script>
