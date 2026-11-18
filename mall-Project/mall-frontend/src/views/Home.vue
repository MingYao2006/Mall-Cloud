<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi, searchApi, cartApi } from '@/api/compat'
import { useUserStore } from '@/stores/user'
import { formatPrice } from '@/utils/format'
import ProductCard from '@/components/ProductCard.vue'
import SectionHeader from '@/components/SectionHeader.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const goodsList = ref([])
const categories = ref([])
const total = ref(0)
const pageNum = ref(1)
/** 今日货架每页 16 条 */
const pageSize = 16
/** 下滑加载中（与筛选/首屏的 loading 区分，避免出骨架） */
const loadingMore = ref(false)
const loading = ref(false)
const showSkeleton = ref(false)
const hasLoaded = ref(false)
const keyword = ref('')
const categoryId = ref(null)
const categoryName = ref('')
let skeletonTimer = null

/** 今日橱窗：轮播展示的商品 */
const showcaseProducts = ref([])
const showcaseIndex = ref(0)
let showcaseTimer = null

const services = [
  { icon: 'Goods', title: '正品保障', desc: '官方授权，假一赔十' },
  { icon: 'RefreshLeft', title: '7 天无理由', desc: '不满意，随时退换' },
  { icon: 'Van', title: '满 99 包邮', desc: '当日下单，次日发出' },
  { icon: 'Service', title: '有人回话', desc: '7×24 小时在线客服' }
]

const searching = computed(() => !!keyword.value.trim())

const filtering = computed(() => categoryId.value != null)

/** 搜索结果的价格分类（与「全部分类」页一致） */
const PRICE_RANGES = {
  all: { label: '不限', minPrice: null, maxPrice: null },
  '0-100': { label: '100元以下', minPrice: null, maxPrice: 100 },
  '100-500': { label: '100-500元', minPrice: 100, maxPrice: 500 },
  '500-1000': { label: '500-1000元', minPrice: 500, maxPrice: 1000 },
  '1000-5000': { label: '1000-5000元', minPrice: 1000, maxPrice: 5000 },
  '5000-10000': { label: '5000-10000元', minPrice: 5000, maxPrice: 10000 },
  '10000-': { label: '10000元以上', minPrice: 10000, maxPrice: null }
}
const priceRange = ref('all')

/** 切换价格分类：重置到第 1 页并重新加载 */
function selectPriceRange(key) {
  if (priceRange.value === key) return
  priceRange.value = key
  pageNum.value = 1
  loadGoods(false)
}

const sectionTitle = computed(() => {
  if (searching.value) return `“${keyword.value}” 的搜索结果`
  if (filtering.value) return categoryName.value || '分类商品'
  return '今日货架'
})

const subtitle = computed(() => {
  if (searching.value) return '看看有没有你要找的那一件'
  if (filtering.value) return '该分类下的全部商品'
  return '从日用百货里，挑几件顺手的'
})

/** 列表标识：仅筛选条件变化时触发列表整体淡入淡出（翻页追加不触发） */
const listKey = computed(
  () => `${categoryId.value ?? 'all'}-${keyword.value || 'all'}`
)

/** 今日橱窗轮播：定时切换当前商品 */
function startShowcaseTimer() {
  stopShowcaseTimer()
  showcaseTimer = window.setInterval(() => {
    if (showcaseProducts.value.length > 1) {
      showcaseIndex.value = (showcaseIndex.value + 1) % showcaseProducts.value.length
    }
  }, 4000)
}

function stopShowcaseTimer() {
  if (showcaseTimer) {
    window.clearInterval(showcaseTimer)
    showcaseTimer = null
  }
}

/** 鼠标悬停橱窗时暂停轮播，方便看清商品 */
function pauseShowcase() {
  stopShowcaseTimer()
}

function resumeShowcase() {
  startShowcaseTimer()
}

function onShowcaseChange(index) {
  showcaseIndex.value = index
}

