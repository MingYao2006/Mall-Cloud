# Mall AI Shopping Guide Agent

独立 Python 服务，为 Mall-Cloud 提供智能导购（DeepSeek + 工具调用 + 混合检索 + SSE 流式）。

## 快速启动

```powershell
cd "D:\Java\project One\mall-ai-agent"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env   # 填入 DEEPSEEK_API_KEY 与 EMBEDDING_API_KEY
uvicorn app.main:app --host 0.0.0.0 --port 8095 --reload
```

密钥只放在 `.env`（已 gitignore），`app/config.py` 里的默认值留空，不要写回源码。

> 服务需要访问外网调用 DeepSeek 与 embedding 接口，若在受限沙箱中运行需放行网络。

## 网关路径

- Health: `GET http://localhost:9000/api/ai/health`（白名单，免 Token）
- Chat: `POST http://localhost:9000/api/ai/chat`（需 Bearer Token）
- Stream: `POST http://localhost:9000/api/ai/chat/stream`
- RAG 索引: `POST http://localhost:9000/api/ai/rag/index`

用户身份通过请求头 `Authorization` / `X-User-Id` 透传到工具层（`app/context.py`），
工具再回打网关，所以购物车、订单等接口天然是按当前用户隔离的。

## SSE 事件协议

```
data: {"type":"meta","request_id":"..."}
data: {"type":"token","content":"..."}     # 多次
data: {"type":"done","reply":"<完整回复>"}
data: {"type":"error","message":"..."}     # 出错时
```

前端用 `fetch + ReadableStream` 解析（`EventSource` 无法携带 Authorization 头）。
注意 `contextvars` 必须在生成器内部 set/reset，否则 Starlette 的独立 task context
会让 `reset()` 抛 `ValueError` 把流掐断。

## 工具（8 个）

`search_products` / `get_product_detail` / `list_categories` / `add_to_cart` /
`get_cart` / `update_cart` / `list_coupons` / `list_orders`。

function calling 循环最多 8 轮，工具参数经 `inspect.signature` 过滤后再派发。

## 检索设计

商品表几乎只有标题（146 件商品里 145 件 `detail` 为空），"耳机" 和
"AirPods Pro 2" 没有任何字面重叠，靠字面检索召不回。所以索引分三步：

1. **LLM 离线打标**（`app/rag/tagger.py`）：索引时让模型给每个商品补 3-6 个中文
   检索标签（品类词/同义口语词/品牌），结果按商品名哈希缓存到 `tags_cache.json`，
   只在首次或新增商品时调用，不占运行期开销。
2. **混合召回 + RRF 融合**（`app/rag/store.py`）：向量相似度与关键词/标签匹配两路
   召回，再用 RRF（倒数排名融合，`RRF_K = 60`）按**名次**合并，命中结果带 `match`
   字段（`vector` / `keyword` / `vector+keyword`）便于排查。

   > 为什么不能直接比分数：两条路径量纲不同——向量是 cosine（bge 下相关项
   > 0.41~0.59），关键词是词覆盖率（0~1）。直接排序会出现"覆盖率 0.667 的手机"
   > 压过"cosine 0.555 的手表"，把正确结果挤出 top-k。典型触发场景是
   > 「智能手表」：它的 bigram「智能」与「智能手机」重合，让手机拿到虚高的关键词分。
3. **兜底与提示**（`app/tools/__init__.py`）：RAG 未命中再回落站点搜索接口；
   若商品存在但被价格过滤掉，返回 `hint` + `out_of_budget`，让模型如实说明
   "最低价 999 元，超出你的预算"，而不是含糊地说"没搜到"。

### Embedding 与阈值标定

生产用真实 embedding（当前为硅基流动 `BAAI/bge-large-zh-v1.5`，1024 维），
未配置时回落到本地哈希伪向量（仅能字面匹配，仅供开发）。

`RAG_SCORE_THRESHOLD` 是按实测分布定的，不能照搬默认值：

| | 分数区间 |
|---|---|
| 全库无关查询最高分 | ≤ 0.24 |
| 相关商品 | ≥ 0.41 |

