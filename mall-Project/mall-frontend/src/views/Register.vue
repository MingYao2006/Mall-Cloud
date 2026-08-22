<template>
  <div class="register-page">
    <div class="register-card">
      <h2>创建账号</h2>
      <p class="sub">加入 MALL，享受品质购物</p>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" prefix-icon="Lock" show-password size="large" />
        </el-form-item>
        <el-form-item prop="confirm">
          <el-input v-model="form.confirm" type="password" placeholder="确认密码" prefix-icon="Lock" show-password size="large" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" prefix-icon="UserFilled" size="large" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号（选填）" prefix-icon="Phone" size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="doRegister" class="reg-btn">注 册</el-button>
        </el-form-item>
      </el-form>
      <div class="extra">
        <span>已有账号？<router-link to="/login">立即登录</router-link></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', password: '', confirm: '', nickname: '', phone: '' })

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirm: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }],
}

const doRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userApi.register({ username: form.username, password: form.password, nickname: form.nickname, phone: form.phone })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error('注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page { display: flex; justify-content: center; align-items: center; min-height: 80vh; background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1440 320"><path fill="%23e1251b" fill-opacity="0.05" d="M0,160L48,176C96,192,192,224,288,208C384,192,480,128,576,122.7C672,117,768,171,864,192C960,213,1056,203,1152,181.3C1248,160,1344,128,1392,112L1440,96L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"></path></svg>') no-repeat bottom/100% auto; }
.register-card { background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 8px 40px rgba(0,0,0,0.12); width: 420px; }
.register-card h2 { font-size: 28px; color: #333; margin-bottom: 4px; }
.sub { color: #999; margin-bottom: 24px; font-size: 14px; }
.reg-btn { width: 100%; height: 46px; font-size: 16px; }
.extra { font-size: 13px; color: #666; text-align: center; }
.extra a { color: var(--jd-red); }
</style>
