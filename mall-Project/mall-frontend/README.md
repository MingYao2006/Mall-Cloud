# Mall-Cloud 商城前台（mall-frontend）

Vue 3 + Vite + Element Plus + Pinia + Vue Router 实现的 PC 电商前台，通过 Vite 代理对接 `mall-gateway`（:9000）的 C 端接口 `/api/c`。

## 页面结构

| 路由 | 页面 | 说明 |
|---|---|---|
| `/home` | 首页 | 分类导航、关键词搜索、商品列表分页、热门推荐 |
| `/product/:id` | 商品详情 | 图片预览、价格/库存、加入购物车、立即购买、相关推荐 |
| `/cart` | 购物车 | 勾选/数量/删除、全选与合计、结算弹窗（收货信息 + 优惠券 + 下单） |
| `/orders` | 我的订单 | 状态筛选、支付宝沙箱支付、支付结果轮询、取消订单、确认收货 |
| `/login`、`/register` | 登录注册 | JWT 登录、手机号 + 验证码注册 |
| `/seckill`、`/coupon` | 限时秒杀、优惠券 | 倒计时横幅、秒杀/领券入口 |
| `/profile` | 个人信息 | 资料修改、修改密码、头像上传入口 |
| `/ai-guide` | AI 智能导购 | 对接 `mall-ai-agent`（FastAPI + LangChain4j 工具链）的 SSE 流式问答 |

## 接口适配层 `src/api/compat.js`

后端是 Spring Cloud 微服务，接口形态与通用电商前端模板并不一致，因此这里加了一层「形状适配」，页面组件统一从 `@/api/compat` 取数：

- 商品：`Product` → 详情页/卡片使用的 spu + sku 结构（后端暂无 SKU 表时合成一个默认规格）
- 金额：后端返回 `BigDecimal`（元），前端统一按元展示
- 订单状态：`0 待支付 / 1 已支付 / 2 已发货 / 3 已完成 / 4 已取消 / 5 已退款`
- 购物车全选、订单状态筛选等后端未直接支持的查询，在前端补足

这样后续替换后端或调整接口时，只需改这一个文件。

## 设计参考

页面视觉与交互参考开源项目 **af-mall-web（阿飞商城）**：<https://github.com/29af29/af-mall-web>
（Vue3 + Vite + Element Plus 的 PC 电商前台），并按本项目的品牌与接口做了本地化改造：

- `src/assets/styles/index.css`：极光渐变背景、玻璃拟态卡片、品牌色与圆角/阴影 token、Element Plus 主题覆盖
- `src/directives/reveal.js`：`v-reveal` 滚动渐入
- `src/utils/animation.js`：加入购物车抛物线动画
- `src/components/`：`NavBar`、`ProductCard`、`CountUp`、`SectionHeader`、`AppFooter`

## 本地启动

```bash
npm install
npm run dev     # http://localhost:3000
```

需要先启动后端（网关 :9000）与相关微服务，`vite.config.js` 中已配置 `/api/c`、`/api/payment`、`/api/ai`、`/uploads` 的代理。
