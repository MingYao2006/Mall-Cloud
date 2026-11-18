<script setup>
/**
 * 全部分类（筛选面板式）
 *
 * 版式照参考图：顶部一条带下拉的搜索栏，下面是分行筛选（分类 / 品牌 /
 * 价格区间 / 排序方式），选中项高亮，右下角「清空筛选条件」。
 * 筛选结果走 POST /product/page/query（多条件组合）。
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { productApi } from '@/api/compat'

const router = useRouter()
const route = useRoute()

const tree = ref([])
const brands = ref([])
const goodsList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 12
const loading = ref(false)
const keyword = ref('')

/** 当前筛选条件 */
const filters = ref({
  categoryId: null,
  brand: '',
  priceRange: 'all',
  sortBy: 'default'
})

/** 价格区间：value 与后端 minPrice / maxPrice 对应 */
const PRICE_RANGES = {
  all: { label: '不限', minPrice: null, maxPrice: null },
  '0-100': { label: '100元以下', minPrice: null, maxPrice: 100 },
  '100-500': { label: '100-500元', minPrice: 100, maxPrice: 500 },
  '500-1000': { label: '500-1000元', minPrice: 500, maxPrice: 1000 },
  '1000-5000': { label: '1000-5000元', minPrice: 1000, maxPrice: 5000 },
  '5000-10000': { label: '5000-10000元', minPrice: 5000, maxPrice: 10000 },
  '10000-': { label: '10000元以上', minPrice: 10000, maxPrice: null }
}

/** 排序方式：orderBy / orderType 对应后端 ProductQuery */
const SORT_OPTIONS = {
  default: { label: '综合', orderBy: null, orderType: 'desc' },
  sales: { label: '销量', orderBy: 'sales', orderType: 'desc' },
  'price-asc': { label: '价格升序', orderBy: 'price', orderType: 'asc' },
  'price-desc': { label: '价格降序', orderBy: 'price', orderType: 'desc' },
  newest: { label: '最新', orderBy: 'createTime', orderType: 'desc' }
}

/** 顶部下拉里的分类（只放一级分类） */
const topCategories = computed(() => tree.value)

const searchSelectValue = computed(() => {
  if (!filters.value.categoryId) return ''
  const hit = tree.value.find((c) => c.id === filters.value.categoryId)
  return hit ? hit.name : ''
})

const hasFilter = computed(
  () =>
    filters.value.categoryId !== null ||
    !!filters.value.brand ||
    filters.value.priceRange !== 'all' ||
    filters.value.sortBy !== 'default' ||
    !!keyword.value.trim()
)

/** 筛选分组：每个维度一行，和参考图一致 */
const filterGroups = computed(() => [
  {
    key: 'categoryId',
    label: '商品分类',
    options: [
      { label: '全部分类', value: null },
      ...tree.value.map((c) => ({ label: c.name, value: c.id }))
    ]
  },
  {
    key: 'brand',
    label: '品牌',
    options: [
      { label: '不限', value: '' },
      ...brands.value.map((b) => ({ label: b, value: b }))
    ]
  },
  {
    key: 'priceRange',
    label: '价格区间',
    options: Object.keys(PRICE_RANGES).map((k) => ({
      label: PRICE_RANGES[k].label,
      value: k
    }))
  },
  {
    key: 'sortBy',
    label: '排序方式',
    options: Object.keys(SORT_OPTIONS).map((k) => ({
      label: SORT_OPTIONS[k].label,
      value: k
    }))
  }
])

async function loadCategories() {
  try {
    tree.value = await productApi.categoryTree()
  } catch (e) {
    tree.value = []
  }
}

/** 品牌列表：从全部商品里去重取 */
async function loadBrands() {
  try {
    const res = await productApi.spuPage({ pageNum: 1, pageSize: 500 })
    const list = res.records || []
    const set = new Set()
    list.forEach((p) => {
      if (p.brand) set.add(p.brand)
    })
    brands.value = Array.from(set)
  } catch (e) {
    brands.value = []
  }
}

