<script setup>
/**
 * 全部分类（一级 + 二级一起展示）
 * 首页货架导视只放前 7 个一级分类，这里展示后端返回的全部分类
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productApi } from '@/api/compat'

const router = useRouter()
const tree = ref([])
const loading = ref(false)

const totalCount = computed(() =>
  tree.value.reduce((sum, c) => sum + 1 + (c.children?.length || 0), 0)
)

async function load() {
  loading.value = true
  try {
    const data = await productApi.categoryTree()
    tree.value = Array.isArray(data) ? data : []
  } catch (e) {
    tree.value = []
  } finally {
    loading.value = false
  }
}

function goCategory(category) {
  router.push({
    path: '/home',
    query: { categoryId: category.id, categoryName: category.name }
  })
}

onMounted(load)
</script>

<template>
  <div class="container category-page">
    <header class="page-head">
      <p class="page-kicker">
        <span class="kicker-rule" aria-hidden="true"></span>
        货架导视
      </p>
      <div class="title-row">
        <h1 class="page-title">全部分类</h1>
        <span v-if="totalCount" class="page-sub">共 {{ totalCount }} 个分类</span>
      </div>
      <p class="page-lede">先找到那一栏，再从货架上慢慢挑。点击分类即可回到首页查看对应商品。</p>
    </header>

    <div v-loading="loading" class="directory">
      <section
        v-for="(c, i) in tree"
        :key="c.id"
        class="cat-card"
      >
        <span class="cat-index" aria-hidden="true">
          {{ String(i + 1).padStart(2, '0') }}
        </span>

        <div class="cat-head" @click="goCategory(c)">
          <span class="cat-icon">{{ (c.name || '分').slice(0, 1) }}</span>
          <div class="cat-title">
            <p class="cat-name">{{ c.name }}</p>
            <p class="cat-count">
              {{ c.children?.length ? `${c.children.length} 个细分` : '直达分类' }}
            </p>
          </div>
          <span class="cat-go">
            进入
            <el-icon :size="12"><ArrowRight /></el-icon>
          </span>
        </div>

        <div v-if="c.children?.length" class="cat-children">
          <button
            v-for="child in c.children"
            :key="child.id"
            type="button"
            class="cat-tag"
            @click="goCategory(child)"
          >
            {{ child.name }}
          </button>
        </div>
      </section>

      <el-empty v-if="!loading && !tree.length" class="empty" description="暂无分类" />
    </div>
  </div>
</template>

<style scoped>
.category-page {
  padding-top: 36px;
}

.page-head {
  padding-bottom: 22px;
  border-bottom: 1px solid var(--line-strong);
}

.page-kicker {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  color: var(--sage);
  font-size: 11px;
  letter-spacing: 0.1em;
}

.kicker-rule {
  display: block;
  width: 32px;
  height: 2px;
  background: var(--gold);
}

.title-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.page-title {
  margin-bottom: 0;
  font-size: 38px;
}

.page-sub {
  color: var(--sage);
  font-family: var(--font-num);
  font-size: 12px;
}

.page-lede {
  max-width: 560px;
  margin-top: 10px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.8;
}

.directory {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1px;
  min-height: 220px;
  margin-top: 24px;
  border: 1px solid var(--line-strong);
  background: var(--line);
}

.cat-card {
  position: relative;
  padding: 22px 22px 18px;
  background: var(--paper);
  transition: background 0.2s ease;
}

.cat-card:hover {
  background: var(--paper-warm);
}

.cat-index {
  position: absolute;
  top: 17px;
  right: 18px;
  color: rgba(22, 58, 46, 0.14);
  font-family: var(--font-num);
  font-size: 23px;
  font-weight: 500;
  line-height: 1;
}

.cat-head {
  display: flex;
  align-items: center;
  gap: 13px;
  cursor: pointer;
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
  color: var(--pine-700);
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 600;
}

.cat-title {
  flex: 1;
  min-width: 0;
  padding-right: 34px;
}

.cat-name {
  overflow: hidden;
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-count {
  margin-top: 2px;
  color: var(--text-hint);
  font-size: 10px;
}

.cat-go {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 3px;
  color: var(--sage);
  font-size: 11px;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.2s ease, transform 0.2s ease, color 0.2s ease;
}

.cat-card:hover .cat-go {
  opacity: 1;
  transform: translateX(0);
  color: var(--vermilion);
}

.cat-children {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dotted var(--line-strong);
}

.cat-tag {
  padding: 4px 10px;
  border: 1px solid var(--line);
  border-radius: 2px;
  background: transparent;
  color: var(--text-body);
  font-family: var(--font-ui);
  font-size: 11px;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.cat-tag:hover {
  border-color: var(--pine-700);
  background: var(--brand-soft);
  color: var(--pine-800);
}

.empty {
  grid-column: 1 / -1;
  background: var(--paper);
}

.cat-card:last-child:nth-child(odd) {
  grid-column: span 2;
}

@media (max-width: 800px) {
  .directory {
    grid-template-columns: 1fr;
  }

  .cat-card:last-child:nth-child(odd) {
    grid-column: span 1;
  }

  .page-title {
    font-size: 30px;
  }
}

@media (max-width: 520px) {
  .category-page {
    padding-top: 24px;
  }

  .cat-card {
    padding: 18px 16px 15px;
  }

  .cat-go {
    display: none;
  }
}
</style>
