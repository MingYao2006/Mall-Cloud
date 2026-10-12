# mall-oymy 公网部署（腾讯云 + Cloudflare Tunnel）

## 架构

```
浏览器 (oymy-mall.online / admin.oymy-mall.online)
        |
        v
Cloudflare 边缘（免费 SSL + WAF）
        |
        |  cloudflared 出站隧道（源站不开放任何入站端口）
        v
mall-cloudflared  --->  mall-nginx:80
                          |-- /            -> C 端前端静态文件
                          |-- /api/  /uploads/ -> mall-gateway:9000
                                                     |
                          +--------------------------+--------------------------+
                          |             |            |            |             |
                     mall-project  project-admin  user-service  product-service  ...(共 14 个)
                          |             |            |            |
                     MySQL / Redis / RabbitMQ / Nacos / Elasticsearch  ← 全部只在内网
```

前端（`mall-Project/mall-frontend`、`mall-admin-frontend`）用的都是相对路径 `/api/...`，
所以**前端代码一行都不用改**，只要 nginx 同源反代到网关即可。

---

## 一、上线前必须先知道的两件事

### 1. 国内服务器 + Cloudflare = 国内访问会变慢

你的服务器在腾讯云国内地域，但 Cloudflare 免费版的边缘节点在境外。国内用户访问的路径是：

> 用户 → Cloudflare 境外节点 → 回到国内的腾讯云

绕一圈出境再回来，延迟通常 200–400ms，体验明显不如直连。

- 如果观众**主要在国内**（面试官、同学），最优解是 **备案后直接解析到腾讯云**，不套 Cloudflare。
- Tunnel 的价值是：**不用等备案、当天就能访问、源站 IP 完全不暴露**。适合先上线再说。

### 2. 备案问题（重要）

- Tunnel 是 `cloudflared` 主动向 Cloudflare 建**出站**连接，源站**不监听 80/443**。
  所以腾讯云"未备案域名占用 80/443 端口"的拦截**不会触发**，技术上能跑通。
- 但按腾讯云的服务协议，对公网提供网站服务仍然需要 ICP 备案，属于灰色地带，
  存在被投诉/被要求整改的风险。**.online 能否备案，要在腾讯云备案系统里实际提交时确认**
  （系统里能选到这个后缀就说明可以）。
- 长期公开运营的话，建议还是走备案 + 直连。

---

## 二、服务器选型（内存账单）

这套是 14 个 JVM + 一堆中间件，**很吃内存**：

| 组件 | 数量 | 约占用 |
|---|---|---|
| Java 微服务 | 14 | 5.5 – 8 GB |
| MySQL | 1 | 0.5 – 1 GB |
| Nacos | 1 | 0.6 GB |
| RabbitMQ | 1 | 0.3 GB |
| Redis | 1 | 0.1 GB |
| nginx + cloudflared | 2 | 0.1 GB |
| Elasticsearch（可选） | 1 | 1.5 GB |
| 系统本身 | — | 0.5 GB |
| **合计** | | **≈ 9 GB（不含 ES）/ 10.5 GB（含 ES）** |

- **推荐 8C16G**，舒服。
- **4C8G 勉强**：把 `JAVA_HEAP_MAX` 调到 `256m`，并且**不要开 search profile**（省掉 ES），
  必要时把 `comment-service`、`cache-service` 也停掉。
- **2C4G 跑不动这套**，建议升配，或者只把网关 + 两个聚合服务 + 核心几个服务放上去。

---

## 三、部署步骤

### 步骤 1：服务器初始化

**安全组：只放行 22（SSH，建议限制来源 IP）。80/443 一个都不要开**——走 Tunnel 根本不需要。

```bash
# 装 Docker
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
systemctl enable --now docker

# 配国内镜像加速（不然拉镜像会非常慢）
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.m.daocloud.io"
  ],
  "log-driver": "json-file",
  "log-opts": { "max-size": "50m", "max-file": "3" }
}
EOF
systemctl restart docker

# 只有在启用 Elasticsearch 时才需要
sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" >> /etc/sysctl.conf
```

### 步骤 2：拉代码并构建 jar