async function loadGoods() {
  loading.value = true
  try {
    const price = PRICE_RANGES[filters.value.priceRange] || PRICE_RANGES.all
    const sort = SORT_OPTIONS[filters.value.sortBy] || SORT_OPTIONS.default

    const res = await productApi.filteredPage({
      pageNum: pageNum.value,
      pageSize,
      categoryId: filters.value.categoryId,
      minPrice: price.minPrice,
      maxPrice: price.maxPrice,
      orderBy: sort.orderBy,
      orderType: sort.orderType,
      keyword: keyword.value.trim()
    })

    let records = res.records || []

    // 品牌筛选在后端 ProductQuery 里没有对应字段，这里在前端补一刀
    if (filters.value.brand) {
      records = records.filter((p) => p.brandName === filters.value.brand)
    }

    goodsList.value = records
    total.value = filters.value.brand
      ? records.length
      : res.total || records.length
  } catch (e) {
    goodsList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 选择筛选项：再次点同一项则取消该维度 */
function selectOption(key, value) {
  const f = filters.value
  if (f[key] === value) {
    f[key] = key === 'categoryId' ? null : key === 'brand' ? '' : key === 'priceRange' ? 'all' : 'default'
  } else {
    f[key] = value
  }
  applyFilters()
}

/** 顶部下拉选分类 */
function onSearchSelect(e) {
  const index = Number(e.detail.value)
  const picked = index === 0 ? null : tree.value[index - 1]
  filters.value.categoryId = picked ? picked.id : null
  applyFilters()
}

function applyFilters() {
  pageNum.value = 1
  loadGoods()
}

function clearFilters() {
  filters.value = {
    categoryId: null,
    brand: '',
    priceRange: 'all',
    sortBy: 'default'
  }
  keyword.value = ''
  applyFilters()
}

function goDetail(id) {
  router.push(`/product/${id}`)
}

function goCategory(category) {
  // 再次点击同一分类 → 取消筛选
  if (filters.value.categoryId === category.id) {
    filters.value.categoryId = null
  } else {
    filters.value.categoryId = category.id
  }
  applyFilters()
}

onMounted(async () => {
  // 支持从首页带 categoryId 进来
  const cid = route.query.categoryId
  if (cid) {
    filters.value.categoryId = Number(cid)
  }
  await loadCategories()
  await loadBrands()
  await loadGoods()
})
</script>

<template>
  <div class="category-page">
    <div class="container">
      <!-- 搜索栏：左侧分类下拉 + 搜索框 + 搜索按钮 -->
      <div class="filter-search">
        <el-select
          class="filter-search-select"
          :model-value="searchSelectValue"
          placeholder="商品分类"
          clearable
          @change="(v) => { filters.categoryId = v === '' || v == null ? null : Number(v); applyFilters() }"
        >
          <el-option label="全部分类" value="" />
          <el-option
            v-for="c in topCategories"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>

        <input
          v-model="keyword"
          class="filter-search-input"
          placeholder="搜索商品、品牌"
          @keyup.enter="applyFilters"
        />

        <button class="filter-search-btn" type="button" @click="applyFilters">
          搜索
        </button>
      </div>

      <!-- 筛选面板 -->
      <div class="filter-panel">
        <div v-for="group in filterGroups" :key="group.key" class="filter-row">
          <span class="filter-label">{{ group.label }}:</span>
          <div class="filter-options">
            <button
              v-for="option in group.options"
              :key="String(option.value)"
              type="button"
              class="filter-option"
              :class="{ active: filters[group.key] === option.value }"
              @click="selectOption(group.key, option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div class="filter-foot">
          <span v-if="total" class="filter-count">共 {{ total }} 件商品</span>
          <button
            v-if="hasFilter"
            class="filter-clear"
            type="button"
            @click="clearFilters"
          >
            清空筛选条件
          </button>
        </div>
      </div>

      <!-- 分类卡片：点击即筛选 -->
      <div v-if="tree.length" class="cat-grid">
        <section
          v-for="c in tree"
          :key="c.id"
          class="cat-card"
          :class="{ active: filters.categoryId === c.id }"
          @click="goCategory(c)"
        >
          <span class="cat-icon">{{ (c.name || '分').slice(0, 1) }}</span>
          <div class="cat-title">
            <p class="cat-name">{{ c.name }}</p>
            <p class="cat-count">
              {{ c.children?.length ? `${c.children.length} 个细分` : '直达分类' }}
            </p>
          </div>
          <div v-if="c.children?.length" class="cat-children">
            <span v-for="child in c.children" :key="child.id" class="cat-tag">
              {{ child.name }}
            </span>
          </div>
        </section>
      </div>

      <!-- 商品列表 -->
      <div v-loading="loading" class="goods-wrap">
        <div class="goods-grid">
          <article
            v-for="g in goodsList"
            :key="g.id"
            class="goods-card"
            @click="goDetail(g.id)"
          >
            <div class="goods-cover">
              <img v-if="g.mainImage" :src="g.mainImage" :alt="g.name" />
              <span v-else class="goods-glyph">集</span>
            </div>
            <div class="goods-body">
              <p class="goods-brand">{{ g.brandName || '集物严选' }}</p>
              <p class="goods-name">{{ g.name }}</p>
              <div class="goods-foot">
                <span class="goods-price">¥{{ g.minPrice }}</span>
                <span class="goods-sales">已售 {{ g.sales || 0 }}</span>
              </div>
            </div>
          </article>
        </div>

        <el-empty
          v-if="!loading && !goodsList.length"
          description="该筛选条件下暂无商品"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.category-page {
  padding: 28px 0 60px;
  background: var(--porcelain);
  min-height: calc(100vh - 68px);
}

/* ---------------- 搜索栏 ---------------- */
.filter-search {
  display: flex;
  align-items: stretch;
  height: 52px;
  border: 2px solid var(--brand-400);
  border-radius: 6px;
  overflow: hidden;
  background: var(--paper);
}

.filter-search-select {
  flex: none;
  width: 150px;
  border-right: 1px solid var(--line);
}

.filter-search-select :deep(.el-select__wrapper) {
  height: 100%;
  min-height: 0;
  box-shadow: none;
  border-radius: 0;
  background: transparent;
}

.filter-search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  padding: 0 18px;
  font-family: var(--font-ui);
  font-size: 14px;
  color: var(--text-body);
  background: transparent;
}

.filter-search-input::placeholder {
  color: var(--text-hint);
}

.filter-search-btn {
  flex: none;
  width: 130px;
  border: none;
  background: var(--brand-400);
  color: #fff;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: background 0.18s ease;
}

.filter-search-btn:hover {
  background: var(--brand-600);
}

/* ---------------- 筛选面板 ---------------- */
.filter-panel {
  margin-top: 16px;
  padding: 6px 0 14px;
  background: var(--paper);
  border: 1px solid var(--line);
}

.filter-row {
  display: flex;
  align-items: flex-start;
  padding: 13px 22px 13px 0;
  border-bottom: 1px dashed var(--line);
}

.filter-row:last-of-type {
  border-bottom: none;
}

.filter-label {
  flex: none;
  width: 92px;
  padding-left: 22px;
  color: var(--text-muted);
  font-family: var(--font-ui);
  font-size: 14px;
  line-height: 26px;
}

.filter-options {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 6px 22px;
}

.filter-option {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--text-body);
  font-family: var(--font-ui);
  font-size: 14px;
  line-height: 26px;
  cursor: pointer;
  transition: color 0.16s ease;
}

