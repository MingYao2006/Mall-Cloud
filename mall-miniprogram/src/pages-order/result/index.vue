<template>
  <page-placeholder
    title="支付结果"
    desc="支付完成后展示结果并回跳订单列表"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '接收参数 status（success / fail）',
  '展示支付结果与订单号',
  '提供「查看订单」与「返回首页」按钮',
  '对应 Web 端 mall-frontend/src/views/PayResult.vue'
]
</script>
