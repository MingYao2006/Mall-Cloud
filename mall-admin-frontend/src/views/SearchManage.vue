<template>
  <div>
    <div class="page-card">
      <h3 style="margin-bottom: 16px;">ES 数据同步</h3>
      <p style="color: #666; margin-bottom: 16px;">将数据库中的商品数据全量导入到 Elasticsearch，用于商品搜索功能。</p>
      <el-button type="primary" size="large" :loading="importing" @click="handleImport">
        <el-icon><Upload /></el-icon> 全量导入商品数据到 ES
      </el-button>
      <el-alert v-if="importResult !== null" :title="importMsg" :type="importResult ? 'success' : 'error'" style="margin-top: 16px;" />
    </div>

    <div class="page-card">
      <h3 style="margin-bottom: 16px;">搜索测试</h3>
      <div style="display: flex; gap: 12px; margin-bottom: 16px;">
        <el-input v-model="searchKeyword" placeholder="输入关键词搜索" style="width: 300px" @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 搜索</el-button>
      </div>
      <el-table :data="searchResults" v-loading="searching" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { searchApi } from '@/api'
import { ElMessage } from 'element-plus'

const importing = ref(false)
const importResult = ref(null)
const importMsg = ref('')
const searchKeyword = ref('')
const searchResults = ref([])
const searching = ref(false)

async function handleImport() {
  importing.value = true
  importResult.value = null
  try {
    const res = await searchApi.importAll()
    importResult.value = true
    importMsg.value = `导入成功，共导入 ${res.data} 条商品数据`
    ElMessage.success('导入完成')
  } catch (e) {
    importResult.value = false
    importMsg.value = '导入失败，请检查搜索服务是否正常'
  } finally {
    importing.value = false
  }
}

async function handleSearch() {
  if (!searchKeyword.value) {
    ElMessage.info('请输入搜索关键词')
    return
  }
  searching.value = true
  try {
    const res = await searchApi.searchProducts(searchKeyword.value)
    searchResults.value = res.data || []
  } finally {
    searching.value = false
  }
}
</script>
