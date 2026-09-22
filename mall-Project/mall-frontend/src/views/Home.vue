<template>
  <div class="home">
    <!-- 第一屏：沉浸式 Hero 区 -->
    <section class="hero" ref="heroRef">
      <div class="hero-bg" :style="{ background: currentBanner.bg }"></div>
      <div class="hero-content" @click="currentBanner.link()">
        <h1 class="hero-title">{{ currentBanner.title }}</h1>
        <p class="hero-subtitle">{{ currentBanner.sub }}</p>
        <button class="hero-cta" @click.stop="currentBanner.link()">
          立即探索
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
      <div class="hero-nav">
        <button v-for="(b, i) in banners" :key="i" 
                :class="['hero-dot', { active: i === bannerIdx }]"
                @click="bannerIdx = i"></button>
      </div>
    </section>

    <!-- 第二屏：分类导航 -->
    <section class="categories reveal" v-if="topCategories.length">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title" style="margin-bottom: 0">探索品类</h2>
          <a @click="viewAllProducts">查看全部</a>
        </div>
        <div class="category-scroll">
          <template v-for="cate in topCategories" :key="cate.id">
            <div class="category-card"
                 @click="goCategory(cate.id)">
              <div class="category-icon" :style="{ background: cate.color || '#e8734a' }">
                <span>{{ cate.name.charAt(0) }}</span>
              </div>
              <span class="category-name">{{ cate.name }}</span>
            </div>
            <div v-for="child in cate.children" :key="child.id"
                 class="category-card"
                 @click="goCategory(child.id)">
              <div class="category-icon category-icon-sm" :style="{ background: cate.color || '#e8734a' }">
                <span>{{ child.name.charAt(0) }}</span>
              </div>
              <span class="category-name">{{ child.name }}</span>
            </div>
          </template>
        </div>
      </div>
    </section>

    <!-- 第三屏：秒杀倒计时 -->
    <section class="seckill reveal" v-if="productsLoaded">
      <div class="container">
        <div class="seckill-header">
          <div class="seckill-title-wrap">
            <span class="seckill-icon">⚡</span>
            <h2>限时秒杀</h2>
          </div>
          <div class="countdown">
            <span class="cd-label">距结束</span>
            <span class="cd-digit">{{ countdown.hh }}</span>
            <span class="cd-sep">:</span>
            <span class="cd-digit">{{ countdown.mm }}</span>
            <span class="cd-sep">:</span>
            <span class="cd-digit">{{ countdown.ss }}</span>
          </div>
        </div>
        <div class="seckill-scroll" v-if="hotProducts.length">
          <div v-for="p in hotProducts.slice(0, 4)" :key="p.id" 
               class="seckill-card"
               @click="$router.push(`/product/${p.id}`)">
            <div class="sc-img">
              <img v-if="p.mainImage" :src="p.mainImage" :alt="p.name" />
              <div v-else class="sc-placeholder">{{ p.name?.charAt(0) }}</div>
              <span class="sc-tag">HOT</span>
            </div>
            <div class="sc-info">
              <p class="sc-name">{{ p.name }}</p>
              <div class="sc-price">
                <span class="sc-now price">¥{{ p.price?.toFixed(2) }}</span>
                <span class="sc-was" v-if="p.originalPrice">¥{{ p.originalPrice?.toFixed(2) }}</span>
              </div>
              <el-button type="primary" size="small" round @click.stop="addCart(p)">
                抢购
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无秒杀商品" />
        <div class="seckill-more" @click="$router.push('/seckill')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </section>

    <!-- 第四屏：精选推荐（混排） -->
    <section class="featured reveal" v-if="productsLoaded">
      <div class="container">
        <div class="section-header">
          <h2>{{ filterLabel || '精选推荐' }}</h2>
          <a v-if="filterLabel" @click="clearFilter">查看全部</a>
        </div>
        
        <el-empty v-if="!displayProducts.length" description="暂无商品">
          <el-button type="primary" @click="clearFilter">返回全部商品</el-button>
        </el-empty>
        
        <div v-else class="featured-grid">
          <!-- 大图 Featured -->
          <div v-if="displayProducts[0]" class="featured-main" @click="$router.push(`/product/${displayProducts[0].id}`)">
            <div class="fm-img">
              <img v-if="displayProducts[0].mainImage" :src="displayProducts[0].mainImage" />
              <div v-else class="fm-placeholder">{{ displayProducts[0].name }}</div>
            </div>
            <div class="fm-info">
              <h3>{{ displayProducts[0].name }}</h3>
              <p class="fm-price">
                <span class="price price-lg">¥{{ displayProducts[0].price?.toFixed(2) }}</span>
                <span class="fm-original" v-if="displayProducts[0].originalPrice">¥{{ displayProducts[0].originalPrice?.toFixed(2) }}</span>
              </p>
              <el-button type="primary" round @click.stop="addCart(displayProducts[0])">
                <el-icon><ShoppingCart /></el-icon> 加入购物车
              </el-button>
            </div>
          </div>
          
          <!-- 小图网格 -->
          <div class="featured-sub" v-if="displayProducts.length > 1">
            <div v-for="p in displayProducts.slice(1, 5)" :key="p.id"
                 class="fs-card"
                 @click="$router.push(`/product/${p.id}`)">
              <div class="fs-img">
                <img v-if="p.mainImage" :src="p.mainImage" />
                <div v-else class="fs-placeholder">{{ p.name?.charAt(0) }}</div>
              </div>
              <div class="fs-info">
                <p class="fs-name">{{ p.name }}</p>
                <p class="fs-price price">¥{{ p.price?.toFixed(2) }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 更多商品（分页展示） -->
    <section class="products reveal" v-if="productsLoaded && displayProducts.length > 5">
      <div class="container">
        <div class="section-header">
          <h2>更多好物</h2>
          <span class="result-count">共 {{ displayProducts.length }} 件，已加载 {{ Math.min(currentProducts.length, displayProducts.length - 5) }} 件</span>
        </div>
        <div class="product-grid">
          <div v-for="p in currentProducts" :key="p.id"
               class="product-card"
               @click="$router.push(`/product/${p.id}`)">
            <div class="pc-img">
              <img v-if="p.mainImage" :src="p.mainImage" :alt="p.name" />
              <div v-else class="pc-placeholder">{{ p.name }}</div>
              <div class="pc-overlay">
                <el-button size="small" type="primary" round @click.stop="addCart(p)">
                  <el-icon><ShoppingCart /></el-icon>
                </el-button>
              </div>
            </div>
            <div class="pc-info">
              <p class="pc-name">{{ p.name }}</p>
              <p class="pc-price price">¥{{ p.price?.toFixed(2) }}</p>
            </div>
          </div>
        </div>
        
        <div class="load-more" v-if="hasMore">
          <el-button type="primary" :loading="loadingMore" @click="loadMoreProducts">
            {{ loadingMore ? '加载中...' : '加载更多' }}
          </el-button>
        </div>
      </div>
    </section>

    <!-- 第五屏：信任背书 -->
    <section class="trust reveal">
      <div class="container">
        <div class="trust-grid">
          <div class="trust-item">
            <el-icon :size="32"><Goods /></el-icon>
            <h4>品质保障</h4>
            <p>正品直供 放心选购</p>
          </div>
          <div class="trust-item">
            <el-icon :size="32"><Van /></el-icon>
            <h4>极速配送</h4>
            <p>全国包邮 闪电发货</p>
          </div>
          <div class="trust-item">
            <el-icon :size="32"><RefreshRight /></el-icon>
            <h4>无忧售后</h4>
            <p>7天退换 省心保障</p>
          </div>
          <div class="trust-item">
            <el-icon :size="32"><Service /></el-icon>
            <h4>专属客服</h4>
            <p>一对一服务 随时在线</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 返回顶部 -->
    <Transition name="fade">
      <div v-show="showBackTop" class="back-top" @click="scrollToTop">
        <el-icon :size="20"><Top /></el-icon>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { productApi, categoryApi, cartApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { ShoppingCart, Top, ArrowRight, Goods, Van, RefreshRight, Service } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 数据
const allProducts = ref([])
const topCategories = ref([])
const productsLoaded = ref(false)
const bannerIdx = ref(0)
const stopBanner = ref(false)
const showBackTop = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const pageNum = ref(1)
const filterLabel = ref('')
const searchKeyword = ref('')
const heroRef = ref(null)

// 分页
const productsPerPage = 12 // 每页显示12个
const currentProducts = ref([]) // 当前已加载的商品

// 加载商品（初始只加载前5个给精选推荐）

// 轮播
const banners = [
  { bg: 'linear-gradient(135deg, #1a1a1a 0%, #333 100%)', title: '新品首发', sub: '科技与美学的完美融合', link: () => router.push('/home') },
  { bg: 'linear-gradient(135deg, #e8734a 0%, #d4623a 100%)', title: '限时特惠', sub: '精选好物 低至 5 折', link: () => router.push('/seckill') },
  { bg: 'linear-gradient(135deg, #4a9e7a 0%, #3d8b6a 100%)', title: '品质生活', sub: '用心甄选 只为更好的你', link: () => router.push('/home') },
]

const currentBanner = computed(() => banners[bannerIdx.value])

// 倒计时
const countdown = ref({ hh: '00', mm: '00', ss: '00' })
let cdTimer = null, bannerTimer = null

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

// 分类
const getCategoryTreeIds = (parentId) => {
  const ids = [parentId]
  const children = topCategories.value.flatMap(c => [c, ...(c.children||[])])
  const find = (pid) => children.filter(c => c.parentId === pid).forEach(c => { ids.push(c.id); find(c.id) })
  find(parentId)
  return ids
}

const goCategory = (id) => router.push({ path: '/home', query: { category: id } })
const clearFilter = () => router.push('/home')

// 探索品类“查看全部”：清除筛选并滚动到商品列表
const viewAllProducts = async () => {
  if (route.query.category || route.query.keyword) {
    await router.push('/home')
  }
  nextTick(() => {
    const el = document.querySelector('.products') || document.querySelector('.featured')
    if (el) el.scrollIntoView({ behavior: 'smooth' })
  })
}

// 筛选
const applyFilter = () => {
  const cid = route.query.category ? Number(route.query.category) : null
  const kw = route.query.keyword || ''
  searchKeyword.value = kw
  if (cid) {
    const allCats = topCategories.value.flatMap(c => [c, ...(c.children||[])])
    const cate = allCats.find(c => c.id === cid)
    filterLabel.value = cate ? cate.name : '商品筛选'
  } else if (kw) {
    filterLabel.value = `搜索"${kw}"`
  } else {
    filterLabel.value = ''
  }
}

watch(() => [route.query.category, route.query.keyword], () => applyFilter())

// 商品数据
const displayProducts = computed(() => {
  const cid = route.query.category ? Number(route.query.category) : null
  const kw = searchKeyword.value
  let list = allProducts.value
  if (cid) {
    const ids = getCategoryTreeIds(cid)
    list = list.filter(p => ids.includes(p.categoryId))
  }
  if (kw) {
    const lower = kw.toLowerCase()
    list = list.filter(p => (p.name && p.name.toLowerCase().includes(lower)) || (p.brand && p.brand.toLowerCase().includes(lower)))
  }
  return list
})

const hotProducts = computed(() => 
  allProducts.value.filter(p => p.status === 1).sort((a, b) => (b.sales || 0) - (a.sales || 0))
)

// 加载
const loadAllProducts = async () => {
  try { 
    console.log('开始加载商品...')
    const res = await productApi.getList()
    allProducts.value = res.data || []
    console.log('加载成功，商品数量:', allProducts.value.length)
    
    // 初始只显示前5个商品（给精选推荐）
    // currentProducts 初始为空，点击“加载更多”后才显示
  }
  catch (e) { 
    console.error('加载商品失败:', e)
  }
  finally { 
    productsLoaded.value = true
    console.log('productsLoaded设为true')
    // 数据加载完成后，强制显示所有区块
    nextTick(() => {
      setTimeout(() => {
        document.querySelectorAll('.reveal:not(.revealed)').forEach(el => {
          if (observer) observer.observe(el)
        })
        setTimeout(() => {
          document.querySelectorAll('.reveal:not(.revealed)').forEach(el => {
            const rect = el.getBoundingClientRect()
            if (rect.top < window.innerHeight && rect.bottom > 0) {
              el.classList.add('revealed')
            }
          })
        }, 300)
        console.log('重新观察元素完成')
      }, 200)
    })
  }
}

// 加载更多商品
const loadMoreProducts = async () => {
  if (loadingMore.value) return
  
  loadingMore.value = true
  
  // 计算本次要加载的商品（从索引5开始，因为前5个在精选推荐区）
  const startIndex = 5 + (pageNum.value - 1) * productsPerPage
  const endIndex = startIndex + productsPerPage
  const newProducts = displayProducts.value.slice(startIndex, endIndex)
  
  // 模拟加载延迟
  await new Promise(resolve => setTimeout(resolve, 300))
  
  currentProducts.value = [...currentProducts.value, ...newProducts]
  
  // 检查是否还有更多
  if (endIndex >= displayProducts.value.length) {
    hasMore.value = false
  } else {
    pageNum.value++
  }
  
  loadingMore.value = false
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.getList()
    const all = res.data || []
    const colors = ['#e8734a', '#4a9e7a', '#5a8fbf', '#8b6a4a', '#9e4a7a', '#6a4a9e', '#4a7a9e', '#9e8a4a']
    topCategories.value = all.filter(c => c.parentId === 0).map((c, i) => ({
      ...c,
      children: all.filter(s => s.parentId === c.id),
      color: colors[i % colors.length]
    }))
    applyFilter()
  } catch (e) { console.error('加载分类失败:', e) }
}

const loadPage = async () => {
  // 旧的加载函数，已被 loadMoreProducts 替代
}

const addCart = async (product) => {
  if (!userStore.token) { ElMessage.warning('请先登录'); router.push('/login'); return }
  try { await cartApi.add(product.id, 1); ElMessage.success('已加入购物车') }
  catch (e) { ElMessage.error('加入失败') }
}

// 滚动
const handleScroll = () => { showBackTop.value = window.scrollY > 500 }
const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

// IntersectionObserver
let observer = null
const setupObserver = () => {
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.1 })
}

