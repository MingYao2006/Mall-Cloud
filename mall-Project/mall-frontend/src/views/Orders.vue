<template>
  <div class="orders-page">
    <h2>我的订单</h2>
    <el-tabs v-model="tab" @tab-change="loadOrders">
      <el-tab-pane label="全部订单" name="all" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="已付款" name="1" />
      <el-tab-pane label="已发货" name="2" />
      <el-tab-pane label="已完成" name="3" />
    </el-tabs>

    <div v-loading="loading">
      <div v-if="orders.length">
        <div v-for="o in orders" :key="o.id" class="order-card">
          <div class="order-hd">
            <span>订单号：{{ o.orderNo }}</span>
            <span class="order-time">{{ o.createTime }}</span>
            <span class="order-status" :style="{ color: statusColor(o.status) }">{{ statusText(o.status) }}</span>
          </div>
          <div class="order-body">
            <div v-for="item in o.orderItems" :key="item.id" class="order-item">
              <img :src="item.productImage || 'https://placehold.co/60x60/eee/999'" />
              <span class="item-name">{{ item.productName }}</span>
              <span class="item-price">¥{{ item.productPrice?.toFixed(2) }}</span>
              <span class="item-qty">x{{ item.quantity }}</span>
            </div>
          </div>
          <div class="order-ft">
            <span>共 {{ o.orderItems?.length || 0 }} 件</span>
            <span class="order-total">合计：<b>¥{{ o.totalAmount?.toFixed(2) }}</b></span>
            <div class="order-actions">
              <el-button v-if="o.status===0" type="primary" size="small" @click="doPay(o)">立即支付</el-button>
              <el-button v-if="o.status===0" size="small" @click="doCancel(o)">取消</el-button>
              <el-button v-if="o.status===2" type="success" size="small" @click="doReceive(o)">确认收货</el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无订单">
        <el-button type="primary" @click="$router.push('/home')">去逛逛</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi, paymentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const tab = ref('all')

const statusMap = { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消', 5: '已退款' }
const statusColorMap = { 0: '#e6a23c', 1: '#409eff', 2: '#67c23a', 3: '#909399', 4: '#f56c6c', 5: '#f56c6c' }
const statusText = (s) => statusMap[s] || '未知'
const statusColor = (s) => statusColorMap[s] || '#999'

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await orderApi.getList()
    let list = res.data || []
    if (tab.value !== 'all') list = list.filter(o => o.status === Number(tab.value))
    orders.value = list
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

const doPay = async (o) => {
  try {
    await ElMessageBox.confirm('将跳转到支付宝沙箱进行支付 ¥' + o.totalAmount?.toFixed(2), '支付确认', {
      confirmButtonText: '去支付',
      type: 'info'
    })
    // 调用支付宝支付接口
    const res = await paymentApi.alipay(o.id, '订单-' + o.orderNo?.substring(0, 12))
    if (res.data?.payForm) {
      // 在新窗口中打开支付宝支付页面
      const payWindow = window.open('', '_blank')
      payWindow.document.write(res.data.payForm)
      payWindow.document.close()
      ElMessage.success('已打开支付页面，请在新窗口中完成支付')
      // 轮询订单状态
      pollOrderStatus(o.id)
    }
  } catch (e) { if (e !== 'cancel') ElMessage.error('支付失败') }
}

const pollOrderStatus = async (orderId) => {
  const maxAttempts = 30
  let attempts = 0
  const timer = setInterval(async () => {
    attempts++
    try {
      const res = await orderApi.getDetail(orderId)
      if (res.data?.status === 1) {
        clearInterval(timer)
        ElMessage.success('支付成功！')
        loadOrders()
      }
    } catch (e) { /* ignore */ }
    if (attempts >= maxAttempts) clearInterval(timer)
  }, 2000)
}

const doCancel = async (o) => {
  try {
    await ElMessageBox.confirm('确定取消？', '提示', { type: 'warning' })
    await orderApi.cancel(o.id)
    ElMessage.success('已取消')
    loadOrders()
  } catch (e) { if (e !== 'cancel') ElMessage.error('取消失败') }
}

const doReceive = async (o) => {
  try { await ElMessageBox.confirm('确认收到货？', '提示', { type: 'info' }); ElMessage.success('已确认收货'); loadOrders() }
  catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

onMounted(() => loadOrders())
</script>

<style scoped>
.orders-page { max-width: 1000px; margin: 20px auto; }
.orders-page h2 { margin-bottom: 16px; }

.order-card { background: #fff; border-radius: 8px; margin-bottom: 16px; overflow: hidden; }
.order-hd { display: flex; align-items: center; gap: 20px; padding: 12px 20px; background: #fafafa; font-size: 13px; color: var(--jd-text-light); }
.order-time { margin-left: auto; }
.order-status { font-weight: 600; }

.order-body { padding: 16px 20px; }
.order-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; }
.order-item img { width: 60px; height: 60px; object-fit: cover; border-radius: 4px; }
.item-name { flex: 1; }
.item-price { color: var(--jd-text-light); }
.item-qty { color: var(--jd-text-lighter); }

.order-ft { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fafafa; border-top: 1px solid #f0f0f0; font-size: 13px; }
.order-total { margin-left: auto; }
.order-total b { font-size: 17px; color: var(--jd-red); }
.order-actions { display: flex; gap: 8px; }
</style>
