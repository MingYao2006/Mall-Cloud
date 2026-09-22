<template>
  <page-placeholder
    title="个人资料"
    desc="对应 Web 端 mall-frontend/src/views/Profile.vue"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '读取并展示 userStore.userInfo（昵称 / 手机号 / 邮箱 / 性别）',
  '修改资料调用 userApi.updateInfo()',
  '头像更换：uni.chooseImage 后调用 userStore.updateAvatar()（内部走 uni.uploadFile）',
  '头像地址为相对路径时需要 resolveFileUrl 补全域名'
]
</script>
