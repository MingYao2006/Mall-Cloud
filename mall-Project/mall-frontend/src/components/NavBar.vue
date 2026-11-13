<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { cartApi, notifyApi, searchApi } from '@/api/compat'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const cartCount = ref(0)
const unreadCount = ref(0)
const scrolled = ref(false)
const cartPop = ref(false)
const menuOpen = ref(false)

/* 搜索联想 */
const suggestions = ref([])
const showSuggest = ref(false)
const activeIndex = ref(-1)
let suggestTimer = null

const isLogin = computed(() => userStore.isLogin)
const nickname = computed(() => userStore.nickname)

const navs = [
  { label: '首页', to: '/home' },
  { label: '全部分类', to: '/category' },
  { label: '热门推荐', hash: 'recommend' },
  { label: '限时秒杀', to: '/seckill' },
  { label: '优惠券', to: '/coupon', auth: true },
  { label: '我的订单', to: '/orders', auth: true }
]

function goHome() {
  keyword.value = ''
  menuOpen.value = false
  router.push('/home')
}

function doSearch() {
  const key = keyword.value.trim()
  closeSuggest()
  menuOpen.value = false
  router.push({ path: '/home', query: key ? { key } : {} })
}

/* ---------------- 搜索联想 ---------------- */
async function fetchSuggest() {
  const kw = keyword.value.trim()
  if (!kw) {
    suggestions.value = []
    showSuggest.value = false
    return
  }
  try {
    const data = await searchApi.suggest(kw)
    suggestions.value = Array.isArray(data) ? data.slice(0, 8) : []
    showSuggest.value = suggestions.value.length > 0
    activeIndex.value = -1
  } catch (e) {
    suggestions.value = []
    showSuggest.value = false
  }
}

/** 输入防抖：300ms 内不再输入才发请求 */
function onInput() {
  clearTimeout(suggestTimer)
  suggestTimer = setTimeout(fetchSuggest, 300)
}

function pickSuggestion(text) {
  keyword.value = text
  doSearch()
}

function onSearchKeydown(event) {
  if (!showSuggest.value || !suggestions.value.length) return

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % suggestions.value.length
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value =
      (activeIndex.value - 1 + suggestions.value.length) % suggestions.value.length
  } else if (event.key === 'Escape') {
    closeSuggest()
  }
}

function onSearchEnter() {
  if (showSuggest.value && activeIndex.value >= 0) {
    pickSuggestion(suggestions.value[activeIndex.value])
    return
  }
  doSearch()
}

function closeSuggest() {
  showSuggest.value = false
  activeIndex.value = -1
}

function onSearchBlur() {
  // 延迟关闭，保证候选词的点击能先触发
  window.setTimeout(closeSuggest, 150)
}

function requireLogin() {
  if (isLogin.value) return true
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

/**
 * 首页锚点滚动：目标区块可能还没渲染出来，重试几次
 */
function scrollToAnchor(hash, tries = 25) {
  const el = document.getElementById(hash)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  if (tries > 0) setTimeout(() => scrollToAnchor(hash, tries - 1), 80)
}

async function onNav(item) {
  menuOpen.value = false
  if (item.auth && !requireLogin()) return

  if (item.hash) {
    // 本项目首页路由是 /home（'/'会重定向），两者都应该直接滚动
    if (route.path === '/' || route.path === '/home') {
      scrollToAnchor(item.hash)
    } else {
      await router.push({ path: '/home', hash: '#' + item.hash })
      scrollToAnchor(item.hash)
    }
    return
  }

  router.push(item.to)
}

function goCart() {
  menuOpen.value = false
  if (requireLogin()) router.push('/cart')
}

function goAiGuide() {
  menuOpen.value = false
  router.push('/ai-guide')
}

function goProfile() {
  if (requireLogin()) router.push('/profile')
}

function goOrder() {
  if (requireLogin()) router.push('/orders')
}

function goLogin() {
  menuOpen.value = false
  router.push('/login')
}

function logout() {
  userStore.logout()
  cartCount.value = 0
  unreadCount.value = 0
  ElMessage.success('已退出登录')
  router.push('/')
}

async function loadBadge() {
  if (!isLogin.value) {
    cartCount.value = 0
    unreadCount.value = 0
    return
  }
  try {
    const count = await cartApi.count()
    cartCount.value = count || 0
  } catch (e) {
    /* 角标失败不影响导航使用 */
  }
}

/* 滚动收缩：滚过 40px 后导航变矮、纸张边缘加深 */
function handleScroll() {
  scrolled.value = window.scrollY > 40
}

/* 加购成功（飞入动画落点）：刷新角标并让角标弹一下 */
function handleCartAdded() {
  loadBadge()
  cartPop.value = false
  requestAnimationFrame(() => {
    cartPop.value = true
  })
  window.setTimeout(() => {
    cartPop.value = false
  }, 520)
}

onMounted(() => {
  handleScroll()
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('cart:added', handleCartAdded)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('cart:added', handleCartAdded)
  clearTimeout(suggestTimer)
})

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
    loadBadge()
  },
  { immediate: true }
)
watch(() => userStore.token, loadBadge)
</script>

