<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import AppFooter from '@/components/AppFooter.vue'

const route = useRoute()

/* ---------------- 回到顶部（带滚动进度环） ---------------- */
const RADIUS = 19
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

const showToTop = ref(false)
const scrollProgress = ref(0)

const ringOffset = computed(() => CIRCUMFERENCE * (1 - scrollProgress.value / 100))

function handleScroll() {
  const el = document.documentElement
  const max = el.scrollHeight - el.clientHeight
  showToTop.value = el.scrollTop > 400
  scrollProgress.value = max > 0 ? Math.min(100, (el.scrollTop / max) * 100) : 0
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  handleScroll()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <div class="app-layout">
    <NavBar v-if="!route.meta.blank" />

    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <AppFooter v-if="!route.meta.blank" />

    <transition name="to-top">
      <button
        v-if="showToTop && !route.meta.blank"
        class="to-top"
        title="回到顶部"
        aria-label="回到顶部"
        @click="scrollToTop"
      >
        <svg class="to-top-ring" viewBox="0 0 44 44" aria-hidden="true">
          <circle class="ring-bg" cx="22" cy="22" :r="RADIUS" />
          <circle
            class="ring-progress"
            cx="22"
            cy="22"
            :r="RADIUS"
            :stroke-dasharray="CIRCUMFERENCE"
            :stroke-dashoffset="ringOffset"
          />
        </svg>
        <el-icon :size="14"><Top /></el-icon>
      </button>
    </transition>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
}

.to-top {
  position: fixed;
  right: 30px;
  bottom: 38px;
  z-index: 90;
  width: 46px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid var(--line-strong);
  border-radius: 2px;
  background: var(--paper);
  box-shadow: 4px 4px 0 rgba(22, 58, 46, 0.12);
  color: var(--pine-800);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.to-top:hover {
  background: var(--paper-warm);
  transform: translate(-2px, -2px);
  box-shadow: 6px 6px 0 var(--gold);
}

.to-top:active {
  transform: translate(0, 0);
  box-shadow: 2px 2px 0 var(--gold);
}

.to-top-ring {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: var(--porcelain-deep);
  stroke-width: 1.5;
}

.ring-progress {
  fill: none;
  stroke: var(--pine-700);
  stroke-width: 1.5;
  stroke-linecap: square;
  transition: stroke-dashoffset 0.15s linear;
}

.to-top-enter-active,
.to-top-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.to-top-enter-from,
.to-top-leave-to {
  opacity: 0;
  transform: translate3d(0, 12px, 0);
}

@media (max-width: 820px) {
  .to-top {
    right: 16px;
    bottom: 22px;
    width: 42px;
    height: 42px;
  }
}
</style>