onMounted(() => {
  startCountdown()
  loadAllProducts()
  loadCategories()
  window.addEventListener('scroll', handleScroll)
  setupObserver()
  
  bannerTimer = setInterval(() => { if (!stopBanner.value) bannerIdx.value = (bannerIdx.value + 1) % banners.length }, 5000)
})

onUnmounted(() => {
  clearInterval(cdTimer)
  clearInterval(bannerTimer)
  window.removeEventListener('scroll', handleScroll)
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.home {
  background: var(--color-bg);
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--spacing-md);
}

/* 滚动淡入 */
.reveal {
  opacity: 0;
  transform: translateY(40px);
  transition: opacity 0.8s cubic-bezier(0.4, 0, 0.2, 1), transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.reveal.revealed {
  opacity: 1;
  transform: translateY(0);
}

/* 第一屏：Hero */
.hero {
  position: relative;
  height: 50vh;
  min-height: 360px;
  overflow: hidden;
  cursor: pointer;
}

.hero-bg {
  position: absolute;
  inset: 0;
  transition: opacity 0.8s ease;
}

.hero-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--spacing-xl);
}

.hero-title {
  font-size: clamp(40px, 6vw, 72px);
  font-weight: 700;
  color: var(--color-white);
  margin-bottom: var(--spacing-md);
  letter-spacing: -1px;
  line-height: 1.1;
}

