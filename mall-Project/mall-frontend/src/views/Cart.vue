<template>
  <div class="cart-page">
    <div class="container">
      <h1 class="page-title">购物车</h1>
      
      <div class="cart-main" v-if="cartItems.length">
        <!-- 表头 -->
        <div class="cart-header">
          <el-checkbox v-model="allChecked" :indeterminate="indeterminate" @change="toggleAll" />
          <span class="col-product">商品</span>
          <span class="col-price">单价</span>
          <span class="col-qty">数量</span>
          <span class="col-subtotal">小计</span>
          <span class="col-action">操作</span>
        </div>

        <!-- 商品列表 -->
        <div class="cart-list">
          <div v-for="item in cartItems" :key="item.id" class="cart-item">
            <el-checkbox v-model="item._checked" />
            <div class="item-product">
              <img :src="item.productImage || 'https://placehold.co/80x80/eee/999'" class="item-img" />
              <span class="item-name" @click="$router.push(`/product/${item.productId}`)">{{ item.productName }}</span>
            </div>
            <span class="item-price price">¥{{ item.productPrice?.toFixed(2) }}</span>
            <div class="item-qty">
              <el-input-number v-model="item.quantity" 
                              :min="1" 
                              :max="item.productStock||999" 
                              size="small" 
                              @change="qtyChange(item)" />
            </div>
            <span class="item-subtotal price">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</span>
            <el-button type="danger" link size="small" @click="removeOne(item)">删除</el-button>
          </div>
        </div>

        <!-- 底部结算栏 -->
        <div class="cart-footer">
          <div class="footer-left">
            <el-checkbox v-model="allChecked" :indeterminate="indeterminate" @change="toggleAll">全选</el-checkbox>
            <el-button link @click="clearAll">清空购物车</el-button>
          </div>
          <div class="footer-right">
            <span class="total-info">已选 <b class="price">{{ checkedCount }}</b> 件</span>
            <span class="total-price">合计：<b class="price price-lg">¥{{ totalPrice }}</b></span>
            <el-button type="primary" size="large" :disabled="!checkedCount" @click="checkout">
              去结算
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="购物车空空如也">
        <el-button type="primary" @click="$router.push('/home')">去逛逛</el-button>
      </el-empty>

      <!-- 结算弹窗 -->
      <el-dialog v-model="dlg" title="填写订单信息" width="520px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="收货人"><el-input v-model="form.receiverName" placeholder="请输入收货人姓名" /></el-form-item>
          <el-form-item label="电话">
            <el-input v-model="form.receiverPhone" 
                      placeholder="请输入11位手机号" 
                      maxlength="11" />
          </el-form-item>
          <el-form-item label="地址"><el-input v-model="form.receiverAddress" placeholder="请输入详细地址" /></el-form-item>
          
          <!-- 优惠券选择 -->
          <el-form-item label="优惠券">
            <div class="coupon-selector" @click="showCouponDialog = true">
              <div v-if="selectedCoupon" class="selected-coupon">
                <div class="coupon-detail">
                  <div class="coupon-header">
                    <span class="coupon-name">{{ selectedCoupon.couponName }}</span>
                    <span class="coupon-value price">-¥{{ selectedCoupon.couponValue }}</span>
                  </div>
                  <div class="coupon-condition" v-if="selectedCoupon.minAmount > 0">
                    满¥{{ selectedCoupon.minAmount }}可用
                  </div>
                  <div class="coupon-condition" v-else>
                    无门槛优惠券
                  </div>
                </div>
                <el-icon class="arrow-icon"><ArrowRight /></el-icon>
              </div>
              <div v-else class="no-coupon">
                <span>暂无优惠券</span>
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </el-form-item>
          
          <el-form-item label="备注"><el-input v-model="form.note" type="textarea" placeholder="可选" /></el-form-item>
          
          <!-- 价格明细 -->
          <div class="price-summary">
            <div class="price-row">
              <span>商品总价</span>
              <span class="price">¥{{ totalPrice }}</span>
            </div>
            <div v-if="selectedCoupon" class="price-row discount">
              <span>优惠券抵扣</span>
              <span class="price">-¥{{ selectedCoupon.value }}</span>
            </div>
            <div class="price-row total">
              <span>应付金额</span>
              <span class="price price-lg">¥{{ finalPrice }}</span>
            </div>
          </div>
        </el-form>
        <template #footer>
          <el-button @click="dlg=false">取消</el-button>
          <el-button type="primary" :loading="submiting" @click="confirmOrder">确认下单 (¥{{ finalPrice }})</el-button>
        </template>
      </el-dialog>
      
      <!-- 优惠券选择弹窗 -->
      <el-dialog v-model="showCouponDialog" title="选择优惠券" width="500px">
        <div class="coupon-list">
          <div v-if="availableCoupons.length === 0" class="empty-coupons">
            <el-empty description="暂无可用优惠券" />
          </div>
          <div v-for="coupon in availableCoupons" :key="coupon.id" 
               class="coupon-item"
               :class="{ selected: selectedCoupon?.id === coupon.id }"
               @click="selectCoupon(coupon)">
            <div class="coupon-left">
              <span class="amount price">¥{{ coupon.couponValue }}</span>
              <span class="condition" v-if="coupon.minAmount > 0">满¥{{ coupon.minAmount }}可用</span>
              <span class="condition" v-else>无门槛</span>
            </div>
            <div class="coupon-right">
              <p class="name">{{ coupon.couponName }}</p>
              <p class="time">有效期至：{{ formatTime(coupon.expireTime) }}</p>
            </div>
            <el-icon v-if="selectedCoupon?.id === coupon.id" class="check-icon" :size="20"><Check /></el-icon>
          </div>
        </div>
        <template #footer>
          <el-button @click="showCouponDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmCoupon">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { cartApi, orderApi, couponApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { ArrowRight, Check } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const cartItems = ref([])
const dlg = ref(false)
const submiting = ref(false)
const form = reactive({ receiverName: '', receiverPhone: '', receiverAddress: '', note: '' })

// 优惠券相关
const showCouponDialog = ref(false)
const availableCoupons = ref([])
const selectedCoupon = ref(null)

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

// 计算最终价格（减去优惠券）
const finalPrice = computed(() => {
  const total = parseFloat(totalPrice.value)
  if (selectedCoupon.value) {
    return Math.max(0, total - selectedCoupon.value.couponValue).toFixed(2)
  }
  return total.toFixed(2)
})

const load = async () => {
  try {
    const res = await cartApi.getList()
    cartItems.value = (res.data || []).map(i => ({ ...i, _checked: i.selected === 1 }))
  } catch (e) { /* ignore */ }
}

// 加载可用优惠券
const loadAvailableCoupons = async () => {
  try {
    const res = await couponApi.getMy(0) // 0=未使用
    let coupons = res.data || []
    
    // 计算商品总价
    const total = checkedItems.value.reduce((s, i) => s + i.productPrice * i.quantity, 0)
    
    console.log('后端返回的优惠券数据:', coupons)
    
    // 过滤出满足条件的优惠券
    availableCoupons.value = coupons.filter(c => {
      // 检查是否满足最低消费
      const minAmount = c.minAmount || 0
      if (minAmount > 0 && total > 0) {
        return total >= minAmount
      }
      return true
    }).map(c => ({
      // 统一字段名
      id: c.id,
      couponId: c.couponId,
      couponName: c.couponName || c.name || '优惠券',
      couponValue: parseFloat(c.couponValue || c.value || 0),
      minAmount: parseFloat(c.minAmount || 0),
      expireTime: c.expireTime
    }))
    
    console.log('可用优惠券:', availableCoupons.value)
  } catch (e) {
    console.error('加载优惠券失败', e)
  }
}

const selectCoupon = (coupon) => {
  selectedCoupon.value = coupon
}

const confirmCoupon = () => {
  showCouponDialog.value = false
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const toggleAll = async (val) => {
  try {
    await cartApi.updateAllSelected(val ? 1 : 0)
    cartItems.value.forEach(i => i._checked = val)
  } catch (e) { ElMessage.error('操作失败') }
}

const qtyChange = async (item) => {
  try { await cartApi.updateQuantity(item.productId, item.quantity) }
  catch (e) { ElMessage.error('更新失败') }
}

const removeOne = async (item) => {
  try { await cartApi.delete(item.productId); load(); ElMessage.success('已删除') }
  catch (e) { ElMessage.error('删除失败') }
}

const clearAll = async () => {
  try { await cartApi.clear(); cartItems.value = []; ElMessage.success('已清空') }
  catch (e) { ElMessage.error('清空失败') }
}

const checkout = async () => {
  if (!checkedCount.value) { ElMessage.warning('请选择商品'); return }
  
  // 加载可用优惠券
  await loadAvailableCoupons()
  
  dlg.value = true
}

const confirmOrder = async () => {
  if (!form.receiverName || !form.receiverPhone || !form.receiverAddress) {
    ElMessage.warning('请填写完整收货信息'); return
  }
  
  // 手机号验证：必须是11位数字，且符合中国大陆手机号格式
  if (!/^1[3-9]\d{9}$/.test(form.receiverPhone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  
  submiting.value = true
  try {
    const orderData = {
      cartItemIds: checkedItems.value.map(i => i.id),
      receiverName: form.receiverName, 
      receiverPhone: form.receiverPhone, 
      receiverAddress: form.receiverAddress, 
      note: form.note
    }
    
    // 如果选择了优惠券，添加到订单数据
    if (selectedCoupon.value) {
      orderData.couponId = selectedCoupon.value.id
      orderData.couponUserId = selectedCoupon.value.id
    }
    
    await orderApi.create(orderData)
    ElMessage.success('下单成功')
    dlg.value = false
    selectedCoupon.value = null // 清空选择的优惠券
    router.push('/orders')
  } catch (e) { ElMessage.error('下单失败') }
  finally { submiting.value = false }
}

onMounted(() => { if (userStore.token) load() })
</script>

<style scoped>
.cart-page {
  padding: var(--spacing-xl) 0;
  min-height: calc(100vh - 200px);
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: var(--spacing-lg);
  color: var(--color-primary);
}

.cart-main {
  background: var(--color-white);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

/* 表头 */
.cart-header {
  display: grid;
  grid-template-columns: 50px 2fr 120px 140px 140px 80px;
  align-items: center;
  padding: 16px 24px;
  background: var(--color-bg);
  font-size: 13px;
  color: var(--color-text-light);
  border-bottom: 1px solid var(--color-border);
}

.col-product { padding-left: 12px; }
.col-price { text-align: center; }
.col-qty { text-align: center; }
.col-subtotal { text-align: center; }
.col-action { text-align: center; }

/* 商品列表 */
.cart-list {
  border-bottom: 1px solid var(--color-border);
}

.cart-item {
  display: grid;
  grid-template-columns: 50px 2fr 120px 140px 140px 80px;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid var(--color-border);
  transition: background 0.2s;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item:hover {
  background: var(--color-bg);
}

.item-product {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-left: 12px;
}

.item-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.item-name {
  font-size: 15px;
  color: var(--color-text);
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

.item-name:hover {
  color: var(--color-accent);
}

.item-price {
  text-align: center;
  font-size: 16px;
}

.item-qty {
  display: flex;
  justify-content: center;
}

.item-subtotal {
  text-align: center;
  font-size: 18px;
}

/* 底部结算栏 */
.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: var(--color-bg);
  border-top: 2px solid var(--color-accent);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.total-info {
  font-size: 14px;
  color: var(--color-text-light);
}

.total-info b {
  margin: 0 4px;
}

.total-price {
  font-size: 16px;
  color: var(--color-text);
}

/* 优惠券选择器 */
.coupon-selector {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;
}

.coupon-selector:hover {
  border-color: var(--color-accent);
}

.selected-coupon {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.coupon-detail {
  flex: 1;
}

.coupon-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.coupon-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.coupon-condition {
  font-size: 12px;
  color: var(--color-text-lighter);
}

.arrow-icon {
  color: var(--color-text-lighter);
  margin-left: 8px;
}

.no-coupon {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--color-text-lighter);
  font-size: 14px;
}

/* 价格明细 */
.price-summary {
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  margin-top: 8px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  color: var(--color-text);
}

.price-row.discount {
  color: var(--color-accent);
}

.price-row.total {
  border-top: 1px solid var(--color-border);
  padding-top: 12px;
  margin-top: 8px;
  font-weight: 600;
  font-size: 16px;
}

/* 优惠券列表 */
.coupon-list {
  max-height: 400px;
  overflow-y: auto;
}

.empty-coupons {
  padding: 40px 0;
}

.coupon-item {
  display: flex;
  align-items: center;
  padding: 16px;
  margin-bottom: 12px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.coupon-item:hover {
  border-color: var(--color-accent);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.coupon-item.selected {
  border-color: var(--color-accent);
  background: var(--color-accent-light);
}

.coupon-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-right: 16px;
  border-right: 1px dashed var(--color-border);
  min-width: 100px;
}

.coupon-left .amount {
  font-size: 24px;
  font-weight: 700;
}

.coupon-left .condition {
  font-size: 12px;
  color: var(--color-text-lighter);
  margin-top: 4px;
}

.coupon-right {
  flex: 1;
  padding-left: 16px;
}

.coupon-right .name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}

.coupon-right .time {
  font-size: 12px;
  color: var(--color-text-lighter);
}

.check-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-accent);
}

/* 响应式 */
@media (max-width: 968px) {
  .cart-header,
  .cart-item {
    grid-template-columns: 40px 1fr 100px 120px 120px 60px;
    padding: 16px;
  }
  
  .item-img {
    width: 60px;
    height: 60px;
  }
}
</style>
