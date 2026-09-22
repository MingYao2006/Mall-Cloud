<template>
  <view class="page">
    <view class="search-bar" @tap="goSearch">
      <text class="search-text">搜索商品</text>
    </view>

    <view class="goods-list">
      <view
        v-for="item in products"
        :key="item.id"
        class="goods-item"
        @tap="goDetail(item.id)"
      >
        <image class="goods-image" :src="imageUrl(item.mainImage)" mode="aspectFill" />
        <view class="goods-info">
          <text class="goods-name">{{ item.name }}</text>
          <text class="goods-price">￥{{ formatPrice(item.price) }}</text>
        </view>
      </view>
    </view>

    <view v-if="loading" class="tip">加载中...</view>
    <view v-else-if="!products.length" class="tip">暂无商品</view>
    <view v-else-if="finished" class="tip">没有更多了</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { productApi } from '@/api/product'
import { formatPrice } from '@/utils/format'
import { resolveFileUrl } from '@/utils/request'

const PAGE_SIZE = 10

const products = ref([])
const pageNum = ref(1)
const loading = ref(false)
const finished = ref(false)

// 后端商品主图可能返回相对路径，小程序无法直接渲染，统一补全
function imageUrl(path) {
  return resolveFileUrl(path)
}

async function loadProducts(reset = false) {
  if (loading.value) {
    return
  }
  loading.value = true
  try {
    const res = await productApi.getPage(pageNum.value, PAGE_SIZE)
    const page = res.data || {}
    const list = page.list || []
    products.value = reset ? list : products.value.concat(list)
    finished.value = list.length < PAGE_SIZE
    if (!finished.value) {
      pageNum.value += 1
    }
  } catch (e) {
    finished.value = true
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  uni.navigateTo({ url: '/pages/product/detail?id=' + id })
}

function goSearch() {
  uni.navigateTo({ url: '/pages/search/index' })
}

onPullDownRefresh(async () => {
  pageNum.value = 1
  finished.value = false
  await loadProducts(true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!finished.value) {
    loadProducts()
  }
})

loadProducts(true)
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 20rpx;
}

.search-bar {
  margin: 20rpx 24rpx;
  height: 72rpx;
  border-radius: 36rpx;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-text {
  color: #999999;
  font-size: 26rpx;
}

.goods-list {
  display: flex;
  flex-wrap: wrap;
  padding: 0 12rpx;
}

.goods-item {
  width: 50%;
  box-sizing: border-box;
  padding: 12rpx;
}

.goods-image {
  width: 100%;
  height: 340rpx;
  border-radius: 12rpx;
  background-color: #eeeeee;
}

.goods-info {
  padding: 12rpx 4rpx;
  display: flex;
  flex-direction: column;
}

.goods-name {
  font-size: 26rpx;
  color: #333333;
  line-height: 36rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-price {
  margin-top: 8rpx;
  color: #e93b3d;
  font-size: 30rpx;
  font-weight: bold;
}

.tip {
  text-align: center;
  color: #999999;
  font-size: 24rpx;
  padding: 24rpx 0;
}
</style>
