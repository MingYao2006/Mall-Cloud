<template>
  <page-placeholder
    title="商品评论"
    desc="commentApi.getByProduct(productId)"
    :todos="todos"
  />
</template>

<script setup>
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const todos = [
  '展示商品评论列表（CommentController: GET /comment/product/{productId}）',
  '已购用户可发表评论：commentApi.add()',
  '评论接口当前未在网关白名单中，需登录后访问'
]
</script>
