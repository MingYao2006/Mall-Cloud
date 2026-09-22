<template>
  <view class="page">
    <view class="search-bar">
      <input
        v-model="keyword"
        class="search-input"
        type="text"
        placeholder="输入商品关键词"
        confirm-type="search"
        @confirm="doSearch"
      />
      <view class="search-btn" @tap="doSearch">搜索</view>
    </view>

    <view class="goods-list">
      <view
        v-for="item in results"
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

    <view v-if="loading" class="tip">搜索中...</view>
    <view v-else-if="searched && !results.length" class="tip">没有找到相关商品</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { productApi } from '@/api/product'
import { formatPrice } from '@/utils/format'
import { resolveFileUrl } from '@/utils/request'

const keyword = ref('')
const results = ref([])
const loading = ref(false)
const searched = ref(false)

function imageUrl(path) {
  return resolveFileUrl(path)
}

async function doSearch() {
  const word = (keyword.value || '').trim()
  if (!word) {
    uni.showToast({ title: '请输入关键词', icon: 'none' })
    return
  }
  loading.value = true
  searched.value = true
  try {
    const res = await productApi.search(word)
    results.value = res.data || []
  } catch (e) {
    results.value = []
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  uni.navigateTo({ url: '/pages/product/detail?id=' + id })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
}

.search-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background-color: #ffffff;
}

.search-input {
  flex: 1;
  height: 72rpx;
  padding: 0 24rpx;
  border-radius: 36rpx;
  background-color: #f2f2f2;
  font-size: 26rpx;
}

.search-btn {
  margin-left: 20rpx;
  color: #e93b3d;
  font-size: 28rpx;
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
}

.goods-image {
  width: 100%;
  height: 320rpx;
  border-radius: 12rpx;
  background-color: #eeeeee;
}

.goods-info {
  display: flex;
  flex-direction: column;
  padding-top: 10rpx;
}

.goods-name {
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
