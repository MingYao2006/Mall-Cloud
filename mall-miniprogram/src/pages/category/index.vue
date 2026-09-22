<template>
  <view class="page">
    <scroll-view class="cate-bar" scroll-x>
      <view
        v-for="cate in categories"
        :key="cate.id"
        class="cate-item"
        :class="{ active: cate.id === currentId }"
        @tap="selectCategory(cate.id)"
      >
        {{ cate.name }}
      </view>
    </scroll-view>

    <view class="goods-list">
      <view
        v-for="item in products"
        :key="item.id"
        class="goods-item"
        @tap="goDetail(item.id)"
      >
        <image class="goods-image" :src="imageUrl(item.mainImage)" mode="aspectFill" />
        <text class="goods-name">{{ item.name }}</text>
        <text class="goods-price">￥{{ formatPrice(item.price) }}</text>
      </view>
    </view>

    <view v-if="loading" class="tip">加载中...</view>
    <view v-else-if="!products.length" class="tip">该分类下暂无商品</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { categoryApi } from '@/api/category'
import { productApi } from '@/api/product'
import { formatPrice } from '@/utils/format'
import { resolveFileUrl } from '@/utils/request'

const categories = ref([])
const currentId = ref(null)
const products = ref([])
const loading = ref(false)

function imageUrl(path) {
  return resolveFileUrl(path)
}

async function loadCategories() {
  const res = await categoryApi.getList()
  categories.value = res.data || []
  if (categories.value.length) {
    selectCategory(categories.value[0].id)
  }
}

async function selectCategory(id) {
  currentId.value = id
  loading.value = true
  try {
    const res = await productApi.getByCategory(id)
    products.value = res.data || []
  } catch (e) {
    products.value = []
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  uni.navigateTo({ url: '/pages/product/detail?id=' + id })
}

loadCategories()
</script>

<style scoped>
.page {
  min-height: 100vh;
}

.cate-bar {
  white-space: nowrap;
  background-color: #ffffff;
  padding: 20rpx 0;
}

.cate-item {
  display: inline-block;
  padding: 10rpx 28rpx;
  margin-left: 20rpx;
  border-radius: 28rpx;
  background-color: #f2f2f2;
  font-size: 26rpx;
  color: #666666;
}

.cate-item.active {
  background-color: #e93b3d;
  color: #ffffff;
}

.goods-list {
  display: flex;
  flex-wrap: wrap;
  padding: 12rpx;
}

.goods-item {
  width: 50%;
  box-sizing: border-box;
  padding: 12rpx;
  display: flex;
  flex-direction: column;
}

.goods-image {
  width: 100%;
  height: 320rpx;
  border-radius: 12rpx;
  background-color: #eeeeee;
}

.goods-name {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #333333;
}

.goods-price {
  margin-top: 6rpx;
  color: #e93b3d;
  font-size: 28rpx;
  font-weight: bold;
}

.tip {
  text-align: center;
  color: #999999;
  font-size: 24rpx;
  padding: 24rpx 0;
}
</style>
