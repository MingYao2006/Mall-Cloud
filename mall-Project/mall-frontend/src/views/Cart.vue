<template>
  <div class="cart-page">
    <div class="cart-main" v-if="cartItems.length">
      <div class="cart-table">
        <div class="cart-hd">
          <el-checkbox v-model="allChecked" :indeterminate="indeterminate" @change="toggleAll" />
          <span>全选</span>
          <span class="hd-name">商品</span>
          <span class="hd-price">单价</span>
          <span class="hd-qty">数量</span>
          <span class="hd-subtotal">小计</span>
          <span class="hd-op">操作</span>
        </div>
        <div v-for="item in cartItems" :key="item.id" class="cart-row">
          <el-checkbox v-model="item._checked" />
          <img :src="item.productImage || 'https://placehold.co/80x80/eee/999'" class="row-img" />
          <span class="row-name" @click="$router.push(`/product/${item.productId}`)">{{ item.productName }}</span>
          <span class="row-price">¥{{ item.productPrice?.toFixed(2) }}</span>
          <el-input-number v-model="item.quantity" :min="1" :max="item.productStock||999" size="small" @change="qtyChange(item)" />
          <span class="row-subtotal">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</span>
          <el-button type="danger" link size="small" @click="removeOne(item)">删除</el-button>
        </div>
      </div>

      <!-- 底部结算栏 -->
      <div class="cart-footer">
        <el-checkbox v-model="allChecked" :indeterminate="indeterminate" @change="toggleAll">全选</el-checkbox>
        <el-button link @click="clearAll">清空购物车</el-button>
        <span class="total-label">已选 <b>{{ checkedCount }}</b> 件</span>
        <span class="total-price">合计：<b>¥{{ totalPrice }}</b></span>
        <el-button type="primary" size="large" :disabled="!checkedCount" @click="checkout">去结算</el-button>
      </div>
    </div>

    <el-empty v-else description="购物车空空如也">
      <el-button type="primary" @click="$router.push('/home')">去逛逛</el-button>
    </el-empty>

    <!-- 结算弹窗 -->
    <el-dialog v-model="dlg" title="填写订单信息" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人"><el-input v-model="form.receiverName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.receiverPhone" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.receiverAddress" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.note" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="primary" :loading="submiting" @click="confirmOrder">确认下单 (¥{{ totalPrice }})</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { cartApi, orderApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const cartItems = ref([])
const dlg = ref(false)
const submiting = ref(false)
const form = reactive({ receiverName: '', receiverPhone: '', receiverAddress: '', note: '' })

const checkedItems = computed(() => cartItems.value.filter(i => i._checked))
const checkedCount = computed(() => checkedItems.value.length)
const allChecked = computed({
  get: () => cartItems.value.length > 0 && cartItems.value.every(i => i._checked),
  set: () => {}
})
const indeterminate = computed(() => {
  const n = checkedCount.value
  return n > 0 && n < cartItems.value.length
})
const totalPrice = computed(() => checkedItems.value.reduce((s, i) => s + i.productPrice * i.quantity, 0).toFixed(2))

const load = async () => {
  try {
    const res = await cartApi.getList(userStore.userInfo.id)
    cartItems.value = (res.data || []).map(i => ({ ...i, _checked: i.selected === 1 }))
  } catch (e) { /* ignore */ }
}

const toggleAll = async (val) => {
  try {
    await cartApi.updateAllSelected(userStore.userInfo.id, val ? 1 : 0)
    cartItems.value.forEach(i => i._checked = val)
  } catch (e) { ElMessage.error('操作失败') }
}

const qtyChange = async (item) => {
  try { await cartApi.updateQuantity(userStore.userInfo.id, item.productId, item.quantity) }
  catch (e) { ElMessage.error('更新失败') }
}

const removeOne = async (item) => {
  try { await cartApi.delete(userStore.userInfo.id, item.productId); load(); ElMessage.success('已删除') }
  catch (e) { ElMessage.error('删除失败') }
}

const clearAll = async () => {
  try { await cartApi.clear(userStore.userInfo.id); cartItems.value = []; ElMessage.success('已清空') }
  catch (e) { ElMessage.error('清空失败') }
}

const checkout = () => {
  if (!checkedCount.value) { ElMessage.warning('请选择商品'); return }
  dlg.value = true
}

const confirmOrder = async () => {
  if (!form.receiverName || !form.receiverPhone || !form.receiverAddress) {
    ElMessage.warning('请填写完整收货信息'); return
  }
  submiting.value = true
  try {
    await orderApi.create({
      cartItemIds: checkedItems.value.map(i => i.id),
      receiverName: form.receiverName, receiverPhone: form.receiverPhone, receiverAddress: form.receiverAddress, note: form.note
    })
    ElMessage.success('下单成功')
    dlg.value = false
    router.push('/orders')
  } catch (e) { ElMessage.error('下单失败') }
  finally { submiting.value = false }
}

onMounted(() => { if (userStore.token) load() })
</script>

<style scoped>
.cart-page { max-width: 1200px; margin: 20px auto; }
.cart-main { background: #fff; border-radius: 8px; overflow: hidden; }

.cart-hd { display: grid; grid-template-columns: 40px 80px 1fr 100px 120px 120px 80px; align-items: center; padding: 14px 16px; background: #fafafa; font-size: 13px; color: var(--jd-text-light); border-bottom: 1px solid #f0f0f0; }
.cart-row { display: grid; grid-template-columns: 40px 80px 1fr 100px 120px 120px 80px; align-items: center; padding: 16px; border-bottom: 1px solid #f5f5f5; font-size: 14px; }
.row-img { width: 64px; height: 64px; object-fit: cover; border-radius: 4px; }
.row-name { cursor: pointer; color: var(--jd-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-name:hover { color: var(--jd-red); }
.row-price, .row-subtotal { color: var(--jd-red); font-weight: 600; }

.cart-footer { display: flex; align-items: center; gap: 16px; padding: 16px 20px; background: #fafafa; border-top: 2px solid var(--jd-red); }
.total-label { margin-left: auto; font-size: 14px; }
.total-price { font-size: 16px; }
.total-price b { font-size: 22px; color: var(--jd-red); }
</style>
