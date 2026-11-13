<script setup>
defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  moreText: { type: String, default: '查看更多' },
  moreTo: { type: String, default: '' }
})
</script>

<template>
  <div class="section-header">
    <div class="left">
      <span class="title-rule" aria-hidden="true"></span>
      <div class="title-copy">
        <h2 class="title">{{ title }}</h2>
        <span v-if="subtitle" class="subtitle">{{ subtitle }}</span>
      </div>
    </div>

    <router-link v-if="moreTo" :to="moreTo" class="more">
      <span>{{ moreText }}</span>
      <el-icon :size="12"><ArrowRight /></el-icon>
    </router-link>
  </div>
</template>

<style scoped>
.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.left {
  display: flex;
  align-items: stretch;
  gap: 14px;
  min-width: 0;
}

.title-rule {
  width: 4px;
  min-height: 40px;
  flex-shrink: 0;
  background: var(--gold);
}

.title-copy {
  display: flex;
  align-items: baseline;
  gap: 15px;
  min-width: 0;
}

.title {
  color: var(--pine-800);
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.25;
}

.subtitle {
  color: var(--sage);
  font-size: 12px;
  letter-spacing: 0.02em;
}

.more {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 5px;
  padding-bottom: 3px;
  border-bottom: 1px solid var(--line-strong);
  color: var(--pine-700);
  font-size: 12px;
  transition: color 0.18s ease, border-color 0.18s ease;
}

.more:hover {
  border-color: var(--vermilion);
  color: var(--vermilion);
}

@media (max-width: 680px) {
  .section-header {
    align-items: flex-end;
  }

  .title-copy {
    flex-direction: column;
    gap: 3px;
  }

  .title {
    font-size: 23px;
  }

  .subtitle {
    font-size: 11px;
  }

  .more {
    font-size: 11px;
  }
}
</style>
