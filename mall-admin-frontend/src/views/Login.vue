<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="brand-side">
        <div class="brand-head">
          <span class="brand-seal" aria-hidden="true">集</span>
          <div>
            <strong>商城运营台</strong>
            <span>运营管理端</span>
          </div>
        </div>

        <div class="brand-copy">
          <h1 class="brand-title">商品、订单、评价，<br />一处打理。</h1>
          <p class="brand-desc">从今天的营业数据，到每一条待审评论，进入后一次处理。</p>

          <ul class="brand-points">
            <li v-for="point in highlights" :key="point.title">
              <span class="point-icon">
                <el-icon :size="15"><component :is="point.icon" /></el-icon>
              </span>
              <span class="point-text">
                <strong>{{ point.title }}</strong>
                <em>{{ point.desc }}</em>
              </span>
            </li>
          </ul>
        </div>
        <span class="brand-watermark" aria-hidden="true">集</span>
      </aside>

      <section class="form-side">
        <div class="form-head">
          <span class="form-mark" aria-hidden="true"></span>
          <h2>管理员登录</h2>
          <p>使用管理员账号进入运营台。</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="用户名" prop="username" class="ledger-field">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password" class="ledger-field">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              autocomplete="current-password"
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
            登录运营台
          </el-button>
        </el-form>

        <p class="form-foot">登录状态由 JWT 维护。仅管理员（role=1）可进入运营台。</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
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
  { icon: 'ChatDotSquare', title: '评论审核', desc: '待审评论一键通过或拒绝' }
]

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    try {
      await store.login(form)
      ElMessage.success('登录成功')
      router.push('/')
    } catch (e) {
      // 错误已在 store / 拦截器中处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 32px 24px;
}

.login-page::before {
  content: '';
  position: fixed;
  inset: 0 0 auto;
  height: 5px;
  background: var(--gold);
}

.login-shell {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  width: 100%;
  max-width: 1040px;
  min-height: 600px;
  overflow: hidden;
  border: 1px solid var(--line-strong);
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 10px 10px 0 rgba(22, 58, 46, 0.1);
  animation: rise-in 0.5s cubic-bezier(0.2, 0.7, 0.3, 1) both;
}

.brand-side {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 40px 44px;
  color: var(--paper);
  background: var(--pine-900);
}

.brand-side::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  background: var(--gold);
}

.brand-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-head strong {
  display: block;
  color: var(--paper-warm);
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
}

.brand-head span:not(.brand-seal) {
  display: block;
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.42);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.brand-copy {
  position: relative;
  z-index: 1;
  max-width: 370px;
  margin: auto 0;
}

.brand-title {
  color: var(--paper-warm);
  font-family: var(--font-display);
  font-size: clamp(28px, 2.6vw, 36px);
  font-weight: 600;
  letter-spacing: 0.01em;
  line-height: 1.35;
}

.brand-desc {
  margin-top: 14px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
  line-height: 1.8;
}

.brand-points {
  margin-top: 34px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  list-style: none;
}

.brand-points li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.point-icon {
  display: grid;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  place-items: center;
  border: 1px solid rgba(228, 166, 47, 0.4);
  border-radius: 2px;
  color: var(--gold);
}

.point-text strong {
  display: block;
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  font-weight: 600;
}

.point-text em {
  display: block;
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 10px;
  font-style: normal;
}

.brand-watermark {
  position: absolute;
  right: -40px;
  bottom: -70px;
  color: rgba(255, 255, 255, 0.035);
  font-family: var(--font-display);
  font-size: 300px;
  line-height: 1;
  pointer-events: none;
  user-select: none;
}

.form-side {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 52px;
}

.form-head h2 {
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 600;
}

.form-head p {
  margin-top: 6px;
  color: var(--sage);
  font-size: 12px;
}

.form-mark {
  display: block;
  width: 26px;
  height: 3px;
  margin-bottom: 18px;
  background: var(--gold);
}

.form-side :deep(.el-form) {
  margin-top: 30px;
}

.form-side :deep(.ledger-field) {
  margin-bottom: 22px;
}

.form-side :deep(.el-form-item__label) {
  padding-bottom: 6px;
  color: var(--pine-700);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  line-height: 1.2;
}

.form-side :deep(.el-input__wrapper) {
  padding: 8px 0;
  border-bottom: 1px solid var(--line-strong);
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.form-side :deep(.el-input__wrapper:hover) {
  border-bottom-color: var(--pine-600);
  box-shadow: none;
}

.form-side :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: var(--pine-800);
  box-shadow: 0 1px 0 0 var(--pine-800);
}

.form-side :deep(.el-input__inner) {
  height: 30px;
  color: var(--pine-800);
  font-size: 14px;
}

.form-side :deep(.el-input__inner::placeholder) {
  color: var(--sage-light);
}

.form-side :deep(.el-form-item.is-error .el-input__wrapper) {
  border-bottom-color: var(--vermilion);
  box-shadow: 0 1px 0 0 var(--vermilion);
}

.form-side :deep(.el-form-item__error) {
  padding-top: 4px;
  color: var(--vermilion);
  font-size: 11px;
}

.login-btn {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  border-radius: 2px;
  box-shadow: 3px 3px 0 rgba(22, 58, 46, 0.14);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.form-foot {
  margin-top: 22px;
  color: var(--sage-light);
  font-size: 10px;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 520px;
    min-height: 0;
  }

  .brand-side {
    padding: 26px 28px 24px;
  }

  .brand-copy {
    margin: 28px 0 0;
  }

  .brand-title {
    font-size: 28px;
  }

  .brand-desc {
    margin-top: 10px;
  }

  .brand-points {
    display: none;
  }

  .brand-watermark {
    right: -30px;
    bottom: -55px;
    font-size: 220px;
  }

  .form-side {
    padding: 34px 28px 36px;
  }
}

@media (max-width: 520px) {
  .login-page {
    padding: 20px 14px;
  }

  .brand-side {
    padding: 22px 22px 20px;
  }

  .brand-title {
    font-size: 24px;
  }

  .form-side {
    padding: 28px 22px 30px;
  }
}
</style>
