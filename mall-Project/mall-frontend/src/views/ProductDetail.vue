<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi, cartApi, commentApi } from '@/api/compat'
import { formatPrice, formatDateTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { flyToCart } from '@/utils/animation'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const spu = ref(null)
const related = ref([])
const loading = ref(false)
const selectedSkuId = ref(null)
const num = ref(1)
const galleryRef = ref(null)

/* ---------------- 图片放大镜 ---------------- */
const ZOOM_LENS_SIZE = 170
const ZOOM_SCALE = 2.6
const zoomOn = ref(false)
const lensPos = ref({ left: 0, top: 0, bgX: 50, bgY: 50 })

const selectedSku = computed(() => {
  if (!spu.value?.skus?.length) return null
  return spu.value.skus.find((s) => s.id === selectedSkuId.value) || spu.value.skus[0]
})

const displayPrice = computed(() => selectedSku.value?.price ?? spu.value?.minPrice)

const stockText = computed(() => {
  const stock = selectedSku.value?.stock
  if (stock == null) return '现货充足'
  if (stock <= 0) return '暂时缺货'
  if (stock <= 10) return `仅剩 ${stock} 件`
  return '现货充足'
})

const isOffShelf = computed(() => spu.value?.status === 0)

const services = [
  { icon: 'Goods', text: '正品保障' },
  { icon: 'RefreshLeft', text: '7天无理由' },
  { icon: 'Van', text: '极速发货' }
]

async function load() {
  loading.value = true
  try {
    spu.value = await productApi.spuDetail(route.params.id)
    if (spu.value?.skus?.length) {
      const firstEnabled = spu.value.skus.find((s) => s.enable) || spu.value.skus[0]
      selectedSkuId.value = firstEnabled.id
    }
    num.value = 1
    loadRelated()
    loadComments()
  } catch (e) {
    spu.value = null
  } finally {
    loading.value = false
  }
}

/* ---------------- 图片放大镜 ---------------- */
function handleZoomEnter() {
  if (spu.value?.mainImage) zoomOn.value = true
}

function handleZoomLeave() {
  zoomOn.value = false
}

function handleZoomMove(event) {
  const el = event.currentTarget
  if (!el || !spu.value?.mainImage) return

  const rect = el.getBoundingClientRect()
  const px = event.clientX - rect.left
  const py = event.clientY - rect.top
  const half = ZOOM_LENS_SIZE / 2

  // 圆窗位置贴边时保持完整，放大内容仍按鼠标真实位置换算
  lensPos.value = {
    left: Math.max(half, Math.min(rect.width - half, px)),
    top: Math.max(half, Math.min(rect.height - half, py)),
    bgX: (px / rect.width) * 100,
    bgY: (py / rect.height) * 100
  }
}

const lensStyle = computed(() => {
  const img = spu.value?.mainImage
  if (!img) return {}
  return {
    left: `${lensPos.value.left}px`,
    top: `${lensPos.value.top}px`,
    backgroundImage: `url("${img}")`,
    backgroundSize: `${ZOOM_SCALE * 100}% ${ZOOM_SCALE * 100}%`,
    backgroundPosition: `${lensPos.value.bgX}% ${lensPos.value.bgY}%`
  }
})

async function loadRelated() {
  try {
    const data = await productApi.spuPage({ pageNum: 1, pageSize: 6 })
    const list = data?.records || []
    related.value = list
      .filter((g) => String(g.id) !== String(route.params.id))
      .slice(0, 3)
  } catch (e) {
    related.value = []
  }
}

/* ---------------- 商品评价 ---------------- */
const comments = ref([])
const commentLoading = ref(false)
const submitting = ref(false)
const commentForm = ref({ rating: 5, content: '', images: '' })

const avgRating = computed(() => {
  if (!comments.value.length) return '0.0'
  return (comments.value.reduce((s, c) => s + (c.rating || 0), 0) / comments.value.length).toFixed(1)
})

async function loadComments() {
  commentLoading.value = true
  try {
    const res = await commentApi.getByProduct(route.params.id)
    comments.value = res.data || []
  } catch (e) {
    comments.value = []
  } finally {
    commentLoading.value = false
  }
}

async function submitComment() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后再评价')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!commentForm.value.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  const userId = userStore.userInfo?.id
  if (!userId) {
    ElMessage.warning('用户信息缺失，请重新登录')
    return
  }
  submitting.value = true
  try {
    await commentApi.add({
      productId: Number(route.params.id),
      userId,
      orderId: null,
      content: commentForm.value.content.trim(),
      rating: commentForm.value.rating,
      images: commentForm.value.images.trim() || null   // 逗号分隔字符串，不是 JSON 数组
    })
    ElMessage.success('评价已提交，审核通过后展示')
    commentForm.value = { rating: 5, content: '', images: '' }
    loadComments()
  } finally {
    submitting.value = false
  }
}

