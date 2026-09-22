<template>
  <page-placeholder
    title="修改密码"
    desc="userApi.updatePassword()"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '原密码 / 新密码 / 确认新密码三个输入框',
  '前端校验两次新密码一致、长度符合要求',
  '调用 userApi.updatePassword()，成功后清空本地态并要求重新登录'
]
</script>