.hero-subtitle {
  font-size: clamp(16px, 2.5vw, 24px);
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: var(--spacing-lg);
  font-weight: 400;
}

.hero-cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  background: var(--color-white);
  color: var(--color-primary);
  border: none;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.hero-cta:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.hero-nav {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
}

.hero-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  border: none;
  cursor: pointer;
  transition: all 0.3s;
}

.hero-dot.active {
  width: 32px;
  border-radius: 4px;
  background: var(--color-white);
}

/* 第二屏：分类 */
.categories {
  padding: var(--spacing-xl) 0;
  background: var(--color-white);
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: var(--spacing-lg);
  text-align: center;
}

.category-scroll {
  display: flex;
  gap: var(--spacing-md);
  overflow-x: auto;
  padding-bottom: var(--spacing-sm);
  scrollbar-width: none;
}

.category-scroll::-webkit-scrollbar {
  display: none;
}

.category-card {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  cursor: pointer;
  transition: all 0.3s;
  min-width: 120px;
}

.category-card:hover {
  transform: translateY(-4px);
}

.category-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-white);
  font-size: 24px;
  font-weight: 700;
}

.category-icon-sm {
  width: 48px;
  height: 48px;
  font-size: 18px;
  opacity: 0.85;
}

.category-name {
  font-size: 14px;
  color: var(--color-text);
  font-weight: 500;
}