function selectSku(sku) {
  if (!sku.enable) return
  selectedSkuId.value = sku.id
  num.value = 1
}

function goGoods(id) {
  router.push(`/product/${id}`)
}

async function addToCart() {
  if (isOffShelf.value) {
    ElMessage.warning('该商品已下架')
    return
  }
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!selectedSku.value) {
    ElMessage.warning('请选择规格')
    return
  }
  if (!selectedSku.value.enable) {
    ElMessage.warning('该规格暂时缺货')
    return
  }
  try {
    await cartApi.add({ skuId: selectedSku.value.id, num: num.value })
    flyToCart(galleryRef.value, spu.value?.mainImage)
    ElMessage.success('已加入购物车')
  } catch (e) {
    /* 错误提示由响应拦截器统一处理 */
  }
}

async function buyNow() {
  if (isOffShelf.value) {
    ElMessage.warning('该商品已下架')
    return
  }
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!selectedSku.value) {
    ElMessage.warning('请选择规格')
    return
  }
  try {
    await cartApi.add({ skuId: selectedSku.value.id, num: num.value })
    flyToCart(galleryRef.value, spu.value?.mainImage)
    router.push('/cart')
  } catch (e) {
    /* 错误提示由响应拦截器统一处理 */
  }
}

onMounted(load)

/* 同一组件内切换商品时重新加载 */
watch(
  () => route.params.id,
  (id) => {
    if (id) load()
  }
)
</script>

<template>
  <div class="container detail-page">
    <!-- 面包屑 -->
    <nav class="crumbs">
      <span class="crumb" @click="router.push('/')">首页</span>
      <el-icon :size="12"><ArrowRight /></el-icon>
      <span class="crumb current">{{ spu?.name || '商品详情' }}</span>
    </nav>

    <div v-if="loading" class="detail-main">
      <div class="gallery skeleton"></div>
      <div class="detail-info">
        <div class="sk-line" style="width: 40%; height: 18px"></div>
        <div class="sk-line" style="width: 80%; height: 26px; margin-top: 16px"></div>
        <div class="sk-line" style="width: 60%; height: 14px; margin-top: 14px"></div>
        <div class="sk-block" style="margin-top: 26px"></div>
      </div>
    </div>

    <div v-else-if="spu" class="detail-main">
      <!-- 图片 -->
      <div ref="galleryRef" class="gallery">
        <div
          class="gallery-main"
          @mouseenter="handleZoomEnter"
          @mouseleave="handleZoomLeave"
          @mousemove="handleZoomMove"
        >
          <img v-if="spu.mainImage" :src="spu.mainImage" :alt="spu.name" />
          <div v-else class="img-placeholder">{{ spu.name.slice(0, 1) }}</div>

          <div v-if="zoomOn" class="zoom-lens" :style="lensStyle"></div>
        </div>

        <p v-if="spu.mainImage" class="zoom-tip">
          <el-icon :size="12"><Search /></el-icon>
          鼠标移到图片上可放大查看
        </p>
      </div>

      <!-- 信息 -->
      <div class="detail-info">
        <span class="brand-tag">{{ spu.brandName || 'Mall-Cloud 严选' }}</span>
        <h1 class="name">{{ spu.name }}</h1>
        <p class="caption">{{ spu.caption }}</p>

        <div v-if="isOffShelf" class="offshelf-notice">
          <el-icon :size="16"><WarningFilled /></el-icon>
          <span>该商品已下架，暂时无法购买</span>
        </div>

        <div class="price-card">
          <div class="price-left">
            <span class="price-label">价格</span>
            <span class="price-value price">{{ formatPrice(displayPrice) }}</span>
          </div>
          <span class="stock-pill" :class="{ danger: isOffShelf || (selectedSku && selectedSku.stock <= 10) }">
            {{ isOffShelf ? '已下架' : stockText }}
          </span>
        </div>

        <ul class="service-list">
          <li v-for="s in services" :key="s.text">
            <el-icon :size="14"><component :is="s.icon" /></el-icon>
            <span>{{ s.text }}</span>
          </li>
        </ul>

        <div v-if="spu.skus && spu.skus.length > 1" class="field">
          <span class="field-label">规格</span>
          <div class="sku-list">
            <button
              v-for="s in spu.skus"
              :key="s.id"
              type="button"
              class="sku-item"
              :class="{ active: s.id === selectedSkuId, disabled: !s.enable }"
              @click="selectSku(s)"
            >
              {{ s.title }}
            </button>
          </div>
        </div>

        <div class="field">
          <span class="field-label">数量</span>
          <el-input-number
            v-model="num"
            :min="1"
            :max="selectedSku?.stock || 1"
            :disabled="isOffShelf || !selectedSku || selectedSku.stock <= 0"
          />
        </div>

        <div class="buy-row">
          <button class="btn-brand btn-cart" :disabled="isOffShelf" @click="addToCart">
            <el-icon :size="16"><ShoppingCart /></el-icon>
            {{ isOffShelf ? '已下架' : '加入购物车' }}
          </button>
          <button class="btn-buy" :disabled="isOffShelf" @click="buyNow">{{ isOffShelf ? '已下架' : '立即购买' }}</button>
        </div>
      </div>

      <!-- 相关推荐 -->
      <aside v-if="related.length" class="related">
        <h3 class="related-title">相关推荐</h3>
        <div
          v-for="r in related"
          :key="r.id"
          class="related-item"
          @click="goGoods(r.id)"
        >
          <div class="related-img">
            <img v-if="r.mainImage" :src="r.mainImage" :alt="r.name" loading="lazy" />
            <span v-else>{{ r.name.slice(0, 1) }}</span>
          </div>
          <div class="related-info">
            <p class="related-name" :title="r.name">{{ r.name }}</p>
            <p class="price related-price">{{ formatPrice(r.minPrice) }}</p>
          </div>
        </div>
      </aside>
    </div>

    <!-- 商品评价 -->
    <section class="comment-section card">
      <h3 class="comment-title">
        商品评价
        <span class="comment-count">{{ comments.length }} 条</span>
        <span v-if="comments.length" class="comment-avg">平均 {{ avgRating }} 分</span>
      </h3>

      <div v-loading="commentLoading">
        <el-empty v-if="!comments.length" description="暂无评价" :image-size="80" />
        <ul v-else class="comment-list">
          <li v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-head">
              <span class="comment-user">用户{{ c.userId }}</span>
              <el-rate :model-value="c.rating" disabled size="small" />
              <span class="comment-time">{{ formatDateTime(c.createTime) }}</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
            <div v-if="c.images" class="comment-images">
              <img v-for="(img, i) in String(c.images).split(',').filter(Boolean)" :key="i" :src="img" loading="lazy" />
            </div>
          </li>
        </ul>
      </div>

      <!-- 下架商品不给评价入口，复用 isOffShelf -->
      <div v-if="!isOffShelf" class="comment-form">
        <el-rate v-model="commentForm.rating" />
        <el-input v-model="commentForm.content" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="说说使用感受…" />
        <el-input v-model="commentForm.images" placeholder="图片 URL，多个用英文逗号分隔（可留空）" />
        <button class="btn-brand" :disabled="submitting" @click="submitComment">
          {{ submitting ? '提交中…' : '发表评价' }}
        </button>
      </div>
    </section>

    <!-- 商品详情 -->
    <section v-if="spu?.detail" class="desc card">
      <h3 class="desc-title">商品详情</h3>
      <p class="desc-body">{{ spu.detail }}</p>
    </section>

    <el-empty v-if="!loading && !spu" description="商品不存在或已下架">
      <el-button type="primary" round @click="router.push('/')">回到首页</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.detail-page {
  padding-top: 22px;
}