<template>
  <header class="navbar" :class="{ scrolled, 'menu-open': menuOpen }">
    <span class="navbar-goldline" aria-hidden="true"></span>

    <div class="container navbar-inner">
      <button
        class="menu-toggle"
        type="button"
        :aria-expanded="menuOpen"
        aria-label="打开导航"
        @click="menuOpen = !menuOpen"
      >
        <span class="menu-bars" aria-hidden="true">
          <i></i>
          <i></i>
          <i></i>
        </span>
      </button>

      <div class="logo" @click="goHome">
        <span class="logo-seal" aria-hidden="true">集</span>
        <span class="logo-copy">
          <span class="logo-text">集物商店</span>
          <span class="logo-sub">JIWU GOODS</span>
        </span>
      </div>

      <nav class="navs" aria-label="主导航">
        <span
          v-for="item in navs"
          :key="item.label"
          class="nav-item"
          :class="{ active: item.to && route.path === item.to }"
          @click="onNav(item)"
        >
          {{ item.label }}
        </span>
      </nav>

      <div class="search">
        <el-input
          v-model="keyword"
          placeholder="搜一件好物，或一个品牌"
          clearable
          @input="onInput"
          @focus="onInput"
          @keydown="onSearchKeydown"
          @keyup.enter="onSearchEnter"
          @blur="onSearchBlur"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <transition name="suggest">
          <div v-if="showSuggest && suggestions.length" class="suggest-panel">
            <div
              v-for="(s, i) in suggestions"
              :key="s"
              class="suggest-item"
              :class="{ active: i === activeIndex }"
              @mousedown.prevent="pickSuggestion(s)"
            >
              <el-icon :size="13"><Search /></el-icon>
              <span class="suggest-text">{{ s }}</span>
            </div>
          </div>
        </transition>
      </div>

      <div class="actions">
        <div class="action ai-action" title="AI 导购" @click="goAiGuide">
          <el-icon :size="18"><ChatDotRound /></el-icon>
          <span class="action-label">问导购</span>
        </div>

        <div class="action cart-action" title="购物车" data-cart-anchor @click="goCart">
          <el-badge
            :value="cartCount"
            :hidden="cartCount === 0"
            :max="99"
            :class="{ 'badge-pop': cartPop }"
          >
            <el-icon :size="19"><ShoppingCart /></el-icon>
          </el-badge>
        </div>

        <el-dropdown v-if="isLogin" trigger="click">
          <span class="user-entry">
            <span class="avatar">{{ nickname.slice(0, 1) }}</span>
            <span class="nickname">{{ nickname }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goOrder">我的订单</el-dropdown-item>
              <el-dropdown-item @click="goProfile">个人信息</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button v-else class="login-btn" type="primary" size="small" @click="goLogin">
          <span class="login-long">登录 / 注册</span>
          <span class="login-short">登录</span>
        </el-button>
      </div>
    </div>

    <transition name="menu">
      <nav v-if="menuOpen" class="mobile-nav" aria-label="移动端导航">
        <div class="container mobile-nav-inner">
          <button
            v-for="item in navs"
            :key="item.label"
            type="button"
            class="mobile-nav-item"
            :class="{ active: item.to && route.path === item.to }"
            @click="onNav(item)"
          >
            <span>{{ item.label }}</span>
            <el-icon :size="13"><ArrowRight /></el-icon>
          </button>
          <button type="button" class="mobile-nav-item" @click="goAiGuide">
            <span>AI 导购</span>
            <el-icon :size="13"><ArrowRight /></el-icon>
          </button>
        </div>
      </nav>
    </transition>
  </header>
</template>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(251, 252, 248, 0.96);
  -webkit-backdrop-filter: blur(14px);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--line-strong);
  transition: background 0.25s ease, box-shadow 0.25s ease;
}