/* 第三屏：秒杀 */
.seckill {
  padding: var(--spacing-xl) 0;
  background: linear-gradient(180deg, var(--color-white) 0%, var(--color-bg) 100%);
}

.seckill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.seckill-title-wrap {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.seckill-icon {
  font-size: 32px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.seckill-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
}

.countdown {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cd-label {
  font-size: 14px;
  color: var(--color-text-lighter);
  margin-right: 8px;
}

.cd-digit {
  background: var(--color-primary);
  color: var(--color-white);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: 24px;
  font-weight: 700;
  font-family: var(--font-mono);
  min-width: 44px;
  text-align: center;
  animation: digitBounce 0.3s ease;
}

@keyframes digitBounce {
  0% { transform: scale(0.9); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

.cd-sep {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
}

.seckill-scroll {
  display: flex;
  gap: var(--spacing-md);
  overflow-x: auto;
  padding-bottom: var(--spacing-sm);
  min-height: 280px;
}

.seckill-card {
  flex-shrink: 0;
  width: 240px;
  background: var(--color-white);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: var(--shadow-sm);
}

.seckill-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-hover);
}

.sc-img {
  position: relative;
  width: 100%;
  height: 200px;
  background: var(--color-bg);
  overflow: hidden;
  display: block;
}

.sc-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.4s;
}

.seckill-card:hover .sc-img img {
  transform: scale(1.08);
}

.sc-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-lighter);
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  min-height: 200px;
  padding: 12px;
  text-align: center;
  word-break: break-word;
}

