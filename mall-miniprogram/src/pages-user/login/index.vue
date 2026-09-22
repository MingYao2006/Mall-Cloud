<template>
  <view class="page">
    <view class="title">欢迎使用 Mall-Cloud</view>
    <view class="subtitle">登录后可同步购物车与订单</view>

    <!-- 微信一键登录 -->
    <view class="wx-login" @tap="onWxLogin">
      <text>微信一键登录</text>
    </view>

    <view class="divider">
      <view class="line" />
      <text class="divider-text">账号登录</text>
      <view class="line" />
    </view>

    <!-- 账号密码 / 验证码登录 -->
    <view class="form">
      <input
        v-model="form.username"
        class="input"
        :placeholder="mode === 'password' ? '用户名' : '手机号'"
        type="text"
      />
      <input
        v-if="mode === 'password'"
        v-model="form.password"
        class="input"
        placeholder="密码"
        password
      />
      <template v-else>
        <view class="code-row">
          <input v-model="form.code" class="input code-input" placeholder="验证码" type="number" />
          <view class="code-btn" :class="{ disabled: counting }" @tap="onSendCode">
            {{ counting ? countdown + 's' : '获取验证码' }}
          </view>
        </view>
      </template>
    </view>

    <view class="submit" @tap="onSubmit">登 录</view>

    <view class="switch-mode" @tap="toggleMode">
      {{ mode === 'password' ? '使用验证码登录' : '使用账号密码登录' }}
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const mode = ref('password')
const counting = ref(false)
const countdown = ref(0)

const form = reactive({
  username: '',
  password: '',
  phone: '',
  code: ''
})

let timer = null

function toggleMode() {
  mode.value = mode.value === 'password' ? 'code' : 'password'
}

function backToPrevious() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/index/index' })
  }
}

async function onSubmit() {
  try {
    if (mode.value === 'password') {
      if (!form.username || !form.password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
        return
      }
      await userStore.login({ username: form.username, password: form.password })
    } else {
      if (!form.phone || !form.code) {
        uni.showToast({ title: '请输入手机号和验证码', icon: 'none' })
        return
      }
      await userStore.loginByCode(form.phone, form.code)
    }
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(backToPrevious, 600)
  } catch (e) {
    // 错误提示已在 request 拦截层统一处理
  }
}

async function onWxLogin() {
  try {
    await userStore.wxLogin()
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(backToPrevious, 600)
  } catch (e) {
    // 后端业务错误已由 request 拦截层 toast，这里只兜住微信授权本身的失败
    if (e && e.wxAuthFailed) {
      uni.showToast({ title: e.message, icon: 'none' })
    }
  }
}

async function onSendCode() {
  if (counting.value) {
    return
  }
  if (!form.phone) {
    uni.showToast({ title: '请输入手机号', icon: 'none' })
    return
  }
  const { userApi } = await import('@/api/user')
  try {
    await userApi.sendCode(form.phone)
    uni.showToast({ title: '验证码已发送', icon: 'none' })
    startCountdown()
  } catch (e) {
    // 忽略，拦截层已提示
  }
}

function startCountdown() {
  counting.value = true
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
      counting.value = false
    }
  }, 1000)
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 120rpx 48rpx;
  background-color: #ffffff;
}

.title {
  font-size: 44rpx;
  font-weight: bold;
  color: #333333;
}

.subtitle {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #999999;
}

.wx-login {
  margin-top: 72rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 44rpx;
  background-color: #07c160;
  color: #ffffff;
  font-size: 30rpx;
}

.divider {
  margin: 60rpx 0 32rpx;
  display: flex;
  align-items: center;
}

.line {
  flex: 1;
  height: 2rpx;
  background-color: #eeeeee;
}

.divider-text {
  padding: 0 20rpx;
  font-size: 24rpx;
  color: #bbbbbb;
}

.form {
  display: flex;
  flex-direction: column;
}

.input {
  height: 88rpx;
  border-bottom: 2rpx solid #eeeeee;
  font-size: 28rpx;
}

.code-row {
  display: flex;
  align-items: center;
}

.code-input {
  flex: 1;
}

.code-btn {
  font-size: 26rpx;
  color: #e93b3d;
  padding-left: 24rpx;
}

.code-btn.disabled {
  color: #bbbbbb;
}

.submit {
  margin-top: 64rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 44rpx;
  background-color: #e93b3d;
  color: #ffffff;
  font-size: 30rpx;
}

.switch-mode {
  margin-top: 32rpx;
  text-align: center;
  font-size: 26rpx;
  color: #576b95;
}
</style>