.navbar-goldline {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gold);
}

.navbar.scrolled {
  background: var(--paper);
  box-shadow: 4px 4px 0 rgba(22, 58, 46, 0.07);
}

.navbar-inner {
  height: var(--header-height);
  display: flex;
  align-items: center;
  gap: 26px;
  transition: height 0.25s ease;
}

.navbar.scrolled .navbar-inner {
  height: 64px;
}

.menu-toggle {
  display: none;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  place-items: center;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 2px;
  background: transparent;
  color: var(--pine-800);
  cursor: pointer;
}

.menu-toggle:hover {
  border-color: var(--pine-700);
  background: var(--brand-soft);
}

.menu-bars {
  display: flex;
  width: 16px;
  flex-direction: column;
  gap: 4px;
}

.menu-bars i {
  display: block;
  width: 100%;
  height: 1px;
  background: var(--pine-800);
  transition: background 0.18s ease;
}

.menu-toggle:hover .menu-bars i {
  background: var(--pine-600);
}

.logo {
  display: flex;
  align-items: center;
  gap: 11px;
  flex-shrink: 0;
  cursor: pointer;
  user-select: none;
}

.logo-seal {
  display: grid;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  place-items: center;
  border: 1px solid var(--gold);
  border-radius: 2px;
  background: var(--pine-800);
  box-shadow: 3px 3px 0 rgba(228, 166, 47, 0.42);
  color: var(--paper);
  font-family: var(--font-display);
  font-size: 21px;
  font-weight: 700;
  line-height: 1;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.logo:hover .logo-seal {
  transform: translate(-1px, -1px) rotate(-2deg);
  box-shadow: 5px 5px 0 rgba(228, 166, 47, 0.5);
}

.logo-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.logo-text {
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.logo-sub {
  margin-top: 4px;
  color: var(--sage);
  font-family: var(--font-num);
  font-size: 9px;
  letter-spacing: 0.18em;
}

.navs {
  display: flex;
  align-items: center;
  gap: 25px;
}

.nav-item {
  position: relative;
  padding: 8px 0 7px;
  color: var(--text-body);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.18s ease;
}

.nav-item::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  background: var(--gold);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.22s ease;
}

.nav-item:hover,
.nav-item.active {
  color: var(--pine-800);
}

.nav-item:hover::after,
.nav-item.active::after {
  transform: scaleX(1);
}

.search {
  position: relative;
  flex: 1;
  max-width: 286px;
  margin-left: auto;
  transition: max-width 0.3s ease;
}

.search:focus-within {
  max-width: 340px;
}

.search :deep(.el-input__wrapper) {
  min-height: 38px;
  padding: 2px 12px;
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 0 0 0 1px var(--line) inset;
}

.search :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--line-strong) inset;
}

.search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--pine-600) inset;
}

.search :deep(.el-input__inner) {
  font-size: 12px;
}

.search :deep(.el-input__prefix) {
  color: var(--pine-600);
}

