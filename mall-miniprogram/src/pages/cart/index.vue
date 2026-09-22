<template>
  <view class="page">
    <!-- 未登录 -->
    <view v-if="!userStore.isLogin" class="empty">
      <text class="empty-text">登录后查看购物车</text>
      <view class="btn" @tap="goLogin">去登录</view>
    </view>

    <template v-else>
      <view v-for="item in cartStore.items" :key="item.id" class="cart-item">
        <view class="check" @tap="toggle(item)">
          <view class="check-box" :class="{ checked: Number(item.selected) === 1 }" />
        </view>
        <image class="item-image" :src="imageUrl(item.productImage)" mode="aspectFill" />
        <view class="item-info">
          <text class="item-name">{{ item.productName }}</text>
          <text class="item-price">￥{{ formatPrice(item.productPrice) }}</text>
          <view class="quantity">
            <view class="qty-btn" @tap="changeQuantity(item, -1)">-</view>
            <text class="qty-value">{{ item.quantity }}</text>
            <view class="qty-btn" @tap="changeQuantity(item, 1)">+</view>
          </view>
        </view>
        <view class="delete" @tap="removeItem(item)">删除</view>
      </view>

      <view v-if="!cartStore.loading && !cartStore.items.length" class="empty">
        <text class="empty-text">购物车还是空的</text>
      </view>

      <view v-if="cartStore.items.length" class="footer">
        <view class="check" @tap="toggleAll">
          <view class="check-box" :class="{ checked: cartStore.allSelected }" />
          <text class="footer-label">全选</text>
        </view>
        <view class="footer-right">
          <text class="total">合计：￥{{ formatPrice(cartStore.selectedAmount) }}</text>
          <view class="btn" @tap="goCheckout">去结算({{ cartStore.selectedCount }})</view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import { formatPrice } from '@/utils/format'
import { resolveFileUrl } from '@/utils/request'

const cartStore = useCartStore()
const userStore = useUserStore()

function imageUrl(path) {
  return resolveFileUrl(path)
}

function goLogin() {
  uni.navigateTo({ url: '/pages-user/login/index' })
}

function toggle(item) {
  cartStore.toggleSelected(item.productId, Number(item.selected) !== 1)
}

function toggleAll() {
  cartStore.toggleAll(!cartStore.allSelected)
}

function changeQuantity(item, delta) {
  const next = Number(item.quantity) + delta
  if (next < 1) {
    uni.showToast({ title: '数量不能少于 1', icon: 'none' })
    return
  }
  cartStore.updateQuantity(item.productId, next)
}

function removeItem(item) {
  uni.showModal({
    title: '提示',
    content: '确认从购物车移除该商品？',
    success: (res) => {
      if (res.confirm) {
        cartStore.remove(item.productId)
      }
    }
  })
}

function goCheckout() {
  if (!cartStore.selectedItems.length) {
    uni.showToast({ title: '请先选择商品', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages-order/confirm' })
}

onShow(() => {
  if (userStore.isLogin) {
    cartStore.load()
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 140rpx;
}

.cart-item {
  display: flex;
  align-items: center;
  background-color: #ffffff;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.check {
  display: flex;
  align-items: center;
}

.check-box {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  border: 2rpx solid #cccccc;
  box-sizing: border-box;
}

.check-box.checked {
  background-color: #e93b3d;
  border-color: #e93b3d;
}

.item-image {
  width: 160rpx;
  height: 160rpx;
  margin: 0 20rpx;
  border-radius: 12rpx;
  background-color: #eeeeee;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.item-name {
  font-size: 28rpx;
  color: #333333;
}

.item-price {
  margin-top: 10rpx;
  color: #e93b3d;
  font-size: 30rpx;
  font-weight: bold;
}

.quantity {
  margin-top: 14rpx;
  display: flex;
  align-items: center;
}

.qty-btn {
  width: 48rpx;
  height: 48rpx;
  line-height: 44rpx;
  text-align: center;
  border: 2rpx solid #dddddd;
  border-radius: 8rpx;
  color: #666666;
}

.qty-value {
  width: 72rpx;
  text-align: center;
  font-size: 28rpx;
}

.delete {
  color: #999999;
  font-size: 24rpx;
  padding-left: 16rpx;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 110rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  background-color: #ffffff;
  border-top: 2rpx solid #eeeeee;
}

.footer-label {
  margin-left: 12rpx;
  font-size: 26rpx;
}

.footer-right {
  display: flex;
  align-items: center;
}

.total {
  color: #e93b3d;
  font-size: 28rpx;
  font-weight: bold;
  margin-right: 20rpx;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}

.empty-text {
  color: #999999;
  font-size: 28rpx;
  margin-bottom: 32rpx;
}

.btn {
  padding: 0 40rpx;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 32rpx;
  background-color: #e93b3d;
  color: #ffffff;
  font-size: 26rpx;
}
</style>
