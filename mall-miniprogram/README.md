# Mall-Cloud 小程序端

基于 **uni-app + Vue3 + Vite** 的商城小程序工程，对接现有后端网关 `http://localhost:9000` 的 C 端接口 `/api/c/**`。

完整规划见上级目录的 `小程序开发计划.md`。

## 一、技术栈

| 项 | 版本 |
|---|---|
| uni-app | 3.0.0-5020620260917001 |
| Vue | 3.4.x |
| Vite | 5.2.8 |
| 状态管理 | Pinia 2.x |

## 二、目录结构

```text
mall-miniprogram/
├── vite.config.js          # H5 端 /api 代理到网关
├── src/
│   ├── main.js             # 应用入口（createSSRApp + Pinia）
│   ├── App.vue             # 启动时恢复登录态
│   ├── pages.json          # 页面路由、分包、tabBar
│   ├── manifest.json       # 小程序 AppID 等平台配置
│   ├── config/index.js     # 环境配置（后端地址、存储 key）
│   ├── utils/
│   │   ├── request.js      # 请求封装（token 注入 / 401 跳登录 / Result 解包 / 上传）
│   │   ├── auth.js         # 登录态存储
│   │   └── format.js       # 金额、时间、订单状态格式化
│   ├── api/                # 按模块拆分的接口层
│   ├── store/              # Pinia：user / cart
│   ├── components/         # 公共组件
│   ├── pages/              # 主包页面
│   ├── pages-order/        # 订单分包
│   ├── pages-user/         # 用户分包
│   └── pages-activity/     # 营销分包
```

## 三、运行

```powershell
cd "D:\Java\project One\mall-miniprogram"
npm install

# 微信小程序：产物在 dist/dev/mp-weixin，用微信开发者工具导入该目录
npm run dev:mp-weixin

# H5：浏览器快速联调（http://localhost:3100）
npm run dev:h5
```

生产构建：

```powershell
npm run build:mp-weixin
```

## 四、必要的配置

1. **AppID**：在 `src/manifest.json` 的 `mp-weixin.appid` 填入自己的小程序 AppID（留空时微信开发者工具会使用测试号）。
2. **后端地址**：在 `src/config/index.js` 修改 `DEV_ORIGIN` 与 `PROD_ORIGIN`。
3. **微信开发者工具**：本地联调需勾选「详情 → 本地设置 → 不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」，否则请求 `http://localhost:9000` 会被拦截。
4. **tabBar 图标**：当前 tabBar 仅使用文字（`iconPath` 为可选项），如需图标请放入 `src/static/tabbar/` 并在 `pages.json` 补充 `iconPath` / `selectedIconPath`。

## 五、与 Web 端请求封装的对应关系

| Web 端（axios） | 小程序端（uni.request） |
|---|---|
| `baseURL: '/api/c'` | `BASE_URL`（H5 走代理，小程序走完整地址） |
| 请求拦截注入 `Authorization` | `utils/request.js` 自动注入 |
| 响应拦截解包 `code !== 200` | 同样解包并 `reject` |
| 401 跳 `/login` | 401 跳 `pages-user/login/index` |
| FormData 上传头像 | `uni.uploadFile` 的 `upload()` |
| `localStorage` | `uni.getStorageSync` / `setStorageSync` |

## 六、后端待实现/待调整项（阻塞小程序完整功能）

| 优先级 | 事项 | 说明 |
|---|---|---|
| ✅ 已完成 | `POST /api/c/users/wx-login` | 已实现：user-service 换 openid 并签发 JWT，网关与聚合层白名单已放行 |
| ✅ 已完成 | `user` 表新增 `openid` / `unionid` 字段 | 迁移脚本 `mall-Project/sql/add_wx_login.sql` 已执行 |
| P0 | HTTPS 域名 + 备案 | 微信小程序正式版强依赖 |
| P1 | 微信支付 JSAPI | 现有支付宝「电脑网站支付」在小程序内不可用 |
| P1 | 收货地址簿 | `user_address` 表与接口，订单确认页需要 |
| P1 | 越权修复 | `cart` / `coupon` / `seckill` 的 `userId` 改由后端从 token 解析 |

## 七、已知接口问题（已写入代码注释）

- 商品详情 `GET /product/{id}`、分类详情 `GET /category/{id}`、评论接口不在网关白名单内，当前需要登录后才能访问。
- Web 端调用的 `/seckill/list`、`/seckill/{id}` 在后端 `SeckillController` 中并不存在；实际可用的是 `POST /seckill/coupon/{couponId}` 与 `GET /seckill/doSeckill`。
- 用户头像接口返回相对路径 `/uploads/avatar/xxx.jpg`，小程序 `<image>` 无法直接渲染，需用 `resolveFileUrl()` 补全。

## 八、联调记录（2026-09-22）

微信登录后端已实现并完成端到端验证。

### 启动顺序（本地联调）

```powershell
# 1. 基础设施：MySQL(3306) / Redis 主从(6379,6380,6381) / Nacos(8848)
#    Redis 由 Docker 提供；Nacos 启动方式见《项目启动流程.md》

# 2. 服务（须在可访问外网的终端启动，否则调不通微信 jscode2session 接口）
mvn -o -pl mall-user-service spring-boot:run   # 8081
mvn -o -pl mall-Project spring-boot:run        # 9090
mvn -o -pl mall-gateway spring-boot:run        # 9000
```

> 首次需先执行 `mvn -o install -DskipTests -pl mall-user-service,mall-Project,mall-gateway -am`，
> 因为这几个模块未配置 spring-boot-maven-plugin，且 mall-common 需要先进入本地仓库。

### 验证结果

| 用例 | 请求 | 结果 |
|---|---|---|
| 微信登录（经网关） | `POST /api/c/users/wx-login`，body `{"code":"testcode"}` | `200` + `{"code":400,"message":"微信登录失败：code 无效或已过期，请重新登录"}` |
| 白名单未被误放开 | `GET /api/c/users/info`（不带 token） | `401` |

`code 无效或已过期` 对应微信错误码 40029，是无效 code 的标准返回；
能返回该文案说明 AppID / AppSecret 有效、`jscode2session` 调用链路正常、错误码映射生效。

### 下一步

在微信开发者工具中导入 `dist/dev/mp-weixin`，填入真实 AppID，
点击「微信一键登录」即可用真实 code 完成登录。
