<template>
  <page-placeholder
    title="限时秒杀"
    desc="后端秒杀实为「秒杀抢券」链路，与优惠券共用接口"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '拉取可秒杀券列表：couponApi.getList()',
  '抢券调用 seckillApi.seckillCoupon(couponId) 或 couponApi.seckill(couponId)',
  '注意：Web 端 api/index.js 中的 seckill/list、seckill/{id} 在后端并不存在',
  '抢购按钮需防连点，配合后端 Sentinel 限流规则'
]
</script>
