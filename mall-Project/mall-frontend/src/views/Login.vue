<script setup>
import {ref, reactive, onUnmounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {useUserStore} from '@/stores/user'
import {authApi} from '@/api/compat'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const props = defineProps({initialMode: {type: String, default: 'login'}})
const mode = ref(props.initialMode === 'register' ? 'register' : 'login')
const loading = ref(false)

const loginForm = reactive({username: '', password: ''})
const registerForm = reactive({username: '', password: '', confirm: ''})

const codeForm = reactive({phone: '', code: ''})
const counting = ref(false)
const countdown = ref(0)
let codeTimer = null

const highlights = [
  {icon: 'Goods', title: '精选好物', desc: '数码、家居、零食全品类覆盖'},
  {icon: 'Van', title: '极速配送', desc: '当天下单，次日送达'},
  {icon: 'Lock', title: '安全支付', desc: 'JWT 鉴权 + 分布式事务保障'}
]

function switchMode(next) {
  if (mode.value === next) return
  mode.value = next
}

async function doLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login({username: loginForm.username, password: loginForm.password})
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch (e) {
    /* 错误已在拦截器提示 */
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (registerForm.password !== registerForm.confirm) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await authApi.register({
      username: registerForm.username,
      password: registerForm.password
    })
    // 后端 register 不返回 token，注册成功后自动登录一次
    await userStore.login({username: registerForm.username, password: registerForm.password})
    ElMessage.success('注册成功，已自动登录，可在个人中心绑定手机号')
    router.push(route.query.redirect || '/')
  } catch (e) {
    /* 错误已在拦截器提示 */
  } finally {
    loading.value = false
  }
}

async function onSendCode() {
  if (counting.value) return
  if (!/^1[3-9]\d{9}$/.test(codeForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    await authApi.sendCode(codeForm.phone)
    ElMessage.success('验证码已发送')
    counting.value = true
    countdown.value = 60
    codeTimer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        clearInterval(codeTimer)
        codeTimer = null
        counting.value = false
      }
    }, 1000)
  } catch (e) {
    /* 错误已在拦截器提示 */
  }
}

