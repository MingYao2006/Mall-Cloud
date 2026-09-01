<template>
  <header class="jd-header">
    <div class="top-bar">
      <div class="top-inner">
        <span class="top-left">送至：<el-icon><Location /></el-icon> 中国大陆</span>
        <div class="top-right">
          <template v-if="userStore.token">
            <span>Hi, {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
            <span class="sep">|</span>
            <a @click="$router.push('/orders')"><el-icon :size="16"><Tickets /></el-icon> 我的订单</a>
            <span class="sep">|</span>
            <a @click="$router.push('/profile')">个人信息</a>
            <span class="sep">|</span>
            <a @click="handleLogout">退出</a>
          </template>
          <template v-else>
            <a @click="$router.push('/login')">你好，请登录</a>
            <span class="sep">|</span>
            <a @click="$router.push('/register')">免费注册</a>
          </template>
          <span class="sep">|</span>
          <a @click="$router.push('/orders')">我的订单</a>
          <span class="sep">|</span>
          <a class="top-cart" @click="$router.push('/cart')">
            <el-icon :size="20"><ShoppingCart /></el-icon> 购物车
            <span v-if="cartCount" class="cart-badge">{{ cartCount }}</span>
          </a>
        </div>
      </div>
    </div>

    <div class="main-bar">
      <div class="main-inner">
        <div class="logo" @click="$router.push('/home')">
          <span class="logo-text">MALL</span><span class="logo-dot">.</span>
        </div>

        <div class="category-btn" @mouseenter="showCate=true" @mouseleave="showCate=false">
          <el-icon :size="18"><Menu /></el-icon>
          <span>全部商品分类</span>
          <div v-show="showCate" class="category-panel">
            <div class="cate-item" v-for="c in categories" :key="c.id"
                 @mouseenter="hoverCate=c.id" @click="goCategory(c.id)">
              <span>{{ c.name }}</span>
              <el-icon><ArrowRight /></el-icon>
              <div v-show="hoverCate===c.id && c.children?.length" class="sub-panel">
                <div class="sub-item" v-for="s in c.children" :key="s.id" @click.stop="goSubCategory(s.id)">
                  <img v-if="s.icon" :src="s.icon" class="sub-icon" />
                  <span>{{ s.name }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="search-box">
          <el-input v-model="keyword" placeholder="搜索商品" size="large"
                    @keyup.enter="doSearch" class="search-input" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
            <template #append>
              <el-button :icon="Search" @click="doSearch" class="search-btn">搜索</el-button>
            </template>
          </el-input>
          <div class="hot-words">
            <span v-for="w in hotWords" :key="w" @click="keyword=w;doSearch()">{{ w }}</span>
          </div>
        </div>

        <div class="header-icons">
          <div class="header-icon-btn" @click="$router.push('/cart')">
            <el-icon :size="28"><ShoppingCart /></el-icon>
            <span class="icon-label">购物车</span>
            <span v-if="cartCount" class="cart-dot">{{ cartCount }}</span>
          </div>
          <div class="header-icon-btn" @click="$router.push('/orders')">
            <el-icon :size="28"><Tickets /></el-icon>
            <span class="icon-label">我的订单</span>
          </div>
        </div>
      </div>
    </div>

    <div class="nav-bar">
      <div class="nav-inner">
        <a v-for="n in navs" :key="n.name" :class="{ active: isNavActive(n) }"
           @click="handleNav(n)">
          {{ n.name }}
        </a>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { categoryApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Search, ArrowRight, ShoppingCart, Menu, Location, Tickets } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const keyword = ref('')
const cartCount = ref(0)
const showCate = ref(false)
const hoverCate = ref(null)
const categories = ref([])

const hotWords = ['iPhone', '华为', '小米', 'MacBook', '格力', '戴森']

const navs = [
  { name: '首页', path: '/home', match: (r) => r.path === '/home' && !r.query.category && !r.query.keyword },
  { name: '秒杀', path: '/seckill', match: (r) => r.path === '/seckill' },
  { name: '手机数码', query: { category: '4' } },
  { name: '电脑办公', query: { category: '5' } },
  { name: '智能穿戴', query: { category: '8' } },
  { name: '大家电', query: { category: '9' } },
  { name: '男装', query: { category: '6' } },
  { name: '女装', query: { category: '7' } },
]

const isNavActive = (n) => {
  if (n.match) return n.match(route)
  if (n.query) return route.query.category === n.query.category
  return false
}

const handleNav = (n) => {
  if (n.path) router.push(n.path)
  else router.push({ path: '/home', query: n.query })
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.getList()
    categories.value = (res.data || []).filter(c => c.parentId === 0).map(c => ({
      ...c,
      children: (res.data || []).filter(s => s.parentId === c.id)
    }))
  } catch (e) { /* ignore */ }
}

const doSearch = () => {
  if (keyword.value.trim()) {
    router.push({ path: '/home', query: { keyword: keyword.value.trim() } })
  }
}

const goCategory = (id) => { showCate.value = false; router.push({ path: '/home', query: { category: id } }) }
const goSubCategory = (id) => { showCate.value = false; router.push({ path: '/home', query: { category: id } }) }

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/login')
}

