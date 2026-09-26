<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartApi, orderApi, couponApi } from '@/api/compat'
import { formatPrice } from '@/utils/format'
import CountUp from '@/components/CountUp.vue'

const router = useRouter()

const cart = ref({ items: [], totalPrice: 0, totalCount: 0 })
const loading = ref(false)

const selectedItems = computed(() => cart.value.items.filter((i) => i.selected))

const selectedTotal = computed(() =>
  selectedItems.value.reduce((sum, i) => sum + i.price * i.num, 0)
)

const selectedCount = computed(() =>
  selectedItems.value.reduce((sum, i) => sum + i.num, 0)
)

const allSelected = computed(
  () => cart.value.items.length > 0 && cart.value.items.every((i) => i.selected)
)

async function load() {
  loading.value = true
  try {
    cart.value = await cartApi.list()
  } catch (e) {
    cart.value = { items: [], totalPrice: 0, totalCount: 0 }
  } finally {
    loading.value = false
  }
}

async function changeNum(item, num) {
  if (!num || num <= 0) return
  try {
    await cartApi.updateNum(item.skuId, num)
    load()
  } catch (e) {
    load()
  }
}

async function toggleSelect(item) {
  await cartApi.select(item.skuId, !item.selected)
  load()
}

async function toggleAll() {
  await cartApi.selectAll(!allSelected.value)
  load()
}

