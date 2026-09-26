<template>
  <div class="profile-page">
    <div class="container">
      <h1 class="page-title">个人信息</h1>
      
      <div class="profile-layout">
        <!-- 左侧边栏 -->
        <div class="profile-sidebar">
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <el-avatar :size="80" :src="user.avatar" />
              <div class="avatar-overlay" @click="triggerUpload">
                <el-icon :size="20"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
              <input type="file" ref="fileRef" hidden accept="image/*" @change="handleUpload" />
            </div>
            <h3 class="user-name">{{ user.nickname || user.username }}</h3>
            <p class="user-phone">{{ user.phone ? maskPhone(user.phone) : '未绑定手机' }}</p>
          </div>
          
          <nav class="side-nav">
            <a :class="{ active: curTab==='info' }" @click="curTab='info'">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </a>
            <a :class="{ active: curTab==='password' }" @click="curTab='password'">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </a>
            <a @click="$router.push('/orders')">
              <el-icon><Tickets /></el-icon>
              <span>我的订单</span>
            </a>
          </nav>
        </div>

        <!-- 右侧主内容 -->
        <div class="profile-content">
          <!-- 个人信息 -->
          <div v-if="curTab==='info'" class="content-panel">
            <h2 class="panel-title">个人信息</h2>
            <el-form :model="infoForm" label-width="100px" class="profile-form">
              <el-form-item label="昵称">
                <el-input v-model="infoForm.nickname" placeholder="请输入昵称" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="手机号">
                <template v-if="user.phone">
                  <span class="phone-bound">{{ maskPhone(user.phone) }}</span>
                  <el-button link type="primary" @click="openBindDialog">更换</el-button>
                </template>
                <el-button v-else type="primary" plain @click="openBindDialog">绑定手机号</el-button>
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="infoForm.gender">
                  <el-radio :value="1">男</el-radio>
                  <el-radio :value="2">女</el-radio>
                  <el-radio :value="0">保密</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="saveInfo" size="large">
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 修改密码 -->
          <div v-if="curTab==='password'" class="content-panel">
            <h2 class="panel-title">修改密码</h2>
            <el-form :model="pwdForm" label-width="120px" class="profile-form">
              <el-form-item label="原密码">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="请再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="pwdSaving" @click="savePwd" size="large">
                  确认修改
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>

    <!-- 绑定手机号弹窗 -->
    <el-dialog v-model="bindDialogVisible" title="绑定手机号" width="420px" destroy-on-close>
      <el-form label-width="70px">
        <el-form-item label="手机号">
          <el-input v-model="bindForm.phone" maxlength="11" placeholder="请输入11位手机号" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="code-row">
            <el-input v-model="bindForm.code" maxlength="6" placeholder="请输入验证码" />
            <el-button type="primary" plain :disabled="bindCountdown > 0" @click="sendBindCode">
              {{ bindCountdown > 0 ? bindCountdown + 's' : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSaving" @click="confirmBind">确认绑定</el-button>
      </template>
    </el-dialog>
    </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Camera, User, Lock, Tickets } from '@element-plus/icons-vue'

const userStore = useUserStore()
const curTab = ref('info')
const saving = ref(false)
const pwdSaving = ref(false)
const fileRef = ref(null)
const user = ref({})

const infoForm = reactive({ nickname: '', email: '', gender: 0 })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

// 绑定手机号弹窗状态
const bindDialogVisible = ref(false)
const bindSaving = ref(false)
const bindCountdown = ref(0)
let bindTimer = null
const bindForm = reactive({ phone: '', code: '' })

const maskPhone = (phone) => phone ? phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') : ''

const loadUser = async () => {
  try {
    const res = await userApi.getInfo()
    user.value = res.data || {}
    Object.assign(infoForm, { nickname: user.value.nickname, email: user.value.email, gender: user.value.gender || 0 })
  } catch (e) { /* ignore */ }
}

const saveInfo = async () => {
  saving.value = true
  try {
    await userStore.updateUserInfo({ nickname: infoForm.nickname, email: infoForm.email, gender: infoForm.gender })
    ElMessage.success('已保存')
  } catch (e) { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const openBindDialog = () => {
  bindForm.phone = ''
  bindForm.code = ''
  bindDialogVisible.value = true
}

const sendBindCode = async () => {
  if (!/^1[3-9]\d{9}$/.test(bindForm.phone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  try {
    await userApi.sendCode(bindForm.phone)
    ElMessage.success('验证码已发送')
    bindCountdown.value = 60
    bindTimer = setInterval(() => {
      bindCountdown.value -= 1
      if (bindCountdown.value <= 0) clearInterval(bindTimer)
    }, 1000)
  } catch (e) { /* 错误已在拦截器提示 */ }
}

const confirmBind = async () => {
  if (!/^1[3-9]\d{9}$/.test(bindForm.phone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  if (!bindForm.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  bindSaving.value = true
  try {
    await userStore.bindPhone(bindForm.phone, bindForm.code)
    ElMessage.success('手机号绑定成功')
    bindDialogVisible.value = false
    await loadUser()
  } catch (e) { /* 错误已在拦截器提示 */ }
  finally { bindSaving.value = false }
}

const savePwd = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) { ElMessage.warning('请填写完整'); return }
  if (pwdForm.newPassword !== pwdForm.confirm) { ElMessage.warning('两次密码不一致'); return }
  pwdSaving.value = true
  try {
    await userApi.updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码已修改')
    Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirm: '' })
  } catch (e) { ElMessage.error('修改失败') }
  finally { pwdSaving.value = false }
}

const triggerUpload = () => fileRef.value?.click()

const handleUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const res = await userApi.uploadAvatar(file)
    user.value.avatar = res.data
    await userStore.refreshUserInfo()
    ElMessage.success('头像已更新')
  } catch (err) { ElMessage.error('上传失败') }
}

onMounted(() => loadUser())

onUnmounted(() => {
  if (bindTimer) clearInterval(bindTimer)
})
</script>

<style scoped>
.profile-page {
  padding: var(--spacing-xl) 0;
  min-height: calc(100vh - 200px);
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: var(--spacing-lg);
  color: var(--color-primary);
}

.profile-layout {
  display: flex;
  gap: var(--spacing-lg);
}

/* 左侧边栏 */
.profile-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  height: fit-content;
}

.avatar-section {
  text-align: center;
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--spacing-md);
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-white);
  font-size: 12px;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  border-radius: 50%;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin: var(--spacing-sm) 0 4px;
  color: var(--color-text);
}

.user-phone {
  font-size: 13px;
  color: var(--color-text-lighter);
}

/* 侧边导航 */
.side-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.side-nav a {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 12px var(--spacing-sm);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--color-text);
  cursor: pointer;
  transition: all 0.2s;
}

.side-nav a:hover {
  background: var(--color-bg);
  color: var(--color-accent);
}

.side-nav a.active {
  background: var(--color-accent-light);
  color: var(--color-accent);
  font-weight: 600;
}

/* 右侧主内容 */
.profile-content {
  flex: 1;
}

.content-panel {
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.panel-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-primary);
}

.profile-form {
  max-width: 600px;
}

.profile-form .el-form-item {
  margin-bottom: 24px;
}

.profile-form .el-input {
  max-width: 400px;
}

.phone-bound {
  margin-right: 8px;
  font-size: 14px;
  color: var(--color-text);
}

.code-row {
  display: flex;
  gap: 10px;
  width: 400px;
  max-width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.code-row .el-button {
  flex-shrink: 0;
}

/* 响应式 */
@media (max-width: 968px) {
  .profile-layout {
    flex-direction: column;
  }
  
  .profile-sidebar {
    width: 100%;
  }
}
</style>
