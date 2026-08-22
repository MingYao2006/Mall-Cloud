<template>
  <div class="login-page">
    <div class="login-card">
      <h2>欢迎登录</h2>
      <p class="sub">MALL 品质电商平台</p>
      
      <!-- 登录方式切换 -->
      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 密码登录 -->
        <el-tab-pane label="密码登录" name="password">
          <el-form :model="form" :rules="rules" ref="formRef">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password size="large" @keyup.enter="doLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" @click="doLogin" class="login-btn">登 录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 验证码登录 -->
        <el-tab-pane label="验证码登录" name="code">
          <el-form :model="codeForm" :rules="codeRules" ref="codeFormRef">
            <el-form-item prop="phone">
              <el-input v-model="codeForm.phone" placeholder="请输入手机号" prefix-icon="Phone" size="large" />
            </el-form-item>
            <el-form-item prop="code">
              <div class="code-input">
                <el-input v-model="codeForm.code" placeholder="请输入验证码" prefix-icon="Message" size="large" @keyup.enter="doCodeLogin" />
                <el-button type="primary" size="large" :disabled="countdown > 0" @click="sendCode" class="code-btn">
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="codeLoading" @click="doCodeLogin" class="login-btn">登 录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      
      <div class="extra">
        <span>还没有账号？<router-link to="/register">立即注册</router-link></span>
        <router-link to="/home">返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const codeLoading = ref(false)
const formRef = ref(null)
const codeFormRef = ref(null)
const activeTab = ref('password')
const countdown = ref(0)

const form = reactive({ username: '', password: '' })
const codeForm = reactive({ phone: '', code: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
}

const codeRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// 密码登录
const doLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}

// 发送验证码
const sendCode = async () => {
  if (!codeFormRef.value) return
  
  // 只验证手机号
  try {
    await codeFormRef.value.validateField('phone')
  } catch (e) {
    return
  }
  
  try {
    await userApi.sendCode(codeForm.phone)
    ElMessage.success('验证码已发送')
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (e) {
    // 错误已在拦截器处理
  }
}

// 验证码登录
const doCodeLogin = async () => {
  const valid = await codeFormRef.value.validate().catch(() => false)
  if (!valid) return
  codeLoading.value = true
  try {
    const res = await userApi.loginByCode(codeForm.phone, codeForm.code)
    // 保存token和用户信息（与密码登录保持一致）
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    codeLoading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; justify-content: center; align-items: center; min-height: 80vh; background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1440 320"><path fill="%23e1251b" fill-opacity="0.05" d="M0,160L48,176C96,192,192,224,288,208C384,192,480,128,576,122.7C672,117,768,171,864,192C960,213,1056,203,1152,181.3C1248,160,1344,128,1392,112L1440,96L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"></path></svg>') no-repeat bottom/100% auto; }
.login-card { background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 8px 40px rgba(0,0,0,0.12); width: 420px; }
.login-card h2 { font-size: 28px; color: #333; margin-bottom: 4px; }
.sub { color: #999; margin-bottom: 20px; font-size: 14px; }
.login-tabs { margin-bottom: 10px; }
.login-btn { width: 100%; height: 46px; font-size: 16px; }
.code-input { display: flex; gap: 10px; width: 100%; }
.code-input .el-input { flex: 1; }
.code-btn { width: 120px; }
.extra { display: flex; justify-content: space-between; font-size: 13px; color: #666; margin-top: 10px; }
.extra a { color: var(--jd-red); }
</style>
