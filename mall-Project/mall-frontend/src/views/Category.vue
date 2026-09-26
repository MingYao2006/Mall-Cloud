<script setup>
/**
 * 全部分类（一级 + 二级一起展示）
 * 首页 Bento 只放前 7 个一级分类，这里展示后端返回的全部分类
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productApi } from '@/api/compat'
import SectionHeader from '@/components/SectionHeader.vue'

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
      <h2 class="page-title">全部分类</h2>
      <span v-if="totalCount" class="page-sub">共 {{ totalCount }} 个分类</span>
    </header>

    <div v-loading="loading" class="cat-list">
      <section
        v-for="(c, i) in tree"
        :key="c.id"
        v-reveal="i * 50"
        class="cat-card"
      >
        <div class="cat-head" @click="goCategory(c)">
          <span class="cat-icon">{{ (c.name || '分').slice(0, 1) }}</span>
          <div class="cat-title">
            <p class="cat-name">{{ c.name }}</p>
            <p class="cat-count">{{ (c.children?.length || 0) + 1 }} 个细分</p>
          </div>
          <el-icon class="cat-arrow" :size="14"><ArrowRight /></el-icon>
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

      <el-empty v-if="!loading && !tree.length" description="暂无分类" />
    </div>
  </div>
</template>

<style scoped>
.category-page {
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

.page-sub {
  font-size: 13px;
  color: var(--text-muted);
}

.cat-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 200px;
}

.cat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.25s ease, transform 0.25s ease;
}

.cat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.cat-head {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.cat-icon {
  width: 38px;
  height: 38px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--brand-gradient);
  color: var(--text-on-brand);
  font-size: 16px;
}

.cat-title {
  flex: 1;
  min-width: 0;
}

.cat-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-title);
}

.cat-count {
  font-size: 12px;
  color: var(--text-hint);
}

.cat-arrow {
  color: var(--text-hint);
}

.cat-children {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
}

.cat-tag {
  padding: 6px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface-sunken);
  font-family: inherit;
  font-size: 13px;
  color: var(--text-body);
  cursor: pointer;
  transition: all 0.22s ease;
}

.cat-tag:hover {
  border-color: var(--brand-400);
  color: var(--brand-500);
  background: var(--brand-soft);
}
</style>
