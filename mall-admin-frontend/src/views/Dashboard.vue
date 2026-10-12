<template>
  <div class="dash">
    <!-- 概览指标 -->
    <div class="stat-grid">
      <div
        v-for="card in statCards"
        :key="card.title"
        class="glass-stat glass-specular"
      >
        <div class="stat-icon" :style="{ background: card.bg }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.title }}</div>
        </div>
      </div>
    </div>

    <!-- 销售趋势 -->
    <div class="page-card glass-specular">
      <div class="card-head">
        <h3>销售趋势</h3>
        <div class="head-actions">
          <el-radio-group v-model="days" size="small" @change="load">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="14">近 14 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
          <el-button size="small" :loading="loading" @click="load">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </div>
      <div ref="trendRef" class="chart-box"></div>
    </div>

    <!-- 商品热度 -->
    <div class="dash-row">
      <div class="page-card glass-specular">
        <div class="card-head">
          <h3>商品热度 Top 8</h3>
          <span class="card-hint">按有效订单销量统计</span>
        </div>
        <div ref="hotRef" class="chart-box"></div>
      </div>

      <div class="page-card glass-specular">
        <div class="card-head">
          <h3>热销榜</h3>
        </div>
        <el-table :data="hotProducts" size="small" max-height="300" empty-text="暂无销量数据">
          <el-table-column label="商品" min-width="190">
            <template #default="{ row, $index }">
              <div class="hot-item">
                <span class="hot-rank" :class="{ 'is-top': $index < 3 }">{{ $index + 1 }}</span>
                <el-image v-if="row.productImage" :src="row.productImage" class="hot-thumb" fit="cover" />
                <span class="hot-name">{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="销量" width="80" align="right" />
          <el-table-column label="销售额" width="110" align="right">
            <template #default="{ row }">{{ money(row.amount) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 待审核评论 -->
    <div class="page-card glass-specular">
      <div class="card-head">
        <h3>
          待审核评论
          <span v-if="pendingTotal" class="pending-badge">{{ pendingTotal }}</span>
        </h3>
        <el-button link type="primary" @click="$router.push('/comments')">查看全部</el-button>
      </div>
      <el-table :data="pendingComments" v-loading="pendingLoading" size="small" empty-text="暂无待审核评论">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="productId" label="商品ID" width="90" />
        <el-table-column prop="content" label="评论内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="auditPending(row, 1)">通过</el-button>
            <el-button size="small" type="danger" @click="auditPending(row, 2)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 快捷入口 -->
    <div class="page-card glass-specular">
      <el-space wrap>
        <el-button type="primary" @click="$router.push('/products')">
          <el-icon><Goods /></el-icon> 商品管理
        </el-button>
        <el-button type="primary" @click="$router.push('/categories')">
          <el-icon><Menu /></el-icon> 分类管理
        </el-button>
        <el-button type="primary" @click="$router.push('/orders')">
          <el-icon><List /></el-icon> 订单管理
        </el-button>
        <el-button type="primary" @click="$router.push('/search')">
          <el-icon><Search /></el-icon> ES数据导入
        </el-button>
      </el-space>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, AxisPointerComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, AxisPointerComponent, CanvasRenderer])
import { commentApi, statsApi } from '@/api'
import { ElMessage } from 'element-plus'

const days = ref(7)
const loading = ref(false)
const overview = ref({})
const hotProducts = ref([])

