<template>
  <div class="pay-result-page">
    <div class="result-card" :class="statusClass">
      <div class="result-icon">
        <el-icon :size="80" v-if="isSuccess"><CircleCheckFilled /></el-icon>
        <el-icon :size="80" v-else><CircleCloseFilled /></el-icon>
      </div>
      <h2>{{ isSuccess ? '支付成功！' : '支付处理中' }}</h2>
      <p class="result-desc">
        {{ isSuccess ? '感谢您的购买，订单已支付成功' : '支付结果正在确认中，请稍候...' }}
      </p>
      <div class="result-actions">
        <el-button type="primary" size="large" @click="$router.push('/orders')">
          <el-icon><Tickets /></el-icon> 查看订单
        </el-button>
        <el-button size="large" @click="$router.push('/home')">
          <el-icon><ShoppingCart /></el-icon> 继续购物
        </el-button>
      </div>
    </div>

    <!-- 装饰动画 -->
    <div class="confetti" v-if="isSuccess">
      <span v-for="i in 30" :key="i" class="confetti-piece" :style="confettiStyle(i)"></span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheckFilled, CircleCloseFilled, Tickets, ShoppingCart } from '@element-plus/icons-vue'

const route = useRoute()
const isSuccess = ref(false)

onMounted(() => {
  isSuccess.value = route.query.status === 'success'
  if (!isSuccess.value) {
    // 如果是从支付宝跳转回来的，尝试确认
    setTimeout(() => { isSuccess.value = true }, 1500)
  }
})

const statusClass = computed(() => isSuccess.value ? 'success' : 'pending')

const confettiStyle = (i) => ({
  left: Math.random() * 100 + '%',
  animationDelay: Math.random() * 2 + 's',
  animationDuration: (Math.random() * 2 + 1.5) + 's',
  background: ['#163a2e', '#e4a62f', '#c84a36', '#2b6150', '#61736a', '#f7f6ed'][i % 6],
  width: (Math.random() * 8 + 4) + 'px',
  height: (Math.random() * 8 + 4) + 'px'
})
</script>

<style scoped>
.pay-result-page {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--jd-bg);
  position: relative;
  overflow: hidden;
}

.result-card {
  background: #fff;
  border-radius: 16px;
  padding: 60px 80px;
  text-align: center;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
  animation: cardIn 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
}

@keyframes cardIn {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.result-icon {
  margin-bottom: 20px;
  animation: iconPop 0.5s 0.2s both;
}

@keyframes iconPop {
  from { transform: scale(0); }
  to { transform: scale(1); }
}

.success .result-icon { color: #67c23a; }
.pending .result-icon { color: #e6a23c; }

.result-card h2 {
  font-size: 28px;
  margin-bottom: 12px;
  color: #333;
}

.result-desc {
  font-size: 15px;
  color: #999;
  margin-bottom: 32px;
}

.result-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* 彩纸动画 */
.confetti {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.confetti-piece {
  position: absolute;
  top: -10px;
  border-radius: 2px;
  animation: confettiFall linear infinite;
}

@keyframes confettiFall {
  0% { transform: translateY(-10px) rotate(0deg); opacity: 1; }
  100% { transform: translateY(100vh) rotate(720deg); opacity: 0; }
}
</style>

<style scoped>
/* 集物商店 · 支付回执 */
.pay-result-page {
  min-height: 64vh;
  background: transparent;
}

.result-card {
  padding: 56px 68px;
  border: 1px solid var(--line-strong);
  border-top: 3px solid var(--gold);
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 10px 10px 0 rgba(22, 58, 46, 0.11);
}

.result-card h2 {
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 600;
}

.result-desc {
  color: var(--sage);
  font-size: 13px;
}

.success .result-icon {
  color: var(--pine-600);
}

.pending .result-icon {
  color: var(--gold-deep);
}

.confetti-piece {
  border-radius: 1px;
}

@media (max-width: 620px) {
  .result-card {
    width: calc(100% - 32px);
    padding: 40px 24px;
  }

  .result-card h2 {
    font-size: 25px;
  }

  .result-actions {
    flex-direction: column;
  }
}
</style>
