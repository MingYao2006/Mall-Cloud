<template>
  <view class="page">
    <view class="header" @tap="onHeaderTap">
      <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
      <view v-else class="avatar avatar-empty" />
      <view class="user-info">
        <text class="nickname">{{ displayName }}</text>
        <text class="sub">{{ userStore.isLogin ? 'ID：' + (userStore.userId || '-') : '点击登录账号' }}</text>
      </view>
    </view>

    <view class="menu">
      <view class="menu-item" @tap="go('/pages-order/list/index')">
        <text>我的订单</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="go('/pages-activity/coupon/index')">
        <text>优惠券</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="go('/pages-activity/my-coupon/index')">
        <text>我的优惠券</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="go('/pages-user/profile/index')">
        <text>个人资料</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="go('/pages-user/address/index')">
        <text>收货地址</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @tap="go('/pages-user/password/index')">
        <text>修改密码</text>
        <text class="arrow">></text>
      </view>
    </view>

    <view v-if="userStore.isLogin" class="logout" @tap="onLogout">退出登录</view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import { resolveFileUrl } from '@/utils/request'

const userStore = useUserStore()
const cartStore = useCartStore()

const displayName = computed(() => {
  const info = userStore.userInfo
  if (!info) {
    return '未登录'
  }
  return info.nickname || info.username || '商城用户'
})

const avatarUrl = computed(() => {
  const info = userStore.userInfo
  if (info && info.avatar) {
    return resolveFileUrl(info.avatar)
  }
  return ''
})

function onHeaderTap() {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages-user/login/index' })
  }
}

function go(url) {
  if (!userStore.ensureLogin()) {
    return
  }
  uni.navigateTo({ url })
}

function onLogout() {
  uni.showModal({
    title: '提示',
    content: '确认退出登录？',
    success: async (res) => {
      if (res.confirm) {
        await userStore.logout()
        cartStore.reset()
        uni.showToast({ title: '已退出登录', icon: 'none' })
      }
    }
  })
}

onShow(() => {
  if (userStore.isLogin) {
    userStore.refreshUserInfo().catch(() => {})
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  padding: 48rpx 32rpx;
  background-color: #ffffff;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background-color: #eeeeee;
}

.avatar-empty {
  background-color: #e0e0e0;
}

.user-info {
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
}

.nickname {
  font-size: 34rpx;
  font-weight: bold;
  color: #333333;
}

.sub {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #999999;
}

.menu {
  margin-top: 20rpx;
  background-color: #ffffff;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 32rpx;
  border-bottom: 2rpx solid #f5f5f5;
  font-size: 28rpx;
}

.arrow {
  color: #cccccc;
}

.logout {
  margin: 40rpx 32rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 44rpx;
  background-color: #ffffff;
  color: #e93b3d;
  font-size: 28rpx;
}
</style>
