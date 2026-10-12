<template>
  <div class="seckill-page">
    <div class="container">
      <!-- 倒计时横幅 -->
      <div class="seckill-banner">
        <div class="banner-content">
          <div class="banner-left">
            <h2>限时秒杀</h2>
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
            <span class="pulse-dot"></span>
            <span>疯狂抢购中</span>
          </div>
        </div>
      </div>

      <!-- 秒杀商品横向滚动 -->
      <div class="seckill-scroll" v-loading="loading">
        <div class="scroll-wrapper">
          <div v-for="item in seckillProducts" :key="item.id" 
               class="seckill-card"
               :class="{ 'can-buy': canBuy(item), 'sold-out': item.stock <= 0 }"
               @click="goDetail(item)">
            <div class="sk-img">
              <img :src="item.mainImage || 'https://placehold.co/300x300/eee/999'" :alt="item.name" />
              <span class="sk-tag" v-if="item.stock <= 0">已抢光</span>
              <span class="sk-tag hot" v-else-if="getStockPercent(item) < 30">即将售罄</span>
            </div>
            <div class="sk-info">
              <p class="sk-name">{{ item.name }}</p>
              <div class="sk-price">
                <span class="sk-current price">¥{{ item.seckillPrice || item.price?.toFixed(2) }}</span>
                <span class="sk-original">¥{{ item.price?.toFixed(2) }}</span>
              </div>
              <div class="sk-progress">
                <div class="sk-bar">
                  <div class="sk-bar-inner" :style="{ width: getStockPercent(item) + '%' }"></div>
                </div>
                <span class="sk-sold">已抢{{ getSoldCount(item) }}件</span>
              </div>
              <el-button type="primary" round size="large" class="sk-btn"
                         :disabled="!canBuy(item)"
                         :loading="buyingId === item.id"
                         @click.stop="handleBuy(item)">
                {{ getBtnText(item) }}
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && seckillProducts.length === 0" description="暂无秒杀活动">
        <el-button type="primary" @click="$router.push('/home')">去逛逛</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { seckillApi, productApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const seckillProducts = ref([])
const buyingId = ref(null)
const countdown = ref({ hh: '00', mm: '00', ss: '00' })

let cdTimer = null

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
  cdTimer = setInterval(tick, 1000)
}

const loadSeckillProducts = async () => {
  loading.value = true
  try {
    // 使用热门商品模拟秒杀
    const res = await productApi.getPage(1, 12)
    const products = (res.data?.list || res.data?.records || []).map(p => ({
      ...p,
      seckillPrice: (p.price * 0.5 + Math.random() * p.price * 0.3).toFixed(2),
      seckillStock: Math.floor(Math.random() * 100) + 10,
      seckillSold: Math.floor(Math.random() * 80)
    }))
    seckillProducts.value = products
  } catch (e) {
    ElMessage.error('加载秒杀商品失败')
  } finally {
    loading.value = false
  }
}

const canBuy = (item) => item.stock > 0 && (item.status === 1 || item.status === undefined)
const getStockPercent = (item) => {
  const total = item.seckillStock || 100
  const sold = item.seckillSold || 0
  return Math.max(0, Math.min(100, ((total - sold) / total) * 100))
}
const getSoldCount = (item) => item.seckillSold || 0
const getBtnText = (item) => {
  if (item.stock <= 0) return '已抢光'
  return '立即抢购'
}

const goDetail = (item) => {
  router.push(`/product/${item.id}`)
}

const handleBuy = async (item) => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  buyingId.value = item.id
  try {
    // 模拟秒杀请求
    await new Promise(resolve => setTimeout(resolve, 800))
    ElMessage.success('秒杀成功！请尽快下单')
    router.push(`/product/${item.id}`)
  } catch (e) {
    ElMessage.error('秒杀失败，请重试')
  } finally {
    buyingId.value = null
  }
}

onMounted(() => {
  startCountdown()
  loadSeckillProducts()
})
onUnmounted(() => {
  if (cdTimer) clearInterval(cdTimer)
})
</script>

<style scoped>
.seckill-page {
  padding: var(--spacing-xl) 0;
  min-height: calc(100vh - 200px);
}

/* 倒计时横幅 */
.seckill-banner {
  background: linear-gradient(135deg, var(--color-accent), #e6452a);
  border-radius: var(--radius-lg);
  padding: 32px 40px;
  margin-bottom: var(--spacing-lg);
  position: relative;
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.seckill-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
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
  letter-spacing: 1px;
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

.banner-right {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-white);
  font-size: 15px;
  font-weight: 500;
}

.pulse-dot {
  width: 10px;
  height: 10px;
  background: var(--color-white);
  border-radius: 50%;
  animation: dotPulse 1.5s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.3); }
}

/* 秒杀商品横向滚动 */
.seckill-scroll {
  overflow-x: auto;
  padding-bottom: var(--spacing-md);
  scrollbar-width: thin;
}

.scroll-wrapper {
  display: flex;
  gap: var(--spacing-md);
  min-width: max-content;
}

.seckill-card {
  width: 280px;
  flex-shrink: 0;
  background: var(--color-white);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-sm);
}

.seckill-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-hover);
}

.seckill-card.can-buy:hover {
  box-shadow: 0 12px 32px var(--color-accent-shadow);
}

.seckill-card.sold-out {
  opacity: 0.6;
}

.sk-img {
  position: relative;
  width: 100%;
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  overflow: hidden;
}

.sk-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.4s;
}

.seckill-card:hover .sk-img img {
  transform: scale(1.06);
}

.sk-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-white);
  background: var(--color-text-lighter);
}

.sk-tag.hot {
  background: var(--color-accent);
}

.sk-info {
  padding: var(--spacing-md);
}

.sk-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 12px;
}

.sk-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 14px;
}

.sk-current {
  font-size: 26px;
  font-weight: 700;
}

.sk-original {
  font-size: 13px;
  color: var(--color-text-lighter);
  text-decoration: line-through;
}

.sk-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.sk-bar {
  flex: 1;
  height: 6px;
  background: rgba(232, 115, 74, 0.15);
  border-radius: 3px;
  overflow: hidden;
}

.sk-bar-inner {
  height: 100%;
  background: var(--color-accent);
  border-radius: 3px;
  transition: width 0.8s ease;
}

.sk-sold {
  font-size: 12px;
  color: var(--color-text-lighter);
  white-space: nowrap;
}

.sk-btn {
  width: 100%;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
</style>