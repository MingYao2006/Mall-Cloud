<template>
  <div class="coupon-page">
    <div class="container">
      <!-- 页面标题 -->
      <h1 class="page-title">优惠券秒杀</h1>

      <!-- 倒计时横幅 -->
      <div class="countdown-banner">
        <div class="banner-content">
          <div class="banner-left">
            <h2>限时抢购</h2>
            <div class="countdown">
              <span class="cd-label">距结束</span>
              <span class="cd-block">{{ countdown.hh }}</span>
              <span class="cd-sep">:</span>
              <span class="cd-block">{{ countdown.mm }}</span>
              <span class="cd-sep">:</span>
              <span class="cd-block">{{ countdown.ss }}</span>
            </div>
          </div>
          <div class="banner-right">
            <el-button type="primary" size="large" @click="showMyCoupons = true">
              我的优惠券 ({{ myCoupons.length }})
            </el-button>
          </div>
        </div>
      </div>

      <!-- 优惠券列表 -->
      <div class="coupon-list" v-loading="loading">
        <el-empty v-if="!loading && coupons.length === 0" description="暂无优惠券活动" />
        
        <div v-for="coupon in coupons" :key="coupon.id" 
             class="coupon-card"
             :class="{ 'can-buy': canSeckill(coupon), 'sold-out': coupon.availableStock <= 0 }">
          <div class="coupon-left" :class="getCouponTypeClass(coupon.type)">
            <div class="coupon-value">
              <span class="currency">¥</span>
              <span class="amount price">{{ coupon.value }}</span>
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
                <span>剩余: {{ coupon.availableStock }} / {{ coupon.totalStock }}</span>
                <div class="stock-bar">
                  <div class="stock-bar-inner" :style="{ width: getStockPercentage(coupon) + '%' }"></div>
                </div>
              </div>
            </div>
            
            <div class="coupon-action">
              <el-button 
                type="primary" 
                size="large"
                :loading="seckillLoading[coupon.id]"
                :disabled="!canSeckill(coupon)"
                @click="handleSeckill(coupon)">
                {{ getSeckillBtnText(coupon) }}
              </el-button>
            </div>
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
                <div class="my-coupon-value price">¥{{ item.couponValue }}</div>
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
                <div class="my-coupon-value price">¥{{ item.couponValue }}</div>
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
                <div class="my-coupon-value price">¥{{ item.couponValue }}</div>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { couponApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'

const userStore = useUserStore()
const loading = ref(false)
const coupons = ref([])
const myCoupons = ref([])
const showMyCoupons = ref(false)
const myCouponTab = ref(0)
const seckillLoading = reactive({})
const countdown = ref({ hh: '00', mm: '00', ss: '00' })

let countdownTimer = null

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

const loadMyCoupons = async () => {
  if (!userStore.userInfo?.id) return
  try {
    const res = await couponApi.getMy()
    myCoupons.value = res.data || []
  } catch (e) {
    console.error('加载我的优惠券失败', e)
  }
}

const handleSeckill = async (coupon) => {
  if (!userStore.userInfo?.id) {
    ElMessage.warning('请先登录')
    return
  }
  
  seckillLoading[coupon.id] = true
  try {
    await couponApi.seckill(coupon.id)
    ElMessage.success('抢券成功！')
    // 延迟一下再刷新，确保数据库已更新
    setTimeout(() => {
      loadCoupons()
      loadMyCoupons()
    }, 500)
  } catch (e) {
    console.error('抢券失败:', e)
    if (e.response?.data) {
      ElMessage.error(e.response.data.message || '抢券失败')
    }
  } finally {
    seckillLoading[coupon.id] = false
  }
}

const canSeckill = (coupon) => {
  if (coupon.status !== 1) return false
  if (coupon.availableStock <= 0) return false
  const now = new Date()
  const start = new Date(coupon.startTime)
  const end = new Date(coupon.endTime)
  if (now < start || now > end) return false
  return true
}

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

const getCouponTypeClass = (type) => {
  const classes = { 1: 'type-discount', 2: 'type-percent', 3: 'type-free' }
  return classes[type] || 'type-discount'
}

const getStockPercentage = (coupon) => {
  if (!coupon.totalStock) return 0
  return Math.round((coupon.availableStock / coupon.totalStock) * 100)
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

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
  padding: var(--spacing-xl) 0;
  min-height: calc(100vh - 200px);
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: var(--spacing-lg);
  color: var(--color-primary);
}

/* 倒计时横幅 */
.countdown-banner {
  background: linear-gradient(135deg, var(--color-accent), #d4623a);
  border-radius: var(--radius-lg);
  padding: 32px 40px;
  margin-bottom: var(--spacing-lg);
  box-shadow: var(--shadow-md);
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.banner-left h2 {
  font-size: 32px;
  color: var(--color-white);
  font-weight: 700;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cd-label {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.cd-block {
  background: rgba(0, 0, 0, 0.35);
  color: var(--color-white);
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-mono);
  min-width: 56px;
  text-align: center;
  letter-spacing: 2px;
}

.cd-sep {
  color: var(--color-white);
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-mono);
}

.banner-right .el-button {
  background: var(--color-white);
  color: var(--color-accent);
  border: none;
  font-weight: 600;
}

.banner-right .el-button:hover {
  background: rgba(255, 255, 255, 0.9);
}

/* 优惠券列表 */
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.coupon-card {
  display: flex;
  background: var(--color-white);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.coupon-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.coupon-card.can-buy:hover {
  box-shadow: 0 12px 32px var(--color-accent-shadow);
}

.coupon-card.sold-out {
  opacity: 0.6;
}

.coupon-left {
  width: 220px;
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-white);
}

.coupon-left.type-discount {
  background: linear-gradient(135deg, var(--color-accent), #d4623a);
}

.coupon-left.type-percent {
  background: linear-gradient(135deg, #4a9e7a, #3d8b6a);
}

.coupon-left.type-free {
  background: linear-gradient(135deg, #5a8fbf, #4a7fa8);
}

.coupon-value {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.coupon-value .currency {
  font-size: 24px;
  margin-right: 4px;
}

.coupon-value .amount {
  font-size: 48px;
  font-weight: 700;
}

.coupon-condition {
  font-size: 14px;
  opacity: 0.9;
}

.coupon-right {
  flex: 1;
  padding: var(--spacing-lg);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.coupon-info {
  flex: 1;
}

.coupon-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--spacing-sm);
}

.coupon-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-lighter);
  margin-bottom: var(--spacing-sm);
}

.coupon-stock {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 13px;
  color: var(--color-text-light);
}

.stock-bar {
  flex: 1;
  max-width: 120px;
  height: 6px;
  background: rgba(232, 115, 74, 0.15);
  border-radius: 3px;
  overflow: hidden;
}

.stock-bar-inner {
  height: 100%;
  background: var(--color-accent);
  border-radius: 3px;
  transition: width 0.8s ease;
}

.coupon-action {
  margin-left: var(--spacing-md);
}

.coupon-action .el-button {
  padding: 12px 32px;
  font-size: 15px;
  font-weight: 600;
}

/* 我的优惠券弹窗 */
.my-coupon-list {
  max-height: 400px;
  overflow-y: auto;
}

.my-coupon-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-sm);
  background: var(--color-bg);
  transition: all 0.2s;
}

.my-coupon-item:hover {
  border-color: var(--color-accent);
}

.my-coupon-item.used,
.my-coupon-item.expired {
  opacity: 0.5;
}

.my-coupon-value {
  font-size: 24px;
  font-weight: 700;
  width: 80px;
}

.my-coupon-info {
  flex: 1;
}

.my-coupon-info p {
  font-size: 14px;
  color: var(--color-text);
  margin-bottom: 4px;
}

.my-coupon-info span {
  font-size: 12px;
  color: var(--color-text-lighter);
}

/* 响应式 */
@media (max-width: 768px) {
  .coupon-card {
    flex-direction: column;
  }
  
  .coupon-left {
    width: 100%;
    padding: var(--spacing-md);
  }
  
  .coupon-right {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }
  
  .coupon-action {
    margin-left: 0;
    width: 100%;
  }
  
  .coupon-action .el-button {
    width: 100%;
  }
  
  .banner-content {
    flex-direction: column;
    gap: var(--spacing-md);
  }
  
  .banner-left {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
