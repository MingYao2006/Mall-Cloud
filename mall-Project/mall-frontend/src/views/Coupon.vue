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

/* 状态语义统一：优先用后端 CouponStatus 派生的 displayStatus
   （0-未上架 1-未开始 2-进行中 3-已抢光 4-已过期 5-已停用），
   旧缓存取不到该字段时按同一判定顺序本地兜底，避免前后端结论不一致 */
const DISPLAY_ONGOING = 2

const canSeckill = (coupon) => {
  if (coupon.displayStatus != null) return coupon.displayStatus === DISPLAY_ONGOING
  if (coupon.status !== 1) return false
  const now = new Date()
  if (coupon.endTime && now > new Date(coupon.endTime)) return false
  if (coupon.startTime && now < new Date(coupon.startTime)) return false
  if (coupon.availableStock <= 0) return false
  return true
}

const getSeckillBtnText = (coupon) => {
  const hintMap = { 0: '未上架', 1: '活动未开始', 3: '已抢光', 4: '活动已结束', 5: '活动已结束' }
  if (coupon.displayStatus != null) return hintMap[coupon.displayStatus] || '立即抢购'
  if (coupon.status !== 1) return '活动已结束'
  const now = new Date()
  if (coupon.endTime && now > new Date(coupon.endTime)) return '活动已结束'
  if (coupon.startTime && now < new Date(coupon.startTime)) return '活动未开始'
  if (coupon.availableStock <= 0) return '已抢光'
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
  background: linear-gradient(135deg, var(--color-accent), #e6452a);
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
  background: linear-gradient(135deg, var(--color-accent), #e6452a);
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

<style scoped>
/* 集物商店 · 领券柜台 */
.coupon-page {
  padding: 34px 0 54px;
}

.page-title {
  margin-bottom: 22px;
  padding-bottom: 13px;
  border-bottom: 1px solid var(--line);
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 31px;
  font-weight: 600;
}

.countdown-banner {
  position: relative;
  overflow: hidden;
  margin-bottom: 24px;
  padding: 28px 34px;
  border: 1px solid var(--pine-700);
  border-top: 3px solid var(--gold);
  border-radius: 2px;
  background: var(--pine-800);
  box-shadow: 6px 6px 0 rgba(22, 58, 46, 0.12);
}

.countdown-banner::after {
  content: '惠';
  position: absolute;
  right: 130px;
  bottom: -46px;
  color: rgba(251, 252, 248, 0.05);
  font-family: var(--font-display);
  font-size: 160px;
  font-weight: 700;
  line-height: 1;
  pointer-events: none;
}

.banner-left {
  gap: 28px;
}

.banner-left h2 {
  color: var(--paper);
  font-family: var(--font-display);
  font-size: 29px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.cd-label {
  color: rgba(251, 252, 248, 0.62);
  font-size: 11px;
}

.cd-block {
  min-width: 44px;
  padding: 6px 10px;
  border: 1px solid rgba(251, 252, 248, 0.22);
  border-radius: 2px;
  background: var(--paper);
  color: var(--pine-800);
  font-family: var(--font-num);
  font-size: 22px;
  font-weight: 600;
}

.cd-sep {
  color: var(--gold);
  font-family: var(--font-num);
  font-size: 22px;
}

.banner-right {
  position: relative;
  z-index: 1;
}

.banner-right .el-button {
  border: 1px solid var(--gold);
  border-radius: 2px;
  background: transparent;
  color: var(--paper);
  font-size: 12px;
}

.banner-right .el-button:hover {
  background: var(--gold);
  color: var(--pine-900);
}

.coupon-list {
  gap: 15px;
}

.coupon-card {
  border: 1px solid var(--line);
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 4px 4px 0 rgba(22, 58, 46, 0.07);
}

.coupon-card:hover {
  transform: translate(-2px, -2px);
  box-shadow: 7px 7px 0 rgba(22, 58, 46, 0.1);
}

.coupon-card.can-buy:hover {
  box-shadow: 7px 7px 0 rgba(228, 166, 47, 0.45);
}

.coupon-left {
  width: 200px;
  padding: 26px 22px;
}

.coupon-left.type-discount {
  background: var(--pine-800);
}

.coupon-left.type-percent {
  background: var(--gold-deep);
}

.coupon-left.type-free {
  background: var(--sage);
}

.coupon-value .currency {
  font-family: var(--font-num);
  font-size: 20px;
}

.coupon-value .amount {
  font-family: var(--font-num);
  font-size: 46px;
  font-weight: 600;
}

.coupon-condition {
  font-size: 12px;
}

.coupon-right {
  padding: 24px 28px;
}

.coupon-name {
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 600;
}

.coupon-time,
.coupon-stock {
  color: var(--sage);
  font-size: 11px;
}

.stock-bar,
.sk-bar {
  border-radius: 1px;
}

.stock-bar {
  background: var(--porcelain-deep);
}

.stock-bar-inner {
  border-radius: 1px;
  background: var(--gold);
}

.coupon-action .el-button {
  padding: 10px 25px;
  font-size: 13px;
}

.my-coupon-item {
  border: 1px solid var(--line);
  border-radius: 2px;
  background: var(--paper-warm);
}

.my-coupon-value {
  color: var(--vermilion);
  font-family: var(--font-num);
}

.my-coupon-info p {
  color: var(--pine-800);
}

@media (max-width: 768px) {
  .countdown-banner {
    padding: 22px 20px;
  }

  .coupon-left {
    width: 100%;
    padding: 18px;
  }

  .coupon-value .amount {
    font-size: 36px;
  }
}
</style>
