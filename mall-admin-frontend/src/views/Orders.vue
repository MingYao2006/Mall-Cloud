<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchUserId" placeholder="按用户ID筛选" style="width: 180px" clearable @clear="handleSearch" @keyup.enter="handleSearch" />
        <el-select v-model="searchStatus" placeholder="订单状态" clearable style="width: 140px" @change="handleSearch">
          <el-option label="待付款" :value="0" />
          <el-option label="已付款" :value="1" />
          <el-option label="已发货" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
        <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="订单ID" width="80" />
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="totalAmount" label="总金额" width="100">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>
      <el-table-column prop="payAmount" label="实付金额" width="100">
        <template #default="{ row }">¥{{ row.payAmount }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <span :class="['status-tag', statusType(row.status)]">{{ statusText(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="receiverName" label="收货人" width="100" />
      <el-table-column prop="receiverPhone" label="联系电话" width="120" />
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row)">详情</el-button>
          <el-button v-if="row.status === 0" size="small" type="warning" @click="handleCancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        :page-sizes="[10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="720px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ detail.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">¥{{ detail.payAmount }}</el-descriptions-item>
        <el-descriptions-item label="运费">¥{{ detail.freightAmount }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detail.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ detail.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.note || '-' }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">¥{{ detail.discountAmount }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.paymentTypeDesc }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail.paymentTime }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ detail.deliveryTime }}</el-descriptions-item>
        <el-descriptions-item label="收货时间">{{ detail.receiveTime }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detail && detail.orderItems" class="order-items">
        <h4 style="margin: 16px 0 8px">商品明细</h4>
        <el-table :data="detail.orderItems" size="small" border>
          <el-table-column prop="productName" label="商品名称" min-width="160" />
          <el-table-column prop="productPrice" label="单价" width="100">
            <template #default="{ row }">¥{{ row.productPrice }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="totalPrice" label="小计" width="100">
            <template #default="{ row }">¥{{ row.totalPrice }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { orderApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const searchUserId = ref('')
const searchStatus = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const detail = ref(null)

function statusText(status) {
  return { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消' }[status] || '未知'
}
function statusType(status) {
  return { 0: 'warning', 1: 'success', 2: 'info', 3: 'success', 4: 'danger' }[status] || 'info'
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handlePageChange(p) {
  pageNum.value = p
  loadList()
}

function handleSizeChange(s) {
  pageSize.value = s
  pageNum.value = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchUserId.value) params.userId = searchUserId.value
    if (searchStatus.value !== null && searchStatus.value !== '') params.status = searchStatus.value
    const res = await orderApi.getList(params)
    const data = res.data || {}
    tableData.value = data.list || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

async function viewDetail(row) {
  try {
    const res = await orderApi.getDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) { /* handled */ }
}

async function handleCancel(row) {
  await ElMessageBox.confirm(`确定取消订单"${row.orderNo}"吗？`, '提示', { type: 'warning' })
  try {
    await orderApi.cancel(row.id)
    ElMessage.success('订单已取消')
    await loadList()
  } catch (e) { /* handled */ }
}

onMounted(() => loadList())
</script>

<style scoped>
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
