<template>
  <div>
    <div class="page-card">
      <h3 style="margin-bottom: 16px;">系统概览</h3>
      <el-row :gutter="16">
        <el-col :span="6" v-for="card in statCards" :key="card.title">
          <div class="stat-card" :style="{ background: card.bg }">
            <el-icon :size="32" style="color: #fff;"><component :is="card.icon" /></el-icon>
            <div>
              <div style="font-size: 24px; font-weight: 700; color: #fff;">{{ card.value }}</div>
              <div style="font-size: 13px; color: rgba(255,255,255,0.8);">{{ card.title }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="page-card">
      <h3 style="margin-bottom: 16px;">快捷操作</h3>
      <el-space wrap>
        <el-button type="primary" @click="$router.push('/products')">
          <el-icon><Goods /></el-icon> 商品管理
        </el-button>
        <el-button type="primary" @click="$router.push('/categories')">
          <el-icon><Menu /></el-icon> 分类管理
        </el-button>
        <el-button type="primary" @click="$router.push('/orders')">
          <el-icon><List /></el-icon> 订单管理
        </el-button>
        <el-button type="primary" @click="$router.push('/search')">
          <el-icon><Search /></el-icon> ES数据导入
        </el-button>
      </el-space>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { productApi, categoryApi, orderApi, couponApi } from '@/api'

const statCards = ref([
  { title: '商品总数', value: '-', icon: 'Goods', bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { title: '分类总数', value: '-', icon: 'Menu', bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
  { title: '订单总数', value: '-', icon: 'List', bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { title: '优惠券数', value: '-', icon: 'Ticket', bg: 'linear-gradient(135deg, #43e97b, #38f9d7)' }
])

onMounted(async () => {
  try {
    const [products, categories, orders, coupons] = await Promise.allSettled([
      productApi.getList(),
      categoryApi.getList(),
      orderApi.getList(),
      couponApi.getList()
    ])
    if (products.status === 'fulfilled') statCards.value[0].value = products.value.data?.length || 0
    if (categories.status === 'fulfilled') statCards.value[1].value = categories.value.data?.length || 0
    if (orders.status === 'fulfilled') statCards.value[2].value = orders.value.data?.length || 0
    if (coupons.status === 'fulfilled') statCards.value[3].value = coupons.value.data?.length || 0
  } catch (e) {
    // 忽略统计加载失败
  }
})
</script>

<style scoped>
.stat-card {
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 80px;
}
</style>
