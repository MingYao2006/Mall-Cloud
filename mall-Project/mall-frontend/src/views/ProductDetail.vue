<template>
  <div class="detail-page">
    <div class="detail-main">
      <!-- 左侧图片 -->
      <div class="gallery">
        <div class="main-img">
          <img v-if="curImage && !curImage.includes('placeholder')" :src="curImage" :alt="product.name" />
          <div v-else class="image-placeholder">
            <span class="placeholder-text">{{ product.name || '商品' }}</span>
          </div>
        </div>
        <div class="thumb-list">
          <div v-for="(img,i) in images" :key="i" :class="{ active: curImage===img }" @click="curImage=img">
            <img v-if="!img.includes('placeholder')" :src="img" />
            <div v-else class="thumb-placeholder">{{ product.name?.charAt(0) || '商' }}</div>
          </div>
        </div>
      </div>

      <!-- 中间信息 -->
      <div class="info">
        <h1>{{ product.name }}</h1>
        <p class="brand" v-if="product.brand">品牌：{{ product.brand }}</p>
        <div class="price-box">
          <span class="price-lg">¥{{ product.price?.toFixed(2) }}</span>
          <span class="price-original" v-if="product.originalPrice">¥{{ product.originalPrice?.toFixed(2) }}</span>
          <span class="discount" v-if="product.originalPrice && product.price">
            {{ Math.round((1 - product.price / product.originalPrice) * 100) }}% OFF
          </span>
        </div>
        <div class="meta">
          <span>库存：{{ product.stock || 0 }}</span>
          <span>销量：{{ product.sales || 0 }}</span>
          <span :class="product.status===1 ? 'on' : 'off'">{{ product.status===1 ? '在售' : '已下架' }}</span>
        </div>
        <div class="quantity-row">
          <span>数量：</span>
          <el-input-number v-model="quantity" :min="1" :max="Math.max(product.stock||1, 1)" />
        </div>
        <div class="actions">
          <el-button type="primary" size="large" @click="buyNow" :disabled="product.status!==1">立即购买</el-button>
          <el-button size="large" @click="addCart" :disabled="product.status!==1">
            <el-icon><ShoppingCart /></el-icon> 加入购物车
          </el-button>
        </div>
      </div>

      <!-- 右侧推荐 -->
      <div class="side">
        <h4>热门推荐</h4>
        <div v-for="p in hotProducts" :key="p.id" class="side-item" @click="$router.push(`/product/${p.id}`)">
          <img :src="p.mainImage" />
          <div>
            <p>{{ p.name }}</p>
            <p class="price-red">¥{{ p.price?.toFixed(2) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情 + 评价 Tabs -->
    <div class="detail-tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="商品详情" name="detail">
          <div class="detail-html" v-html="product.detail || '暂无详情'"></div>
        </el-tab-pane>
        <el-tab-pane label="商品评价" name="reviews">
          <div v-if="comments.length">
            <div v-for="c in comments" :key="c.id" class="review-item">
              <div class="review-hd">
                <el-rate :model-value="c.rating" disabled size="small" />
                <span class="review-user">{{ c.isAnonymous ? '匿名用户' : '用户' }}</span>
                <span class="review-time">{{ c.createTime }}</span>
              </div>
              <p>{{ c.content }}</p>
              <div v-if="c.images" class="review-imgs">
                <img v-for="(img,i) in c.images.split(',')" :key="i" :src="img" />
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无评价" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi, cartApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref({})
const images = ref([])
const curImage = ref('')
const quantity = ref(1)
const activeTab = ref('detail')
const comments = ref([])
const hotProducts = ref([])

const buyNow = async () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); router.push('/login'); return }
  router.push(`/product/${product.value.id}?buy=1&qty=${quantity.value}`)
}

const addCart = async () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); router.push('/login'); return }
  try {
    await cartApi.add(product.value.id, quantity.value)
    ElMessage.success('已加入购物车')
  } catch (e) { ElMessage.error('加入失败') }
}

onMounted(async () => {
  const id = route.params.id
  try {
    const res = await productApi.getDetail(id)
    product.value = res.data || {}
    const imgs = []
    if (product.value.mainImage) imgs.push(product.value.mainImage)
    if (product.value.subImages) imgs.push(...product.value.subImages.split(',').filter(Boolean))
    // 如果没有图片，使用默认占位图
    if (imgs.length === 0) {
      imgs.push(getDefaultImage(product.value.name))
    }
    images.value = imgs
    curImage.value = images.value[0]
  } catch (e) { /* ignore */ }

  try {
    const hot = await productApi.getPage(1, 4)
    hotProducts.value = hot.data?.list || hot.data?.records || []
  } catch (e) { /* ignore */ }
})