const statCards = computed(() => {
  const o = overview.value || {}
  return [
    { title: '订单总数', value: o.totalAllOrders ?? '-', icon: 'List', bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
    { title: '累计销售额', value: money(o.totalSales), icon: 'Money', bg: 'linear-gradient(135deg, #43e97b, #38f9d7)' },
    { title: '今日订单', value: o.todayOrders ?? '-', icon: 'Calendar', bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
    { title: '今日销售额', value: money(o.todaySales), icon: 'TrendCharts', bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
    { title: '待支付', value: o.pendingOrders ?? '-', icon: 'AlarmClock', bg: 'linear-gradient(135deg, #f6d365, #fda085)' },
    { title: '待发货', value: o.unshippedOrders ?? '-', icon: 'Van', bg: 'linear-gradient(135deg, #a18cd1, #fbc2eb)' }
  ]
})

function money (v) {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')

// ---------------- 图表 ----------------
const trendRef = ref(null)
const hotRef = ref(null)
let trendChart = null
let hotChart = null

function renderTrend (trend) {
  if (!trendRef.value) return
  if (!trendChart) trendChart = echarts.init(trendRef.value)

  const dates = trend.map(t => t.statDate)
  const amounts = trend.map(t => Number(t.salesAmount || 0))
  const counts = trend.map(t => Number(t.orderCount || 0))

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const date = params[0]?.axisValue || ''
        const rows = params.map(p => {
          const v = p.seriesName === '销售额' ? money(p.value) : `${p.value} 单`
          return `${p.marker}${p.seriesName}：${v}`
        })
        return [date, ...rows].join('<br/>')
      }
    },
    legend: { right: 0, top: 0, icon: 'roundRect', textStyle: { color: '#4a5578' } },
    grid: { left: 8, right: 8, top: 46, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: 'rgba(31, 42, 71, 0.16)' } },
      axisLabel: { color: '#8892ad' }
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        nameTextStyle: { color: '#8892ad' },
        splitLine: { lineStyle: { color: 'rgba(31, 42, 71, 0.08)' } },
        axisLabel: { color: '#8892ad' }
      },
      {
        type: 'value',
        name: '订单数',
        nameTextStyle: { color: '#8892ad' },
        splitLine: { show: false },
        axisLabel: { color: '#8892ad' }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: amounts,
        itemStyle: { color: '#5b6fd8' },
        lineStyle: { width: 3, color: '#5b6fd8' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(91, 111, 216, 0.35)' },
            { offset: 1, color: 'rgba(91, 111, 216, 0)' }
          ])
        }
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        yAxisIndex: 1,
        data: counts,
        itemStyle: { color: '#f2a65a' },
        lineStyle: { width: 3, color: '#f2a65a' }
      }
    ]
  }, true)
}

function renderHot (list) {
  if (!hotRef.value) return
  if (!hotChart) hotChart = echarts.init(hotRef.value)

  const top = list.slice(0, 8).reverse()
  hotChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        return `${p.name}<br/>销量：${p.value} 件`
      }
    },
    grid: { left: 8, right: 30, top: 12, bottom: 4, containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(31, 42, 71, 0.08)' } },
      axisLabel: { color: '#8892ad' }
    },
    yAxis: {
      type: 'category',
      data: top.map(p => p.productName),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#4a5578', width: 120, overflow: 'truncate' }
    },
    series: [
      {
        type: 'bar',
        data: top.map(p => Number(p.quantity || 0)),
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 7, 7, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#8698e4' },
            { offset: 1, color: '#5b6fd8' }
          ])
        }
      }
    ]
  }, true)
}

function handleResize () {
  trendChart?.resize()
  hotChart?.resize()
}

// ---------------- 数据加载 ----------------
async function load () {
  loading.value = true
  try {
    const res = await statsApi.getOverview(days.value)
    overview.value = res.data || {}
    hotProducts.value = overview.value.hotProducts || []
    await nextTick()
    renderTrend(overview.value.trend || [])
    renderHot(hotProducts.value)
  } catch (e) {
    overview.value = {}
    hotProducts.value = []
  } finally {
    loading.value = false
  }
}

// ---------------- 待审核评论 ----------------
const pendingComments = ref([])
const pendingTotal = ref(0)
const pendingLoading = ref(false)

async function loadPendingComments () {
  pendingLoading.value = true
  try {
    const res = await commentApi.getPage({ status: 0, pageNum: 1, pageSize: 5 })
    const data = res.data || {}
    pendingComments.value = data.list || []
    pendingTotal.value = Number(data.total || 0)
  } catch (e) {
    pendingComments.value = []
    pendingTotal.value = 0
  } finally {
    pendingLoading.value = false
  }
}

async function auditPending (row, status) {
  try {
    await commentApi.audit(row.id, status)
    ElMessage.success(status === 1 ? '已通过' : '已拒绝')
    loadPendingComments()
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  load()
  loadPendingComments()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  hotChart?.dispose()
  trendChart = null
  hotChart = null
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.stat-body {
  min-width: 0;
}

.dash-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.dash-row > .page-card {
  margin-top: 0 !important;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.card-head h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2a47;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-hint {
  font-size: 12px;
  color: #8892ad;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hot-rank {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #6b769a;
  background: rgba(31, 42, 71, 0.06);
}

.hot-rank.is-top {
  color: #fff;
  background: linear-gradient(135deg, #5b6fd8, #8f5fd0);
}

.hot-thumb {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  flex-shrink: 0;
}

.hot-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pending-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 10px;
  border-radius: 12px;
  background: rgba(245, 108, 108, 0.12);
  color: #f56c6c;
  font-size: 13px;
  vertical-align: middle;
}

.page-card + .page-card,
.dash > .page-card {
  margin-top: 20px;
}

@media (max-width: 1100px) {
  .dash-row {
    grid-template-columns: 1fr;
  }
}
</style>