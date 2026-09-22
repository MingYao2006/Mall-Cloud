<template>
  <div class="page-card">
    <div class="toolbar">
      <span style="font-size: 16px; font-weight: 600;">分类列表</span>
      <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 添加分类</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" row-key="id" :tree-props="{ children: 'children' }" stripe>
      <el-table-column prop="name" label="分类名称" min-width="200" />
      <el-table-column prop="level" label="层级" width="80">
        <template #default="{ row }">
          <el-tag :type="row.level === 1 ? '' : row.level === 2 ? 'success' : 'warning'" size="small">L{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <span :class="['status-tag', row.status === 1 ? 'success' : 'danger']">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <div style="white-space: nowrap;">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingCategory.id ? '编辑分类' : '添加分类'" width="480px">
      <el-form :model="editingCategory" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="editingCategory.name" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="editingCategory.parentId" placeholder="顶级分类(选填)" style="width: 100%" clearable>
            <el-option v-for="c in parentCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="层级">
          <el-input-number v-model="editingCategory.level" :min="1" :max="3" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editingCategory.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="editingCategory.icon" placeholder="图标URL(选填)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { categoryApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const parentCategories = ref([])
const dialogVisible = ref(false)
const editingCategory = reactive({})

async function loadList() {
  loading.value = true
  try {
    const res = await categoryApi.getList()
    tableData.value = res.data || []
    // 构建父分类选项（1级和2级可作为父分类）
    parentCategories.value = (res.data || []).filter(c => c.level < 3)
  } finally {
    loading.value = false
  }
}

function openDialog(row = null) {
  Object.keys(editingCategory).forEach(k => delete editingCategory[k])
  if (row) {
    Object.assign(editingCategory, row)
  } else {
    Object.assign(editingCategory, { name: '', parentId: null, level: 1, sort: 0, icon: '', status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (editingCategory.id) {
      await categoryApi.update(editingCategory)
    } else {
      await categoryApi.add(editingCategory)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadList()
  } catch (e) { /* handled */ }
}

async function toggleStatus(row) {
  try {
    await categoryApi.updateStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    await loadList()
  } catch (e) { /* handled */ }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除分类"${row.name}"吗？`, '提示', { type: 'warning' })
  try {
    await categoryApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch (e) { /* handled */ }
}

onMounted(() => loadList())
</script>