/**
 * 拉取货架数据
 * @param {boolean} append true=追加到现有列表末尾（下滑翻页用），false=重新加载
 */
async function loadGoods(append = false) {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
    // 首次加载直接出骨架；后续筛选只在超过 250ms 时才出骨架，避免快接口闪一下
    clearTimeout(skeletonTimer)
    if (hasLoaded.value) {
      skeletonTimer = window.setTimeout(() => {
        showSkeleton.value = true
      }, 250)
    } else {
      showSkeleton.value = true
    }
  }

  try {
    const params = { pageNum: pageNum.value, pageSize }
    let data

    if (searching.value) {
      // 搜索结果按价格分类：走多条件分页接口，keyword + 价格区间
      const price = PRICE_RANGES[priceRange.value] || PRICE_RANGES.all
      data = await productApi.filteredPage({
        pageNum: pageNum.value,
        pageSize,
        keyword: keyword.value.trim(),
        minPrice: price.minPrice,
        maxPrice: price.maxPrice
      })
    } else {
      if (categoryId.value != null) params.category1Id = categoryId.value
      data = await productApi.spuPage(params)
    }

    const records = data?.records || []
    // 追加时按 id 去重，避免重复项
    if (append) {
      const seen = new Set(goodsList.value.map((g) => g.id))
      goodsList.value = goodsList.value.concat(records.filter((g) => !seen.has(g.id)))
    } else {
      goodsList.value = records
    }
    total.value = data?.total || 0
  } catch (e) {
    if (!append) {
      goodsList.value = []
      total.value = 0
    }
  } finally {
    clearTimeout(skeletonTimer)
    loading.value = false
    loadingMore.value = false
    showSkeleton.value = false
    hasLoaded.value = true
  }
}

/** 下滑加载下一页：把下页商品追加到本页下方，不替换、不清空、不跳回顶部 */
function loadMore() {
  if (loading.value || loadingMore.value) return
  if (pageNum.value * pageSize >= total.value) return
  pageNum.value += 1
  loadGoods(true)
}

/** 加载今日橱窗的商品：取前若干件做轮播 */
async function loadShowcase() {
  try {
    const data = await productApi.spuPage({ pageNum: 1, pageSize: 8 })
    showcaseProducts.value = data?.records || []
    showcaseIndex.value = 0
    startShowcaseTimer()
  } catch (e) {
    showcaseProducts.value = []
  }
}

async function loadCategories() {
  try {
    const data = await productApi.categoryTree()
    categories.value = Array.isArray(data) ? data.slice(0, 7) : []
  } catch (e) {
    categories.value = []
  }
}

function goDetail(id) {
  router.push(`/product/${id}`)
}

function clearFilter() {
  router.push({ path: '/home' })
}

async function addToCart(product) {
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    await cartApi.add({ skuId: product.skuId || product.id, num: 1 })
    ElMessage.success('已加入购物车')
  } catch (e) {
    /* 错误提示由响应拦截器统一处理 */
  }
}