/* ---------------- 面包屑 ---------------- */
.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  font-size: 13px;
  color: var(--text-muted);
}

.crumb {
  cursor: pointer;
  transition: color 0.2s ease;
}

.crumb:hover {
  color: var(--brand-500);
}

.crumb.current {
  color: var(--text-body);
  cursor: default;
}

.crumb.current:hover {
  color: var(--text-body);
}

/* ---------------- 主区域 ---------------- */
.detail-main {
  display: flex;
  align-items: flex-start;
  gap: 28px;
}

.gallery {
  width: 396px;
  flex-shrink: 0;
}

.gallery-main {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: 22px;
  background: var(--surface);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-in;
}

.gallery-main img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 放大镜圆窗 */
.zoom-lens {
  position: absolute;
  width: 170px;
  height: 170px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 32px rgba(31, 42, 71, 0.24);
  background-repeat: no-repeat;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 2;
}

.zoom-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--text-hint);
}

.img-placeholder {
  font-size: 86px;
  font-weight: 500;
  color: #cdd5e8;
}

.detail-info {
  flex: 1;
  min-width: 0;
}

.brand-tag {
  display: inline-block;
  padding: 4px 12px;
  margin-bottom: 14px;
  border-radius: var(--radius-pill);
  background: var(--brand-soft);
  font-size: 12px;
  color: var(--brand-500);
}

.name {
  font-size: 24px;
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: -0.3px;
  color: var(--text-title);
  margin-bottom: 10px;
}

.caption {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-muted);
  margin-bottom: 20px;
}

.price-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  margin-bottom: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eef1fd 0%, #f3e9fa 100%);
}

.price-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price-label {
  font-size: 13px;
  color: var(--text-muted);
}

