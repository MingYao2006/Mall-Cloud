<script setup>
// 注册页与登录页是同一张卡片（参考项目 af-mall-web 的 Login.vue 自带登录/注册切换）
// 这里直接复用，避免两套样式分叉
import Login from './Login.vue'
</script>

<template>
  <Login initial-mode="register" />
</template>