function scrollToRecommend() {
  const el = document.getElementById('recommend')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToCategory() {
  const el = document.getElementById('category')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/**
 * 滚动到底自动加载下一页
 *
 * 下页商品追加到本页下方（不清空、不跳回顶部），
 * 用户滚动位置保持不变，体验连续。
 */
function onScroll() {
  if (loading.value || loadingMore.value) return
  const doc = document.documentElement
  const scrollTop = window.scrollY || doc.scrollTop
  const viewport = window.innerHeight
  const full = doc.scrollHeight
  if (scrollTop + viewport >= full - 200) {
    loadMore()
  }
}

/** 从路由参数同步筛选条件（关键词 / 分类） */
function syncFromQuery() {
  keyword.value = route.query.key || route.query.keyword || ''
  priceRange.value = 'all'
  const cid = route.query.categoryId || route.query.category
  categoryId.value = cid ? Number(cid) : null
  categoryName.value = route.query.categoryName || ''
  pageNum.value = 1
}

watch(
  () => route.query,
  () => {
    syncFromQuery()
    loadGoods()
  }
)

onMounted(() => {
  syncFromQuery()
  loadCategories()
  loadGoods()
  loadShowcase()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  stopShowcaseTimer()
  clearTimeout(skeletonTimer)
})
</script>

<template>
  <div class="home">
    <!-- 橱窗：首屏只做一件事，让“集物”的货架感先立住（搜索时只显示商品） -->
    <section v-if="!searching" class="shopfront">
      <div class="container shopfront-inner">
        <div class="shopfront-copy">
          <p v-reveal class="shopfront-kicker">
            <span class="kicker-rule" aria-hidden="true"></span>
            <span>集物商店 / 线上百货</span>
          </p>

          <h1 v-reveal="70" class="hero-title">
            <span>把日常，</span>
            <span>收进好物里。</span>
          </h1>

          <p v-reveal="140" class="hero-lede">
            数码、家居、食品与礼物。我们只挑那些会用很久、也愿意天天看见的东西。
          </p>

          <div v-reveal="210" class="hero-actions">
            <button class="btn-brand hero-btn" type="button" @click="scrollToRecommend">
              逛今日货架
              <el-icon :size="14"><ArrowRight /></el-icon>
            </button>
            <button
              v-if="categories.length"
              class="hero-link"
              type="button"
              @click="scrollToCategory"
            >
              按分类慢慢看
              <el-icon :size="12"><ArrowRight /></el-icon>
            </button>
          </div>

          <div v-reveal="260" class="hero-footnote">
            <span>正品保障</span>
            <span>满 99 元包邮</span>
            <span>7 天无理由退换</span>
          </div>
        </div>

        <div
          v-reveal="120"
          class="shopwindow"
          @mouseenter="pauseShowcase"
          @mouseleave="resumeShowcase"
        >
          <div class="window-topline">
            <span>今日橱窗</span>
            <span>从货架上取一件</span>
          </div>

          <!-- 橱窗轮播：多件商品自动旋转切换 -->
          <el-carousel
            v-if="showcaseProducts.length"
            class="window-carousel"
            :interval="4000"
            :duration="600"
            arrow="hover"
            indicator-position="none"
            height="360px"
            @change="onShowcaseChange"
          >
            <el-carousel-item v-for="item in showcaseProducts" :key="item.id">
              <div
                class="window-body"
                role="link"
                :tabindex="0"
                @click="goDetail(item.id)"
                @keydown.enter="goDetail(item.id)"
              >
                <div class="window-image">
                  <img v-if="item.mainImage" :src="item.mainImage" :alt="item.name" />
                  <span v-else class="window-glyph" aria-hidden="true">集</span>
                </div>

                <div class="window-caption">
                  <div class="wc-copy">
                    <p class="wc-brand">{{ item.brandName || '集物严选' }}</p>
                    <p class="wc-name">{{ item.name }}</p>
                  </div>
                  <p class="wc-price">¥{{ formatPrice(item.minPrice) }}</p>
                </div>

                <span class="window-seal" aria-hidden="true">集</span>
              </div>
            </el-carousel-item>
          </el-carousel>

          <div v-else class="window-body is-empty">
            <div class="window-image">
              <span class="window-glyph" aria-hidden="true">集</span>
            </div>
            <div class="window-caption">
              <div class="wc-copy">
                <p class="wc-brand">集物严选</p>
                <p class="wc-name">今日好物正在上架</p>
              </div>
              <span class="wc-status">待上架</span>
            </div>
            <span class="window-seal" aria-hidden="true">集</span>
          </div>

          <!-- 轮播指示点 -->
          <div v-if="showcaseProducts.length > 1" class="window-dots">
            <button
              v-for="(item, i) in showcaseProducts"
              :key="'dot-' + item.id"
              type="button"
              class="window-dot"
              :class="{ active: showcaseIndex === i }"
              :aria-label="`第 ${i + 1} 件`"
              @click="showcaseIndex = i"
            />
          </div>

          <div class="window-shelf" aria-hidden="true"></div>
        </div>
      </div>
    </section>

    <!-- 店铺承诺：深色营业条，给货架一点重量（搜索时只显示商品） -->
    <section v-if="!searching" class="service-ribbon">
      <div class="container service-ribbon-inner">
        <div class="service-title">
          <span>店铺承诺</span>
          <small>每一单都按这个标准</small>
        </div>
        <div class="service-list">
          <div v-for="s in services" :key="s.title" class="service-item">
            <span class="service-icon">
              <el-icon :size="17"><component :is="s.icon" /></el-icon>
            </span>
            <div>
              <p class="service-name">{{ s.title }}</p>
              <p class="service-desc">{{ s.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="container">
      <!-- 今日货架 -->
      <section id="recommend" class="block goods-block">
        <SectionHeader v-reveal :title="sectionTitle" :subtitle="subtitle" />

        <!-- 搜索结果：按价格分类（与「全部分类」页一致），仅显示商品 -->
        <div v-if="searching" class="search-price-filter">
          <span class="spf-label">价格分类</span>
          <div class="spf-options">
            <button
              v-for="(opt, key) in PRICE_RANGES"
              :key="key"
              type="button"
              class="spf-item"
              :class="{ active: priceRange === key }"
              @click="selectPriceRange(key)"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>

        <div v-if="searching || filtering" class="filter-note">
          <span>正在查看{{ searching ? '搜索结果' : '分类商品' }}，共 {{ total }} 件</span>
          <button type="button" @click="clearFilter">清除筛选</button>
        </div>

        <transition name="skeleton-fade" mode="out-in">
          <div v-if="showSkeleton" key="skeleton" class="goods-grid">
            <div v-for="n in pageSize" :key="n" class="skeleton-card">
              <div class="sk-cover"></div>
              <div class="sk-body">
                <div class="sk-line" style="width: 72%"></div>
                <div class="sk-line sk-line-sm" style="width: 40%"></div>
              </div>
            </div>
          </div>

          <div
            v-else-if="goodsList.length"
            :key="listKey"
            class="goods-grid"
            :class="{ 'is-switching': loading }"
          >
            <ProductCard
              v-for="g in goodsList"
              :key="g.id"
              :product="g"
              @select="goDetail(g.id)"
              @add="addToCart"
            />
          </div>

          <el-empty v-else-if="!loading" key="empty" description="这一栏暂时空着，换个分类看看">
            <el-button v-if="searching || filtering" type="primary" @click="clearFilter">
              查看全部商品
            </el-button>
          </el-empty>
        </transition>

        <!-- 下滑加载更多：下页商品追加到本页下方 -->
        <div v-if="!loading && goodsList.length" class="load-more">
          <button
            v-if="goodsList.length < total"
            type="button"
            class="load-more-btn"
            :disabled="loadingMore"
            @click="loadMore"
          >
            {{ loadingMore ? '加载中…' : '下滑或点击加载更多' }}
          </button>
          <p v-else class="load-more-end">— 已经到底啦，共 {{ total }} 件商品 —</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.home {
  padding-bottom: 20px;
}

/* ---------------- 首屏橱窗 ---------------- */
.shopfront {
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid var(--line);
}

.shopfront::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 42%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(228, 166, 47, 0.06));
  pointer-events: none;
}

.shopfront-inner {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(340px, 0.92fr);
  align-items: center;
  gap: 64px;
  min-height: 548px;
  padding-top: 54px;
  padding-bottom: 54px;
}

.shopfront-copy {
  max-width: 570px;
}

.shopfront-kicker {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 23px;
  color: var(--pine-700);
  font-size: 12px;
  letter-spacing: 0.1em;
}

.kicker-rule {
  display: block;
  width: 38px;
  height: 2px;
  background: var(--gold);
}

.hero-title {
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: clamp(48px, 5.4vw, 78px);
  font-weight: 600;
  line-height: 1.16;
  letter-spacing: 0.015em;
}

.hero-title span {
  display: block;
}

.hero-title span:last-child {
  position: relative;
  padding-left: 0.9em;
}

.hero-title span:last-child::before {
  content: '';
  position: absolute;
  top: 0.28em;
  left: 0;
  width: 0.36em;
  height: 0.36em;
  background: var(--gold);
}

.hero-lede {
  max-width: 440px;
  margin-top: 24px;
  color: var(--text-muted);
  font-size: 15px;
  line-height: 1.95;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 22px;
  margin-top: 30px;
}

.hero-btn {
  min-width: 154px;
  padding-top: 13px;
  padding-bottom: 13px;
}

.hero-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 0;
  border: none;
  border-bottom: 1px solid var(--line-strong);
  background: transparent;
  color: var(--pine-700);
  font-size: 12px;
  cursor: pointer;
  transition: color 0.18s ease, border-color 0.18s ease;
}

.hero-link:hover {
  border-color: var(--vermilion);
  color: var(--vermilion);
}

.hero-footnote {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  margin-top: 36px;
  padding-top: 13px;
  border-top: 1px solid var(--line);
  color: var(--sage);
  font-size: 11px;
}

.hero-footnote span {
  padding: 0 14px;
  border-left: 1px solid var(--line);
}

.hero-footnote span:first-child {
  padding-left: 0;
  border-left: none;
}

/* 橱窗右侧 */
.shopwindow {
  position: relative;
  width: 100%;
  max-width: 432px;
  justify-self: end;
  animation: window-settle 0.72s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

@keyframes window-settle {
  from {
    opacity: 0;
    transform: translate3d(18px, 10px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

.window-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 2px 10px;
  border-bottom: 1px solid var(--line-strong);
  color: var(--sage);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.window-topline span:first-child {
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
}

/* 橱窗轮播 */
.window-carousel {
  margin-top: 14px;
}

.window-carousel :deep(.el-carousel__container) {
  border-radius: 2px;
}

.window-carousel :deep(.el-carousel__arrow) {
  background-color: rgba(22, 58, 46, 0.45);
  width: 30px;
  height: 30px;
}

/* 轮播里的卡片要撑满 carousel-item 的高度 */
.window-carousel :deep(.el-carousel__item) {
  height: 100%;
}

.window-carousel .window-body {
  height: 100%;
  margin-top: 0;
  display: flex;
  flex-direction: column;
}

.window-carousel .window-image {
  flex: 1;
  min-height: 0;
}

/* 轮播指示点 */
.window-dots {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin-top: 12px;
}

.window-dot {
  width: 7px;
  height: 7px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(22, 58, 46, 0.2);
  cursor: pointer;
  transition: all 0.25s ease;
}

.window-dot.active {
  width: 20px;
  border-radius: 4px;
  background: var(--gold);
}

.window-body {
  position: relative;
  margin-top: 14px;
  padding: 14px 14px 0;
  border: 1px solid var(--line-strong);
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 9px 9px 0 rgba(22, 58, 46, 0.1);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.window-body:hover {
  transform: translate(-3px, -3px);
  box-shadow: 12px 12px 0 rgba(22, 58, 46, 0.13);
}

.window-body.is-empty {
  cursor: default;
}

.window-image {
  position: relative;
  aspect-ratio: 1 / 1.05;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--porcelain-deep);
  border: 1px solid var(--line);
}

.window-image::after {
  content: '';
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(251, 252, 248, 0.55);
  pointer-events: none;
}

.window-image img {
  width: 100%;
  height: 100%;
  /* contain：完整展示商品，不裁切；留白用底色补齐 */
  object-fit: contain;
  padding: 10px;
  box-sizing: border-box;
  transition: transform 0.55s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.window-body:hover .window-image img {
  transform: scale(1.03);
}

.window-glyph {
  color: rgba(22, 58, 46, 0.18);
  font-family: var(--font-display);
  font-size: 132px;
  font-weight: 700;
  line-height: 1;
}

.window-caption {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 3px 17px;
}

.wc-copy {
  min-width: 0;
}

.wc-brand {
  overflow: hidden;
  color: var(--sage);
  font-size: 10px;
  letter-spacing: 0.06em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wc-name {
  margin-top: 4px;
  overflow: hidden;
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wc-price {
  flex-shrink: 0;
  color: var(--vermilion);
  font-family: var(--font-num);
  font-size: 25px;
  font-weight: 600;
  line-height: 1.1;
}

.wc-status {
  flex-shrink: 0;
  color: var(--sage);
  font-family: var(--font-num);
  font-size: 15px;
  letter-spacing: 0.08em;
}

.window-seal {
  position: absolute;
  top: -18px;
  right: -18px;
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  border: 1px solid var(--vermilion);
  background: var(--paper);
  box-shadow: 4px 4px 0 rgba(200, 74, 54, 0.16);
  color: var(--vermilion);
  font-family: var(--font-display);
  font-size: 25px;
  font-weight: 700;
  line-height: 1;
  transform: rotate(5deg);
  animation: seal-swing 0.8s 0.28s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes seal-swing {
  0% {
    opacity: 0;
    transform: rotate(-8deg) scale(0.8);
  }
  100% {
    opacity: 1;
    transform: rotate(5deg) scale(1);
  }
}

.window-shelf {
  position: relative;
  height: 10px;
  margin: 18px 12px 0 0;
  background: var(--pine-800);
  box-shadow: 0 5px 0 rgba(22, 58, 46, 0.12);
}

.window-shelf::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 0;
  right: 0;
  height: 1px;
  background: var(--gold);
}

/* ---------------- 服务营业条 ---------------- */
.service-ribbon {
  border-top: 3px solid var(--gold);
  background: var(--pine-800);
  color: var(--paper);
}

.service-ribbon-inner {
  display: flex;
  align-items: stretch;
  gap: 28px;
  padding-top: 17px;
  padding-bottom: 17px;
}

.service-title {
  display: flex;
  width: 170px;
  flex-shrink: 0;
  flex-direction: column;
  justify-content: center;
  padding-right: 26px;
  border-right: 1px solid rgba(251, 252, 248, 0.18);
}

.service-title span {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.service-title small {
  margin-top: 3px;
  color: rgba(251, 252, 248, 0.52);
  font-size: 10px;
}

.service-list {
  display: grid;
  flex: 1;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
}

.service-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 20px;
  border-left: 1px solid rgba(251, 252, 248, 0.14);
}

.service-item:first-child {
  border-left: none;
}

.service-icon {
  display: flex;
  flex-shrink: 0;
  color: var(--gold);
}

.service-name {
  color: var(--paper);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
}

.service-desc {
  margin-top: 2px;
  color: rgba(251, 252, 248, 0.52);
  font-size: 10px;
  line-height: 1.4;
}

/* ---------------- 区块 ---------------- */
.block {
  margin-top: 58px;
}

.category-block {
  margin-top: 52px;
}

/* ---------------- 分类导视牌 ---------------- */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: 126px;
  gap: 13px;
}

.bento-item {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 16px 17px;
  border: 1px solid var(--line);
  border-radius: 2px;
  background: var(--paper);
  color: var(--pine-800);
  text-align: left;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.bento-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gold);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.25s ease;
}

.bento-item:hover {
  border-color: var(--line-strong);
  transform: translate(-2px, -2px);
  box-shadow: 5px 5px 0 rgba(22, 58, 46, 0.08);
}

.bento-item:hover::before {
  transform: scaleX(1);
}

.bento-item:first-child {
  grid-column: span 2;
  grid-row: span 2;
  padding: 24px 25px;
  background: var(--pine-800);
  border-color: var(--pine-800);
  color: var(--paper);
}

.bento-item:nth-child(6),
.bento-item:nth-child(7) {
  grid-column: span 2;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
}

.bento-code {
  position: relative;
  z-index: 1;
  color: var(--sage);
  font-family: var(--font-num);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.bento-item:first-child .bento-code {
  color: var(--gold);
}

.bento-info {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.bento-name {
  overflow: hidden;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bento-item:first-child .bento-name {
  font-size: 25px;
}

.bento-desc {
  margin-top: 6px;
  color: var(--sage-light);
  font-size: 11px;
}

.bento-ghost {
  position: absolute;
  right: 6px;
  bottom: -20px;
  color: rgba(22, 58, 46, 0.055);
  font-family: var(--font-display);
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
  pointer-events: none;
}

.bento-item:first-child .bento-ghost {
  right: 16px;
  bottom: -28px;
  color: rgba(251, 252, 248, 0.07);
  font-size: 154px;
}

.bento-item:nth-child(6) .bento-ghost,
.bento-item:nth-child(7) .bento-ghost {
  right: auto;
  left: 14px;
  bottom: -26px;
  font-size: 84px;
}

.bento-arrow {
  position: absolute;
  right: 15px;
  bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 25px;
  height: 25px;
  border: 1px solid var(--line);
  border-radius: 2px;
  color: var(--sage);
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.2s ease, transform 0.2s ease, color 0.2s ease;
}

.bento-item:hover .bento-arrow {
  opacity: 1;
  transform: translateX(0);
  color: var(--pine-800);
}

.bento-item:first-child .bento-arrow {
  border-color: rgba(251, 252, 248, 0.32);
  color: var(--gold);
}

.bento-item:nth-child(6) .bento-arrow,
.bento-item:nth-child(7) .bento-arrow {
  position: static;
  margin-left: auto;
  flex-shrink: 0;
}

/* ---------------- 商品货架 ---------------- */
.goods-block {
  margin-bottom: 20px;
}

/* 搜索结果价格分类栏 */
.search-price-filter {
  display: flex;
  align-items: center;
  gap: 18px;
  margin: 0 0 18px;
  padding: 12px 16px;
  border: 1px solid var(--line);
  background: var(--paper);
}

.spf-label {
  flex-shrink: 0;
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
}

.spf-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.spf-item {
  padding: 5px 13px;
  border: 1px solid var(--line-strong);
  border-radius: 2px;
  background: transparent;
  color: var(--text-body);
  font-family: var(--font-ui);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.spf-item:hover {
  border-color: var(--pine-700);
  color: var(--pine-700);
}

.spf-item.active {
  border-color: var(--pine-800);
  background: var(--pine-800);
  color: #fff;
}

.filter-note {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: -5px 0 18px;
  padding: 10px 13px;
  border: 1px solid var(--line);
  background: var(--paper-warm);
  color: var(--sage);
  font-size: 12px;
}

.filter-note button {
  padding: 0;
  border: none;
  border-bottom: 1px solid var(--line-strong);
  background: transparent;
  color: var(--pine-700);
  font-size: 12px;
  cursor: pointer;
}

.filter-note button:hover {
  border-color: var(--vermilion);
  color: var(--vermilion);
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.goods-grid.is-switching {
  opacity: 0.5;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

/* 骨架屏 */
.skeleton-card {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 2px;
  background: var(--paper);
}

.sk-cover {
  aspect-ratio: 1 / 1;
  border-bottom: 1px solid var(--line);
  background: linear-gradient(90deg, #e8ece4 25%, #f4f6f0 37%, #e8ece4 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.sk-body {
  padding: 15px;
}

.sk-line {
  height: 12px;
  border-radius: 1px;
  background: linear-gradient(90deg, #e8ece4 25%, #f4f6f0 37%, #e8ece4 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.sk-line + .sk-line {
  margin-top: 10px;
}

.sk-line-sm {
  height: 9px;
}

@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

.load-more {
  display: flex;
  justify-content: center;
  margin-top: 34px;
}

.load-more-btn {
  min-width: 220px;
  padding: 12px 32px;
  border: 1px solid var(--line-strong);
  border-radius: 2px;
  background: var(--paper);
  color: var(--pine-700);
  font-family: var(--font-ui);
  font-size: 13px;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.load-more-btn:hover:not(:disabled) {
  border-color: var(--pine-700);
  background: var(--brand-soft);
  color: var(--pine-800);
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.load-more-end {
  margin: 0;
  color: var(--text-hint);
  font-size: 12px;
  letter-spacing: 0.04em;
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1120px) {
  .shopfront-inner {
    gap: 40px;
    min-height: 500px;
  }

  .goods-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .bento-grid {
    grid-template-columns: repeat(2, 1fr);
    grid-auto-rows: 120px;
  }

  .bento-item:first-child {
    grid-column: span 2;
    grid-row: span 1;
    padding: 20px 22px;
  }

  .bento-item:nth-child(6),
  .bento-item:nth-child(7) {
    grid-column: span 1;
    flex-direction: column;
    align-items: flex-start;
    justify-content: space-between;
  }

  .bento-item:first-child .bento-ghost,
  .bento-item:nth-child(6) .bento-ghost,
  .bento-item:nth-child(7) .bento-ghost {
    right: 8px;
    left: auto;
    bottom: -24px;
    font-size: 96px;
  }

  .bento-item:first-child .bento-name {
    font-size: 21px;
  }

  .bento-arrow,
  .bento-item:nth-child(6) .bento-arrow,
  .bento-item:nth-child(7) .bento-arrow {
    position: absolute;
    right: 14px;
    bottom: 14px;
    margin-left: 0;
  }
}

@media (max-width: 900px) {
  .shopfront-inner {
    grid-template-columns: 1fr;
    gap: 42px;
    padding-top: 42px;
    padding-bottom: 46px;
  }

  .shopfront-copy {
    max-width: 640px;
  }

  .shopwindow {
    max-width: 520px;
    justify-self: start;
  }

  .window-image {
    aspect-ratio: 1 / 0.82;
  }

  .service-ribbon-inner {
    flex-direction: column;
    gap: 14px;
  }

  .service-title {
    width: auto;
    padding-right: 0;
    padding-bottom: 10px;
    border-right: none;
    border-bottom: 1px solid rgba(251, 252, 248, 0.18);
  }

  .service-list {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px 0;
  }

  .service-item:nth-child(3) {
    border-left: none;
  }

  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .hero-title {
    font-size: 42px;
  }

  .hero-title span:last-child {
    padding-left: 0.75em;
  }

  .hero-lede {
    margin-top: 18px;
    font-size: 14px;
    line-height: 1.85;
  }

  .hero-actions {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
    margin-top: 24px;
  }

  .hero-btn {
    width: 100%;
  }

  .hero-footnote {
    gap: 8px 0;
    margin-top: 26px;
  }

  .hero-footnote span {
    padding: 0 9px;
    font-size: 10px;
  }

  .window-body {
    padding: 9px 9px 0;
    box-shadow: 6px 6px 0 rgba(22, 58, 46, 0.1);
  }

  .window-seal {
    top: -14px;
    right: -10px;
    width: 46px;
    height: 46px;
    font-size: 21px;
  }

  .wc-name {
    font-size: 15px;
  }

  .wc-price {
    font-size: 21px;
  }

  .bento-grid {
    gap: 10px;
  }

  .bento-item {
    padding: 14px;
  }

  .bento-item:first-child {
    padding: 17px 18px;
  }

  .bento-name {
    font-size: 15px;
  }

  .bento-item:first-child .bento-name {
    font-size: 19px;
  }

  .bento-desc {
    font-size: 10px;
  }

  .goods-grid {
    gap: 11px;
  }

  .block {
    margin-top: 44px;
  }
}

@media (max-width: 380px) {
  .hero-title {
    font-size: 38px;
  }

  .hero-title span:last-child {
    padding-left: 0.6em;
  }

  .hero-title span:last-child::before {
    width: 0.3em;
    height: 0.3em;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shopwindow,
  .window-seal,
  .sk-cover,
  .sk-line {
    animation: none;
  }
}
</style>
