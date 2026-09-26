<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中...</view>

    <template v-else-if="product">
      <image class="banner" :src="imageUrl(product.mainImage)" mode="aspectFill" />

      <view class="block">
        <view v-if="isOffShelf" class="offshelf-tag">该商品已下架</view>
        <text class="price">￥{{ formatPrice(product.price) }}</text>
        <text v-if="product.originalPrice" class="origin-price">￥{{ formatPrice(product.originalPrice) }}</text>
        <text class="name">{{ product.name }}</text>
        <view class="meta">
          <text>库存：{{ product.stock }}</text>
          <text class="meta-right">销量：{{ product.sales }}</text>
        </view>
      </view>

      <view class="block">
        <text class="section-title">商品详情</text>
        <text class="detail-text">{{ product.detail || '暂无详情' }}</text>
      </view>

      <view class="footer">
        <view class="footer-btn ghost" :class="{ disabled: isOffShelf }" @tap="addToCart">{{ isOffShelf ? '已下架' : '加入购物车' }}</view>
        <view class="footer-btn primary" :class="{ disabled: isOffShelf }" @tap="buyNow">{{ isOffShelf ? '已下架' : '立即购买' }}</view>
      </view>
    </template>

    <view v-else class="tip">商品不存在或已被下架</view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { productApi } from '@/api/product'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import { formatPrice } from '@/utils/format'
import { resolveFileUrl } from '@/utils/request'

const product = ref(null)
const loading = ref(true)
const isOffShelf = computed(() => product.value?.status === 0)

const cartStore = useCartStore()
const userStore = useUserStore()

let productId = null

function imageUrl(path) {
  return resolveFileUrl(path)
}

async function loadProduct() {
  loading.value = true
  try {
    const res = await productApi.getDetail(productId)
    product.value = res.data
  } catch (e) {
    product.value = null
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  if (isOffShelf.value) {
    uni.showToast({ title: '该商品已下架', icon: 'none' })
    return
  }
  if (!userStore.ensureLogin()) {
    return
  }
  await cartStore.add(productId, 1)
  uni.showToast({ title: '已加入购物车', icon: 'none' })
}

function buyNow() {
  if (isOffShelf.value) {
    uni.showToast({ title: '该商品已下架', icon: 'none' })
    return
  }
  if (!userStore.ensureLogin()) {
    return
  }
  uni.navigateTo({ url: '/pages-order/confirm?productId=' + productId + '&quantity=1' })
}

onLoad((options) => {
  productId = options && options.id ? options.id : null
  if (!productId) {
    loading.value = false
    return
  }
  loadProduct()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 140rpx;
}

.banner {
  width: 100%;
  height: 750rpx;
  background-color: #eeeeee;
}

.block {
  background-color: #ffffff;
  padding: 28rpx 32rpx;
  margin-bottom: 16rpx;
  display: flex;
  flex-direction: column;
}

.offshelf-tag {
  align-self: flex-start;
  margin-bottom: 12rpx;
  padding: 6rpx 18rpx;
  border-radius: 999rpx;
  background-color: #fff0ec;
  color: #e93b3d;
  font-size: 24rpx;
}

.price {
  color: #e93b3d;
  font-size: 44rpx;
  font-weight: bold;
}

.origin-price {
  margin-top: 6rpx;
  color: #999999;
  font-size: 24rpx;
  text-decoration: line-through;
}

.name {
  margin-top: 14rpx;
  font-size: 32rpx;
  color: #333333;
  line-height: 44rpx;
}

.meta {
  margin-top: 16rpx;
  display: flex;
  justify-content: space-between;
  color: #999999;
  font-size: 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333333;
  margin-bottom: 14rpx;
}

.detail-text {
  font-size: 26rpx;
  color: #666666;
  line-height: 40rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  padding: 16rpx 24rpx;
  background-color: #ffffff;
  border-top: 2rpx solid #eeeeee;
}

.footer-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
  border-radius: 42rpx;
  font-size: 28rpx;
}

.footer-btn.ghost {
  border: 2rpx solid #e93b3d;
  color: #e93b3d;
  margin-right: 20rpx;
}

.footer-btn.primary {
  background-color: #e93b3d;
  color: #ffffff;
}

.footer-btn.disabled {
  opacity: 0.5;
}

.tip {
  text-align: center;
  color: #999999;
  font-size: 26rpx;
  padding: 120rpx 0;
}
</style>
