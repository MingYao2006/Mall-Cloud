<template>
  <div class="detail-page">
    <div class="detail-main">
      <!-- 左侧图片 -->
      <div class="gallery">
        <div class="main-img">
          <img :src="curImage" :alt="product.name" />
        </div>
        <div class="thumb-list">
          <div v-for="(img,i) in images" :key="i" :class="{ active: curImage===img }" @click="curImage=img">
            <img :src="img" />
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
    await cartApi.add(userStore.userInfo.id, product.value.id, quantity.value)
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
    images.value = imgs.length ? imgs : ['https://placehold.co/400x400/eee/999?text=No+Image']
    curImage.value = images.value[0]
  } catch (e) { /* ignore */ }

  try {
    const hot = await productApi.getPage(1, 4)
    hotProducts.value = hot.data?.list || hot.data?.records || []
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.detail-page { max-width: 1200px; margin: 20px auto; }
.detail-main { display: flex; gap: 24px; background: #fff; padding: 24px; border-radius: 8px; }

.gallery { width: 420px; }
.main-img { width: 100%; height: 420px; display: flex; align-items: center; justify-content: center; border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden; }
.main-img img { max-width: 100%; max-height: 100%; object-fit: contain; }
.thumb-list { display: flex; gap: 8px; margin-top: 12px; }
.thumb-list div { width: 64px; height: 64px; border: 2px solid transparent; border-radius: 4px; overflow: hidden; cursor: pointer; }
.thumb-list div.active { border-color: var(--jd-red); }
.thumb-list img { width: 100%; height: 100%; object-fit: cover; }

.info { flex: 1; }
.info h1 { font-size: 22px; margin-bottom: 8px; }
.brand { font-size: 14px; color: var(--jd-text-light); margin-bottom: 16px; }
.price-box { background: #fff5f5; padding: 16px; border-radius: 8px; margin-bottom: 16px; display: flex; align-items: baseline; gap: 12px; }
.price-lg { font-size: 28px; color: var(--jd-red); font-weight: 700; }
.price-original { font-size: 14px; color: #999; text-decoration: line-through; }
.discount { background: var(--jd-red); color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.meta { display: flex; gap: 24px; color: var(--jd-text-light); margin-bottom: 16px; font-size: 13px; }
.meta .on { color: #67c23a; }
.meta .off { color: #f56c6c; }
.quantity-row { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.actions { display: flex; gap: 12px; }

.side { width: 200px; }
.side h4 { margin-bottom: 12px; }
.side-item { display: flex; gap: 8px; cursor: pointer; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }
.side-item img { width: 60px; height: 60px; object-fit: cover; border-radius: 4px; }
.side-item p { font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; width: 120px; }

.detail-tabs { background: #fff; margin-top: 20px; padding: 20px; border-radius: 8px; }
.detail-html { padding: 20px 0; }

.review-item { padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
.review-hd { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.review-user { font-size: 13px; color: var(--jd-text-light); }
.review-time { font-size: 12px; color: var(--jd-text-lighter); margin-left: auto; }
.review-imgs { display: flex; gap: 8px; margin-top: 8px; }
.review-imgs img { width: 80px; height: 80px; object-fit: cover; border-radius: 4px; }
</style>
