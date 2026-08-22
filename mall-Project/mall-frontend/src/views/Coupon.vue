<template>
  <div class="coupon-page">
    <div class="page-header">
      <h2><el-icon><Ticket /></el-icon> 优惠券秒杀</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showMyCoupons = true">
          <el-icon><User /></el-icon> 我的优惠券 ({{ myCoupons.length }})
        </el-button>
      </div>
    </div>

    <!-- 倒计时区域 -->
    <div class="countdown-banner">
      <div class="countdown-content">
        <span class="countdown-label">限时抢购 距结束</span>
        <div class="countdown-timer">
          <span class="time-block">{{ countdown.hh }}</span>
          <span class="time-sep">:</span>
          <span class="time-block">{{ countdown.mm }}</span>
          <span class="time-sep">:</span>
          <span class="time-block">{{ countdown.ss }}</span>
        </div>
      </div>
    </div>

    <!-- 优惠券列表 -->
    <div class="coupon-list" v-loading="loading">
      <el-empty v-if="!loading && coupons.length === 0" description="暂无优惠券活动" />
      
      <div v-for="coupon in coupons" :key="coupon.id" class="coupon-card">
        <div class="coupon-left" :class="getCouponTypeClass(coupon.type)">
          <div class="coupon-value">
            <span class="currency">¥</span>
            <span class="amount">{{ coupon.value }}</span>
          </div>
          <div class="coupon-condition" v-if="coupon.type === 1">
            满{{ coupon.minAmount }}可用
          </div>
          <div class="coupon-condition" v-else-if="coupon.type === 2">
            {{ coupon.value }}折
          </div>
          <div class="coupon-condition" v-else>
            无门槛
          </div>
        </div>
        
        <div class="coupon-right">
          <div class="coupon-info">
            <h3 class="coupon-name">{{ coupon.name }}</h3>
            <p class="coupon-time">
              <el-icon><Clock /></el-icon>
              {{ formatTime(coupon.startTime) }} - {{ formatTime(coupon.endTime) }}
            </p>
            <div class="coupon-stock">
              <span>库存: {{ coupon.availableStock }} / {{ coupon.totalStock }}</span>
              <el-progress 
                :percentage="getStockPercentage(coupon)" 
                :color="getProgressColor(coupon)"
                :stroke-width="6"
                :show-text="false"
              />
            </div>
          </div>
          
          <div class="coupon-action">
            <el-button 
              type="primary" 
              :loading="seckillLoading[coupon.id]"
              :disabled="!canSeckill(coupon)"
              @click="handleSeckill(coupon)"
            >
              {{ getSeckillBtnText(coupon) }}
            </el-button>
          </div>
        </div>
        
        <div class="coupon-status" v-if="coupon.status !== 1">
          <span :class="getStatusClass(coupon)">{{ getStatusText(coupon) }}</span>
        </div>
      </div>
    </div>

    <!-- 我的优惠券弹窗 -->
    <el-dialog v-model="showMyCoupons" title="我的优惠券" width="600px">
      <el-tabs v-model="myCouponTab">
        <el-tab-pane label="未使用" :name="0">
          <div class="my-coupon-list">
            <el-empty v-if="myCoupons.filter(c => c.status === 0).length === 0" description="暂无优惠券" />
            <div v-for="item in myCoupons.filter(c => c.status === 0)" :key="item.id" class="my-coupon-item">
              <div class="my-coupon-value">¥{{ item.couponValue }}</div>
              <div class="my-coupon-info">
                <p>{{ item.couponName }}</p>
                <span>{{ formatTime(item.expireTime) }} 到期</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="已使用" :name="1">
          <div class="my-coupon-list">
            <el-empty v-if="myCoupons.filter(c => c.status === 1).length === 0" description="暂无优惠券" />
            <div v-for="item in myCoupons.filter(c => c.status === 1)" :key="item.id" class="my-coupon-item used">
              <div class="my-coupon-value">¥{{ item.couponValue }}</div>
              <div class="my-coupon-info">
                <p>{{ item.couponName }}</p>
                <span>已使用</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="已过期" :name="2">
          <div class="my-coupon-list">
            <el-empty v-if="myCoupons.filter(c => c.status === 2).length === 0" description="暂无优惠券" />
            <div v-for="item in myCoupons.filter(c => c.status === 2)" :key="item.id" class="my-coupon-item expired">
              <div class="my-coupon-value">¥{{ item.couponValue }}</div>
              <div class="my-coupon-info">
                <p>{{ item.couponName }}</p>
                <span>已过期</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { couponApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Ticket, User, Clock } from '@element-plus/icons-vue'

const userStore = useUserStore()
const loading = ref(false)
const coupons = ref([])
const myCoupons = ref([])
const showMyCoupons = ref(false)
const myCouponTab = ref(0)
const seckillLoading = reactive({})
const countdown = ref({ hh: '00', mm: '00', ss: '00' })

let countdownTimer = null

// 加载优惠券列表
const loadCoupons = async () => {
  loading.value = true
  try {
    const res = await couponApi.getList()
    coupons.value = res.data || []
  } catch (e) {
    ElMessage.error('加载优惠券失败')
  } finally {
    loading.value = false
  }
}

// 加载我的优惠券
const loadMyCoupons = async () => {
  if (!userStore.userInfo?.id) return
  try {
    const res = await couponApi.getMy(userStore.userInfo.id)
    myCoupons.value = res.data || []
  } catch (e) {
    console.error('加载我的优惠券失败', e)
  }
}

