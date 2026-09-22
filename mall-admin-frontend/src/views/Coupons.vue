<template>
  <div class="page-card">
    <div class="toolbar">
      <span style="font-size: 16px; font-weight: 600;">优惠券列表</span>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="name" label="优惠券名称" min-width="180"/>
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          {{ row.type === 1 ? '满减券' : row.type === 2 ? '折扣券' : '代金券' }}
        </template>
      </el-table-column>
      <el-table-column prop="value" label="面额/折扣" width="100">
        <template #default="{ row }">
          {{ row.type === 2 ? row.value + '折' : '¥' + row.value }}
        </template>
      </el-table-column>
      <el-table-column prop="minAmount" label="门槛" width="100">
        <template #default="{ row }">满¥{{ row.minAmount }}</template>
      </el-table-column>
      <el-table-column prop="totalStock" label="总库存" width="80"/>
      <el-table-column prop="availableStock" label="剩余库存" width="80"/>
      <el-table-column prop="startTime" label="开始时间" width="160"/>
      <el-table-column prop="endTime" label="结束时间" width="160"/>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <span :class="['status-tag', row.status === 1 ? 'success' : 'danger']">
            {{ row.status === 1 ? '进行中' : '已停止' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="detailVisible" title="优惠券详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ detail?.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail?.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ detail?.type === 1 ? '满减券' : detail?.type === 2 ? '折扣券' : '代金券' }}
        </el-descriptions-item>
        <el-descriptions-item label="面额/折扣">
          {{ detail?.type === 2 ? detail?.value + '折' : '¥' + detail?.value }}
        </el-descriptions-item>
        <el-descriptions-item label="使用门槛">满¥{{ detail?.minAmount }}</el-descriptions-item>
        <el-descriptions-item label="每人限领">{{ detail?.limitPerUser }}</el-descriptions-item>
        <el-descriptions-item label="总库存">{{ detail?.totalStock }}</el-descriptions-item>
        <el-descriptions-item label="剩余库存">{{ detail?.availableStock }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail?.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail?.endTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detail?.status === 1 ? '进行中' : '已停止' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail?.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {couponApi} from '@/api'

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const detail = ref(null)


async function loadList() {
  loading.value = true
  try {
    const res = await couponApi.getList()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function viewDetail(row) {
  try {
    const res = await couponApi.getDetail(row.id)
    detail.value = res.data || null
    detailVisible.value = true
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理，这里不用再写
  }
}


onMounted(() => loadList())
</script>
