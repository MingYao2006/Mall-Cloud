<template>
  <page-placeholder
    title="确认订单"
    desc="从购物车结算或商品详情页立即购买进入"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '读取来源参数：购物车已选商品 或 productId + quantity',
  '收货信息表单，对应 CreateOrderRequest 的 receiverName / receiverPhone / receiverAddress',
  '可选：接入收货地址簿（后端 user_address 表尚未实现，见开发计划 P1-7）',
  '提交 orderApi.create() 创建订单，成功后进入支付流程',
  '备注字段 note 与支付方式 paymentType（1-微信 2-支付宝）'
]
</script>
