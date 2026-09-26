<template>
  <header class="header">
    <div class="header-inner">
      <!-- Logo -->
      <div class="logo" @click="$router.push('/home')">
        <span class="logo-text">MALL</span>
      </div>

      <!-- 搜索框 -->
      <div class="search-box">
        <el-input v-model="keyword" 
                  placeholder="搜索商品" 
                  size="large"
                  @keyup.enter="doSearch" 
                  clearable
                  class="search-input">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 右侧导航 -->
      <nav class="nav-right">
        <template v-if="userStore.token">
          <a class="nav-item" @click="$router.push('/ai-guide')">
            <el-icon :size="18"><ChatDotRound /></el-icon>
            <span>AI 导购</span>
          </a>
          <a class="nav-item" @click="$router.push('/coupon')">
            <el-icon :size="18"><Ticket /></el-icon>
            <span>优惠券</span>
          </a>
          <a class="nav-item" @click="$router.push('/orders')">
            <el-icon :size="18"><Tickets /></el-icon>
            <span>订单</span>
          </a>
          <a class="nav-item" @click="$router.push('/cart')">
            <el-icon :size="18"><ShoppingCart /></el-icon>
            <span>购物车</span>
            <span v-if="cartCount" class="badge">{{ cartCount }}</span>
          </a>
          <div class="nav-divider"></div>
          <a class="nav-item" @click="$router.push('/profile')">
            <el-icon :size="18"><User /></el-icon>
            <span>{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          </a>
          <a class="nav-item" @click="handleLogout">退出</a>
        </template>
        <template v-else>
          <a class="nav-item" @click="$router.push('/login')">登录</a>
          <a class="nav-item primary" @click="$router.push('/register')">注册</a>
        </template>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart, Tickets, Ticket, User, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const cartCount = ref(0)

const doSearch = () => {
  if (keyword.value.trim()) {
    router.push({ path: '/home', query: { keyword: keyword.value.trim() } })
  }
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/login')
}

onMounted(() => {})
</script>

<style scoped>
.header {
  background: var(--color-white);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 1000;
  backdrop-filter: blur(8px);
  background: rgba(255, 255, 255, 0.95);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px var(--spacing-md);
  display: flex;
  align-items: center;
  gap: 32px;
}

/* Logo */
.logo {
  cursor: pointer;
  flex-shrink: 0;
}

.logo-text {
  font-size: 28px;
  font-weight: 800;
  color: var(--color-primary);
  letter-spacing: 2px;
}

/* 搜索框 */
.search-box {
  flex: 1;
  max-width: 560px;
}

.search-input :deep(.el-input__wrapper) {
  background: var(--color-bg);
  box-shadow: none;
  border: 1px solid var(--color-border);
  border-radius: 24px;
  padding: 10px 16px;
  transition: all 0.2s;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: #ccc;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--color-accent);
  background: var(--color-white);
}

.search-input :deep(.el-input__prefix) {
  color: var(--color-text-lighter);
}

/* 右侧导航 */
.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--color-text);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
  padding: 8px 0;
}

.nav-item:hover {
  color: var(--color-accent);
}

.nav-item.primary {
  background: var(--color-accent);
  color: var(--color-white);
  padding: 8px 20px;
  border-radius: 20px;
  font-weight: 500;
}

.nav-item.primary:hover {
  background: var(--color-accent-hover);
  color: var(--color-white);
}

.nav-divider {
  width: 1px;
  height: 20px;
  background: var(--color-border);
}

.nav-user {
  font-size: 14px;
  color: var(--color-text);
  font-weight: 500;
}

.badge {
  position: absolute;
  top: 0;
  right: -8px;
  background: var(--color-accent);
  color: var(--color-white);
  border-radius: 10px;
  padding: 0 6px;
  font-size: 11px;
  line-height: 16px;
  min-width: 18px;
  text-align: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-inner {
    gap: 16px;
    padding: 12px var(--spacing-sm);
  }
  
  .search-box {
    display: none;
  }
}
</style>
