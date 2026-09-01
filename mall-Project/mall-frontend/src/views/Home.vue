<template>
  <div class="home">
    <!-- 悬浮粒子背景 -->
    <div class="particles-bg">
      <span v-for="i in 20" :key="'p'+i" class="particle" :style="particleStyle(i)"></span>
    </div>

    <!-- 主体区域 -->
    <div class="hero-section">
      <div class="hero-inner">
        <div class="hero-cates" @mouseleave="hoverCate=null">
          <!-- 骨架屏 -->
          <template v-if="!categoriesLoaded">
            <div v-for="i in 8" :key="'sk'+i" class="skeleton-cate"></div>
          </template>
          <div v-for="c in topCategories" :key="c.id" class="hero-cate-item"
               :class="{ active: activeCate === c.id }"
               @mouseenter="hoverCate=c.id" @click="goCategory(c.id)">
            <span>{{ c.name }}</span>
            <el-icon><ArrowRight /></el-icon>
            <Transition name="sub-fade">
              <div v-show="hoverCate===c.id && c.children?.length" class="hero-subpop">
                <a v-for="s in c.children" :key="s.id" @click.stop="goCategory(s.id)">{{ s.name }}</a>
              </div>
            </Transition>
          </div>
        </div>

        <div class="hero-banner" ref="bannerRef"
             @mouseenter="stopBanner=true" @mouseleave="stopBanner=false; bannerParallax={x:0,y:0}"
             @mousemove="onBannerMove">
          <div class="banner-track" :style="{ transform: `translateX(-${bannerIdx*100}%)` }">
            <div v-for="(b,i) in banners" :key="i" class="banner-slide"
                 :style="{ background: b.bg, transform: `translate(${bannerParallax.x*20}px, ${bannerParallax.y*20}px)` }"
                 @click="b.link()">
              <div class="banner-text">
                <h2 class="anim-slide-up">{{ b.title }}</h2>
                <p class="anim-slide-up delay-1">{{ b.sub }}</p>
                <span class="banner-btn anim-slide-up delay-2">{{ b.btn }}</span>
              </div>
            </div>
          </div>
          <div class="banner-dots">
            <span v-for="(b,i) in banners" :key="i" :class="{ active: i===bannerIdx }" @click="bannerIdx=i"></span>
          </div>
        </div>

        <div class="hero-news">
          <div class="news-header"><span>商城快讯</span><a @click="$router.push('/orders')">更多</a></div>
          <div class="news-list">
            <a v-for="(n,i) in news" :key="i" class="news-item" :style="{ animationDelay: i*0.3+'s' }">{{ n }}</a>
          </div>
          <div class="news-promo">
            <div class="promo-box" @click="$router.push('/cart')"><el-icon :size="26"><ShoppingCart /></el-icon><span>购物车</span></div>
            <div class="promo-box" @click="$router.push('/orders')"><el-icon :size="26"><Tickets /></el-icon><span>我的订单</span></div>
            <div class="promo-box" @click="$router.push('/coupon')"><el-icon :size="26"><Ticket /></el-icon><span>优惠券</span></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 搜索 / 分类筛选结果 ===== -->
    <div v-if="filterLabel" class="section reveal-section">
      <div class="section-hd">
        <h3>🔍 {{ filterLabel }}</h3>
        <span class="result-count">共 {{ displayProducts.length }} 件商品</span>
        <a @click="clearFilter">✕ 清除筛选</a>
      </div>
      <div class="product-grid" v-if="displayProducts.length">
        <div v-for="(p, idx) in displayProducts" :key="p.id" class="product-card card-3d reveal-item"
             :style="{ transitionDelay: idx*0.05+'s' }"
             @mousemove="onCardTilt" @mouseleave="resetCardTilt"
             @click="$router.push(`/product/${p.id}`)">
          <div class="pc-img">
            <img :src="p.mainImage || 'https://placehold.co/300x300/eee/999?text='+p.name" :alt="p.name" />
            <div class="pc-actions">
              <el-button size="small" type="primary" circle @click.stop="addCart(p)"><el-icon><ShoppingCart /></el-icon></el-button>
            </div>
          </div>
          <div class="pc-info">
            <p class="pc-name">{{ p.name }}</p>
            <p class="pc-price price-red">¥{{ p.price?.toFixed(2) }}
              <span class="pc-original" v-if="p.originalPrice">¥{{ p.originalPrice?.toFixed(2) }}</span>
            </p>
            <p class="pc-meta">
              <span>{{ p.brand || '品牌' }}</span>
              <span class="pc-sold"><el-icon><Sell /></el-icon> {{ p.sales || 0 }}</span>
            </p>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无匹配商品">
        <el-button type="primary" @click="clearFilter">返回全部商品</el-button>
      </el-empty>
    </div>

    <!-- 骨架屏（加载中） -->
    <template v-if="!productsLoaded && !filterLabel">
      <div class="section">
        <div class="section-hd"><div class="skeleton-bar" style="width:150px;height:24px;"></div></div>
        <div class="flash-grid">
          <div v-for="i in 6" :key="'fsk'+i" class="skeleton-flash"></div>
        </div>
      </div>
      <div class="section">
        <div class="section-hd"><div class="skeleton-bar" style="width:120px;height:24px;"></div></div>
        <div class="brand-grid">
          <div v-for="i in 8" :key="'bsk'+i" class="skeleton-brand"></div>
        </div>
      </div>
      <div class="section">
        <div class="section-hd"><div class="skeleton-bar" style="width:120px;height:24px;"></div></div>
        <div class="product-grid">
          <div v-for="i in 8" :key="'psk'+i" class="skeleton-product"></div>
        </div>
      </div>
    </template>

    <!-- ===== 秒杀区 ===== -->
    <div class="section flash-sale reveal-section" v-if="!filterLabel && productsLoaded">
      <div class="section-hd">
        <h3><el-icon><Timer /></el-icon> 限时秒杀</h3>
        <div class="countdown">
          <span class="cd-label">距结束</span>
          <span v-for="(v,k) in countdown" :key="k" class="cd-num">{{ v }}<span v-if="k!=='ss'">:</span></span>
        </div>
        <a @click="$router.push('/seckill')">查看更多 <el-icon><ArrowRight /></el-icon></a>
      </div>
      <div class="flash-grid">
        <div v-for="(p, idx) in hotProducts" :key="p.id" class="flash-card card-3d reveal-item"
             :style="{ transitionDelay: idx*0.06+'s' }"
             @mousemove="onCardTilt" @mouseleave="resetCardTilt"
             @click="$router.push(`/product/${p.id}`)">
          <img :src="p.mainImage || 'https://placehold.co/200x200/eee/999?text='+p.name" :alt="p.name" />
          <p class="flash-name">{{ p.name }}</p>
          <p class="price-red">¥{{ p.price?.toFixed(2) }}</p>
          <p class="flash-old">¥{{ p.originalPrice?.toFixed(2) }}</p>
          <div class="flash-progress"><div :style="{ width: Math.min((p.sales||0)/(p.stock||100)*100, 100)+'%' }"></div></div>
        </div>
      </div>
    </div>

    <!-- ===== 品牌精选 ===== -->
    <div class="section brands reveal-section" v-if="!filterLabel && productsLoaded">
      <div class="section-hd"><h3>品牌精选</h3></div>
      <div class="brand-grid">
        <div v-for="(b, idx) in brands" :key="b.name" class="brand-card reveal-item"
             :style="{ transitionDelay: idx*0.05+'s' }"
             @click="searchBrand(b.keyword)">
          <div class="brand-avatar" :style="{ background: b.color }">{{ b.name.charAt(0) }}</div>
          <span>{{ b.name }}</span>
        </div>
      </div>
    </div>

    <!-- ===== 新品推荐 ===== -->
    <div class="section reveal-section" v-if="!filterLabel && productsLoaded">
      <div class="section-hd"><h3>新品推荐</h3></div>
      <div class="product-grid">
        <div v-for="(p, idx) in newProducts" :key="p.id" class="product-card card-3d reveal-item"
             :style="{ transitionDelay: idx*0.05+'s' }"
             @mousemove="onCardTilt" @mouseleave="resetCardTilt"
             @click="$router.push(`/product/${p.id}`)">
          <div class="pc-img">
            <img :src="p.mainImage || 'https://placehold.co/300x300/eee/999?text='+p.name" :alt="p.name" />
            <span class="tag-new">NEW</span>
            <div class="pc-actions">
              <el-button size="small" type="primary" circle @click.stop="addCart(p)"><el-icon><ShoppingCart /></el-icon></el-button>
            </div>
          </div>
          <div class="pc-info">
            <p class="pc-name">{{ p.name }}</p>
            <p class="pc-price price-red">¥{{ p.price?.toFixed(2) }}
              <span class="pc-original" v-if="p.originalPrice">¥{{ p.originalPrice?.toFixed(2) }}</span>
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 按分类分区 ===== -->
    <template v-if="!filterLabel && productsLoaded">
      <div class="section reveal-section" v-for="cate in topCategories" :key="'c'+cate.id">
        <div class="section-hd">
          <h3>{{ cate.name }}</h3>
          <div class="section-tags">
            <span v-for="s in cate.children" :key="s.id" @click="goCategory(s.id)" class="cate-tag">{{ s.name }}</span>
          </div>
          <a @click="goCategory(cate.id)">查看更多 <el-icon><ArrowRight /></el-icon></a>
        </div>
        <div class="product-grid">
          <div v-for="(p, idx) in getCateProducts(cate)" :key="p.id" class="product-card card-3d reveal-item"
               :style="{ transitionDelay: idx*0.05+'s' }"
               @mousemove="onCardTilt" @mouseleave="resetCardTilt"
               @click="$router.push(`/product/${p.id}`)">
            <div class="pc-img">
              <img :src="p.mainImage || 'https://placehold.co/300x300/eee/999?text='+p.name" :alt="p.name" />
              <div class="pc-actions">
                <el-button size="small" type="primary" circle @click.stop="addCart(p)"><el-icon><ShoppingCart /></el-icon></el-button>
              </div>
            </div>
            <div class="pc-info">
              <p class="pc-name">{{ p.name }}</p>
              <p class="pc-price price-red">¥{{ p.price?.toFixed(2) }}</p>
            </div>
          </div>
        </div>
        <el-empty v-if="!getCateProducts(cate).length" description="暂无商品" :image-size="60" />
      </div>
    </template>

    <div class="load-more" v-if="hasMore && !filterLabel && productsLoaded">
      <el-button :loading="loadingMore" @click="loadPage">加载更多</el-button>
    </div>

    <Transition name="back-fade">
      <div v-show="showBackTop" class="back-top" @click="scrollToTop"><el-icon :size="24"><Top /></el-icon></div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { productApi, categoryApi, cartApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { ArrowRight, ShoppingCart, Tickets, Timer, Top, Sell, Ticket } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const allProducts = ref([])
const topCategories = ref([])
const hoverCate = ref(null)
const activeCate = ref(null)
const bannerIdx = ref(0)
const stopBanner = ref(false)
const pageNum = ref(1)
const loadingMore = ref(false)
const hasMore = ref(true)
const showBackTop = ref(false)
const searchKeyword = ref('')
const productsLoaded = ref(false)
const categoriesLoaded = ref(false)
const bannerRef = ref(null)
const bannerParallax = ref({ x: 0, y: 0 })

// Intersection Observer for scroll reveal
let observer = null

const brands = [
  { name: 'Apple', keyword: 'Apple', color: '#333' },
  { name: '华为', keyword: '华为', color: '#c41230' },
  { name: '小米', keyword: '小米', color: '#ff6900' },
  { name: '三星', keyword: 'Samsung', color: '#1428a0' },
  { name: '海尔', keyword: '海尔', color: '#1a73e8' },
  { name: '格力', keyword: '格力', color: '#0066cc' },
  { name: 'Nike', keyword: 'Nike', color: '#000' },
  { name: '戴森', keyword: '戴森', color: '#2c3e50' },
]

const banners = [
  { bg: 'linear-gradient(135deg, #e1251b, #ff6b6b)', title: '新品首发', sub: '最新数码产品 震撼上市', btn: '立即抢购', link: () => goCategory(1) },
  { bg: 'linear-gradient(135deg, #1a73e8, #4fc3f7)', title: '家电焕新', sub: '格力空调 低至 ¥3299', btn: '去看看', link: () => goCategory(3) },
  { bg: 'linear-gradient(135deg, #6c5ce7, #a29bfe)', title: '时尚穿搭', sub: '春夏新品 满199减50', btn: '立即选购', link: () => goCategory(2) },
]

const news = ['iPhone 15 限时优惠中', 'MacBook 新品热卖', '华为 Mate 60 现货', '格力空调 年中大促', '戴森吸尘器 直降500']

const countdown = ref({ hh: '00', mm: '00', ss: '00' })
let cdTimer = null, bannerTimer = null

// 粒子背景
const particleStyle = (i) => ({
  left: (i * 5.7 + 3) % 100 + '%',
  top: (i * 3.3 + 7) % 100 + '%',
  animationDelay: (i * 0.7) + 's',
  animationDuration: (8 + (i % 5) * 3) + 's',
  width: (4 + (i % 3) * 3) + 'px',
  height: (4 + (i % 3) * 3) + 'px',
  opacity: 0.15 + (i % 5) * 0.05
})

const startCountdown = () => {
  const end = new Date(); end.setHours(23, 59, 59)
  const tick = () => {
    const diff = Math.max(0, Math.floor((end - Date.now()) / 1000))
    countdown.value = {
      hh: String(Math.floor(diff / 3600)).padStart(2, '0'),
      mm: String(Math.floor(diff % 3600 / 60)).padStart(2, '0'),
      ss: String(diff % 60).padStart(2, '0'),
    }
  }
  tick(); cdTimer = setInterval(tick, 1000)
}

// 3D 卡片倾斜
const onCardTilt = (e) => {
  const card = e.currentTarget
  const rect = card.getBoundingClientRect()
  const x = (e.clientX - rect.left) / rect.width - 0.5
  const y = (e.clientY - rect.top) / rect.height - 0.5
  card.style.transform = `perspective(600px) rotateY(${x * 12}deg) rotateX(${-y * 12}deg) translateY(-4px)`
  card.style.boxShadow = `${x * 10}px ${y * 10}px 30px rgba(0,0,0,0.12)`
}

const resetCardTilt = (e) => {
  const card = e.currentTarget
  card.style.transform = 'perspective(600px) rotateY(0) rotateX(0) translateY(0)'
  card.style.boxShadow = ''
}

// Banner 视差
const onBannerMove = (e) => {
  const rect = e.currentTarget.getBoundingClientRect()
  bannerParallax.value = {
    x: (e.clientX - rect.left) / rect.width - 0.5,
    y: (e.clientY - rect.top) / rect.height - 0.5
  }
}

// 分类递归
const getCategoryTreeIds = (parentId) => {
  const ids = [parentId]
  const children = topCategories.value.flatMap(c => [c, ...(c.children||[])])
  const findChildren = (pid) => {
    children.filter(c => c.parentId === pid).forEach(c => { ids.push(c.id); findChildren(c.id) })
  }
  findChildren(parentId)
  return ids
}

const filterLabel = ref('')

const applyFilter = () => {
  const cid = route.query.category ? Number(route.query.category) : null
  const kw = route.query.keyword || ''
  searchKeyword.value = kw

  if (cid) {
    activeCate.value = cid
    const allCats = topCategories.value.flatMap(c => [c, ...(c.children||[])])
    const cate = allCats.find(c => c.id === cid)
    filterLabel.value = cate ? cate.name : '商品筛选'
  } else if (kw) {
    activeCate.value = null
    filterLabel.value = `搜索："${kw}"`
  } else {
    activeCate.value = null
    filterLabel.value = ''
  }
}

watch(() => [route.query.category, route.query.keyword], () => applyFilter())

const displayProducts = computed(() => {
  const cid = activeCate.value
  const kw = searchKeyword.value
  let list = allProducts.value

  if (cid) {
    const ids = getCategoryTreeIds(cid)
    list = list.filter(p => ids.includes(p.categoryId))
  }
  if (kw) {
    const lower = kw.toLowerCase()
    list = list.filter(p =>
      (p.name && p.name.toLowerCase().includes(lower)) ||
      (p.brand && p.brand.toLowerCase().includes(lower))
    )
  }
  return list
})

const hotProducts = computed(() => allProducts.value.filter(p => p.status===1).sort((a,b)=>(b.sales||0)-(a.sales||0)).slice(0, 6))
const newProducts = computed(() => [...allProducts.value].sort((a,b)=>new Date(b.createTime||0)-new Date(a.createTime||0)).slice(0, 8))

const getCateProducts = (cate) => {
  const ids = getCategoryTreeIds(cate.id)
  return allProducts.value.filter(p => ids.includes(p.categoryId)).slice(0, 6)
}

const goCategory = (id) => { hoverCate.value = null; if (id) router.push({ path: '/home', query: { category: id } }) }
const searchBrand = (kw) => router.push({ path: '/home', query: { keyword: kw } })
const clearFilter = () => router.push('/home')

const loadAllProducts = async () => {
  try { const res = await productApi.getList(); allProducts.value = res.data || [] }
  catch (e) { /* ignore */ }
  finally { productsLoaded.value = true }
}
const loadCategories = async () => {
  try {
    const res = await categoryApi.getList()
    const all = res.data || []
    topCategories.value = all.filter(c => c.parentId === 0).map(c => ({ ...c, children: all.filter(s => s.parentId === c.id) }))
    applyFilter()
  } catch (e) { /* ignore */ }
  finally { categoriesLoaded.value = true }
}
const loadPage = async () => {
  loadingMore.value = true
  try {
    const res = await productApi.getPage(pageNum.value + 1, 10)
    const data = res.data?.list || []
    if (data.length) { allProducts.value.push(...data); pageNum.value++ }
    else hasMore.value = false
  } catch { hasMore.value = false }
  finally { loadingMore.value = false }
}

const addCart = async (product) => {
  if (!userStore.token) { ElMessage.warning('请先登录'); router.push('/login'); return }
  try { await cartApi.add(userStore.userInfo.id, product.id, 1); ElMessage.success('已加入购物车') }
  catch (e) { ElMessage.error('加入失败') }
}

const handleScroll = () => { showBackTop.value = window.scrollY > 500 }
const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

// Intersection Observer：滚动触发动画
const setupObserver = () => {
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' })

  nextTick(() => {
    document.querySelectorAll('.reveal-item, .reveal-section').forEach(el => {
      observer.observe(el)
    })
  })
}

bannerTimer = setInterval(() => { if (!stopBanner.value) bannerIdx.value = (bannerIdx.value + 1) % banners.length }, 4000)

onMounted(() => {
  startCountdown()
  loadAllProducts()
  loadCategories()
  window.addEventListener('scroll', handleScroll)
  setupObserver()
})
onUnmounted(() => {
  clearInterval(cdTimer); clearInterval(bannerTimer)
  window.removeEventListener('scroll', handleScroll)
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.home { background: var(--jd-bg); padding-bottom: 40px; position: relative; overflow-x: hidden; }

/* ---- 粒子背景 ---- */
.particles-bg { position: fixed; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; z-index: 0; }
.particle {
  position: absolute; background: var(--jd-red); border-radius: 50%;
  animation: particleFloat linear infinite;
}
@keyframes particleFloat {
  0% { transform: translateY(0) translateX(0) scale(1); opacity: 0; }
  20% { opacity: 0.3; }
  80% { opacity: 0.3; }
  100% { transform: translateY(-100vh) translateX(30px) scale(0); opacity: 0; }
}

/* ---- hero ---- */
.hero-section { background: #fff; position: relative; z-index: 1; }
.hero-inner { max-width: 1200px; margin: 0 auto; display: flex; height: 480px; }
.hero-cates { width: 220px; background: #fafafa; padding: 12px 0; }
.hero-cate-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; cursor: pointer; font-size: 14px; position: relative; transition: all .2s; }
.hero-cate-item:hover, .hero-cate-item.active { color: var(--jd-red); background: #fff; font-weight: 600; }
.hero-subpop { position: absolute; left: 100%; top: 0; background: #fff; min-width: 200px; box-shadow: 0 4px 16px rgba(0,0,0,0.1); z-index: 100; padding: 8px 0; border-radius: 4px; }
.hero-subpop a { display: block; padding: 8px 20px; font-size: 13px; transition: all .15s; }
.hero-subpop a:hover { color: var(--jd-red); background: #fff5f5; padding-left: 24px; }
.sub-fade-enter-active { transition: opacity .2s,transform .2s; }
.sub-fade-leave-active { transition: opacity .1s; }
.sub-fade-enter-from { opacity: 0; transform: translateX(-8px); }

.hero-banner { flex: 1; overflow: hidden; position: relative; cursor: pointer; }
.banner-track { display: flex; transition: transform .6s cubic-bezier(.4,0,.2,1); height: 100%; }
.banner-slide { min-width: 100%; display: flex; align-items: center; padding: 60px; transition: transform .1s ease-out; }
.banner-text h2 { font-size: 42px; color: #fff; margin-bottom: 12px; animation: slideUp .6s both; }
.banner-text p { font-size: 20px; color: rgba(255,255,255,.9); margin-bottom: 24px; animation: slideUp .6s .15s both; }
.banner-btn { display: inline-block; padding: 10px 30px; background: #fff; color: var(--jd-red); border-radius: 20px; font-size: 15px; font-weight: 600; animation: slideUp .6s .3s both; transition: transform .2s; }
.banner-btn:hover { transform: scale(1.05); }
.banner-dots { position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); display: flex; gap: 8px; }
.banner-dots span { width: 10px; height: 10px; border-radius: 50%; background: rgba(255,255,255,.5); cursor: pointer; transition: all .3s; }
.banner-dots span.active { width: 24px; border-radius: 5px; background: #fff; }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

.hero-news { width: 200px; padding: 12px; }
.news-header { display: flex; justify-content: space-between; font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.news-header a { font-size: 12px; color: var(--jd-text-lighter); }
.news-item { display: block; font-size: 12px; color: var(--jd-text-light); padding: 4px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; animation: newsIn .5s both; }
.news-item:hover { color: var(--jd-red); }
.news-promo { display: flex; gap: 8px; margin-top: 16px; }
.promo-box { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 12px 0; background: #f8f8f8; border-radius: 4px; cursor: pointer; color: var(--jd-text-light); transition: all .2s; }
.promo-box:hover { color: var(--jd-red); transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,0.06); }
@keyframes newsIn { from { opacity: 0; transform: translateX(-12px); } to { opacity: 1; transform: translateX(0); } }

/* ---- sections ---- */
.section { max-width: 1200px; margin: 20px auto 0; background: #fff; border-radius: 8px; padding: 20px; position: relative; z-index: 1; }
.section-hd { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.section-hd h3 { font-size: 20px; display: flex; align-items: center; gap: 6px; white-space: nowrap; }
.section-hd a { font-size: 13px; color: var(--jd-text-lighter); margin-left: auto; cursor: pointer; white-space: nowrap; }
.section-hd a:hover { color: var(--jd-red); }
.result-count { font-size: 13px; color: var(--jd-text-lighter); }
.section-tags { display: flex; gap: 8px; }
.cate-tag { font-size: 13px; padding: 3px 12px; background: #f5f5f5; border-radius: 12px; cursor: pointer; transition: all .2s; }
.cate-tag:hover { background: var(--jd-red); color: #fff; }

.countdown { display: flex; align-items: center; gap: 4px; }
.cd-label { font-size: 12px; color: #999; margin-right: 4px; }
.cd-num { background: #333; color: #fff; padding: 2px 6px; border-radius: 3px; font-size: 14px; font-weight: 600; }
.cd-num span { margin: 0 2px; }

.flash-sale { background: linear-gradient(135deg, #fff5f5, #fff); animation: flashPulse 3s ease-in-out infinite; }
@keyframes flashPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(225, 37, 27, 0); }
  50% { box-shadow: 0 0 0 6px rgba(225, 37, 27, 0.06); }
}
.flash-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.flash-card { text-align: center; padding: 12px; background: #fafafa; border-radius: 8px; cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.flash-card:hover { background: #fff; transform: translateY(-4px); box-shadow: 0 8px 24px rgba(255, 77, 79, 0.15); }
.flash-card img { width: 100%; height: 140px; object-fit: contain; margin-bottom: 8px; }
.flash-name { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.flash-old { font-size: 12px; color: #999; text-decoration: line-through; }
.flash-progress { height: 4px; background: #ffe0e0; border-radius: 2px; margin-top: 8px; overflow: hidden; }
.flash-progress div { height: 100%; background: var(--jd-red); border-radius: 2px; transition: width 1s; }

.brand-grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 16px; }
.brand-card { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 20px; background: #fafafa; border-radius: 8px; cursor: pointer; transition: all 0.3s; }
.brand-card:hover { background: #fff; transform: translateY(-4px); box-shadow: 0 6px 20px rgba(0,0,0,0.08); }
.brand-avatar { width: 50px; height: 50px; border-radius: 50%; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700; transition: transform .3s; }
.brand-card:hover .brand-avatar { transform: rotate(-10deg) scale(1.1); }

.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.product-card { background: #fff; border-radius: 8px; overflow: hidden; border: 1px solid #f0f0f0; position: relative; cursor: pointer; transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1); }
.pc-img { width: 100%; height: 200px; display: flex; align-items: center; justify-content: center; background: #fafafa; position: relative; overflow: hidden; }
.pc-img img { width: 100%; height: 100%; object-fit: contain; transition: transform .4s; }
.product-card:hover .pc-img img { transform: scale(1.08); }
.pc-actions { position: absolute; bottom: 8px; right: 8px; opacity: 0; transform: translateY(8px); transition: all .3s; }
.product-card:hover .pc-actions { opacity: 1; transform: translateY(0); }
.tag-new { position: absolute; top: 8px; left: 8px; background: var(--jd-red); color: #fff; padding: 2px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; }
.pc-info { padding: 12px; }
.pc-name { font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 6px; }
.pc-price { font-size: 18px; display: flex; align-items: baseline; gap: 8px; }
.pc-original { font-size: 12px; color: #999; text-decoration: line-through; font-weight: 400; }
.pc-meta { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #999; margin-top: 6px; }
.pc-sold { display: flex; align-items: center; gap: 2px; }

/* ---- 滚动触发动画 ---- */
.reveal-item {
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.reveal-item.revealed {
  opacity: 1;
  transform: translateY(0);
}
.reveal-section .reveal-item:nth-child(1) { transition-delay: 0s; }
.reveal-section .reveal-item:nth-child(2) { transition-delay: 0.06s; }
.reveal-section .reveal-item:nth-child(3) { transition-delay: 0.12s; }
.reveal-section .reveal-item:nth-child(4) { transition-delay: 0.18s; }
.reveal-section .reveal-item:nth-child(5) { transition-delay: 0.24s; }
.reveal-section .reveal-item:nth-child(6) { transition-delay: 0.30s; }
.reveal-section .reveal-item:nth-child(7) { transition-delay: 0.36s; }
.reveal-section .reveal-item:nth-child(8) { transition-delay: 0.42s; }

/* ---- 骨架屏 ---- */
.skeleton-cate, .skeleton-bar, .skeleton-flash, .skeleton-brand, .skeleton-product {
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeletonShimmer 1.5s ease-in-out infinite;
  border-radius: 4px;
}
@keyframes skeletonShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
.skeleton-cate { height: 36px; margin: 4px 12px; }
.skeleton-flash { height: 220px; border-radius: 8px; }
.skeleton-brand { height: 80px; border-radius: 8px; }
.skeleton-product { height: 260px; border-radius: 8px; }

.load-more { text-align: center; padding: 30px; position: relative; z-index: 1; }

.back-top { position: fixed; bottom: 60px; right: 30px; width: 44px; height: 44px; background: var(--jd-red); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 4px 12px rgba(225,37,27,.4); z-index: 999; transition: transform .2s; }
.back-top:hover { transform: scale(1.1); }
.back-fade-enter-active, .back-fade-leave-active { transition: opacity .3s; }
.back-fade-enter-from, .back-fade-leave-to { opacity: 0; }
</style>