.suggest-panel {
  position: absolute;
  top: calc(100% + 7px);
  left: 0;
  right: 0;
  z-index: 200;
  padding: 5px;
  border: 1px solid var(--line-strong);
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 5px 5px 0 rgba(22, 58, 46, 0.1);
}

.suggest-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 11px;
  border-radius: 2px;
  color: var(--text-body);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease;
}

.suggest-item:hover,
.suggest-item.active {
  background: var(--brand-soft);
  color: var(--pine-800);
}

.suggest-text {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.suggest-enter-active,
.suggest-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.suggest-enter-from,
.suggest-leave-to {
  opacity: 0;
  transform: translate3d(0, -5px, 0);
}

.actions {
  display: flex;
  align-items: center;
  gap: 17px;
}

.action {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--pine-800);
  font-size: 12px;
  cursor: pointer;
  transition: color 0.18s ease, transform 0.18s ease;
}

.action:hover {
  color: var(--vermilion);
  transform: translateY(-1px);
}

.action-label {
  white-space: nowrap;
}

.cart-action {
  padding: 2px;
}

.cart-action :deep(.el-badge__content) {
  border: none;
  border-radius: 2px;
  background: var(--vermilion);
  font-size: 10px;
}

.badge-pop :deep(.el-badge__content) {
  animation: badge-pop 0.52s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes badge-pop {
  0% {
    transform: translateY(-50%) translateX(100%) scale(1);
  }
  35% {
    transform: translateY(-50%) translateX(100%) scale(1.5);
  }
  100% {
    transform: translateY(-50%) translateX(100%) scale(1);
  }
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.avatar {
  display: flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--gold);
  border-radius: 2px;
  background: var(--pine-800);
  color: var(--paper);
  font-family: var(--font-display);
  font-size: 14px;
  transition: box-shadow 0.2s ease;
}

.user-entry:hover .avatar {
  box-shadow: 3px 3px 0 rgba(228, 166, 47, 0.55);
}

.nickname {
  max-width: 72px;
  overflow: hidden;
  color: var(--pine-800);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-btn {
  height: 34px;
  padding: 0 15px;
  border-radius: 2px;
  font-size: 12px;
}

.login-short {
  display: none;
}

.mobile-nav {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line-strong);
  background: var(--paper);
  box-shadow: 5px 5px 0 rgba(22, 58, 46, 0.1);
}

.mobile-nav-inner {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px 18px;
  padding-top: 14px;
  padding-bottom: 16px;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 2px;
  border: none;
  border-bottom: 1px solid var(--line);
  background: transparent;
  color: var(--text-body);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.mobile-nav-item:hover,
.mobile-nav-item.active {
  color: var(--pine-800);
}

.menu-enter-active,
.menu-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.menu-enter-from,
.menu-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 1160px) {
  .menu-toggle {
    display: grid;
  }

  .navs {
    display: none;
  }

  .navbar-inner {
    gap: 18px;
  }

  .action-label {
    display: none;
  }
}

@media (max-width: 760px) {
  .logo-sub,
  .nickname {
    display: none;
  }

  .logo-seal {
    width: 34px;
    height: 34px;
    font-size: 19px;
  }

  .logo-text {
    font-size: 16px;
  }

  .search {
    max-width: 180px;
  }

  .search:focus-within {
    max-width: 210px;
  }

  .actions {
    gap: 12px;
  }

  .login-btn {
    padding: 0 10px;
  }
}

@media (max-width: 520px) {
  .navbar-inner {
    gap: 10px;
  }

  .logo-copy {
    display: none;
  }

  .search {
    max-width: none;
    margin-left: 0;
  }

  .search:focus-within {
    max-width: none;
  }

  .ai-action {
    display: none;
  }

  .mobile-nav-inner {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 380px) {
  .navbar-inner {
    gap: 8px;
    padding-left: 12px;
    padding-right: 12px;
  }

  .actions {
    gap: 8px;
  }

  .login-btn {
    padding: 0 8px;
  }

  .login-long {
    display: none;
  }

  .login-short {
    display: inline;
  }
}
</style>