onMounted(() => loadCategories())
</script>

<style scoped>
.top-bar { background: #f1f1f1; font-size: 13px; color: #666; }
.top-inner { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; padding: 8px 0; }
.top-right { display: flex; align-items: center; gap: 8px; }
.top-right a { cursor: pointer; display: flex; align-items: center; gap: 3px; font-size: 13px; }
.top-right a:hover { color: var(--jd-red); }
.sep { color: #ddd; }
.top-cart { position: relative; }
.cart-badge { position: absolute; top: -8px; right: -14px; background: var(--jd-red); color: #fff; border-radius: 10px; padding: 0 6px; font-size: 11px; line-height: 16px; }

.main-bar { background: #fff; border-bottom: 2px solid var(--jd-red); }
.main-inner { max-width: 1200px; margin: 0 auto; display: flex; align-items: center; padding: 14px 0; gap: 16px; }
.logo { cursor: pointer; flex-shrink: 0; }
.logo-text { font-size: 30px; font-weight: 800; color: var(--jd-red); letter-spacing: 2px; }
.logo-dot { font-size: 30px; color: var(--jd-red); }

.category-btn { display: flex; align-items: center; gap: 6px; background: var(--jd-red); color: #fff; padding: 10px 16px; border-radius: 4px; cursor: pointer; position: relative; font-size: 14px; white-space: nowrap; flex-shrink: 0; }
.category-panel { position: absolute; top: 100%; left: 0; background: #fff; color: #333; width: 220px; box-shadow: 0 4px 16px rgba(0,0,0,0.1); z-index: 999; border-radius: 0 0 4px 4px; }
.cate-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; cursor: pointer; font-size: 13px; position: relative; }
.cate-item:hover { color: var(--jd-red); background: #fff5f5; }
.sub-panel { position: absolute; left: 100%; top: 0; background: #fff; min-width: 400px; padding: 16px; box-shadow: 0 4px 16px rgba(0,0,0,0.1); display: flex; flex-wrap: wrap; gap: 10px; z-index: 1000; }
.sub-item { display: flex; align-items: center; gap: 6px; cursor: pointer; padding: 6px 12px; border-radius: 4px; }
.sub-item:hover { background: #f5f5f5; color: var(--jd-red); }
.sub-icon { width: 28px; height: 28px; border-radius: 4px; }

.search-box { flex: 1; max-width: 460px; }
.search-input :deep(.el-input__wrapper) { border: 2px solid var(--jd-red); border-radius: 4px 0 0 4px; box-shadow: none; }
.search-input :deep(.el-input__prefix) { color: #999; }
.search-btn { background: var(--jd-red) !important; border-color: var(--jd-red) !important; border-radius: 0 4px 4px 0 !important; font-size: 16px; padding: 0 24px !important; height: 42px; color: #fff !important; }
.hot-words { margin-top: 6px; display: flex; gap: 12px; font-size: 12px; color: #999; }
.hot-words span { cursor: pointer; }
.hot-words span:hover { color: var(--jd-red); }

.header-icons { display: flex; gap: 20px; flex-shrink: 0; margin-left: auto; }
.header-icon-btn { display: flex; flex-direction: column; align-items: center; gap: 4px; cursor: pointer; position: relative; color: #333; padding: 0 8px; transition: color .2s; }
.header-icon-btn:hover { color: var(--jd-red); }
.icon-label { font-size: 12px; }
.cart-dot { position: absolute; top: -4px; right: -2px; background: var(--jd-red); color: #fff; border-radius: 10px; min-width: 18px; height: 18px; text-align: center; line-height: 18px; font-size: 11px; padding: 0 5px; }

.nav-bar { background: #fff; border-bottom: 1px solid #eee; }
.nav-inner { max-width: 1200px; margin-left: 236px; display: flex; gap: 28px; }
.nav-inner a { cursor: pointer; font-size: 15px; padding: 14px 0; position: relative; color: #333; transition: color .2s; }
.nav-inner a.active { color: var(--jd-red); font-weight: 700; }
.nav-inner a.active::after { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 2px; background: var(--jd-red); }
.nav-inner a:hover { color: var(--jd-red); }
</style>
