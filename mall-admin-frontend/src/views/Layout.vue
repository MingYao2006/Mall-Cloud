<template>
  <div class="admin-layout">
    <div
      v-if="sidebarOpen"
      class="sidebar-scrim"
      role="presentation"
      @click="sidebarOpen = false"
    ></div>

    <aside id="admin-sidebar" class="admin-sidebar" :class="{ 'is-open': sidebarOpen }">
      <div class="sidebar-brand">
        <span class="brand-seal" aria-hidden="true">集</span>
        <div class="brand-copy">
          <strong>商城运营台</strong>
          <span>运营管理端</span>
        </div>
        <button class="sidebar-close" type="button" aria-label="关闭导航" @click="sidebarOpen = false">
          <el-icon :size="16"><Close /></el-icon>
        </button>
      </div>

      <nav class="sidebar-nav" aria-label="主导航">
        <div v-for="group in menuGroups" :key="group.label" class="nav-group">
          <p class="nav-group-title">{{ group.label }}</p>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ 'is-active': $route.path === item.path }"
            @click="sidebarOpen = false"
          >
            <el-icon :size="17"><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-foot">
        <span class="foot-label">今日</span>
        <strong>{{ sidebarDate }}</strong>
        <p>商品、订单与评价，集中在这里处理。</p>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <button
            class="menu-toggle"
            type="button"
            :aria-expanded="sidebarOpen"
            aria-controls="admin-sidebar"
            aria-label="打开导航"
            @click="sidebarOpen = !sidebarOpen"
          >
            <el-icon :size="18"><component :is="sidebarOpen ? 'Close' : 'Expand'" /></el-icon>
          </button>
          <span class="header-mark" aria-hidden="true"></span>
          <h1>{{ $route.meta.title || '控制台' }}</h1>
        </div>

        <div class="header-right">
          <span class="header-date">{{ headerDate }}</span>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info" role="button" tabindex="0" aria-label="账号菜单">
              <span class="user-avatar">
                <img v-if="userInfo?.avatar" :src="userInfo.avatar" alt="" />
                <template v-else>{{ avatarText }}</template>
              </span>
              <span class="user-meta">
                <strong>{{ displayName }}</strong>
                <em>管理员</em>
              </span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = useAdminStore()

const sidebarOpen = ref(false)
const now = ref(new Date())
const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
let clockTimer = null

const userInfo = computed(() => store.userInfo)
const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '管理员')
const avatarText = computed(() => displayName.value.slice(0, 1))

const dateParts = computed(() => {
  const date = now.value
  return {
    year: date.getFullYear(),
    month: date.getMonth() + 1,
    day: date.getDate(),
    weekday: weekdays[date.getDay()]
  }
})

const headerDate = computed(() => {
  const { year, month, day, weekday } = dateParts.value
  return `${year}年${month}月${day}日 ${weekday}`
})

const sidebarDate = computed(() => {
  const { month, day, weekday } = dateParts.value
  return `${month}月${day}日 ${weekday}`
})

const menuGroups = [
  {
    label: '经营',
    items: [{ path: '/dashboard', title: '控制台', icon: 'Odometer' }]
  },
  {
    label: '货品',
    items: [
      { path: '/products', title: '商品管理', icon: 'Goods' },
      { path: '/categories', title: '分类管理', icon: 'Menu' },
      { path: '/search', title: '搜索管理', icon: 'Search' }
    ]
  },
  {
    label: '交易',
    items: [
      { path: '/orders', title: '订单管理', icon: 'List' },
      { path: '/coupons', title: '优惠券管理', icon: 'Ticket' }
    ]
  },
  {
    label: '会员与口碑',
    items: [
      { path: '/comments', title: '评论管理', icon: 'ChatDotSquare' },
      { path: '/users', title: '用户管理', icon: 'User' }
    ]
  }
]

function handleCommand(command) {
  if (command !== 'logout') return

  ElMessageBox.confirm('退出后需要重新登录才能进入运营台。', '退出登录', {
    confirmButtonText: '退出登录',
    cancelButtonText: '继续留下',
    type: 'warning'
  }).then(() => {
    store.logout()
    router.push('/login')
  }).catch(() => {})
}

function handleKeydown(event) {
  if (event.key === 'Escape') sidebarOpen.value = false
}

watch(() => route.path, () => {
  sidebarOpen.value = false
})

watch(sidebarOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

onMounted(() => {
  clockTimer = window.setInterval(() => {
    now.value = new Date()
  }, 60 * 1000)
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.clearInterval(clockTimer)
  window.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>
