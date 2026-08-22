<template>
  <div class="profile-page">
    <div class="profile-side">
      <div class="avatar-box">
        <el-avatar :size="80" :src="user.avatar" />
        <div class="avatar-overlay" @click="triggerUpload">
          <el-icon><Camera /></el-icon>
          <span>更换头像</span>
        </div>
        <input type="file" ref="fileRef" hidden accept="image/*" @change="handleUpload" />
      </div>
      <h3>{{ user.nickname || user.username }}</h3>
      <p class="side-phone">{{ user.phone || '未绑定手机' }}</p>
      <el-menu :default-active="curTab" @select="curTab=$event" class="side-menu">
        <el-menu-item index="info"><el-icon><User /></el-icon> 个人信息</el-menu-item>
        <el-menu-item index="password"><el-icon><Lock /></el-icon> 修改密码</el-menu-item>
        <el-menu-item index="orders" @click="$router.push('/orders')"><el-icon><Tickets /></el-icon> 我的订单</el-menu-item>
      </el-menu>
    </div>

    <div class="profile-main">
      <!-- 个人信息 -->
      <div v-if="curTab==='info'" class="panel">
        <h4>个人信息</h4>
        <el-form :model="infoForm" label-width="80px">
          <el-form-item label="昵称"><el-input v-model="infoForm.nickname" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="infoForm.email" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="infoForm.phone" /></el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="infoForm.gender">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
              <el-radio :value="0">保密</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveInfo">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 密码 -->
      <div v-if="curTab==='password'" class="panel">
        <h4>修改密码</h4>
        <el-form :model="pwdForm" label-width="100px">
          <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
          <el-form-item label="确认新密码"><el-input v-model="pwdForm.confirm" type="password" show-password /></el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="pwdSaving" @click="savePwd">确认修改</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
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

const infoForm = reactive({ nickname: '', email: '', phone: '', gender: 0 })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const loadUser = async () => {
  try {
    const res = await userApi.getInfo()
    user.value = res.data || {}
    Object.assign(infoForm, { nickname: user.value.nickname, email: user.value.email, phone: user.value.phone, gender: user.value.gender || 0 })
  } catch (e) { /* ignore */ }
}

const saveInfo = async () => {
  saving.value = true
  try {
    await userApi.updateInfo({ nickname: infoForm.nickname, email: infoForm.email, phone: infoForm.phone, gender: infoForm.gender })
    await userStore.updateInfo()
    ElMessage.success('已保存')
  } catch (e) { ElMessage.error('保存失败') }
  finally { saving.value = false }
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
    await userStore.updateInfo()
    ElMessage.success('头像已更新')
  } catch (err) { ElMessage.error('上传失败') }
}

onMounted(() => loadUser())
</script>

<style scoped>
.profile-page { max-width: 1000px; margin: 20px auto; display: flex; gap: 24px; }
.profile-side { width: 240px; background: #fff; border-radius: 8px; padding: 30px 0; text-align: center; }
.avatar-box { position: relative; display: inline-block; cursor: pointer; border-radius: 50%; overflow: hidden; }
.avatar-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.4); display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; font-size: 12px; opacity: 0; transition: opacity .2s; border-radius: 50%; }
.avatar-box:hover .avatar-overlay { opacity: 1; }
.profile-side h3 { margin-top: 12px; font-size: 18px; }
.side-phone { font-size: 13px; color: var(--jd-text-lighter); margin-bottom: 16px; }
.side-menu { border-right: none; text-align: left; }

.profile-main { flex: 1; }
.panel { background: #fff; border-radius: 8px; padding: 24px; }
.panel h4 { font-size: 16px; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }
</style>
