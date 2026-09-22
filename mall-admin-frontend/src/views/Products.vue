<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchKeyword" placeholder="搜索商品名称" style="width: 180px" clearable @clear="handleSearch" @keyup.enter="handleSearch" />
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 130px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 110px">
          <el-option label="已上架" :value="1" />
          <el-option label="未上架" :value="0" />
        </el-select>
        <el-input-number v-model="query.minPrice" :min="0" :precision="2" placeholder="最低价" style="width: 130px" />
        <el-input-number v-model="query.maxPrice" :min="0" :precision="2" placeholder="最高价" style="width: 130px" />
        <el-select v-model="query.orderBy" placeholder="默认排序" clearable style="width: 110px">
          <el-option label="价格" value="price" />
          <el-option label="销量" value="sales" />
          <el-option label="创建时间" value="createTime" />
        </el-select>
        <el-button @click="handleSearch"><el-icon><Search /></el-icon> 搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 添加商品</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="mainImage" label="图片" width="70">
        <template #default="{ row }">
          <img v-if="row.mainImage" :src="row.mainImage" class="table-thumb" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="brand" label="品牌" width="100" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="sales" label="销量" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <span :class="['status-tag', row.status === 1 ? 'success' : 'warning']">
            {{ row.status === 1 ? '已上架' : '未上架' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" style="margin-left: 4px" @click="openStockDialog(row)">库存</el-button>
          <el-button size="small" style="margin-left: 4px" :type="row.status === 1 ? 'warning' : 'success'" @click="togglePublish(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" style="margin-left: 4px" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingProduct.id ? '编辑商品' : '添加商品'" width="600px">
      <el-form :model="editingProduct" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="editingProduct.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editingProduct.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="editingProduct.brand" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="editingProduct.price" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="editingProduct.originalPrice" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="editingProduct.stock" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="主图">
          <el-upload
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/*"
          >
            <img v-if="editingProduct.mainImage" :src="editingProduct.mainImage" class="table-thumb" />
            <el-button v-else size="small"><el-icon><Upload /></el-icon> 上传图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="详情">
          <el-input v-model="editingProduct.detail" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 库存调整弹窗 -->
    <el-dialog v-model="stockDialogVisible" title="调整库存" width="400px">
      <el-form label-width="80px">
        <el-form-item label="商品">
          <span>{{ stockForm.name }}</span>
        </el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="stockForm.type">
            <el-radio :value="1">增加</el-radio>
            <el-radio :value="-1">减少</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="stockForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStockSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { productApi, categoryApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const categories = ref([])
const dialogVisible = ref(false)
const editingProduct = reactive({})
const query = reactive({
  categoryId: null,
  status: null,
  minPrice: null,
  maxPrice: null,
  orderBy: ''
})

async function loadPage() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined,
      categoryId: query.categoryId,
      status: query.status,
      minPrice: query.minPrice,
      maxPrice: query.maxPrice,
      orderBy: query.orderBy
    }
    const res = await productApi.getPageByCondition(params)
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

function openDialog(row = null) {
  Object.keys(editingProduct).forEach(k => delete editingProduct[k])
  if (row) {
    Object.assign(editingProduct, row)
  } else {
    Object.assign(editingProduct, {
      name: '', categoryId: null, brand: '', price: 0, originalPrice: 0,
      stock: 0, mainImage: '', detail: '', status: 0
    })
  }
  dialogVisible.value = true
}

async function handleUpload({ file }) {
  try {
    const res = await productApi.uploadImage(file)
    editingProduct.mainImage = res.data
    ElMessage.success('图片上传成功')
  } catch (e) { /* handled */ }
}

async function handleSave() {
  try {
    if (editingProduct.id) {
      await productApi.update(editingProduct)
    } else {
      await productApi.add(editingProduct)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadPage()
  } catch (e) { /* handled */ }
}

async function togglePublish(row) {
  try {
    if (row.status === 1) {
      await productApi.unpublish(row.id)
      ElMessage.success('已下架')
    } else {
      await productApi.publish(row.id)
      ElMessage.success('已上架')
    }
    await loadPage()
  } catch (e) { /* handled */ }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除商品"${row.name}"吗？`, '提示', { type: 'warning' })
  try {
    await productApi.delete(row.id)
    ElMessage.success('删除成功')
    await loadPage()
  } catch (e) { /* handled */ }
}

function resetQuery() {
  searchKeyword.value = ''
  query.categoryId = null
  query.status = null
  query.minPrice = null
  query.maxPrice = null
  query.orderBy = ''
  pageNum.value = 1
  loadPage()
}

const stockDialogVisible = ref(false)
const stockForm = reactive({ id: null, name: '', type: 1, quantity: 1 })

function openStockDialog(row) {
  stockForm.id = row.id
  stockForm.name = row.name
  stockForm.type = 1
  stockForm.quantity = 1
  stockDialogVisible.value = true
}

async function handleStockSubmit() {
  if (stockForm.quantity < 1) {
    ElMessage.warning('数量必须大于0')
    return
  }
  try {
    if (stockForm.type === 1) {
      await productApi.increaseStock(stockForm.id, stockForm.quantity)
    } else {
      await productApi.decreaseStock(stockForm.id, stockForm.quantity)
    }
    ElMessage.success('库存调整成功')
    stockDialogVisible.value = false
    await loadPage()
  } catch (e) { /* handled */ }
}

onMounted(async () => {
  await loadPage()
  try {
    const res = await categoryApi.getList()
    categories.value = res.data || []
  } catch (e) { /* ignore */ }
})
</script>