// 秒杀抢券
const handleSeckill = async (coupon) => {
  if (!userStore.userInfo?.id) {
    ElMessage.warning('请先登录')
    return
  }
  
  seckillLoading[coupon.id] = true
  try {
    await couponApi.seckill(coupon.id, userStore.userInfo.id)
    ElMessage.success('抢券成功！')
    // 刷新列表
    loadCoupons()
    loadMyCoupons()
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    seckillLoading[coupon.id] = false
  }
}

// 判断是否可以抢券
const canSeckill = (coupon) => {
  if (coupon.status !== 1) return false
  if (coupon.availableStock <= 0) return false
  const now = new Date()
  const start = new Date(coupon.startTime)
  const end = new Date(coupon.endTime)
  if (now < start || now > end) return false
  return true
}

// 获取抢券按钮文字
const getSeckillBtnText = (coupon) => {
  if (coupon.status !== 1) return '活动已结束'
  if (coupon.availableStock <= 0) return '已抢光'
  const now = new Date()
  const start = new Date(coupon.startTime)
  const end = new Date(coupon.endTime)
  if (now < start) return '活动未开始'
  if (now > end) return '活动已结束'
  return '立即抢购'
}

// 获取优惠券类型样式
const getCouponTypeClass = (type) => {
  const classes = { 1: 'type-discount', 2: 'type-percent', 3: 'type-free' }
  return classes[type] || 'type-discount'
}

// 获取库存百分比
const getStockPercentage = (coupon) => {
  if (!coupon.totalStock) return 0
  return Math.round((coupon.availableStock / coupon.totalStock) * 100)
}

// 获取进度条颜色
const getProgressColor = (coupon) => {
  const percent = getStockPercentage(coupon)
  if (percent > 50) return '#67c23a'
  if (percent > 20) return '#e6a23c'
  return '#f56c6c'
}

// 获取状态样式
const getStatusClass = (coupon) => {
  if (coupon.status === 0) return 'status-pending'
  if (coupon.status === 2) return 'status-ended'
  return ''
}

// 获取状态文字
const getStatusText = (coupon) => {
  if (coupon.status === 0) return '未开始'
  if (coupon.status === 2) return '已结束'
  return ''
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 倒计时
const startCountdown = () => {
  const end = new Date()
  end.setHours(23, 59, 59)
  
  const tick = () => {
    const diff = Math.max(0, Math.floor((end - Date.now()) / 1000))
    countdown.value = {
      hh: String(Math.floor(diff / 3600)).padStart(2, '0'),
      mm: String(Math.floor(diff % 3600 / 60)).padStart(2, '0'),
      ss: String(diff % 60).padStart(2, '0')
    }
  }
  
  tick()
  countdownTimer = setInterval(tick, 1000)
}

onMounted(() => {
  loadCoupons()
  loadMyCoupons()
  startCountdown()
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.coupon-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  color: #333;
}

/* 倒计时横幅 */
.countdown-banner {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  border-radius: 12px;
  padding: 20px 30px;
  margin-bottom: 24px;
  color: #fff;
}

.countdown-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.countdown-label {
  font-size: 18px;
  font-weight: 600;
}

.countdown-timer {
  display: flex;
  align-items: center;
  gap: 6px;
}

.time-block {
  background: rgba(0, 0, 0, 0.3);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 24px;
  font-weight: 700;
  min-width: 50px;
  text-align: center;
}

.time-sep {
  font-size: 24px;
  font-weight: 700;
}

/* 优惠券列表 */
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.coupon-card {
  display: flex;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  position: relative;
  transition: transform 0.3s, box-shadow 0.3s;
  min-height: 160px;
}

.coupon-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.18);
}

.coupon-left {
  width: 240px;
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  position: relative;
}

.coupon-left.type-discount {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
}

.coupon-left.type-percent {
  background: linear-gradient(135deg, #a29bfe, #6c5ce7);
}

.coupon-left.type-free {
  background: linear-gradient(135deg, #55efc4, #00b894);
}

.coupon-value {
  display: flex;
  align-items: baseline;
}

.coupon-value .currency {
  font-size: 28px;
  margin-right: 4px;
}

.coupon-value .amount {
  font-size: 56px;
  font-weight: 700;
}

.coupon-condition {
  font-size: 16px;
  margin-top: 12px;
  opacity: 0.9;
}

.coupon-right {
  flex: 1;
  padding: 28px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.coupon-info {
  flex: 1;
}

.coupon-name {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.coupon-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #999;
  margin-bottom: 16px;
}

.coupon-stock {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 15px;
  color: #666;
}

.coupon-stock .el-progress {
  width: 150px;
}

.coupon-action {
  margin-left: 24px;
}

.coupon-action .el-button {
  padding: 16px 36px;
  font-size: 17px;
  font-weight: 600;
}

/* 优惠券状态标签 */
.coupon-status {
  position: absolute;
  top: 20px;
  right: 20px;
}

.coupon-status span {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.status-pending {
  background: #fff3e0;
  color: #ff9800;
}

.status-ended {
  background: #f5f5f5;
  color: #999;
}

/* 我的优惠券弹窗 */
.my-coupon-list {
  max-height: 400px;
  overflow-y: auto;
}

.my-coupon-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fafafa;
}

.my-coupon-item.used,
.my-coupon-item.expired {
  opacity: 0.6;
}

.my-coupon-value {
  font-size: 24px;
  font-weight: 700;
  color: #ff6b6b;
  width: 80px;
}

.my-coupon-info {
  flex: 1;
}

.my-coupon-info p {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.my-coupon-info span {
  font-size: 12px;
  color: #999;
}
</style>
