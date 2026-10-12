<template>
  <div class="login-page">
    <!-- 背景光斑 -->
    <span class="orb orb-1"></span>
    <span class="orb orb-2"></span>
    <span class="orb orb-3"></span>

    <div class="login-card glass-specular">
      <!-- 左侧品牌区 -->
      <aside class="brand-side">
        <div class="brand-head">
          <div class="brand-mark">
            <el-icon :size="18"><Shop /></el-icon>
          </div>
          <span class="brand-name">商城管理后台</span>
        </div>

        <h1 class="brand-title">数据驱动的<br />商城运营台</h1>
        <p class="brand-desc">商品、订单、评论、优惠券一体化管理</p>

        <ul class="brand-points">
          <li v-for="point in highlights" :key="point.title">
            <span class="point-icon">
              <el-icon><component :is="point.icon" /></el-icon>
            </span>
            <span class="point-text">
              <strong>{{ point.title }}</strong>
              <em>{{ point.desc }}</em>
            </span>
          </li>
        </ul>
      </aside>

      <!-- 右侧表单区 -->
      <section class="form-side">
        <h2 class="form-title">管理员登录</h2>
        <p class="form-sub">请使用管理员账号登录</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="0"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button
            class="login-btn"
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>

        <p class="form-foot">登录状态由 JWT 维护，仅管理员（role=1）可访问</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { ElMessage } from 'element-plus'

const router = useRouter()
const store = useAdminStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const highlights = [
  { icon: 'Goods', title: '商品与分类', desc: '上下架、库存与分类维护' },
  { icon: 'List', title: '订单与售后', desc: '分页检索、状态流转与取消' },
  { icon: 'ChatDotSquare', title: '评论审核', desc: '待审评论一键通过 / 拒绝' }
]

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await store.login(form)
      ElMessage.success('登录成功')
      router.push('/')
    } catch (e) {
      // 错误已在 store/拦截器中处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  overflow: hidden;
}

/* ---------------- 背景光斑 ---------------- */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(64px);
  pointer-events: none;
}

.orb-1 {
  width: 420px;
  height: 420px;
  top: -140px;
  left: -110px;
  background: rgba(91, 111, 216, 0.42);
  animation: orb-drift 18s ease-in-out infinite;
}

.orb-2 {
  width: 380px;
  height: 380px;
  bottom: -150px;
  right: -90px;
  background: rgba(186, 140, 235, 0.38);
  animation: orb-drift 23s ease-in-out infinite reverse;
}

.orb-3 {
  width: 260px;
  height: 260px;
  top: 46%;
  right: 14%;
  background: rgba(110, 200, 226, 0.34);
  animation: orb-drift 15s ease-in-out infinite;
}

@keyframes orb-drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(22px, -26px, 0) scale(1.08);
  }
}

/* ---------------- 玻璃卡片 ---------------- */
.login-card {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  max-width: 920px;
  min-height: 540px;
  border-radius: 26px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.62);
  -webkit-backdrop-filter: blur(22px) saturate(180%);
  backdrop-filter: blur(22px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow: 0 24px 70px rgba(31, 42, 71, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.95);
  animation: card-in 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translate3d(0, 24px, 0) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
}

/* ---------------- 左侧品牌区 ---------------- */
.brand-side {
  position: relative;
  width: 46%;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(150deg, rgba(91, 111, 216, 0.14), rgba(186, 140, 235, 0.12));
  border-right: 1px solid rgba(255, 255, 255, 0.6);
}

.brand-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 26px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #5b6fd8, #8f5fd0);
  box-shadow: 0 6px 16px rgba(91, 111, 216, 0.35);
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2a47;
  letter-spacing: 0.4px;
}

.brand-title {
  font-size: 30px;
  line-height: 1.32;
  font-weight: 700;
  color: #1f2a47;
  margin-bottom: 12px;
}

.brand-desc {
  font-size: 14px;
  color: #6b769a;
  margin-bottom: 30px;
}

.brand-points {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.brand-points li {
  display: flex;
  align-items: center;
  gap: 12px;
}

.point-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5b6fd8;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 12px rgba(31, 42, 71, 0.08);
}

.point-text {
  display: flex;
  flex-direction: column;
}

.point-text strong {
  font-size: 14px;
  color: #1f2a47;
  font-weight: 600;
}

.point-text em {
  font-size: 12px;
  font-style: normal;
  color: #8892ad;
}

/* ---------------- 右侧表单区 ---------------- */
.form-side {
  flex: 1;
  padding: 56px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-title {
  font-size: 22px;
  font-weight: 700;
  color: #1f2a47;
  margin-bottom: 6px;
}

.form-sub {
  font-size: 13px;
  color: #8892ad;
  margin-bottom: 28px;
}

.form-side :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.9) inset;
  border-radius: 12px;
}

.form-side :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--brand-500) inset;
}

.login-btn {
  width: 100%;
  margin-top: 6px;
  height: 44px;
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #5b6fd8, #4356c4);
  border: none;
  box-shadow: 0 10px 24px rgba(91, 111, 216, 0.32);
}

.login-btn:hover {
  background: linear-gradient(135deg, #6b7ee0, #4a5dcb);
}

.form-foot {
  margin-top: 22px;
  font-size: 12px;
  color: #a8b0c8;
  text-align: center;
}

@media (max-width: 860px) {
  .brand-side {
    display: none;
  }

  .login-card {
    max-width: 420px;
  }

  .form-side {
    padding: 44px 28px;
  }
}
</style>