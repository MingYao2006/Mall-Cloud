<template>
  <div class="page-card">
    <div class="toolbar">
      <span style="font-size: 16px; font-weight: 600;">用户管理</span>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
      当前用户管理功能仅支持查看管理员自身信息。完整的用户列表/禁用/启用等管理操作需在 mall-user-service 微服务侧补充管理接口后扩展。
    </el-alert>

    <el-descriptions :column="2" border v-if="userInfo">
      <el-descriptions-item label="用户ID">{{ userInfo.id }}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ userInfo.nickname }}</el-descriptions-item>
      <el-descriptions-item label="角色">
        <el-tag type="danger" size="small">管理员</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="手机号">{{ userInfo.phone || '-' }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ userInfo.email || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <span :class="['status-tag', userInfo.status === 1 ? 'success' : 'danger']">
          {{ userInfo.status === 1 ? '正常' : '禁用' }}
        </span>
      </el-descriptions-item>
      <el-descriptions-item label="注册时间">{{ userInfo.createTime }}</el-descriptions-item>
    </el-descriptions>

    <div style="margin-top: 16px;">
      <el-button @click="loadInfo"><el-icon><Refresh /></el-icon> 刷新</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '@/api'

const userInfo = ref(null)

async function loadInfo() {
  try {
    const res = await userApi.getInfo()
    userInfo.value = res.data
  } catch (e) { /* handled */ }
}

onMounted(() => loadInfo())
</script>
