<script setup>
import { ref, computed } from 'vue'
import CountUp from '@/components/CountUp.vue'
import { flyToCart } from '@/utils/animation'

const props = defineProps({
  product: { type: Object, required: true },
  badge: { type: String, default: '' }
})

const emit = defineEmits(['select', 'add'])

const coverRef = ref(null)

const cover = computed(() => props.product.mainImage || '')
const initial = computed(() => (props.product.name || '商').slice(0, 1))
const catalogNo = computed(() => {
  if (props.product.id == null) return ''
  return 'NO.' + String(props.product.id).padStart(4, '0')
})

/* 加购：先从商品图飞向购物车，再交给业务层处理 */
function handleAdd() {
  flyToCart(coverRef.value, props.product.mainImage)
  emit('add', props.product)
}
</script>

<template>
  <article class="product-card" @click="emit('select', product)">
    <div ref="coverRef" class="cover">
      <img v-if="cover" :src="cover" :alt="product.name" loading="lazy" />
      <div v-else class="cover-placeholder">{{ initial }}</div>
      <span v-if="badge" class="badge">{{ badge }}</span>
    </div>

    <div class="body">
      <div class="meta-line">
        <span class="brand">{{ product.brandName || '集物严选' }}</span>
        <span v-if="catalogNo" class="catalog-no">{{ catalogNo }}</span>
      </div>

      <h3 class="name" :title="product.name">{{ product.name }}</h3>

      <div class="foot">
        <div class="price-wrap">
          <span class="price-label">售价</span>
          <CountUp class="price-value" :value="product.minPrice" />
        </div>
        <button
          class="add-btn"
          type="button"
          :aria-label="`将${product.name}加入购物车`"
          @click.stop="handleAdd"
        >
          <el-icon :size="15"><Plus /></el-icon>
          <span class="add-text">加入</span>
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.product-card {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 3px 3px 0 rgba(22, 58, 46, 0.07);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.product-card:hover {
  border-color: var(--line-strong);
  transform: translate(-2px, -2px);
  box-shadow: 7px 7px 0 rgba(22, 58, 46, 0.11);
}

.cover {
  position: relative;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--porcelain-deep);
  border-bottom: 1px solid var(--line);
}

.cover::after {
  content: '';
  position: absolute;
  inset: 10px;
  border: 1px solid rgba(22, 58, 46, 0.08);
  pointer-events: none;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.product-card:hover .cover img {
  transform: scale(1.045);
}

.cover-placeholder {
  color: var(--sage-light);
  font-family: var(--font-display);
  font-size: 56px;
  font-weight: 600;
}

.badge {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 1;
  padding: 3px 9px;
  border: 1px solid var(--vermilion);
  border-radius: 2px;
  background: var(--paper);
  color: var(--vermilion);
  font-size: 10px;
  letter-spacing: 0.06em;
}

.body {
  padding: 14px 15px 15px;
}

.meta-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 16px;
}

.brand {
  overflow: hidden;
  color: var(--sage);
  font-size: 11px;
  letter-spacing: 0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.catalog-no {
  flex-shrink: 0;
  color: var(--text-hint);
  font-family: var(--font-num);
  font-size: 9px;
  letter-spacing: 0.08em;
}

.name {
  margin-top: 7px;
  overflow: hidden;
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.55;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.18s ease;
}

.product-card:hover .name {
  color: var(--pine-600);
}

.foot {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-top: 13px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.price-wrap {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.price-label {
  color: var(--text-hint);
  font-size: 9px;
  letter-spacing: 0.1em;
}

.price-value {
  margin-top: 1px;
  color: var(--vermilion);
  font-family: var(--font-num);
  font-size: 21px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.01em;
}

.add-btn {
  display: inline-flex;
  height: 34px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 0 9px;
  border: 1px solid var(--pine-800);
  border-radius: 2px;
  background: var(--pine-800);
  color: var(--paper);
  font-size: 11px;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease,
    transform 0.18s ease, box-shadow 0.18s ease;
}

.add-text {
  letter-spacing: 0.04em;
}

.add-btn:hover {
  border-color: var(--vermilion);
  background: var(--vermilion);
  transform: translateY(-1px);
  box-shadow: 2px 2px 0 var(--gold);
}

.add-btn:active {
  transform: translateY(0) scale(0.97);
  box-shadow: none;
}

@media (max-width: 520px) {
  .body {
    padding: 11px 12px 12px;
  }

  .name {
    font-size: 14px;
  }

  .add-btn {
    width: 32px;
    padding: 0;
  }

  .add-text {
    display: none;
  }

  .price-value {
    font-size: 19px;
  }
}
</style>
