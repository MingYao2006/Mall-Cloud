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
      <div class="pending-head">
        <h3 style="margin-bottom: 16px;">
          待审核评论
          <span v-if="pendingTotal" class="pending-badge">{{ pendingTotal }}</span>
        </h3>
        <el-button link type="primary" @click="$router.push('/comments')">查看全部</el-button>
      </div>
      <el-table :data="pendingComments" v-loading="pendingLoading" size="small" empty-text="暂无待审核评论">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="productId" label="商品ID" width="90" />
        <el-table-column prop="content" label="评论内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="auditPending(row, 1)">通过</el-button>
            <el-button size="small" type="danger" @click="auditPending(row, 2)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card">
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
import { productApi, categoryApi, orderApi, couponApi, commentApi } from '@/api'
import { ElMessage } from 'element-plus'

const statCards = ref([
  { title: '商品总数', value: '-', icon: 'Goods', bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { title: '分类总数', value: '-', icon: 'Menu', bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
  { title: '订单总数', value: '-', icon: 'List', bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { title: '优惠券数', value: '-', icon: 'Ticket', bg: 'linear-gradient(135deg, #43e97b, #38f9d7)' }
])

// 待审核评论（首页优先展示）
const pendingComments = ref([])
const pendingTotal = ref(0)
const pendingLoading = ref(false)

const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')

async function loadPendingComments () {
  pendingLoading.value = true
  try {
    const res = await commentApi.getPage({ status: 0, pageNum: 1, pageSize: 5 })
    const data = res.data || {}
    pendingComments.value = data.list || []
    pendingTotal.value = Number(data.total || 0)
  } catch (e) {
    pendingComments.value = []
    pendingTotal.value = 0
  } finally {
    pendingLoading.value = false
  }
}

async function auditPending (row, status) {
  try {
    await commentApi.audit(row.id, status)
    ElMessage.success(status === 1 ? '已通过' : '已拒绝')
    loadPendingComments()
  } catch (e) { /* 拦截器已提示 */ }
}

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
  loadPendingComments()
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

.pending-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pending-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 10px;
  border-radius: 12px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 13px;
  vertical-align: middle;
}
</style>