取 **0.40** 作分界：噪声查询返回 0 条，相关查询正常召回。
bge 系列基线相似度偏高（无关文本也有 0.25 左右），阈值设成 0.45 以上会把
0.41 附近的正确结果一起切掉。实测加 bge 的 query 指令前缀没有增益，故未采用。

效果对比（同一批无字面重叠的查询）：

| 查询 | 哈希伪向量 | bge-large-zh-v1.5 |
|---|---|---|
| 通勤路上听歌用的 | 无结果 | 华为FreeBuds Pro 0.416 |
| 在家做顿简餐要用的电器 | 无结果 | 电饭煲 0.459 / 微波炉 0.440 |
| 今天天气怎么样 | 随机噪声 | 0 条 |
| 帮我写个快速排序 | 随机噪声 | 0 条 |

远程 embedding 按 32 条一批提交，单条截断到 480 字（模型上下文 512 token）。

### 向量库（Milvus）

```powershell
cd "D:\Java\project One\mall-ai-agent"
docker compose up -d etcd minio milvus-standalone   # 容器：milvus-standalone / milvus-etcd / milvus-minio
# Milvus 控制台（可选）：http://localhost:19531
```

Milvus 不可用时自动回落到进程内内存索引，功能不中断（只是重启要重建）。

三个必须注意的实现点（pymilvus 3.x）：

- **`index_params` 要传 `IndexParams` 对象**，传 dict 会报 `ParamError`；
- **full-schema 模式下 `create_collection` 会忽略 `index_params`**，索引必须单独调
  `create_index`。没有索引时 `load_collection` 报 `index not found`、`search` 报
  `collection not loaded`，很容易误判成连接问题；
- **用 `upsert` 而不是 `delete + insert`**。Milvus 的 delete 是软删除，每次全量重建
  都会留下墓碑（`row_count` 会翻倍，实际 `query` 行数仍然正确）。

#### 启动时跳过重建

`index_all_products(force=False)` 会算一个**语料指纹**（商品字段 + 分类名 + 当前
embedding 模型）存到 `app/rag/index_manifest.json`，与 Milvus 里的集合比对：

| 情况 | 行为 |
|---|---|
| 指纹一致 + 集合 schema 正常 + 非空 | 跳过重建，零 embedding 调用 |
| 指纹变了（商品/分类改动） | 重建 |
| 换了 embedding 模型 | 重建（向量空间不同） |
| 集合缺字段或维度不符 | 自动 drop 并重建 |
| `POST /rag/index`（手动） | 一律 `force=True` 重建 |

跳过重建时内存关键词索引是空的，所以会**从 Milvus 回填**一份轻量文档
（`load_keyword_index_from_milvus`，不含向量），否则混合召回会退化成纯向量检索。

验证过的降级行为：Milvus 挂掉时服务不会崩，`_client()` 会被清掉重连，
检索回落到内存关键词索引，仍能返回结果。

## 评测

```powershell
python -m eval.run_eval --with-rag
```

33 条导购问题（含推荐、加购、查订单、无货兜底等），实测（真实 embedding）：

| 指标 | 结果 |
|---|---|
| 工具调用准确率 | 32/33 = 97.0% |
| 回复关键词命中率 | 33/33 = 100% |
| Top-3 召回（朴素关键词基线） | 6/26 = 23.1% |
| Top-3 召回（混合检索） | 13/26 = 50.0% |

Top-3 口径说明：26 条检索样本里有 9 条问的品类站内根本不存在（玩具、咖啡、
面膜、猫粮等），基线与混合检索都必然判负，可达上限是 17/26。另外该指标要求
品类词字面出现在 Top-3 的名称/标签里，所以它衡量的是"字面命中"而非语义质量——
真实 embedding 的收益主要体现在下表中无字面重叠的语义查询上。

唯一漏掉工具调用的是 q30「那有没有更便宜的」：省略主语的追问，评测不带上下文，
属预期行为（早期版本里未登录导致的 `get_cart` 401 也属预期）。

### 样本设计：把检索词和期望关键词解耦