async function doCodeLogin() {
  if (!/^1[3-9]\d{9}$/.test(codeForm.phone) || !codeForm.code) {
    ElMessage.warning('请输入手机号和验证码')
    return
  }
  loading.value = true
  try {
    await userStore.loginByCode(codeForm.phone, codeForm.code)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch (e) {
    /* 错误已在拦截器提示 */
  } finally {
    loading.value = false
  }
}

    onUnmounted(() => {
      if (codeTimer) clearInterval(codeTimer)
    })
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 左侧品牌展示 -->
      <aside class="brand-side">
        <div class="brand-head">
          <span class="brand-seal">集</span>
          <span class="brand-copy">
            <span class="brand-name">集物商店</span>
            <span class="brand-en">JIWU GOODS</span>
          </span>
        </div>

        <p class="brand-kicker">线上百货 / 二十八号铺</p>
        <h2 class="brand-title">进店坐坐，<br/>把喜欢的先收好。</h2>
        <p class="brand-sub">登录后，购物车、订单与优惠券都会替你留着。</p>

        <ul class="brand-list">
          <li v-for="h in highlights" :key="h.title">
            <span class="hl-icon">
              <el-icon :size="15"><component :is="h.icon"/></el-icon>
            </span>
            <div>
              <p class="hl-title">{{ h.title }}</p>
              <p class="hl-desc">{{ h.desc }}</p>
            </div>
          </li>
        </ul>
      </aside>

      <!-- 右侧表单 -->
      <section class="form-side">
        <router-link class="back-home" to="/home">← 返回商店</router-link>
        <header class="form-head">
          <h3>{{ mode === 'login' ? '欢迎回来' : (mode === 'register' ? '创建账号' : '手机验证码登录') }}</h3>
          <p>
            {{ mode === 'login' ? '登录后可同步购物车与订单' : (mode === 'register' ? '注册只需一步，立即开始选购' : '未绑定手机号请先用账号密码登录') }}
          </p>
        </header>

        <div class="tabs">
          <span
              class="tab-slider"
              :style="{ transform: mode === 'register' ? 'translateX(100%)' : 'translateX(0)' }"
          ></span>
          <button
              class="tab"
              :class="{ active: mode === 'login' }"
              type="button"
              @click="switchMode('login')"
          >
            登录
          </button>
          <button
              class="tab"
              :class="{ active: mode === 'register' }"
              type="button"
              @click="switchMode('register')"
          >
            注册
          </button>
        </div>

        <transition name="swap" mode="out-in">
          <form v-if="mode === 'login'" key="login" class="form" @submit.prevent="doLogin">
            <el-input v-model="loginForm.username" placeholder="用户名" size="large">
              <template #prefix>
                <el-icon>
                  <User/>
                </el-icon>
              </template>
            </el-input>

            <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                size="large"
                show-password
            >
              <template #prefix>
                <el-icon>
                  <Lock/>
                </el-icon>
              </template>
            </el-input>

            <button class="btn-brand submit" type="submit" :disabled="loading">
              {{ loading ? '登录中…' : '登 录' }}
            </button>
            <p class="tip"><span @click="switchMode('code')">手机验证码登录</span></p>
          </form>

          <form v-else-if="mode === 'code'" key="code" class="form" @submit.prevent="doCodeLogin">
            <el-input v-model="codeForm.phone" placeholder="手机号" size="large" maxlength="11">
              <template #prefix>
                <el-icon>
                  <Iphone/>
                </el-icon>
              </template>
            </el-input>

            <div class="code-row">
              <el-input v-model="codeForm.code" placeholder="验证码" size="large" maxlength="6">
                <template #prefix>
                  <el-icon>
                    <Message/>
                  </el-icon>
                </template>
              </el-input>
              <button class="btn-brand code-btn" type="button" :disabled="counting" @click="onSendCode">
                {{ counting ? countdown + 's' : '获取验证码' }}
              </button>
            </div>

            <button class="btn-brand submit" type="submit" :disabled="loading">
              {{ loading ? '登录中…' : '登录' }}
            </button>
            <p class="tip"><span @click="switchMode('login')">返回账号密码登录</span></p>
          </form>

          <form v-else-if="mode === 'register'" key="register" class="form" @submit.prevent="doRegister">
            <el-input v-model="registerForm.username" placeholder="用户名" size="large">
              <template #prefix>
                <el-icon>
                  <User/>
                </el-icon>
              </template>
            </el-input>

            <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码"
                size="large"
                show-password
            >
              <template #prefix>
                <el-icon>
                  <Lock/>
                </el-icon>
              </template>
            </el-input>

            <el-input
                v-model="registerForm.confirm"
                type="password"
                placeholder="确认密码"
                size="large"
                show-password
            >
              <template #prefix>
                <el-icon>
                  <Lock/>
                </el-icon>
              </template>
            </el-input>

            <button class="btn-brand submit" type="submit" :disabled="loading">
              {{ loading ? '注册中…' : '注 册' }}
            </button>
          </form>
        </transition>

        <p class="tip">
          {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
          <span @click="switchMode(mode === 'login' ? 'register' : 'login')">
            {{ mode === 'login' ? '立即注册' : '去登录' }}
          </span>
        </p>

        <p class="demo-tip">体验账号：zhangsan / 123456</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.login-card {
  display: flex;
  width: 100%;
  max-width: 940px;
  min-height: 560px;
  border-radius: 26px;
  overflow: hidden;
  background: var(--surface);
  box-shadow: 0 24px 70px rgba(31, 42, 71, 0.14);
  animation: card-in 0.75s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translate3d(0, 26px, 0) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
}

/* ---------------- 左侧品牌区 ---------------- */
.brand-side {
  position: relative;
  width: 44%;
  padding: 52px 42px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(150deg, #eef3ff 0%, #e4ecfd 45%, #f2e8fa 100%);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(46px);
}

.orb-1 {
  width: 270px;
  height: 270px;
  top: -110px;
  left: -90px;
  background: #a5c0ef;
  opacity: 0.82;
  animation: orb-drift 16s ease-in-out infinite;
}

.orb-2 {
  width: 230px;
  height: 230px;
  bottom: -100px;
  right: -70px;
  background: #c3b0ec;
  opacity: 0.75;
  animation: orb-drift 21s ease-in-out infinite reverse;
}

.orb-3 {
  width: 150px;
  height: 150px;
  top: 42%;
  right: 6%;
  background: #f1c1de;
  opacity: 0.6;
  animation: orb-drift 13s ease-in-out infinite;
}

@keyframes orb-drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(18px, -22px, 0) scale(1.1);
  }
}

.brand-head {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 30px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  background: var(--brand-gradient);
  box-shadow: 0 6px 16px rgba(91, 111, 216, 0.32);
}

.brand-name {
  font-size: 17px;
  font-weight: 500;
  color: var(--text-title);
}

.brand-title {
  position: relative;
  font-size: 27px;
  font-weight: 500;
  line-height: 1.35;
  letter-spacing: -0.4px;
  color: var(--text-title);
  margin-bottom: 12px;
}

.brand-sub {
  position: relative;
  font-size: 14px;
  line-height: 1.75;
  color: var(--text-muted);
  margin-bottom: 34px;
}

.brand-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.brand-list li {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hl-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--brand-500);
}