.sc-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  background: var(--color-accent);
  color: var(--color-white);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.sc-info {
  padding: var(--spacing-sm);
}

.sc-name {
  font-size: 14px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}

.sc-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.sc-now {
  font-size: 20px;
  font-weight: 700;
}

.sc-was {
  font-size: 12px;
  color: var(--color-text-lighter);
  text-decoration: line-through;
}

.sc-info .el-button {
  width: 100%;
}

.seckill-more {
  text-align: center;
  margin-top: var(--spacing-md);
  color: var(--color-text-lighter);
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: color 0.2s;
}

.seckill-more:hover {
  color: var(--color-accent);
}

/* 第四屏：精选推荐 */
.featured {
  padding: var(--spacing-xl) 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.section-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
}

.section-header a {
  font-size: 14px;
  color: var(--color-text-lighter);
  cursor: pointer;
  transition: color 0.2s;
}

.section-header a:hover {
  color: var(--color-accent);
}

.featured-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
}

.featured-main {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: var(--shadow-sm);
}

.featured-main:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-hover);
}

.fm-img {
  width: 100%;
  height: 320px;
  background: var(--color-bg);
  overflow: hidden;
  display: block;
}

.fm-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.4s;
}

.featured-main:hover .fm-img img {
  transform: scale(1.05);
}