// 生成默认图片标记（使用 placeholder 标记来触发 CSS 占位图）
const getDefaultImage = (name) => {
  // 返回一个特殊标记，前端会识别这个标记并显示 CSS 占位图
  return `placeholder-${name}`
}
</script>

<style scoped>
.detail-page {
  max-width: 1200px;
  margin: var(--spacing-xl) auto;
  padding: 0 var(--spacing-md);
}

.detail-main {
  display: flex;
  gap: var(--spacing-lg);
  background: var(--color-white);
  padding: var(--spacing-lg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

/* 图片画廊 */
.gallery {
  width: 480px;
  flex-shrink: 0;
}

.main-img {
  width: 100%;
  height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-bg);
}

.main-img img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

/* 主图占位符 */
.main-img .image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
  padding: var(--spacing-lg);
}

.main-img .placeholder-text {
  font-size: 24px;
  font-weight: 600;
  color: #666;
  text-align: center;
  word-break: break-all;
  line-height: 1.4;
}

.thumb-list {
  display: flex;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
}

.thumb-list div {
  width: 64px;
  height: 64px;
  border: 2px solid transparent;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s;
}

.thumb-list div.active {
  border-color: var(--color-accent);
}

.thumb-list img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 缩略图占位符 */
.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
  font-size: 24px;
  font-weight: 600;
  color: #999;
}

/* 商品信息 */
.info {
  flex: 1;
}

.info h1 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: var(--spacing-sm);
  color: var(--color-primary);
  line-height: 1.4;
}

.brand {
  font-size: 14px;
  color: var(--color-text-light);
  margin-bottom: var(--spacing-md);
}

.price-box {
  background: var(--color-bg);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-md);
  display: flex;
  align-items: baseline;
  gap: var(--spacing-sm);
}

.price-lg {
  font-size: 32px;
  font-weight: 700;
}

.price-original {
  font-size: 16px;
}

.discount {
  background: var(--color-accent);
  color: var(--color-white);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}

.meta {
  display: flex;
  gap: var(--spacing-md);
  color: var(--color-text-light);
  margin-bottom: var(--spacing-md);
  font-size: 14px;
}

.meta .on { color: var(--color-success); }
.meta .off { color: var(--color-danger); }

.quantity-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
  font-size: 14px;
  color: var(--color-text);
}

.actions {
  display: flex;
  gap: var(--spacing-sm);
}

.actions .el-button {
  flex: 1;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

/* 侧边推荐 */
.side {
  width: 220px;
  flex-shrink: 0;
}

.side h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: var(--spacing-sm);
  color: var(--color-primary);
}

.side-item {
  display: flex;
  gap: var(--spacing-xs);
  cursor: pointer;
  padding: var(--spacing-xs) 0;
  border-bottom: 1px solid var(--color-border);
  transition: all 0.2s;
}

.side-item:last-child {
  border-bottom: none;
}

.side-item:hover {
  transform: translateX(4px);
}

.side-item img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.side-item p {
  font-size: 13px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 130px;
  margin-bottom: 4px;
}

/* Tabs */
.detail-tabs {
  background: var(--color-white);
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.detail-html {
  padding: var(--spacing-md) 0;
  line-height: 1.8;
}

/* 评价 */
.review-item {
  padding: var(--spacing-sm) 0;
  border-bottom: 1px solid var(--color-border);
}

.review-item:last-child {
  border-bottom: none;
}

.review-hd {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-xs);
}

.review-user {
  font-size: 13px;
  color: var(--color-text-light);
}

.review-time {
  font-size: 12px;
  color: var(--color-text-lighter);
  margin-left: auto;
}

.review-item > p {
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.6;
  margin-bottom: var(--spacing-xs);
}

.review-imgs {
  display: flex;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-xs);
}

.review-imgs img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: transform 0.2s;
}

.review-imgs img:hover {
  transform: scale(1.05);
}

/* 响应式 */
@media (max-width: 968px) {
  .detail-main {
    flex-direction: column;
  }
  
  .gallery {
    width: 100%;
  }
  
  .side {
    display: none;
  }
}
</style>
