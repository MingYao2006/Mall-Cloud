<template>
  <page-placeholder
    title="优惠券"
    desc="couponApi.getList() 免登录可浏览"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '展示可领取优惠券列表',
  '抢券调用 couponApi.seckill(couponId)',
  '抢券成功后跳转「我的优惠券」'
]
</script>
