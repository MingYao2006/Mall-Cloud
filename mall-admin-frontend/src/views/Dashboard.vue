<template>
  <div class="dash">
    <section class="business-board">
      <div class="board-main">
        <div class="board-topline">
          <span class="board-kicker">今日营业</span>
          <span class="board-updated">{{ updatedText }}</span>
        </div>

        <div class="board-figure">
          <span v-if="hasTodaySales" class="board-currency">¥</span>
          <span class="board-amount">{{ todaySalesAmount }}</span>
        </div>
        <div class="board-label">今日销售额</div>

        <div class="board-metrics">
          <div class="board-metric">
            <span>今日订单</span>
            <strong>{{ todayOrderCount }}</strong>
          </div>
          <div class="board-metric">
            <span>客单价</span>
            <strong>{{ averageOrderValue }}</strong>
          </div>
          <div class="board-metric">
            <span>累计订单</span>
            <strong>{{ totalOrderCount }}</strong>
          </div>
          <div class="board-metric">
            <span>累计销售额</span>
            <strong>{{ totalSalesAmount }}</strong>
          </div>
        </div>

        <p v-if="statsError" class="board-error">
          经营数据暂时没有加载出来。
          <button type="button" @click="load">重新加载</button>
        </p>
      </div>

      <aside class="board-todo" aria-label="待处理事项">
        <div class="todo-head">
          <span>待处理</span>
          <strong>{{ todoTotal }}</strong>
        </div>
        <router-link to="/orders" class="todo-link">
          <span>待支付订单</span>
          <em>{{ pendingOrderCount }}</em>
        </router-link>
        <router-link to="/orders" class="todo-link">
          <span>待发货订单</span>
          <em>{{ unshippedOrderCount }}</em>
        </router-link>
        <router-link to="/comments" class="todo-link">
          <span>待审核评论</span>
          <em>{{ pendingTotal }}</em>
        </router-link>
        <p class="todo-foot">处理完待办，再去看趋势。</p>
      </aside>
    </section>

    <section class="page-card trend-panel">
      <header class="panel-head">
        <div>
          <h2>销售趋势</h2>
          <p>销售额与订单量，按天汇总</p>
        </div>
        <div class="head-actions">
          <div class="range-switch" role="group" aria-label="选择时间范围">
            <button
              v-for="option in rangeOptions"
              :key="option.value"
              type="button"
              :class="{ active: days === option.value }"
              :aria-pressed="days === option.value"
              @click="changeDays(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
          <el-button :loading="loading" @click="load">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </header>

      <div v-loading="loading" class="chart-stage trend-stage">
        <div ref="trendRef" class="chart-box"></div>
        <div v-if="!loading && !trend.length" class="chart-empty">
          <el-icon :size="26"><DataLine /></el-icon>
          <strong>还没有销售趋势</strong>
          <span>有订单之后，这里会按天留下记录。</span>
        </div>
      </div>
    </section>

    <div class="dash-row">
      <section class="page-card">
        <header class="panel-head">
          <div>
            <h2>货架热度</h2>
            <p>按有效订单销量排序，取前 8 件</p>
          </div>
        </header>
        <div v-loading="loading" class="chart-stage hot-stage">
          <div ref="hotRef" class="chart-box"></div>
          <div v-if="!loading && !hotProducts.length" class="chart-empty">
            <el-icon :size="26"><Goods /></el-icon>
            <strong>货架还没有热度</strong>
            <span>商品产生有效订单后，这里会显示销量排名。</span>
          </div>
        </div>
      </section>

      <section class="page-card">
        <header class="panel-head">
          <div>
            <h2>热销榜</h2>
            <p>销量与销售额</p>
          </div>
        </header>
        <el-table class="hot-table" :data="hotProducts" v-loading="loading" size="small" max-height="300" empty-text="暂无销量数据">
          <el-table-column label="商品" min-width="150">
            <template #default="{ row, $index }">
              <div class="hot-item">
                <span class="hot-rank" :class="{ 'is-top': $index < 3 }">{{ $index + 1 }}</span>
                <el-image v-if="row.productImage" :src="row.productImage" class="hot-thumb" fit="cover" />
                <span class="hot-name">{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="销量" width="64" align="right">
            <template #default="{ row }"><span class="num">{{ row.quantity }}</span></template>
          </el-table-column>
          <el-table-column label="销售额" width="92" align="right">
            <template #default="{ row }"><span class="num">{{ money(row.amount) }}</span></template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <section class="page-card">
      <header class="panel-head pending-panel-head">
        <div>
          <div class="panel-title-row">
            <h2>待审核评论</h2>
            <span v-if="pendingTotal" class="count-stamp">{{ pendingTotal }}</span>
          </div>
          <p>新评论会先进入这里，通过后才对用户展示。</p>
        </div>
        <router-link class="text-link" to="/comments">查看全部</router-link>
      </header>

      <div v-if="isMobile" v-loading="pendingLoading" class="pending-cards">
        <div v-if="pendingLoading" class="pending-loading">正在读取待审评论</div>
        <article v-for="row in pendingComments" :key="row.id" class="pending-card">
          <div class="pending-card-meta">
            <span class="num">#{{ row.id }}</span>
            <span class="num">商品 {{ row.productId }}</span>
            <span class="num pending-time">{{ formatTime(row.createTime) }}</span>
          </div>
          <p class="pending-card-content">{{ row.content }}</p>
          <div class="pending-card-actions">
            <el-button size="small" type="success" plain @click="auditPending(row, 1)">通过</el-button>
            <el-button size="small" type="danger" plain @click="auditPending(row, 2)">拒绝</el-button>
          </div>
        </article>
        <p v-if="!pendingLoading && !pendingComments.length" class="pending-empty">
          {{ pendingError ? '待审核评论暂时加载失败，请稍后刷新' : '暂无待审核评论，评论区很安静' }}
        </p>
      </div>
      <el-table
        v-else
        class="pending-table"
        :data="pendingComments"
        v-loading="pendingLoading"
        size="small"
        :empty-text="pendingError ? '待审核评论暂时加载失败，请稍后刷新' : '暂无待审核评论，评论区很安静'"
      >
        <el-table-column prop="id" label="ID" width="64">
          <template #default="{ row }"><span class="num">{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="productId" label="商品ID" width="84">
          <template #default="{ row }"><span class="num">{{ row.productId }}</span></template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="180" show-overflow-tooltip />
        <el-table-column label="时间" width="160">
          <template #default="{ row }"><span class="num time">{{ formatTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button size="small" type="success" plain @click="auditPending(row, 1)">通过</el-button>
            <el-button size="small" type="danger" plain @click="auditPending(row, 2)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <nav class="quick-strip" aria-label="常用入口">
      <router-link v-for="entry in quickEntries" :key="entry.path" :to="entry.path" class="quick-item">
        <el-icon :size="19"><component :is="entry.icon" /></el-icon>
        <span>
          <strong>{{ entry.title }}</strong>
          <em>{{ entry.desc }}</em>
        </span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { AxisPointerComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { commentApi, statsApi } from '@/api'
import { ElMessage } from 'element-plus'

echarts.use([LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, AxisPointerComponent, CanvasRenderer])

const days = ref(7)
const loading = ref(false)
const overview = ref({})
const hotProducts = ref([])
const updatedAt = ref('')
const statsError = ref(false)
const mobileQuery = window.matchMedia ? window.matchMedia('(max-width: 640px)') : null
const isMobile = ref(mobileQuery?.matches ?? false)

const rangeOptions = [
  { value: 7, label: '近 7 天' },
  { value: 14, label: '近 14 天' },
  { value: 30, label: '近 30 天' }
]

const quickEntries = [
  { path: '/products', title: '商品管理', desc: '上新、库存与上下架', icon: 'Goods' },
  { path: '/categories', title: '分类管理', desc: '调整货架结构', icon: 'Menu' },
  { path: '/orders', title: '订单管理', desc: '查单与取消订单', icon: 'List' },
  { path: '/search', title: '搜索管理', desc: '同步商品到 ES', icon: 'Search' }
]

const trend = computed(() => overview.value.trend || [])
const hasTodaySales = computed(() => numberOrNull(overview.value.todaySales) !== null)
const todaySalesAmount = computed(() => amountOnly(overview.value.todaySales))
const todayOrderCount = computed(() => countOnly(overview.value.todayOrders))
const totalOrderCount = computed(() => countOnly(overview.value.totalAllOrders))
const totalSalesAmount = computed(() => money(overview.value.totalSales))
const pendingOrderCount = computed(() => countOnly(overview.value.pendingOrders))
const unshippedOrderCount = computed(() => countOnly(overview.value.unshippedOrders))

const averageOrderValue = computed(() => {
  const sales = numberOrNull(overview.value.todaySales)
  const orders = numberOrNull(overview.value.todayOrders)
  if (sales === null || orders === null || orders <= 0) return '—'
  return '¥' + (sales / orders).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
})

const updatedText = computed(() => {
  if (loading.value) return '正在读取数据'
  if (statsError.value) return '数据未更新'
  return updatedAt.value ? `更新于 ${updatedAt.value}` : '等待数据'
})

const pendingComments = ref([])
const pendingTotal = ref(0)
const pendingLoading = ref(false)
const pendingError = ref(false)

const todoTotal = computed(() => {
  return [overview.value.pendingOrders, overview.value.unshippedOrders, pendingTotal.value]
    .map(numberOrNull)
    .filter(value => value !== null)
    .reduce((sum, value) => sum + value, 0)
})

function numberOrNull(value) {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function amountOnly(value) {
  const number = numberOrNull(value)
  if (number === null) return '—'
  return number.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

function countOnly(value) {
  const number = numberOrNull(value)
  return number === null ? '—' : number.toLocaleString('zh-CN')
}

function money(value) {
  if (value === null || value === undefined || value === '') return '—'
  const number = Number(value)
  if (!Number.isFinite(number)) return '—'
  return '¥' + number.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

const formatTime = value => (value ? String(value).replace('T', ' ').slice(0, 19) : '—')

// ---------------- 图表 ----------------
const trendRef = ref(null)
const hotRef = ref(null)
let trendChart = null
let hotChart = null

const chartTooltip = {
  backgroundColor: '#FBFCF8',
  borderColor: 'rgba(22, 58, 46, 0.22)',
  borderWidth: 1,
  padding: [10, 12],
  textStyle: {
    color: '#163A2E',
    fontFamily: 'HarmonyOS Sans SC',
    fontSize: 12
  },
  extraCssText: 'border-radius:2px; box-shadow:4px 4px 0 rgba(22,58,46,.08);'
}

function renderTrend(data) {
  if (!trendRef.value || !data.length) return
  if (!trendChart) trendChart = echarts.init(trendRef.value)

  const dates = data.map(item => item.statDate)
  const amounts = data.map(item => Number(item.salesAmount || 0))
  const counts = data.map(item => Number(item.orderCount || 0))

  trendChart.setOption({
    animationDuration: 650,
    animationEasing: 'cubicOut',
    tooltip: {
      ...chartTooltip,
      trigger: 'axis',
      axisPointer: {
        type: 'line',
        lineStyle: { color: 'rgba(228, 166, 47, 0.7)', type: 'dashed' }
      },
      formatter: params => {
        const date = params[0]?.axisValue || ''
        const rows = params.map(item => {
          const value = item.seriesName === '销售额' ? money(item.value) : `${item.value} 单`
          return `${item.marker}${item.seriesName}：${value}`
        })
        return [date, ...rows].join('<br/>')
      }
    },
    legend: {
      data: [
        { name: '销售额', icon: 'rect' },
        { name: '订单数', icon: 'circle' }
      ],
      right: 0,
      top: 0,
      itemWidth: 9,
      itemHeight: 9,
      itemGap: 18,
      textStyle: { color: '#61736A', fontFamily: 'HarmonyOS Sans SC', fontSize: 11 }
    },
    grid: { left: 8, right: 10, top: 48, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: true,
      data: dates,
      axisLine: { lineStyle: { color: 'rgba(22, 58, 46, 0.18)' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#61736A',
        fontFamily: 'DIN Next LT Pro',
        fontSize: 10,
        margin: 12,
        formatter: value => {
          const text = String(value)
          return text.length >= 10 ? text.slice(5).replace('-', '/') : text
        }
      }
    },
    yAxis: [
      {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(22, 58, 46, 0.09)', type: 'dashed' } },
        axisLabel: {
          color: '#61736A',
          fontFamily: 'DIN Next LT Pro',
          fontSize: 10,
          formatter: value => (value >= 10000 ? `${(value / 10000).toFixed(1)}万` : value)
        }
      },
      {
        type: 'value',
        splitLine: { show: false },
        axisLabel: {
          color: '#B7791F',
          fontFamily: 'DIN Next LT Pro',
          fontSize: 10
        }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'bar',
        data: amounts,
        barMaxWidth: 26,
        itemStyle: { color: '#2B6150', borderRadius: [2, 2, 0, 0] },
        emphasis: { itemStyle: { color: '#163A2E' } }
      },
      {
        name: '订单数',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.28,
        showSymbol: false,
        symbol: 'circle',
        symbolSize: 6,
        data: counts,
        itemStyle: { color: '#C58A1E' },
        lineStyle: { width: 1.8, color: '#C58A1E' },
        emphasis: { focus: 'series' }
      }
    ]
  }, true)
}

function renderHot(list) {
  if (!hotRef.value || !list.length) return
  if (!hotChart) hotChart = echarts.init(hotRef.value)

  const top = list.slice(0, 8).reverse()
  const values = top.map(item => Number(item.quantity || 0))
  const maxValue = Math.max(...values, 1)

  hotChart.setOption({
    animationDuration: 600,
    animationEasing: 'cubicOut',
    tooltip: {
      ...chartTooltip,
      trigger: 'axis',
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(22, 58, 46, 0.04)' } },
      formatter: params => {
        const item = params[0]
        return `${item.name}<br/>销量：${item.value} 件`
      }
    },
    grid: { left: 8, right: 42, top: 8, bottom: 4, containLabel: true },
    xAxis: {
      type: 'value',
      max: Math.ceil(maxValue * 1.18),
      splitLine: { lineStyle: { color: 'rgba(22, 58, 46, 0.08)', type: 'dashed' } },
      axisLabel: {
        color: '#61736A',
        fontFamily: 'DIN Next LT Pro',
        fontSize: 10
      }
    },
    yAxis: {
      type: 'category',
      data: top.map(item => item.productName),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#4E6459',
        fontFamily: 'HarmonyOS Sans SC',
        fontSize: 11,
        width: 120,
        overflow: 'truncate'
      }
    },
    series: [
      {
        type: 'bar',
        data: values,
        barWidth: 12,
        itemStyle: {
          borderRadius: [0, 2, 2, 0],
          color: params => {
            const rank = top.length - 1 - params.dataIndex
            return rank < 3 ? '#E4A62F' : '#9DB0A6'
          }
        },
        label: {
          show: true,
          position: 'right',
          color: '#4E6459',
          fontFamily: 'DIN Next LT Pro',
          fontSize: 10
        }
      }
    ]
  }, true)
}

function handleResize() {
  trendChart?.resize()
  hotChart?.resize()
}

async function load() {
  loading.value = true
  statsError.value = false
  try {
    const res = await statsApi.getOverview(days.value)
    overview.value = res.data || {}
    hotProducts.value = overview.value.hotProducts || []
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    await nextTick()
    if (trend.value.length) {
      renderTrend(trend.value)
    } else {
      trendChart?.clear()
    }
    if (hotProducts.value.length) {
      renderHot(hotProducts.value)
    } else {
      hotChart?.clear()
    }
  } catch (e) {
    overview.value = {}
    hotProducts.value = []
    statsError.value = true
    trendChart?.clear()
    hotChart?.clear()
  } finally {
    loading.value = false
  }
}

function changeDays(value) {
  if (days.value === value) return
  days.value = value
  load()
}

// ---------------- 待审核评论 ----------------
async function loadPendingComments() {
  pendingLoading.value = true
  pendingError.value = false
  try {
    const res = await commentApi.getPage({ status: 0, pageNum: 1, pageSize: 5 })
    const data = res.data || {}
    pendingComments.value = data.list || []
    pendingTotal.value = Number(data.total || 0)
  } catch (e) {
    pendingComments.value = []
    pendingTotal.value = 0
    pendingError.value = true
  } finally {
    pendingLoading.value = false
  }
}

async function auditPending(row, status) {
  try {
    await commentApi.audit(row.id, status)
    ElMessage.success(status === 1 ? '已通过' : '已拒绝')
    loadPendingComments()
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => {
  mobileQuery?.addEventListener('change', syncMobile)
  window.addEventListener('resize', handleResize)
  load()
  loadPendingComments()
})

onUnmounted(() => {
  mobileQuery?.removeEventListener('change', syncMobile)
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  hotChart?.dispose()
  trendChart = null
  hotChart = null
})

function syncMobile(event) {
  isMobile.value = event.matches
}
</script>

<style scoped>
.dash {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.business-board {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.75fr);
  overflow: hidden;
  border: 1px solid var(--pine-950);
  border-radius: 2px;
  color: var(--paper);
  background: var(--pine-900);
  box-shadow: var(--shadow-print-strong);
  animation: rise-in 0.55s cubic-bezier(0.2, 0.7, 0.3, 1) both;
}

.business-board::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  z-index: 2;
  height: 4px;
  background: var(--gold);
  transform-origin: left;
  animation: rule-draw 0.7s 0.1s cubic-bezier(0.2, 0.7, 0.3, 1) both;
}

.board-main {
  min-width: 0;
  padding: 34px 36px 30px;
}

.board-topline {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.board-kicker {
  color: var(--paper-warm);
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
}

.board-updated {
  color: rgba(255, 255, 255, 0.55);
  font-family: var(--font-num);
  font-size: 11px;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.board-figure {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 22px;
}

.board-currency {
  color: var(--gold);
  font-family: var(--font-num);
  font-size: 22px;
}

.board-amount {
  color: #fff;
  font-family: var(--font-num);
  font-size: clamp(42px, 5vw, 64px);
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  letter-spacing: -0.02em;
  line-height: 0.95;
}

.board-label {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 11px;
  letter-spacing: 0.06em;
}

.board-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 28px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.board-metric {
  min-width: 0;
}

.board-metric span {
  display: block;
  color: rgba(255, 255, 255, 0.55);
  font-size: 10px;
}

.board-metric strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: var(--paper-warm);
  font-family: var(--font-num);
  font-size: 17px;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.board-error {
  margin-top: 16px;
  color: #f3c7be;
  font-size: 12px;
}

.board-error button {
  border: none;
  color: var(--gold);
  background: none;
  font: inherit;
  text-decoration: underline;
  cursor: pointer;
}

.board-todo {
  display: flex;
  flex-direction: column;
  padding: 30px 26px 26px;
  border-left: 1px dashed rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.035);
  animation: ticket-in 0.55s 0.18s cubic-bezier(0.2, 0.7, 0.3, 1) both;
}

.todo-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
}

.todo-head span {
  color: var(--paper-warm);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
}

.todo-head strong {
  color: var(--gold);
  font-family: var(--font-num);
  font-size: 32px;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  line-height: 1;
}

.todo-link {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.76);
  font-size: 13px;
  transition: color 0.18s ease, padding-left 0.18s ease;
}

.todo-link:hover {
  padding-left: 4px;
  color: #fff;
}

.todo-link:last-of-type {
  border-bottom: none;
}

.todo-link em {
  color: var(--paper-warm);
  font-family: var(--font-num);
  font-size: 18px;
  font-style: normal;
  font-variant-numeric: tabular-nums;
}

.todo-foot {
  margin-top: auto;
  padding-top: 16px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 11px;
  line-height: 1.6;
}

.trend-panel {
  animation: rise-in 0.45s 0.12s cubic-bezier(0.2, 0.7, 0.3, 1) both;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-head h2 {
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.3;
}

.panel-head p {
  margin-top: 3px;
  color: var(--sage);
  font-size: 11px;
  line-height: 1.6;
}

.pending-panel-head {
  flex-direction: row;
  align-items: flex-start;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.range-switch {
  display: inline-flex;
  border: 1px solid var(--line-strong);
  background: var(--paper);
}

.range-switch button {
  padding: 5px 10px;
  border: none;
  border-right: 1px solid var(--line);
  color: var(--sage);
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}

.range-switch button:last-child {
  border-right: none;
}

.range-switch button.active {
  color: #fff;
  background: var(--pine-700);
}

.range-switch button:focus-visible {
  outline: 2px solid var(--gold);
  outline-offset: -2px;
}

.chart-stage {
  position: relative;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.trend-stage .chart-box {
  height: 320px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 20px;
  color: var(--sage);
  background: rgba(251, 252, 248, 0.88);
  text-align: center;
}

.chart-empty .el-icon {
  color: var(--sage-light);
}

.chart-empty strong {
  color: var(--pine-700);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
}

.chart-empty span {
  font-size: 11px;
}

.dash-row {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 20px;
}

.dash-row > .page-card {
  min-width: 0;
  margin-top: 0;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.hot-table :deep(.el-table__cell) {
  padding: 9px 0;
}

.hot-rank {
  display: grid;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 2px;
  color: var(--sage);
  font-family: var(--font-num);
  font-size: 11px;
}

.hot-rank.is-top {
  border-color: var(--gold);
  color: var(--pine-900);
  background: var(--gold);
  font-weight: 600;
}

.hot-thumb {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.hot-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.num {
  font-family: var(--font-num);
  font-variant-numeric: tabular-nums;
}

.time {
  color: var(--sage);
  font-size: 11px;
}

.panel-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.count-stamp {
  display: inline-grid;
  min-width: 22px;
  height: 20px;
  padding: 0 6px;
  place-items: center;
  border-radius: 2px;
  color: #fff;
  background: var(--vermilion);
  font-family: var(--font-num);
  font-size: 11px;
  transform: rotate(-1.5deg);
}

.pending-cards {
  border-top: 1px solid var(--line);
}

.pending-loading {
  display: flex;
  min-height: 88px;
  align-items: center;
  justify-content: center;
  color: var(--sage);
  font-size: 12px;
}

.pending-card {
  padding: 14px 0;
  border-bottom: 1px solid var(--line);
}

.pending-card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px 12px;
  color: var(--sage);
  font-size: 10px;
}

.pending-time {
  margin-left: auto;
}

.pending-card-content {
  margin-top: 7px;
  color: var(--pine-800);
  font-size: 13px;
  line-height: 1.65;
}

.pending-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.pending-empty {
  padding: 28px 0;
  color: var(--sage);
  font-size: 12px;
  text-align: center;
}

.text-link {
  padding-bottom: 1px;
  border-bottom: 1px solid var(--gold);
  color: var(--pine-700);
  font-size: 12px;
  font-weight: 600;
}

.text-link:hover {
  color: var(--gold-deep);
}

.quick-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid var(--line);
  border-radius: 2px;
  background: var(--paper);
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-right: 1px solid var(--line);
  color: var(--pine-800);
  transition: background 0.18s ease;
}

.quick-item:last-child {
  border-right: none;
}

.quick-item:hover {
  background: #edf1ea;
}

.quick-item .el-icon {
  flex-shrink: 0;
  color: var(--gold-deep);
}

.quick-item span {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.quick-item strong {
  font-size: 12px;
  font-weight: 600;
}

.quick-item em {
  overflow: hidden;
  margin-top: 2px;
  color: var(--sage);
  font-size: 10px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .business-board {
    grid-template-columns: 1fr;
  }

  .board-todo {
    border-top: 1px dashed rgba(255, 255, 255, 0.22);
    border-left: none;
  }

  .dash-row {
    grid-template-columns: 1fr;
  }

  .quick-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-item:nth-child(2) {
    border-right: none;
  }

  .quick-item:nth-child(-n + 2) {
    border-bottom: 1px solid var(--line);
  }
}

@media (max-width: 640px) {
  .board-main {
    padding: 24px 20px;
  }

  .board-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 16px;
  }

  .board-todo {
    padding: 22px 20px;
  }

  .board-figure {
    margin-top: 16px;
  }

  .panel-head {
    flex-direction: column;
  }

  .pending-panel-head {
    flex-direction: row;
    align-items: flex-start;
    gap: 12px;
  }

  .head-actions {
    width: 100%;
    justify-content: space-between;
  }

  .quick-strip {
    grid-template-columns: 1fr;
  }

  .quick-item,
  .quick-item:nth-child(2) {
    border-right: none;
    border-bottom: 1px solid var(--line);
  }

  .quick-item:last-child {
    border-bottom: none;
  }

  .chart-box,
  .trend-stage .chart-box {
    height: 260px;
  }

}
</style>