纯语义样本的整句里不能出现品类词，否则又退化成字面匹配，测不出向量召回的价值。
所以样本用 `retrieval_probe` 指定实际检索词，`expect_keywords` 只管回复里该出现的词
（`run_eval.py` 里 `probe = item.get("retrieval_probe") or keywords[0] or query`）。

后来补的 3 条基线必然拿不到的样本：

| id | 查询 | 检索词 | 测什么 | 基线 | 混合 |
|---|---|---|---|---|---|
| q31 | 想买个智能手表 | 智能手表 | 跨类目共享字词（「智能」与智能手机重合） | ✗ | ✓ |
| q32 | 通勤路上听歌用的 | 通勤路上听歌用的 | 纯语义，整句无「耳机」二字 | ✗ | ✓ |
| q33 | 在家做顿简餐要用的电器 | 在家做顿简餐要用的电器 | 纯语义，无品类词 | ✗ | ✓ |

基线用的是站点 `/product/search`（`name LIKE`，免登录），口径与混合检索对等：
q01/q23 这类「耳机」查询基线必然 0 条，因为商品标题里根本没有品类词。

### 这套指标仍然测不出什么

「智能手表」这类样本盖住了跨类目共享字词的**场景**，但当前标签索引里手表的词覆盖率
已经是 1.0（标签里带「智能手表」），所以它测不出「直接比原始分数」那个具体缺陷：
那个缺陷当初是靠直接观测两路召回列表才发现的（关键字路径把「智能手机」排到 0.667、
手表只有向量路径的 0.555），评测修前修后同为 10/23，完全没反应。

要真正卡住这类回归，得对融合逻辑做单元测试——直接断言两路原始分数不可比，
而不是指望端到端指标。这是当前评测体系最大的短板。

## 数据修复

商城种子数据的分类整体错位了一圈，导致 RAG 索引文本里的「分类」字段是错的，
会让检索把吸尘器、剃须刀当成厨房电器。已在 `mall` 库一次性纠正 15 行：

| 商品 | 原分类 | 现分类 |
|---|---|---|
| 123-125 Apple Watch / 华为Watch / 小米手环 | 8 大家电 | 11 智能穿戴 |
| 134-137 冰箱 / 空调 / 洗衣机 / 电视 | 9 小家电 | 8 大家电 |
| 138-141 吸尘器 / 剃须刀 / 净化器 / 吹风机 | 10 厨房电器 | 9 小家电 |
| 142-145 微波炉 / 豆浆机 / 电饭煲 / 空气炸锅 | 11 智能穿戴 | 10 厨房电器 |

因为是四组循环错位，必须用单条 `UPDATE ... SET category_id = CASE ... END` 原子完成，
否则分步改会互相覆盖；`WHERE` 里带原值校验，重复执行无副作用。改完需重建一次索引
（`POST /rag/index`）。

## 国内网络下的镜像

`minio` / `milvus` / `etcd` 从 Docker Hub、quay.io 拉取，国内直连经常超时，
默认镜像源也可能对个别仓库返回 403。可先用镜像站拉取再打回标准 tag，
这样 `docker-compose.yml` 不用改成私有源：

```powershell
docker pull docker.m.daocloud.io/minio/minio:RELEASE.2023-03-20T20-16-18Z
docker tag  docker.m.daocloud.io/minio/minio:RELEASE.2023-03-20T20-16-18Z minio/minio:RELEASE.2023-03-20T20-16-18Z

docker pull quay.m.daocloud.io/coreos/etcd:v3.5.16
docker tag  quay.m.daocloud.io/coreos/etcd:v3.5.16 quay.io/coreos/etcd:v3.5.16

docker pull docker.m.daocloud.io/milvusdb/milvus:v2.4.15
docker tag  docker.m.daocloud.io/milvusdb/milvus:v2.4.15 milvusdb/milvus:v2.4.15
```

## 把服务也放进容器

```powershell
docker compose up -d mall-ai-agent
```

（本地开发直接跑 uvicorn 即可，上面的 compose 只用来起 Milvus 三件套。）