```bash
cd /opt
git clone https://github.com/MingYao2006/Mall-Cloud.git mall
cd mall

# 服务器上装 JDK 23 + Maven（或用腾讯云自带的，注意版本必须 23）
# 构建全部可执行 jar
mvn -q clean package -DskipTests
ls mall-*/target/*.jar      # 确认生成了可执行 jar
```

> 如果服务器内存小于 8G，建议**在本地构建好再 rsync 上传** `mall-*/target/*.jar`，
> 避免在服务器上跑 Maven 把内存吃满。

### 步骤 3：配置环境变量

```bash
cd deploy
cp .env.example .env
chmod 600 .env
vi .env        # 填 MYSQL_PASSWORD / JWT_SECRET / RABBITMQ_PASSWORD / TUNNEL_TOKEN
```

要点：
- `JWT_SECRET` 用 `openssl rand -base64 48` 生成，**网关、mall-Project、user-service、管理端必须一致**（这里统一注入，天然一致）。
- 生产环境走 `SPRING_PROFILES_ACTIVE=cloud`，**不会加载 `application-local.yaml`**（该文件被 gitignore，服务器上也没有），
  所有密钥只能来自环境变量——这正是我们想要的行为。

### 步骤 4：启动

```bash
cd /opt/mall/deploy

# 核心版（不含搜索引擎）
docker compose up -d --build

# 需要商品搜索就加上 ES
docker compose --profile search up -d

docker compose ps          # 全部 Up / healthy
docker compose logs -f gateway
```

先本地验证（宿主机回环地址，公网访问不到）：

```bash
curl -I http://127.0.0.1:8088/
curl http://127.0.0.1:8088/api/c/product/page?page=1&size=5
```

### 步骤 5：建 Cloudflare Tunnel

在 Cloudflare 面板（**不需要在服务器上装 cloudflared CLI，用 Token 模式即可**）：

1. `Zero Trust` → `Networks` → `Tunnels` → `Create a tunnel` → 选 `Cloudflared` → 起名 `mall`
2. 复制命令里 `--token` 后面那串，填到 `deploy/.env` 的 `TUNNEL_TOKEN`，然后 `docker compose up -d cloudflared`
3. 回到 Tunnel 的 `Public Hostname` 页签，加三条（**service 填容器名，因为 cloudflared 和 nginx 在同一个 compose 网络里**）：

   | Subdomain | Domain | Service |
   |---|---|---|
   | （留空） | oymy-mall.online | `http://nginx:80` |
   | www | oymy-mall.online | `http://nginx:80` |
   | admin | oymy-mall.online | `http://nginx:80` |

4. 站点 `SSL/TLS` 模式设为 **Full**（或 Full strict），并打开 **Always Use HTTPS**

### 步骤 6：上线自检 & 安全清单

- [ ] `https://oymy-mall.online` 能打开 C 端商城
- [ ] `https://admin.oymy-mall.online` 能打开管理后台
- [ ] **改掉默认管理员 `admin` / `123456`**（见 `mall-Project/sql/mall_database.sql:164`，这密码是我用 bcrypt 验证过的，公网弱口令 = 直接沦陷）
- [ ] 服务器安全组确认**只开了 22**，`docker compose ps` 里除了 nginx 的 `127.0.0.1:8088` 没有任何公网端口
- [ ] `docker compose exec mysql mysql -uroot -p -e "show databases"` 确认表建好了
- [ ] 关掉接口文档（已在 compose 里通过 `KNIFE4J_ENABLE=false` / `SPRINGDOC_*=false` 关闭）
- [ ] `/uploads/` 只允许图片等安全类型，别让人传 `.jsp`/`.php`
- [ ] 建议在 Cloudflare 开 WAF，并对 `/api/admin` 做速率限制或 IP 白名单
- [ ] 定期备份 `mysql-data` 卷

---

## 四、常见坑

