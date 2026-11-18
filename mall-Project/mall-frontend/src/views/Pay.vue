<script setup>
/**
 * 支付页
 *
 * 下单成功后跳到这里：先展示刚才下的订单（含商品明细），
 * 点「支付宝支付」后在新窗口打开支付宝沙箱完成付款。
 * 用户从沙箱返回后，本页轮询订单状态，支付成功即提示并引导去「我的订单」。
 */
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, payApi } from '@/api/compat'
import { formatPrice, formatDateTime, orderStatusText } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(false)
const paying = ref(false)
const paid = ref(false)
let pollTimer = null

const orderId = computed(() => route.query.orderId || null)

const items = computed(() => order.value?.items || [])

async function loadOrder() {
  if (!orderId.value) return
  loading.value = true
  try {
    const detail = await orderApi.detail(orderId.value)
    // detail 是适配后的对象；若接口返回带 data 包裹则再拆一层
    order.value = detail?.orderNo ? detail : detail?.data || null
    if (order.value?.status === 1 || order.value?.status === 2) {
      paid.value = true
    }
  } catch (e) {
    order.value = null
  } finally {
    loading.value = false
  }
}

/** 打开支付宝沙箱 */
async function startPay() {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      `即将跳转支付宝沙箱支付 ${formatPrice(order.value.payAmount ?? order.value.totalAmount)}`,
      '支付确认',
      { type: 'info', confirmButtonText: '去支付', cancelButtonText: '再想想' }
    )
  } catch (e) {
    return
  }

  // 必须在点击的同步阶段开窗口，等 await 之后再开会被弹窗拦截
  const payWindow = window.open('', '_blank')
  paying.value = true
  try {
    const res = await payApi.pay({
      orderId: order.value.id,
      subject: '订单-' + String(order.value.orderNo || '').slice(0, 12)
    })
    const data = res?.data || res || {}
    if (data.payForm) {
      if (payWindow) {
        payWindow.document.write(data.payForm)
        payWindow.document.close()
      } else {
        document.open()
        document.write(data.payForm)
        document.close()
      }
      ElMessage.success('已打开支付宝沙箱，请在沙箱中完成支付')
      startPolling()
    } else {
      if (payWindow) payWindow.close()
      ElMessage.warning('支付表单获取失败，请稍后重试')
    }
  } catch (e) {
    if (payWindow) payWindow.close()
  } finally {
    paying.value = false
  }
}

/** 轮询订单状态，支付成功后更新页面 */
function startPolling() {
  stopPolling()
  let tries = 0
  pollTimer = window.setInterval(async () => {
    tries += 1
    try {
      const detail = await orderApi.detail(orderId.value)
      const d = detail?.orderNo ? detail : detail?.data
      if (d && (d.status === 1 || d.status === 2)) {
        order.value = d
        paid.value = true
        stopPolling()
        ElMessage.success('支付成功！订单已保存到「我的订单」')
        return
      }
    } catch (e) {
      /* 单次失败不中断 */
    }
    if (tries >= 30) stopPolling() // 最多约 60 秒
  }, 2000)
}

function stopPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

function goOrders() {
  router.push('/orders')
}

function goHome() {
  router.push('/home')
}

onMounted(() => {
  loadOrder()
  // 从支付宝 return 跳回来时也可能带 status=success
  if (route.query.status === 'success') {
    paid.value = true
    startPolling()
  }
})

onBeforeUnmount(stopPolling)
</script>

<template>
  <div class="container pay-page">
    <header class="page-head">
      <h2 class="page-title">{{ paid ? '支付成功' : '订单支付' }}</h2>
      <span class="page-sub">订单提交成功后，请选择支付方式完成付款</span>
    </header>

    <div v-loading="loading" class="pay-layout">
      <!-- 订单明细 -->
      <section class="pay-card order-summary">
        <div class="pay-card-head">
          <span>订单信息</span>
          <span v-if="order" class="status-tag" :class="{ paid: paid }">
            {{ paid ? '已支付' : orderStatusText(order.status) }}
          </span>
        </div>

        <template v-if="order">
          <p class="order-meta">订单号：{{ order.orderNo }}</p>
          <p class="order-meta">下单时间：{{ formatDateTime(order.createTime) }}</p>

          <div class="item-list">
            <div v-for="item in items" :key="item.id || item.productId" class="pay-item">
              <div class="pi-img">
                <img v-if="item.image" :src="item.image" :alt="item.title" />
                <span v-else>{{ (item.title || '商').slice(0, 1) }}</span>
              </div>
              <div class="pi-info">
                <p class="pi-title">{{ item.title }}</p>
                <p class="pi-sub">{{ formatPrice(item.price) }} × {{ item.num }}</p>
              </div>
              <span class="pi-total price">{{ formatPrice(item.totalPrice || item.price * item.num) }}</span>
            </div>
          </div>

          <div class="pay-total">
            <span>应付金额</span>
            <span class="price total-price">{{ formatPrice(order.payAmount ?? order.totalAmount) }}</span>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="未找到订单信息" />
      </section>

      <!-- 支付方式 -->
      <aside class="pay-card method-card">
        <div class="pay-card-head"><span>选择支付方式</span></div>

        <div class="method-row">
          <span class="method-icon alipay">支</span>
          <div class="method-info">
            <p class="method-name">支付宝沙箱</p>
            <p class="method-desc">跳转到支付宝沙箱环境完成付款</p>
          </div>
          <el-radio :model-value="true" disabled />
        </div>

        <div v-if="paid" class="paid-box">
          <p class="paid-title">支付已完成</p>
          <p class="paid-desc">订单已保存到「我的订单」，可以随时查看。</p>
          <button class="btn-brand" type="button" @click="goOrders">查看我的订单</button>
          <button class="btn-ghost" type="button" @click="goHome">继续购物</button>
        </div>
        <template v-else>
          <button
            class="btn-brand pay-btn"
            type="button"
            :disabled="!order || paying"
            @click="startPay"
          >
            {{ paying ? '正在打开支付页面…' : '支付宝支付' }}
          </button>
          <button class="btn-ghost back-btn" type="button" @click="goOrders">返回我的订单</button>
          <p class="pay-hint">支付完成后请返回本页，状态会自动更新；订单始终保存在「我的订单」中。</p>
        </template>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.pay-page {
  padding-top: 26px;
  padding-bottom: 50px;
}

