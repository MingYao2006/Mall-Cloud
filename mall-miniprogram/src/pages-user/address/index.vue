<template>
  <page-placeholder
    title="收货地址"
    desc="后端地址簿模块尚未实现，需要先补接口"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '后端新增 user_address 表与 AddressController（见开发计划 P1-7）',
  '地址列表 / 新增 / 编辑 / 删除 / 设为默认',
  '订单确认页可跳转本页选择地址并回填收货信息',
  '当前若未实现，订单确认页只能手工填写收货信息'
]
</script>