.price-value {
  font-size: 30px;
  letter-spacing: -0.5px;
}

.stock-pill {
  padding: 5px 14px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  color: var(--brand-500);
}

.stock-pill.danger {
  color: var(--accent-500);
}

.offshelf-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 18px;
  border-radius: 14px;
  background: var(--accent-soft);
  border: 1px solid #ffd9cf;
  font-size: 13px;
  color: var(--accent-500);
}

.service-list {
  display: flex;
  gap: 22px;
  margin-bottom: 24px;
}

.service-list li {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.field {
  display: flex;
  align-items: flex-start;
  margin-bottom: 18px;
}

.field-label {
  width: 46px;
  flex-shrink: 0;
  padding-top: 9px;
  font-size: 13px;
  color: var(--text-muted);
}

.sku-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sku-item {
  padding: 8px 18px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  font-size: 13px;
  font-family: inherit;
  color: var(--text-body);
  cursor: pointer;
  transition: all 0.2s ease;
}

.sku-item:hover {
  border-color: var(--brand-400);
  color: var(--brand-500);
}

.sku-item.active {
  border-color: var(--brand-500);
  color: var(--brand-500);
  background: var(--brand-soft);
  box-shadow: 0 0 0 1px var(--brand-500) inset;
}

.sku-item.disabled {
  opacity: 0.42;
  cursor: not-allowed;
  text-decoration: line-through;
}

.buy-row {
  display: flex;
  gap: 14px;
  margin-top: 26px;
}

.btn-cart {
  padding: 13px 30px;
  font-size: 15px;
}

.btn-buy {
  padding: 13px 32px;
  border: 1.5px solid var(--brand-500);
  border-radius: var(--radius-pill);
  background: transparent;
  font-size: 15px;
  font-family: inherit;
  color: var(--brand-500);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.16s ease;
}

.btn-buy:hover {
  background: var(--brand-soft);
}

.btn-buy:active {
  transform: scale(0.97);
}

.btn-cart:disabled,
.btn-buy:disabled {
  background: var(--surface-sunken);
  border-color: var(--border-strong);
  color: var(--text-muted);
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
}

.btn-cart:disabled:hover,
.btn-buy:disabled:hover {
  background: var(--surface-sunken);
}

/* ---------------- 相关推荐 ---------------- */
.related {
  width: 228px;
  flex-shrink: 0;
  padding: 20px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.related-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-title);
  margin-bottom: 16px;
}

.related-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.related-item + .related-item {
  margin-top: 6px;
}

.related-item:hover {
  background: var(--surface-sunken);
  transform: translateX(3px);
}

.related-img {
  width: 58px;
  height: 58px;
  flex-shrink: 0;
  border-radius: 12px;
  background: var(--surface-sunken);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 20px;
  color: #cdd5e8;
}

.related-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.related-info {
  min-width: 0;
}

.related-name {
  font-size: 13px;
  color: var(--text-title);
  margin-bottom: 5px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-price {
  font-size: 14px;
}

/* ---------------- 商品评价 ---------------- */
.comment-section {
  margin-top: 34px;
  padding: 28px 30px;
}

.comment-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-title);
  margin-bottom: 20px;
}

.comment-count {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
}

.comment-avg {
  font-size: 12px;
  color: var(--brand-500);
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.comment-item {
  padding: 18px 0;
  border-bottom: 1px solid var(--border);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.comment-user {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-title);
}

.comment-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-hint);
}

.comment-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-body);
  white-space: pre-wrap;
}

.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.comment-images img {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  object-fit: cover;
  background: var(--surface-sunken);
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 22px;
  padding: 20px;
  border-radius: var(--radius-lg);
  background: var(--surface-sunken);
}

.comment-form .btn-brand {
  align-self: flex-start;
  padding: 11px 30px;
}

/* ---------------- 商品详情 ---------------- */
.desc {
  margin-top: 34px;
  padding: 28px 30px;
}

.desc-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-title);
  margin-bottom: 16px;
}

.desc-body {
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-body);
  white-space: pre-wrap;
}

/* ---------------- 骨架屏 ---------------- */
.skeleton {
  aspect-ratio: 1 / 1;
  border-radius: 22px;
  background: linear-gradient(90deg, #eef1f8 25%, #f7f9fd 37%, #eef1f8 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.sk-line,
.sk-block {
  border-radius: 8px;
  background: linear-gradient(90deg, #eef1f8 25%, #f7f9fd 37%, #eef1f8 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.sk-line {
  height: 16px;
}

.sk-block {
  height: 96px;
  border-radius: 18px;
}

@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1080px) {
  .related {
    display: none;
  }
}

@media (max-width: 900px) {
  .detail-main {
    flex-direction: column;
  }
  .gallery {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton,
  .sk-line,
  .sk-block {
    animation: none;
  }
}
</style>