async function removeItem(item) {
  try {
    await ElMessageBox.confirm(`确定删除「${item.title}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  await cartApi.remove(item.skuId)
  ElMessage.success('已删除')
  load()
}

async function goCheckout() {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先勾选商品')
    return
  }
  await loadAvailableCoupons()
  dlg.value = true
}

onMounted(load)

/* ==================== 结算弹窗（收货信息 + 优惠券，沿用原项目下单流程） ==================== */
const dlg = ref(false)
const submiting = ref(false)
const form = reactive({ receiverName: '', receiverPhone: '', receiverAddress: '', note: '' })
const showCouponDialog = ref(false)
const availableCoupons = ref([])
const selectedCoupon = ref(null)

const orderTotal = computed(() =>
  selectedItems.value.reduce((s, i) => s + i.price * i.num, 0)
)

const finalPrice = computed(() => {
  const total = orderTotal.value
  if (selectedCoupon.value) {
    return Math.max(0, total - selectedCoupon.value.couponValue).toFixed(2)
  }
  return total.toFixed(2)
})

async function loadAvailableCoupons() {
  try {
    const res = await couponApi.getMy(0)
    const coupons = res.data || []
    availableCoupons.value = coupons
      .filter((c) => {
        const minAmount = Number(c.minAmount || 0)
        return minAmount > 0 ? orderTotal.value >= minAmount : true
      })
      .map((c) => ({
        id: c.id,
        couponId: c.couponId,
        couponName: c.couponName || c.name || '\u4f18\u60e0\u5238',
        couponValue: Number(c.couponValue || c.value || 0),
        minAmount: Number(c.minAmount || 0),
        expireTime: c.expireTime
      }))
  } catch (e) {
    availableCoupons.value = []
  }
}

function selectCoupon(coupon) {
  selectedCoupon.value = coupon
}

function confirmCoupon() {
  showCouponDialog.value = false
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function confirmOrder() {
  if (!form.receiverName || !form.receiverPhone || !form.receiverAddress) {
    ElMessage.warning('\u8bf7\u586b\u5199\u5b8c\u6574\u6536\u8d27\u4fe1\u606f')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.receiverPhone)) {
    ElMessage.warning('\u8bf7\u8f93\u5165\u6b63\u786e\u7684 11 \u4f4d\u624b\u673a\u53f7')
    return
  }
  submiting.value = true
  try {
    const orderData = {
      cartItemIds: selectedItems.value.map((i) => i.id),
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
      note: form.note
    }
    if (selectedCoupon.value) {
      orderData.couponId = selectedCoupon.value.id
      orderData.couponUserId = selectedCoupon.value.id
    }
    await orderApi.create(orderData)
    ElMessage.success('\u4e0b\u5355\u6210\u529f')
    dlg.value = false
    selectedCoupon.value = null
    form.receiverName = ''
    form.receiverPhone = ''
    form.receiverAddress = ''
    form.note = ''
    router.push('/orders')
  } catch (e) {
    /* 统一错误提示 */
  } finally {
    submiting.value = false
  }
}
</script>

<template>
  <div class="container cart-page">
    <header class="page-head">
      <h2 class="page-title">购物车</h2>
      <span v-if="cart.items.length" class="head-count">
        共 {{ cart.items.length }} 种商品
      </span>
    </header>

    <div v-loading="loading" class="cart-layout">
      <template v-if="cart.items.length">
        <!-- 商品列表 -->
        <section class="cart-list">
          <div class="list-head">
            <el-checkbox :model-value="allSelected" @change="toggleAll">全选</el-checkbox>
            <span>商品信息</span>
            <span class="t-right">单价</span>
            <span class="t-center">数量</span>
            <span class="t-right">小计</span>
            <span></span>
          </div>

          <transition-group name="row" tag="div" class="rows">
            <div v-for="item in cart.items" :key="item.skuId" class="cart-item">
              <el-checkbox
                :model-value="item.selected"
                @change="toggleSelect(item)"
              />

              <div class="goods">
                <div class="goods-img">
                  <img
                v-if="item.image"
                :src="item.image"
                :alt="item.title"
                loading="lazy"
                @error="item.image = ''"
              />
                  <span v-else>{{ (item.title || '商').slice(0, 1) }}</span>
                </div>
                <div class="goods-meta">
                  <p class="goods-title" :title="item.title">{{ item.title }}</p>
                  <p class="goods-sub">库存 {{ item.stock ?? '—' }} 件</p>
                </div>
              </div>

              <span class="price col-price">{{ formatPrice(item.price) }}</span>

              <div class="col-num">
                <el-input-number
                  :model-value="item.num"
                  :min="1"
                  :max="item.stock || 99"
                  size="small"
                  controls-position="right"
                  @change="(v) => changeNum(item, v)"
                />
              </div>

              <span class="price sub-total">{{ formatPrice(item.price * item.num) }}</span>

              <button class="del-btn" title="删除" @click="removeItem(item)">
                <el-icon :size="15"><Delete /></el-icon>
              </button>
            </div>
          </transition-group>
        </section>

        <!-- 结算卡 -->
        <aside class="checkout">
          <h3 class="checkout-title">订单结算</h3>

          <div class="sum-row">
            <span>商品总价</span>
            <CountUp class="price" :value="selectedTotal" />
          </div>
          <div class="sum-row">
            <span>优惠券</span>
            <span class="free">暂无可用</span>
          </div>
          <div class="sum-row">
            <span>运费</span>
            <span class="free">免运费</span>
          </div>

          <div class="sum-total">
            <span>合计</span>
            <CountUp class="price total-price" :value="selectedTotal" />
          </div>

          <button
            class="btn-brand pay-btn"
            :disabled="selectedItems.length === 0"
            @click="goCheckout"
          >
            去结算{{ selectedCount ? ` (${selectedCount})` : '' }}
          </button>

          <p class="checkout-hint">
            已选 {{ selectedItems.length }} 种 · 共 {{ selectedCount }} 件
          </p>
        </aside>
      </template>

      <el-empty v-else-if="!loading" class="empty" description="购物车还是空的">
        <el-button type="primary" round @click="router.push('/')">去逛逛</el-button>
      </el-empty>
    </div>

      <!-- 结算弹窗：收货信息 + 优惠券 -->
      <el-dialog v-model="dlg" title="填写订单信息" width="520px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="收货人">
            <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
          </el-form-item>
          <el-form-item label="电话">
            <el-input v-model="form.receiverPhone" placeholder="请输入11位手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="地址">
            <el-input v-model="form.receiverAddress" placeholder="请输入详细地址" />
          </el-form-item>

          <el-form-item label="优惠券">
            <div class="coupon-selector" @click="showCouponDialog = true">
              <div v-if="selectedCoupon" class="selected-coupon">
                <div>
                  <div class="coupon-header">
                    <span class="coupon-name">{{ selectedCoupon.couponName }}</span>
                    <span class="coupon-value price">-¥{{ selectedCoupon.couponValue }}</span>
                  </div>
                  <div class="coupon-condition">
                    {{ selectedCoupon.minAmount > 0 ? `满¥${selectedCoupon.minAmount}可用` : '无门槛优惠券' }}
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

          <el-form-item label="备注">
            <el-input v-model="form.note" type="textarea" placeholder="可选" />
          </el-form-item>

          <div class="price-summary">
            <div class="price-row">
              <span>商品总价</span>
              <span class="price">¥{{ orderTotal.toFixed(2) }}</span>
            </div>
            <div v-if="selectedCoupon" class="price-row discount">
              <span>优惠券抵扣</span>
              <span class="price">-¥{{ selectedCoupon.couponValue }}</span>
            </div>
            <div class="price-row total">
              <span>应付金额</span>
              <span class="price price-lg">¥{{ finalPrice }}</span>
            </div>
          </div>
        </el-form>
        <template #footer>
          <el-button @click="dlg = false">取消</el-button>
          <el-button type="primary" :loading="submiting" @click="confirmOrder">
            确认下单 (¥{{ finalPrice }})
          </el-button>
        </template>
      </el-dialog>

      <!-- 优惠券选择 -->
      <el-dialog v-model="showCouponDialog" title="选择优惠券" width="500px">
        <div class="coupon-list">
          <el-empty v-if="availableCoupons.length === 0" description="暂无可用优惠券" />
          <div
            v-for="coupon in availableCoupons"
            :key="coupon.id"
            class="coupon-item"
            :class="{ selected: selectedCoupon?.id === coupon.id }"
            @click="selectCoupon(coupon)"
          >
            <div class="coupon-left">
              <span class="amount price">¥{{ coupon.couponValue }}</span>
              <span class="condition">
                {{ coupon.minAmount > 0 ? `满¥${coupon.minAmount}可用` : '无门槛' }}
              </span>
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
</template>

<style scoped>
.cart-page {
  padding-top: 24px;
}

.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 18px;
}

.page-head .page-title {
  margin-bottom: 0;
}

.head-count {
  font-size: 13px;
  color: var(--text-muted);
}

.cart-layout {
  display: flex;
  align-items: flex-start;
  gap: 22px;
  min-height: 260px;
}

/* ---------------- 商品列表 ---------------- */
.cart-list {
  position: relative;
  flex: 1;
  min-width: 0;
  padding: 8px 26px 20px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.list-head,
.cart-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 104px 132px 108px 48px;
  align-items: center;
  gap: 12px;
}

.list-head {
  padding: 16px 0 14px;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-hint);
}

.cart-item {
  padding: 18px 0;
  transition: background 0.2s ease;
}

.cart-item + .cart-item {
  border-top: 1px solid var(--border);
}

.cart-item:hover {
  background: rgba(245, 247, 252, 0.7);
}

.t-right {
  text-align: right;
}

.t-center {
  text-align: center;
}

.goods {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.goods-img {
  width: 74px;
  height: 74px;
  flex-shrink: 0;
  border-radius: 14px;
  background: var(--surface-sunken);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 24px;
  color: #cdd5e8;
}

.goods-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.cart-item:hover .goods-img img {
  transform: scale(1.08);
}

.goods-meta {
  min-width: 0;
}

.goods-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-title);
  line-height: 1.5;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-sub {
  font-size: 12px;
  color: var(--text-hint);
}

.col-price {
  font-size: 14px;
  color: var(--text-body);
  text-align: right;
}

.col-num {
  display: flex;
  justify-content: center;
}

.col-num :deep(.el-input-number) {
  width: 108px;
}

.sub-total {
  font-size: 16px;
  font-weight: 500;
  text-align: right;
}

.del-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--text-hint);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.del-btn:hover {
  background: var(--accent-soft);
  color: var(--accent-500);
  transform: scale(1.08);
}

/* ---------------- 结算卡 ---------------- */
.checkout {
  position: sticky;
  top: 88px;
  width: 288px;
  flex-shrink: 0;
  padding: 24px 22px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.checkout-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-title);
  padding-bottom: 16px;
  margin-bottom: 6px;
  border-bottom: 1px solid var(--border);
}

.sum-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 0;
  font-size: 13px;
  color: var(--text-muted);
}

.free {
  color: var(--text-body);
}

.sum-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px dashed var(--border-strong);
  font-size: 14px;
  color: var(--text-title);
}

.total-price {
  font-size: 26px;
  letter-spacing: -0.5px;
}

.pay-btn {
  width: 100%;
  margin-top: 20px;
  padding: 13px 26px;
  font-size: 15px;
}

.pay-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.checkout-hint {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
  color: var(--text-hint);
}

/* ---------------- 行入场过渡 ---------------- */
.row-enter-active,
.row-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.row-enter-from {
  opacity: 0;
  transform: translateX(-14px);
}

.row-leave-to {
  opacity: 0;
  transform: translateX(14px);
}

.row-leave-active {
  position: absolute;
}

.empty {
  width: 100%;
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1024px) {
  .cart-layout {
    flex-direction: column;
  }
  .checkout {
    position: static;
    width: 100%;
  }
}

@media (max-width: 760px) {
  .list-head {
    display: none;
  }
  .cart-list {
    padding: 8px 18px 16px;
  }
  .cart-item {
    grid-template-columns: 40px minmax(0, 1fr) 44px;
    row-gap: 10px;
  }
  .col-price,
  .col-num,
  .sub-total {
    grid-column: 2 / 4;
    text-align: left;
    display: flex;
    justify-content: flex-start;
  }
}

/* ---------------- 结算弹窗 ---------------- */
.coupon-selector {
  width: 100%;
  padding: 12px 14px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--surface-sunken);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.coupon-selector:hover {
  border-color: var(--brand-500);
  background: var(--brand-soft);
}

.selected-coupon,
.no-coupon {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.no-coupon {
  color: var(--text-muted);
  font-size: 13px;
}

.coupon-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.coupon-name {
  font-size: 13px;
  color: var(--text-title);
}

.coupon-value {
  font-size: 14px;
  color: var(--accent-500);
}

.coupon-condition {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-hint);
}

.arrow-icon {
  color: var(--text-hint);
}

.price-summary {
  margin: 4px 0 0 80px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--surface-sunken);
}

.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-body);
}

.price-row + .price-row {
  margin-top: 10px;
}

.price-row.discount {
  color: var(--accent-500);
}

.price-row.total {
  padding-top: 10px;
  border-top: 1px solid var(--border);
  font-size: 14px;
  color: var(--text-title);
}

.price-lg {
  font-size: 20px;
  font-weight: 500;
  color: var(--accent-500);
}

.coupon-list {
  max-height: 420px;
  overflow-y: auto;
}

.coupon-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.coupon-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.coupon-item.selected {
  border-color: var(--brand-500);
  background: var(--brand-soft);
}

.coupon-left {
  flex-shrink: 0;
  width: 96px;
  text-align: center;
  padding-right: 14px;
  border-right: 1px dashed var(--border-strong);
}

.coupon-left .amount {
  display: block;
  font-size: 20px;
  font-weight: 500;
  color: var(--accent-500);
}

.coupon-left .condition {
  font-size: 12px;
  color: var(--text-hint);
}

.coupon-right .name {
  font-size: 14px;
  color: var(--text-title);
}

.coupon-right .time {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-hint);
}

.check-icon {
  position: absolute;
  right: 14px;
  top: 14px;
  color: var(--brand-500);
}
</style>