.page-head {
  padding-bottom: 13px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 21px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-head .page-title {
  font-size: 30px;
  margin-bottom: 0;
}

.page-sub {
  color: var(--sage);
  font-size: 12px;
}

.pay-layout {
  display: flex;
  align-items: flex-start;
  gap: 22px;
}

.pay-card {
  border: 1px solid var(--line);
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 5px 5px 0 rgba(22, 58, 46, 0.07);
}

.order-summary {
  flex: 1;
  min-width: 0;
  padding: 20px 24px 24px;
}

.method-card {
  width: 320px;
  flex-shrink: 0;
  padding: 20px 22px 24px;
}

.pay-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 14px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--line);
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
}

.status-tag {
  padding: 3px 11px;
  border: 1px solid var(--line-strong);
  border-radius: 2px;
  color: var(--vermilion);
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 400;
}

.status-tag.paid {
  color: var(--pine-600);
  border-color: var(--pine-600);
}

.order-meta {
  margin: 0 0 6px;
  color: var(--sage);
  font-size: 12px;
}

.item-list {
  margin-top: 16px;
  border-top: 1px dashed var(--line-strong);
}

.pay-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px dashed var(--line);
}

.pi-img {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 2px;
  background: var(--porcelain-deep);
  color: var(--sage-light);
  font-family: var(--font-display);
  font-size: 22px;
}

.pi-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.pi-info {
  flex: 1;
  min-width: 0;
}

.pi-title {
  margin: 0;
  overflow: hidden;
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pi-sub {
  margin: 5px 0 0;
  color: var(--sage);
  font-size: 12px;
}

.pi-total {
  color: var(--text-body);
  font-size: 14px;
}

.pay-total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 16px;
  padding-top: 18px;
  color: var(--text-body);
  font-size: 13px;
}

.total-price {
  color: var(--vermilion);
  font-family: var(--font-num);
  font-size: 26px;
  font-weight: 600;
}

.method-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--pine-700);
  background: var(--brand-soft);
}

.method-icon.alipay {
  display: grid;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 4px;
  background: #1677ff;
  color: #fff;
  font-size: 19px;
  font-weight: 700;
}

.method-info {
  flex: 1;
  min-width: 0;
}

.method-name {
  margin: 0;
  color: var(--pine-800);
  font-size: 14px;
  font-weight: 600;
}

.method-desc {
  margin: 4px 0 0;
  color: var(--sage);
  font-size: 11px;
}

.btn-brand,
.btn-ghost {
  width: 100%;
  padding: 12px 20px;
  border-radius: 2px;
  font-family: var(--font-ui);
  font-size: 14px;
  cursor: pointer;
}

.btn-brand {
  border: none;
  background: var(--pine-800);
  color: #fff;
}

.btn-brand:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-ghost {
  border: 1px solid var(--line-strong);
  background: transparent;
  color: var(--pine-700);
}

.pay-btn {
  margin-top: 20px;
}

.back-btn {
  margin-top: 10px;
}

.pay-hint {
  margin: 14px 0 0;
  color: var(--text-hint);
  font-size: 11px;
  line-height: 1.6;
}

.paid-box {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px dashed var(--line-strong);
  text-align: center;
}

.paid-title {
  margin: 0 0 6px;
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
}

.paid-desc {
  margin: 0 0 18px;
  color: var(--sage);
  font-size: 12px;
  line-height: 1.5;
}

.paid-box .btn-ghost {
  margin-top: 10px;
}

@media (max-width: 900px) {
  .pay-layout {
    flex-direction: column;
  }
  .method-card {
    width: 100%;
  }
}
</style>
