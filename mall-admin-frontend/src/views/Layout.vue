<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar">
      <div class="logo">商城管理后台</div>
      <el-menu
        :default-active="$route.path"
        router
        :collapse="false"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 主区域 -->
    <div class="admin-main">
      <!-- 顶栏 -->
      <header class="admin-header">
        <div class="header-left">{{ $route.meta.title || '控制台' }}</div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="28" :src="userInfo?.avatar || ''" />
              <span style="margin-left: 8px; font-size: 14px;">{{ userInfo?.nickname || userInfo?.username || '管理员' }}</span>
              <el-icon style="margin-left: 4px;"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const store = useAdminStore()

const userInfo = computed(() => store.userInfo)

const menuItems = [
  { path: '/dashboard', title: '控制台', icon: 'Odometer' },
  { path: '/products', title: '商品管理', icon: 'Goods' },
  { path: '/categories', title: '分类管理', icon: 'Menu' },
  { path: '/orders', title: '订单管理', icon: 'List' },
  { path: '/comments', title: '评论管理', icon: 'ChatDotSquare' },
  { path: '/coupons', title: '优惠券管理', icon: 'Ticket' },
  { path: '/search', title: '搜索管理', icon: 'Search' },
  { path: '/users', title: '用户管理', icon: 'User' }
]

function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      store.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}
</style>