.fm-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-lighter);
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  min-height: 320px;
  padding: 16px;
  text-align: center;
  word-break: break-word;
}

.fm-info {
  padding: var(--spacing-md);
}

.fm-info h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--spacing-sm);
}

.fm-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: var(--spacing-sm);
}

.fm-original {
  font-size: 14px;
  color: var(--color-text-lighter);
  text-decoration: line-through;
}

.featured-sub {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
}

.fs-card {
  background: var(--color-white);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: var(--shadow-sm);
}

.fs-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.fs-img {
  width: 100%;
  height: 160px;
  background: var(--color-bg);
  overflow: hidden;
  display: block;
}

.fs-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.4s;
}

.fs-card:hover .fs-img img {
  transform: scale(1.08);
}

.fs-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-lighter);
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  min-height: 160px;
  padding: 8px;
  text-align: center;
  word-break: break-word;
}

.fs-info {
  padding: var(--spacing-sm);
}

.fs-name {
  font-size: 13px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.fs-price {
  font-size: 16px;
  font-weight: 700;
}

/* 更多商品 */
.products {
  padding: var(--spacing-xl) 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
}

.product-card {
  background: var(--color-white);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: var(--shadow-sm);
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-hover);
}

.pc-img {
  width: 100%;
  height: 240px;
  background: var(--color-bg);
  position: relative;
  overflow: hidden;
  display: block;
}

.pc-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.4s;
}

.product-card:hover .pc-img img {
  transform: scale(1.06);
}

.pc-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-lighter);
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  min-height: 240px;
  padding: 12px;
  text-align: center;
  word-break: break-word;
}

.pc-overlay {
  position: absolute;
  bottom: 12px;
  right: 12px;
  opacity: 0;
  transition: all 0.3s;
}

.product-card:hover .pc-overlay {
  opacity: 1;
}

.pc-info {
  padding: var(--spacing-sm);
}

.pc-name {
  font-size: 14px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.pc-price {
  font-size: 18px;
  font-weight: 700;
}

.load-more {
  text-align: center;
  padding: var(--spacing-lg) 0;
}

/* 第五屏：信任背书 */
.trust {
  padding: var(--spacing-xl) 0;
  background: var(--color-white);
  margin-top: var(--spacing-xl);
}

.trust-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
}

.trust-item {
  text-align: center;
  color: var(--color-text);
}

.trust-item .el-icon {
  color: var(--color-accent);
  margin-bottom: var(--spacing-sm);
}

.trust-item h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.trust-item p {
  font-size: 13px;
  color: var(--color-text-lighter);
}

/* 返回顶部 */
.back-top {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 44px;
  height: 44px;
  background: var(--color-white);
  color: var(--color-text);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition: all 0.3s;
}

.back-top:hover {
  transform: scale(1.1);
  box-shadow: var(--shadow-lg);
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 响应式 */
@media (max-width: 968px) {
  .featured-grid {
    grid-template-columns: 1fr;
  }
  
  .featured-sub {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .trust-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero {
    height: 70vh;
    min-height: 500px;
  }
  
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .trust-grid {
    grid-template-columns: 1fr 1fr;
    gap: var(--spacing-md);
  }
}
</style>
