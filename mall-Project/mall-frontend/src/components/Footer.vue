<template>
  <footer class="jd-footer">
    <!-- 服务保证 -->
    <div class="service-bar">
      <div class="service-inner">
        <div v-for="s in services" :key="s.title" class="service-item">
          <el-icon :size="36" color="#e1251b"><component :is="s.icon" /></el-icon>
          <div>
            <h4>{{ s.title }}</h4>
            <p>{{ s.desc }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 主链接区 -->
    <div class="footer-main">
      <div class="footer-inner">
        <div v-for="col in links" :key="col.title" class="footer-col">
          <h4>{{ col.title }}</h4>
          <a v-for="item in col.items" :key="item" @click="() => {}">{{ item }}</a>
        </div>
        <div class="footer-col qr">
          <h4>扫码下载</h4>
          <div class="qr-placeholder">MALL</div>
          <p>扫码下载 APP</p>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <div class="footer-bottom">
      <div class="footer-links">
        <a v-for="l in bottomLinks" :key="l">{{ l }}</a>
      </div>
      <p>&copy; 2026 MALL - 仿京东电商演示项目</p>
    </div>
  </footer>
</template>

<script setup>
import { Goods, Van, ChatDotRound, Wallet } from '@element-plus/icons-vue'

const services = [
  { icon: 'Goods', title: '品质保障', desc: '正品保证 假一赔十' },
  { icon: 'Van', title: '极速物流', desc: '全国包邮 极速送达' },
  { icon: 'ChatDotRound', title: '售后无忧', desc: '7天无理由退换' },
  { icon: 'Wallet', title: '价格保护', desc: '买贵补差 诚信经营' },
]

const links = [
  { title: '购物指南', items: ['购物流程', '会员介绍', '常见问题', '联系客服'] },
  { title: '配送方式', items: ['上门自提', '快递运输', '配送范围', '配送时间'] },
  { title: '支付方式', items: ['货到付款', '在线支付', '分期付款', '微信支付'] },
  { title: '售后服务', items: ['退换货政策', '退换货流程', '价格保护', '投诉建议'] },
]

const bottomLinks = ['关于我们','联系我们','加入我们','友情链接','法律声明','隐私政策']
</script>

<style scoped>
.service-bar { background: #fff; padding: 30px 0; border-bottom: 1px solid #eee; }
.service-inner { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-around; }
.service-item { display: flex; align-items: center; gap: 12px; }
.service-item h4 { font-size: 15px; color: #333; }
.service-item p { font-size: 12px; color: #999; margin-top: 4px; }

.footer-main { background: #f8f8f8; padding: 40px 0; }
.footer-inner { max-width: 1200px; margin: 0 auto; display: flex; gap: 60px; }
.footer-col { display: flex; flex-direction: column; gap: 8px; }
.footer-col h4 { font-size: 14px; color: #333; margin-bottom: 8px; }
.footer-col a { font-size: 12px; color: #666; cursor: pointer; }
.footer-col a:hover { color: var(--jd-red); }
.qr .qr-placeholder { width: 100px; height: 100px; background: var(--jd-red); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700; border-radius: 4px; }
.qr p { font-size: 12px; color: #666; text-align: center; width: 100px; }

.footer-bottom { background: #2b2b2b; padding: 20px 0; text-align: center; }
.footer-links { display: flex; justify-content: center; gap: 16px; margin-bottom: 10px; }
.footer-links a { font-size: 12px; color: #999; cursor: pointer; }
.footer-links a:hover { color: #fff; }
.footer-bottom p { font-size: 12px; color: #666; }
</style>