.hl-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-title);
  margin-bottom: 2px;
}

.hl-desc {
  font-size: 12px;
  color: var(--text-hint);
}

/* ---------------- 右侧表单区 ---------------- */
.form-side {
  flex: 1;
  padding: 56px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-head {
  margin-bottom: 26px;
}

.form-head h3 {
  font-size: 23px;
  font-weight: 500;
  letter-spacing: -0.3px;
  color: var(--text-title);
  margin-bottom: 8px;
}

.form-head p {
  font-size: 13px;
  color: var(--text-muted);
}

/* ---------------- 登录 / 注册 切换 ---------------- */
.tabs {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 4px;
  margin-bottom: 26px;
  border-radius: var(--radius-pill);
  background: var(--surface-sunken);
}

.tab-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  border-radius: var(--radius-pill);
  background: var(--brand-gradient);
  box-shadow: 0 4px 12px rgba(91, 111, 216, 0.28);
  transition: transform 0.35s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.tab {
  position: relative;
  z-index: 1;
  padding: 9px 0;
  border: none;
  background: transparent;
  font-size: 14px;
  font-family: inherit;
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.25s ease;
}

.tab.active {
  color: #fff;
}

/* ---------------- 表单 ---------------- */
.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form :deep(.el-input__wrapper) {
  padding: 7px 16px;
  border-radius: 14px;
  background: var(--surface-sunken);
  box-shadow: none;
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.form :deep(.el-input__wrapper:hover) {
  background: #eef1f9;
}

.form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1.5px var(--brand-400) inset;
}

.submit {
  width: 100%;
  margin-top: 10px;
  padding: 13px 26px;
  font-size: 15px;
}

.submit:disabled {
  opacity: 0.62;
  cursor: not-allowed;
}

.code-row {
  display: flex;
  gap: 10px;
}

.code-row .el-input {
  flex: 1;
}

.code-btn {
  flex: 0 0 118px;
  padding: 0;
  font-size: 14px;
}

.tip {
  margin-top: 22px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}

.tip span {
  margin-left: 2px;
  color: var(--brand-500);
  cursor: pointer;
}

.tip span:hover {
  text-decoration: underline;
}

.demo-tip {
  margin-top: 10px;
  text-align: center;
  font-size: 12px;
  color: var(--text-hint);
}

/* ---------------- 表单切换过渡 ---------------- */
.swap-enter-active,
.swap-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.swap-enter-from {
  opacity: 0;
  transform: translate3d(14px, 0, 0);
}

.swap-leave-to {
  opacity: 0;
  transform: translate3d(-14px, 0, 0);
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 860px) {
  .login-card {
    max-width: 440px;
    min-height: auto;
  }

  .brand-side {
    display: none;
  }

  .form-side {
    padding: 44px 32px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card,
  .orb {
    animation: none;
  }
}
</style>

<style scoped>
/* 集物商店 · 登录登记台 */
.login-page {
  padding: 42px 24px;
  background:
    linear-gradient(90deg, rgba(22, 58, 46, 0.045) 1px, transparent 1px) 0 0 / 25% 100%,
    transparent;
}

.login-card {
  max-width: 980px;
  min-height: 590px;
  border: 1px solid var(--line-strong);
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 10px 10px 0 rgba(22, 58, 46, 0.12);
  animation: register-in 0.62s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

@keyframes register-in {
  from {
    opacity: 0;
    transform: translate3d(0, 18px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

.brand-side {
  position: relative;
  flex: 0 0 43%;
  overflow: hidden;
  padding: 44px 42px;
  background: var(--pine-800);
  color: var(--paper);
}

.brand-side::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--gold);
}

.brand-side::after {
  content: '集';
  position: absolute;
  right: -22px;
  bottom: -68px;
  color: rgba(251, 252, 248, 0.055);
  font-family: var(--font-display);
  font-size: 260px;
  font-weight: 700;
  line-height: 1;
  pointer-events: none;
}

.brand-head {
  position: relative;
  z-index: 1;
  margin-bottom: 52px;
}

.brand-seal {
  display: grid;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  place-items: center;
  border: 1px solid var(--gold);
  border-radius: 2px;
  background: rgba(251, 252, 248, 0.06);
  box-shadow: 3px 3px 0 rgba(228, 166, 47, 0.3);
  color: var(--paper);
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-name {
  color: var(--paper);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.brand-en {
  margin-top: 4px;
  color: rgba(251, 252, 248, 0.42);
  font-family: var(--font-num);
  font-size: 9px;
  letter-spacing: 0.18em;
}

.brand-kicker {
  position: relative;
  z-index: 1;
  margin-bottom: 14px;
  color: var(--gold);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.brand-title {
  position: relative;
  z-index: 1;
  margin-bottom: 14px;
  color: var(--paper);
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 600;
  line-height: 1.45;
  letter-spacing: 0.02em;
}

.brand-sub {
  position: relative;
  z-index: 1;
  max-width: 290px;
  margin-bottom: 30px;
  color: rgba(251, 252, 248, 0.6);
  font-size: 12px;
  line-height: 1.9;
}

.brand-list {
  position: relative;
  z-index: 1;
  gap: 0;
  padding-top: 8px;
  border-top: 1px solid rgba(251, 252, 248, 0.16);
}

.brand-list li {
  align-items: flex-start;
  padding: 13px 0;
  border-bottom: 1px solid rgba(251, 252, 248, 0.1);
}

.hl-icon {
  width: 30px;
  height: 30px;
  border: 1px solid rgba(228, 166, 47, 0.45);
  border-radius: 2px;
  background: transparent;
  color: var(--gold);
}

.hl-title {
  color: var(--paper);
  font-size: 12px;
  font-weight: 500;
}

.hl-desc {
  color: rgba(251, 252, 248, 0.46);
  font-size: 10px;
}

.form-side {
  padding: 45px 52px;
  background: var(--paper);
}

.back-home {
  align-self: flex-start;
  margin-bottom: 28px;
  padding-bottom: 3px;
  border-bottom: 1px solid var(--line);
  color: var(--sage);
  font-size: 11px;
  transition: color 0.18s ease, border-color 0.18s ease;
}

.back-home:hover {
  border-color: var(--vermilion);
  color: var(--vermilion);
}

.form-head {
  margin-bottom: 22px;
}

.form-head h3 {
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 29px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.form-head p {
  margin-top: 5px;
  color: var(--sage);
  font-size: 12px;
}

.tabs {
  grid-template-columns: 1fr 1fr;
  gap: 0;
  padding: 0;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--line);
  border-radius: 0;
  background: transparent;
}

.tab-slider {
  display: none;
}

.tab {
  padding: 11px 0 10px;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  background: transparent;
  color: var(--sage);
  font-size: 13px;
  text-align: left;
  transition: color 0.18s ease, border-color 0.18s ease;
}

.tab.active {
  border-bottom-color: var(--gold);
  background: transparent;
  color: var(--pine-800);
  font-weight: 600;
}

.form :deep(.el-input__wrapper) {
  min-height: 44px;
  padding: 7px 13px;
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 0 0 0 1px var(--line) inset;
}

.form :deep(.el-input__wrapper:hover) {
  background: var(--paper);
  box-shadow: 0 0 0 1px var(--line-strong) inset;
}

.form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--pine-600) inset;
}

.submit {
  min-height: 45px;
  margin-top: 8px;
  padding: 12px 26px;
  border-radius: 2px;
  font-size: 14px;
}

.tip {
  margin-top: 19px;
  color: var(--sage);
  font-size: 12px;
}

.tip span {
  color: var(--pine-700);
}

.tip span:hover {
  color: var(--vermilion);
}

.demo-tip {
  margin-top: 14px;
  padding-top: 13px;
  border-top: 1px dotted var(--line-strong);
  color: var(--text-hint);
  font-size: 10px;
}

@media (max-width: 860px) {
  .login-card {
    max-width: 470px;
    min-height: auto;
  }

  .form-side {
    padding: 34px 30px;
  }

  .back-home {
    margin-bottom: 20px;
  }
}

@media (max-width: 520px) {
  .login-page {
    padding: 22px 14px;
  }

  .form-side {
    padding: 28px 22px;
  }

  .form-head h3 {
    font-size: 25px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card {
    animation: none;
  }
}
</style>
