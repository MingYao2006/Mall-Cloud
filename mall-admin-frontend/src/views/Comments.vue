<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="searchType" style="width: 110px" @change="searchId = ''">
          <el-option label="商品ID" value="product" />
          <el-option label="用户ID" value="user" />
        </el-select>
        <el-input v-model="searchId" :placeholder="searchType === 'product' ? '按商品ID筛选评论' : '按用户ID筛选评论'" style="width: 200px" clearable @clear="loadList" @keyup.enter="loadList" />
        <el-button @click="loadList"><el-icon><Search /></el-icon> 查询</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="productId" label="商品ID" width="80" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="content" label="评论内容" min-width="240" show-overflow-tooltip />
      <el-table-column prop="rating" label="评分" width="80">
        <template #default="{ row }">
          <el-rate :model-value="row.rating" disabled size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <span :class="['status-tag', row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning']">
            {{ row.status === 1 ? '已通过' : row.status === 2 ? '已拒绝' : '待审核' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="160" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 1" size="small" type="success" @click="handleAudit(row, 1)">通过</el-button>
          <el-button v-if="row.status !== 2" size="small" type="danger" @click="handleAudit(row, 2)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { commentApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const searchType = ref('product')
const searchId = ref('')

async function loadList() {
  if (!searchId.value) {
    ElMessage.info('请输入商品ID进行查询')
    return
  }
  loading.value = true
  try {
    const res = searchType.value==='product'?
        await commentApi.getProductComments(searchId.value) :
        await commentApi.getUserComments(searchId.value)
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleAudit(row, status) {
  try {
    await commentApi.audit(row.id, status)
    ElMessage.success(status === 1 ? '已通过' : '已拒绝')
    await loadList()
  } catch (e) { /* handled */ }
}

onMounted(() => {
  // 默认加载商品ID为1的评论
  searchId.value = '1'
  loadList()
})
</script>