| 现象 | 原因 / 解决 |
|---|---|
| 服务全部启动失败，日志说连不上 Nacos | Nacos 启动慢（约 30–60s）。服务有 `restart: unless-stopped`，会自动重试；`docker compose ps` 等一会儿 |
| 报 `ACCESS_REFUSED - Login was refused` | RabbitMQ 用了 `guest`。`guest` 只允许本机登录，必须用 `.env` 里的 `RABBITMQ_USER/PASSWORD` |
| `java -jar` 报 `no main manifest attribute` | 该模块缺 `spring-boot-maven-plugin`（本项目 `mall-project-admin` 已修复并补上） |
| 内存不够、容器被 OOM Kill | 调小 `JAVA_HEAP_MAX`，或不开 search profile，或升配 |
| 国内访问很慢 | 见第一节：国内服务器 + Cloudflare 免费版的固有代价 |
| 支付宝回调收不到 | 把 `notify-url` 改成 `https://oymy-mall.online/api/payment/alipay/notify`（原来的 ngrok 域名会失效） |

---

## 五、AI 智能导购（可选，额外约 3–4 GB）

AI 服务依赖 Milvus（etcd + minio + milvus 三件套），比较重，单独部署：

```bash
cd /opt/mall/mall-ai-agent
cp .env.example .env && vi .env        # 填 DEEPSEEK_API_KEY 等
docker compose up -d

# 让主 compose 的网关能访问到它，把 8095 暴露到宿主机回环
# 然后在 deploy/docker-compose.yml 的 cloudflared 后面按需加路由
```

内存吃紧时建议先不上 AI 导购，保证商城和后台稳定。

---

## 六、SkyWalking 链路监控（可选）

Java Agent 已经在 `deploy/Dockerfile.runtime` 里打进运行时镜像，14 个 Java 服务不用改代码就会上报链路；只需要额外起 OAP + UI 两个容器（存储复用 `search` profile 里的 Elasticsearch）。

```bash
cd deploy

# --profile skywalking 会一并拉起 elasticsearch + oap + skywalking-ui
docker compose --profile skywalking up -d --build

docker compose ps        # mall-skywalking-oap / mall-skywalking-ui 应为 Up
```

- 控制台：http://127.0.0.1:8080（若本机 8080 被 Sentinel Dashboard 占用，改 compose 里的端口映射）
- OAP：127.0.0.1:11800（Agent gRPC）、127.0.0.1:12800（HTTP），只绑定回环地址，不对外暴露

接入后能看到：

- 拓扑图：gateway → mall-project / mall-project-admin → 各微服务 → MySQL / Redis / RabbitMQ / Nacos
- 链路详情：按 traceId 看一次请求经过哪些服务、每段耗时、异常堆栈、慢 SQL
- 实例指标：JVM 堆、GC、线程数，按服务实例拆分

| 变量 | 默认值 | 说明 |
|---|---|---|
| `SW_AGENT_COLLECTOR_BACKEND_SERVICES` | `oap:11800` | Agent 上报地址，可在 `deploy/.env` 覆盖 |
| `SW_AGENT_NAME` | 见 `docker-compose.yml` | 控制台里显示的服务名，与 Nacos 注册名保持一致 |

关闭单个服务的监控：在该服务的 `environment` 里加一行 `JDK_JAVA_OPTIONS: ""`（为空时 JVM 不会加载 Agent）。

本地用 IDE 或 `java -jar` 调试时，手动挂上 Agent：

```powershell
# 解压 apache-skywalking-java-agent-9.7.0.tgz 到 D:\tools\skywalking-agent
java -javaagent:D:\tools\skywalking-agent\skywalking-agent.jar `
     -Dskywalking.agent.service_name=mall-order-service `
     -Dskywalking.collector.backend_service=localhost:11800 `
     -jar mall-order-service\target\mall-order-service-1.0-SNAPSHOT.jar
```

注意事项：

- Agent 版本由构建参数控制：`docker compose build --build-arg SKYWALKING_AGENT_VERSION=9.7.0`。
- 多数模块按 JDK 23 编译、运行时是 `eclipse-temurin:23-jre`，而 Agent 主要在 JDK 21 及以下验证。若启动日志出现 `Unknown Java version: 67` 之类的 Byte Buddy 报错，删掉镜像里的 `/skywalking-agent/bootstrap-plugins` 目录（只损失线程池等 JDK 内部埋点），或换更新的 Agent 版本。
- OAP 与 Elasticsearch 都吃内存（OAP 默认 512M 堆），内存紧张时不要开这个 profile。
- 存储默认 `SW_STORAGE: elasticsearch`；若 OAP 启动报找不到存储实现，改成 `elasticsearch7` 再试。
