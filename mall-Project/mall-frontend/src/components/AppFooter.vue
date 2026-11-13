<script setup>
const columns = [
  { title: '购物指南', links: ['购物流程', '会员介绍', '常见问题', '联系客服'] },
  { title: '配送方式', links: ['上门自提', '限时达服务', '配送服务查询', '运费标准'] },
  { title: '支付方式', links: ['在线支付', '货到付款', '分期付款', '公司转账'] },
  { title: '售后服务', links: ['售后政策', '价格保护', '退款说明', '返修退换'] }
]
</script>

<template>
  <footer class="footer">
    <span class="footer-goldline" aria-hidden="true"></span>
    <span class="footer-watermark" aria-hidden="true">集</span>

    <div class="container footer-top">
      <div class="brand-col">
        <div class="brand">
          <span class="brand-seal">集</span>
          <span class="brand-copy">
            <span class="brand-name">集物商店</span>
            <span class="brand-sub">JIWU GOODS</span>
          </span>
        </div>
        <p class="brand-desc">精选日用百货，替你把关每一件日常。慢慢挑，买得值。</p>
        <p class="brand-meta">
          Spring Boot 3 · Spring Cloud Alibaba · Nacos · Gateway · Sentinel · LangChain4j
        </p>
      </div>

      <div class="link-cols">
        <div v-for="col in columns" :key="col.title" class="link-col">
          <h4>{{ col.title }}</h4>
          <ul>
            <li v-for="link in col.links" :key="link">
              <span class="link">{{ link }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <div class="container footer-bottom">
      <span>© 2026 集物商店 · 学习与技术演示项目，非商业用途</span>
      <span>Vue 3 + Vite</span>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  position: relative;
  overflow: hidden;
  margin-top: 72px;
  padding: 52px 0 26px;
  background: var(--pine-900);
  color: var(--paper);
}

.footer-goldline {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gold);
}

.footer-watermark {
  position: absolute;
  right: 3%;
  bottom: -48px;
  color: rgba(251, 252, 248, 0.045);
  font-family: var(--font-display);
  font-size: 230px;
  font-weight: 700;
  line-height: 1;
  pointer-events: none;
}

.footer-top {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 72px;
  padding-bottom: 38px;
  border-bottom: 1px solid rgba(251, 252, 248, 0.14);
}

.brand-col {
  width: 280px;
  flex-shrink: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 15px;
}

.brand-seal {
  display: grid;
  width: 37px;
  height: 37px;
  place-items: center;
  border: 1px solid var(--gold);
  border-radius: 2px;
  background: rgba(251, 252, 248, 0.06);
  color: var(--paper);
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-name {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.brand-sub {
  margin-top: 4px;
  color: rgba(251, 252, 248, 0.42);
  font-family: var(--font-num);
  font-size: 9px;
  letter-spacing: 0.18em;
}

.brand-desc {
  color: rgba(251, 252, 248, 0.68);
  font-size: 12px;
  line-height: 1.8;
}

.brand-meta {
  margin-top: 12px;
  color: rgba(251, 252, 248, 0.34);
  font-family: var(--font-num);
  font-size: 9px;
  line-height: 1.8;
  letter-spacing: 0.04em;
}

.link-cols {
  display: grid;
  flex: 1;
  grid-template-columns: repeat(4, 1fr);
  gap: 26px;
}

.link-col h4 {
  margin-bottom: 14px;
  color: var(--gold);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.link-col li + li {
  margin-top: 10px;
}

.link {
  color: rgba(251, 252, 248, 0.58);
  font-size: 12px;
  cursor: pointer;
  transition: color 0.18s ease;
}

.link:hover {
  color: var(--paper);
}

.footer-bottom {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 20px;
  color: rgba(251, 252, 248, 0.36);
  font-size: 10px;
}

@media (max-width: 960px) {
  .footer-top {
    flex-direction: column;
    gap: 34px;
  }

  .brand-col {
    width: 100%;
  }
}

@media (max-width: 620px) {
  .footer {
    margin-top: 52px;
    padding-top: 38px;
  }

  .link-cols {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px 18px;
  }

  .footer-bottom {
    align-items: flex-start;
    flex-direction: column;
    gap: 5px;
  }

  .footer-watermark {
    font-size: 150px;
    bottom: -24px;
  }
}
</style>
