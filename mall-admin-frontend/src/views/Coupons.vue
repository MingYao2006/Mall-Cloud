<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" placeholder="搜索优惠券名称" style="width: 180px" clearable
                  @clear="handleSearch" @keyup.enter="handleSearch" />
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 130px">
          <el-option label="满减券" :value="1" />
          <el-option label="折扣券" :value="2" />
          <el-option label="无门槛券" :value="3" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="未上架" :value="0" />
          <el-option label="已上架" :value="1" />
          <el-option label="已停用" :value="2" />
        </el-select>
        <el-button @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 新建优惠券</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="优惠券名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ typeText(row.type) }}</template>
      </el-table-column>
      <el-table-column label="面额/折扣" width="100">
        <template #default="{ row }">
          {{ row.type === 2 ? row.value + ' 折' : '¥' + row.value }}
        </template>
      </el-table-column>
      <el-table-column label="使用门槛" width="100">
        <template #default="{ row }">
          {{ Number(row.minAmount) > 0 ? '满¥' + row.minAmount : '无门槛' }}
        </template>
      </el-table-column>
      <el-table-column label="库存(剩余/总)" width="130">
        <template #default="{ row }">
          <span :style="{ color: Number(row.availableStock) <= 0 ? '#f56c6c' : 'inherit' }">
            {{ row.availableStock }}
          </span>
          / {{ row.totalStock }}
        </template>
      </el-table-column>
      <el-table-column label="已领取" width="80">
        <template #default="{ row }">{{ claimedCount(row) }}</template>
      </el-table-column>
      <el-table-column prop="limitPerUser" label="每人限领" width="90" />
      <el-table-column label="有效期" width="200">
        <template #default="{ row }">
          <div>{{ fmtTime(row.startTime) }}</div>
          <div style="color: #909399;">至 {{ fmtTime(row.endTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <span :class="['status-tag', row.displayStatusType || 'info']">
            {{ row.displayStatusText || '未知' }}
          </span>
          <div style="font-size: 12px; color: #909399; margin-top: 2px;">
            {{ row.status === 1 ? '已上架' : row.status === 2 ? '已停用' : '未上架' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-dropdown style="margin: 0 12px 0 12px;" @command="cmd => handleStatusCommand(row, cmd)">
            <el-button link type="primary">状态<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="on" :disabled="row.status === 1 || row.status === 2">上架</el-dropdown-item>
                <el-dropdown-item command="off" :disabled="row.status === 0">下架</el-dropdown-item>
                <el-dropdown-item command="stop" :disabled="row.status === 2" divided>停用</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button link @click="openStockDialog(row)">库存</el-button>
          <el-button link @click="openRecordsDialog(row)">明细</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadPage"
      />
    </div>

    <!-- 新建 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑优惠券' : '新建优惠券'" width="620px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit placeholder="例如：双十一满 100 减 20" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
            <el-radio :value="3">无门槛券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.type === 2 ? '折扣数' : '面额'" prop="value">
          <el-input-number v-model="form.value" :min="0.01" :max="form.type === 2 ? 9.9 : 99999"
                           :precision="2" :step="form.type === 2 ? 0.1 : 1" style="width: 200px" />
          <span class="form-tip">{{ form.type === 2 ? '填折扣数，8 表示 8 折（必须小于 10）' : '单位：元' }}</span>
        </el-form-item>
        <el-form-item label="使用门槛" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" style="width: 200px" />
          <span class="form-tip">0 表示无门槛</span>
        </el-form-item>
        <el-form-item label="每人限领" prop="limitPerUser">
          <el-input-number v-model="form.limitPerUser" :min="1" :max="99" style="width: 200px" />
          <span class="form-tip">张</span>
        </el-form-item>
        <el-form-item label="总库存" prop="totalStock">
          <el-input-number v-model="form.totalStock" :min="0" :disabled="!!form.id" style="width: 200px" />
          <span class="form-tip">{{ form.id ? '改库存请用列表里的「库存」按钮（会同步清理抢券库存）' : '创建后仍可调整' }}</span>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="选择开始时间" style="width: 200px" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="选择结束时间" style="width: 200px" />
        </el-form-item>
        <el-alert v-if="!form.id" type="info" :closable="false" show-icon
                  title="新建的优惠券默认是「未上架」，确认信息无误后再用「状态 - 上架」。" />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 调整库存弹窗 -->
    <el-dialog v-model="stockVisible" title="调整总库存" width="440px">
      <el-form label-width="90px">
        <el-form-item label="优惠券">{{ stockForm.name }}</el-form-item>
        <el-form-item label="已领取">
          <span>{{ stockForm.used }} 张（总库存不能小于它）</span>
        </el-form-item>
        <el-form-item label="总库存">
          <el-input-number v-model="stockForm.totalStock" :min="stockForm.used" style="width: 100%" />
        </el-form-item>
        <el-form-item label="剩余库存">
          <span>调整后：{{ Math.max(0, Number(stockForm.totalStock || 0) - stockForm.used) }} 张</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleStockSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 领取明细弹窗 -->
    <el-dialog v-model="recordsVisible" :title="'领取明细 - ' + recordsCoupon.name" width="780px">
      <el-row :gutter="12" style="margin-bottom: 12px;">
        <el-col :span="6">
          <div class="stat-card"><div class="stat-label">已领取</div><div class="stat-value">{{ stats.claimed || 0 }}</div></div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card"><div class="stat-label">未使用</div><div class="stat-value">{{ stats.unused || 0 }}</div></div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card"><div class="stat-label">已使用</div><div class="stat-value">{{ stats.used || 0 }}</div></div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card"><div class="stat-label">已过期</div><div class="stat-value">{{ stats.expired || 0 }}</div></div>
        </el-col>
      </el-row>
      <el-table :data="records" v-loading="recordsLoading" stripe size="small" max-height="360">
        <el-table-column prop="id" label="记录ID" width="90" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span :class="['status-tag', recordStatusType(row.status)]">{{ recordStatusText(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="领取时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.obtainTime) }}</template>
        </el-table-column>
        <el-table-column label="使用时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.useTime) }}</template>
        </el-table-column>
        <el-table-column label="过期时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.expireTime) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="recordsVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { couponApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const query = reactive({ keyword: '', type: null, status: null })

const formRef = ref()
const dialogVisible = ref(false)
const form = reactive({})

const stockVisible = ref(false)
const stockForm = reactive({ id: null, name: '', used: 0, totalStock: 0 })

const recordsVisible = ref(false)
const recordsLoading = ref(false)
const records = ref([])
const stats = ref({})
const recordsCoupon = reactive({ id: null, name: '' })

const formRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择优惠券类型', trigger: 'change' }],
  value: [{
    required: true,
    validator: (rule, value, callback) => {
      if (value === null || value === undefined || value === '') return callback(new Error('请输入面额/折扣'))
      if (Number(value) <= 0) return callback(new Error('面额必须大于 0'))
      if (form.type === 2 && Number(value) >= 10) return callback(new Error('折扣券请填折扣数，如 8 表示 8 折'))
      callback()
    },
    trigger: 'blur'
  }],
  limitPerUser: [{ required: true, message: '请输入每人限领数量', trigger: 'blur' }],
  totalStock: [{ required: true, message: '请输入总库存', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{
    required: true,
    validator: (rule, value, callback) => {
      if (!value) return callback(new Error('请选择结束时间'))
      // 统一是 YYYY-MM-DDTHH:mm:ss，按字符串比较即可，避免时区带来的偏差
      if (form.startTime && value <= form.startTime) {
        return callback(new Error('结束时间必须晚于开始时间'))
      }
      callback()
    },
    trigger: 'change'
  }]
}

function pad(n) {
  return String(n).padStart(2, '0')
}

function isoLocal(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/** 后端可能返回 ISO（带 T）也可能返回空格分隔，统一成 date-picker 认得的格式 */
function toIso(value) {
  return value ? String(value).replace(' ', 'T').slice(0, 19) : ''
}

function fmtTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-'
}

function typeText(type) {
  return type === 1 ? '满减券' : type === 2 ? '折扣券' : type === 3 ? '无门槛券' : '未知'
}

function claimedCount(row) {
  return Number(row.totalStock || 0) - Number(row.availableStock || 0)
}

function recordStatusText(status) {
  return status === 1 ? '已使用' : status === 2 ? '已过期' : '未使用'
}

function recordStatusType(status) {
  return status === 1 ? 'success' : status === 2 ? 'info' : 'warning'
}

async function loadPage() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (query.keyword) params.keyword = query.keyword
    if (query.type !== null && query.type !== undefined && query.type !== '') params.type = query.type
    if (query.status !== null && query.status !== undefined && query.status !== '') params.status = query.status
    const res = await couponApi.getPage(params)
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pageNum.value = 1
  await loadPage()
}

function resetQuery() {
  query.keyword = ''
  query.type = null
  query.status = null
  pageNum.value = 1
  loadPage()
}

function openDialog(row = null) {
  Object.keys(form).forEach(k => delete form[k])
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      type: row.type,
      value: row.value,
      minAmount: row.minAmount ?? 0,
      limitPerUser: row.limitPerUser,
      totalStock: row.totalStock,
      startTime: toIso(row.startTime),
      endTime: toIso(row.endTime)
    })
  } else {
    const start = new Date()
    const end = new Date(start.getTime() + 7 * 24 * 3600 * 1000)
    Object.assign(form, {
      id: null,
      name: '',
      type: 1,
      value: 10,
      minAmount: 0,
      limitPerUser: 1,
      totalStock: 100,
      startTime: isoLocal(start),
      endTime: isoLocal(end)
    })
  }
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSave() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return // 校验不通过，el-form 已在表单项上给出提示
  }
  // 只提交后端认的字段：库存另有 /stock 接口，status 由「状态」菜单管理
  const payload = {
    name: form.name,
    type: form.type,
    value: form.value,
    minAmount: form.minAmount,
    limitPerUser: form.limitPerUser,
    totalStock: form.totalStock,
    startTime: form.startTime,
    endTime: form.endTime
  }
  saving.value = true
  try {
    if (form.id) {
      payload.id = form.id
      await couponApi.update(payload)
    } else {
      await couponApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadPage()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleStatusCommand(row, command) {
  if (command === 'stop') {
    try {
      await ElMessageBox.confirm(`停用后「${row.name}」不能再上架，确定停用吗？`, '提示', { type: 'warning' })
    } catch (e) {
      return
    }
  }
  const status = command === 'on' ? 1 : command === 'off' ? 0 : 2
  try {
    await couponApi.updateStatus(row.id, status)
    ElMessage.success(command === 'on' ? '已上架' : command === 'off' ? '已下架' : '已停用')
    await loadPage()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

function openStockDialog(row) {
  stockForm.id = row.id
  stockForm.name = row.name
  stockForm.used = claimedCount(row)
  stockForm.totalStock = Number(row.totalStock || 0)
  stockVisible.value = true
}

async function handleStockSubmit() {
  if (Number(stockForm.totalStock) < stockForm.used) {
    ElMessage.warning(`总库存不能小于已领取数量 ${stockForm.used}`)
    return
  }
  saving.value = true
  try {
    await couponApi.adjustStock(stockForm.id, stockForm.totalStock)
    ElMessage.success('库存已更新')
    stockVisible.value = false
    await loadPage()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function openRecordsDialog(row) {
  recordsCoupon.id = row.id
  recordsCoupon.name = row.name
  stats.value = {}
  records.value = []
  recordsVisible.value = true
  recordsLoading.value = true
  try {
    const [statsRes, recordsRes] = await Promise.all([
      couponApi.getStats(row.id),
      couponApi.getRecords(row.id)
    ])
    stats.value = statsRes.data || {}
    records.value = recordsRes.data || []
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    recordsLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除优惠券「${row.name}」吗？已有领取记录的券不能删除。`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await couponApi.remove(row.id)
    ElMessage.success('删除成功')
    await loadPage()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

onMounted(() => loadPage())
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.stat-card {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px 12px;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
}
</style>