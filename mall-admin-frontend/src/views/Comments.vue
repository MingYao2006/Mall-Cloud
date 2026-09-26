<template>
  <div class="page-card">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane v-for="tab in tabs" :key="tab.value" :name="tab.value" :label="tab.label">
        <template #label>
          <span>{{ tab.label }}</span>
          <el-badge v-if="counts[tab.value]" :value="counts[tab.value]" class="tab-badge" />
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="searchType" style="width: 110px" @change="handleSearch">
          <el-option label="商品ID" value="product" />
          <el-option label="用户ID" value="user" />
        </el-select>
        <el-input
            v-model="searchId"
            :placeholder="searchType === 'product' ? '按商品ID筛选评论' : '按用户ID筛选评论'"
            style="width: 200px"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon> 查询
        </el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe empty-text="暂无评论">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="productId" label="商品ID" width="90" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="content" label="评论内容" min-width="240" show-overflow-tooltip />
      <el-table-column label="图片" width="90">
        <template #default="{ row }">
          <span v-if="!row.images">-</span>
          <span v-else>{{ String(row.images).split(',').filter(Boolean).length }} 张</span>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="150">
        <template #default="{ row }">
          <el-rate :model-value="row.rating" disabled size="small" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span :class="['status-tag', statusClass(row.status)]">{{ statusText(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 1" size="small" type="success" @click="handleAudit(row, 1)">通过</el-button>
          <el-button v-if="row.status !== 2" size="small" type="danger" @click="handleAudit(row, 2)">拒绝</el-button>
          <el-button v-if="row.status !== 0" size="small" @click="handleAudit(row, 0)">置为待审</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="loadList"
        @size-change="handleSearch"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { commentApi } from '@/api'
import { ElMessage } from 'element-plus'

const tabs = [
  { label: '待审核', value: '0' },
  { label: '已审核', value: '1' },
  { label: '已拒绝', value: '2' }
]

const activeTab = ref('0')
const counts = reactive({ 0: 0, 1: 0, 2: 0 })
const loading = ref(false)
const tableData = ref([])
const searchType = ref('product')
const searchId = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusText = (s) => (s === 1 ? '已通过' : s === 2 ? '已拒绝' : '待审核')
const statusClass = (s) => (s === 1 ? 'success' : s === 2 ? 'danger' : 'warning')
const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')

/** 搜索条件：商品ID/用户ID 二选一，跟 tab 组合后一起发给后端 */
function searchParams () {
  const id = String(searchId.value || '').trim()
  return id ? { [searchType.value === 'product' ? 'productId' : 'userId']: Number(id) } : {}
}

async function loadList () {
  loading.value = true
  try {
    const res = await commentApi.getPage({
      status: Number(activeTab.value),
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...searchParams()
    })
    const data = res.data || {}
    tableData.value = data.list || []
    total.value = Number(data.total || 0)
  } catch (e) {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadCounts () {
  try {
    const res = await commentApi.getCount(searchParams())
    Object.assign(counts, res.data || {})
  } catch (e) { /* 统计失败不影响主流程 */ }
}

function handleTabChange () {
  pageNum.value = 1
  loadList()
}

function handleSearch () {
  pageNum.value = 1
  loadList()
  loadCounts()
}

function resetSearch () {
  searchId.value = ''
  handleSearch()
}

async function handleAudit (row, status) {
  try {
    await commentApi.audit(row.id, status)
    ElMessage.success(status === 1 ? '已通过' : status === 2 ? '已拒绝' : '已重置为待审核')
    loadList()
    loadCounts()
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => {
  loadList()
  loadCounts()
})
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.toolbar-left { display: flex; gap: 10px; align-items: center; }
.tab-badge { margin-left: 6px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.status-tag { padding: 2px 8px; border-radius: 10px; font-size: 12px; }
.status-tag.success { color: #67c23a; background: rgba(103, 194, 58, 0.1); }
.status-tag.warning { color: #e6a23c; background: rgba(230, 162, 60, 0.1); }
.status-tag.danger { color: #f56c6c; background: rgba(245, 108, 108, 0.1); }
</style>