.filter-option:hover {
  color: var(--brand-500);
}

.filter-option.active {
  color: var(--brand-400);
  font-weight: 600;
}

.filter-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 18px;
  padding: 12px 22px 0;
  border-top: 1px dashed var(--line);
  margin-top: 2px;
}

.filter-count {
  color: var(--text-hint);
  font-family: var(--font-ui);
  font-size: 12px;
}

.filter-clear {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--text-hint);
  font-family: var(--font-ui);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.16s ease;
}

.filter-clear:hover {
  color: var(--vermilion);
}

/* ---------------- 分类卡片 ---------------- */
.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.cat-card {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 13px;
  padding: 16px 18px;
  background: var(--paper);
  border: 1px solid var(--line);
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.cat-card:hover,
.cat-card.active {
  border-color: var(--brand-400);
  background: var(--brand-soft);
}

.cat-icon {
  display: grid;
  width: 40px;
  height: 40px;
  flex: none;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 2px;
  background: var(--brand-soft);
  color: var(--brand-600);
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 600;
}

.cat-title {
  flex: 1;
  min-width: 0;
}

.cat-name {
  margin: 0;
  overflow: hidden;
  color: var(--brand-600);
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-count {
  margin: 2px 0 0;
  color: var(--text-hint);
  font-size: 11px;
}

.cat-children {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  width: 100%;
  padding-top: 11px;
  border-top: 1px dotted var(--line-strong);
}

.cat-tag {
  padding: 3px 9px;
  border: 1px solid var(--line);
  border-radius: 2px;
  color: var(--text-body);
  font-family: var(--font-ui);
  font-size: 12px;
}

.cat-card:hover .cat-tag,
.cat-card.active .cat-tag {
  border-color: var(--brand-400);
}

/* ---------------- 商品列表 ---------------- */
.goods-wrap {
  margin-top: 22px;
  min-height: 240px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 16px;
}

.goods-card {
  background: var(--paper);
  border: 1px solid var(--line);
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.goods-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 22px rgba(22, 58, 46, 0.12);
}

.goods-cover {
  aspect-ratio: 1 / 1;
  display: grid;
  place-items: center;
  background: var(--porcelain-deep);
  overflow: hidden;
}

.goods-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.goods-card:hover .goods-cover img {
  transform: scale(1.05);
}

.goods-glyph {
  font-family: var(--font-display);
  font-size: 30px;
  color: var(--text-hint);
}

.goods-body {
  padding: 12px 14px 14px;
}

.goods-brand {
  margin: 0;
  color: var(--text-hint);
  font-size: 11px;
  letter-spacing: 0.04em;
}

.goods-name {
  margin: 6px 0 0;
  overflow: hidden;
  color: var(--text-title);
  font-family: var(--font-display);
  font-size: 15px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 10px;
}

.goods-price {
  color: var(--vermilion);
  font-family: var(--font-num);
  font-size: 19px;
  font-weight: 700;
}

.goods-sales {
  color: var(--text-hint);
  font-size: 11px;
}

@media (max-width: 800px) {
  .filter-label {
    width: 76px;
    padding-left: 14px;
  }

  .filter-row {
    padding-right: 14px;
  }

  .filter-search-select {
    width: 116px;
  }

  .filter-search-btn {
    width: 96px;
    font-size: 15px;
  }
}
</style>
