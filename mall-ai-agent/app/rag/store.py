"""Vector store for product retrieval.

Two retrieval paths are merged:
  1. vector similarity (Milvus, or the in-process fallback store)
  2. keyword / tag matching over the product corpus

Path 2 exists because the catalogue is almost title-only: a colloquial query
like "耳机" has zero token overlap with "AirPods Pro 2", so vector recall built
on the local hash embedder cannot find it. The tags produced by `tagger.py`
give that path something to match on.
"""
from __future__ import annotations

import logging
import re
from functools import lru_cache
from typing import Any, Optional

from app.config import get_settings
from app.rag.embedder import embed_query, embed_texts, get_embedding_dim

logger = logging.getLogger(__name__)

# In-memory fallback when Milvus is unavailable
_MEMORY_DOCS: list[dict[str, Any]] = []

_TAG_RE = re.compile(r"<[^>]+>")
_ASCII_RE = re.compile(r"[a-z0-9]+")
_CJK_RE = re.compile(r"[\u4e00-\u9fff]+")


@lru_cache(maxsize=1)
def _client():
    """单例 MilvusClient（旧 connections.connect ORM API 在 pymilvus 3.1 会被移除）。"""
    from pymilvus import MilvusClient

    settings = get_settings()
    return MilvusClient(uri=f"http://{settings.milvus_host}:{settings.milvus_port}")


def _reset_client() -> None:
    _client.cache_clear()


def _collection_schema_info(client, name: str) -> tuple[Optional[int], set[str]]:
    """返回 (向量维度, 字段名集合)。"""
    try:
        desc = client.describe_collection(collection_name=name)
        fields = desc.get("fields") or []
        names = {f.get("name") for f in fields if f.get("name")}
        dim = None
        for f in fields:
            params = f.get("params") or {}
            if "dim" in params:
                dim = int(params["dim"])
        return dim, names
    except Exception as e:
        logger.debug("describe_collection failed: %s", e)
        return None, set()


REQUIRED_FIELDS = {"id", "product_id", "category_id", "price", "text", "tags", "embedding"}


def _index_params():
    """pymilvus 3.x 要求 IndexParams 对象，传 dict 会报 ParamError。"""
    from pymilvus.milvus_client.index import IndexParams

    params = IndexParams()
    params.add_index(
        field_name="embedding",
        index_type="IVF_FLAT",
        index_name="embedding_idx",
        metric_type="IP",
        params={"nlist": 128},
    )
    return params


def ensure_collection(dim: int) -> bool:
    """Make sure the collection exists, is indexed and is loaded.

    full-schema 模式下 `create_collection` 会忽略 `index_params`，索引必须单独用
    `create_index` 建；没有索引时 `load_collection` 报 index not found、search 报
    collection not loaded。
    """
    try:
        from pymilvus import CollectionSchema, DataType, FieldSchema

        settings = get_settings()
        name = settings.milvus_collection
        client = _client()

        if client.has_collection(name):
            existing_dim, existing_fields = _collection_schema_info(client, name)
            if existing_dim is not None and existing_dim != dim:
                # 换 embedding 模型后维度会变，旧集合写不进新向量，直接重建
                logger.warning(
                    "collection %s dim=%s but need %s, recreating", name, existing_dim, dim
                )
                client.drop_collection(collection_name=name)
            elif not REQUIRED_FIELDS.issubset(existing_fields):
                logger.warning(
                    "collection %s missing fields %s, recreating",
                    name,
                    REQUIRED_FIELDS - existing_fields,
                )
                client.drop_collection(collection_name=name)

        if not client.has_collection(name):
            schema = CollectionSchema(
                [
                    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=False),
                    FieldSchema(name="product_id", dtype=DataType.INT64),
                    FieldSchema(name="category_id", dtype=DataType.INT64),
                    FieldSchema(name="price", dtype=DataType.FLOAT),
                    FieldSchema(name="text", dtype=DataType.VARCHAR, max_length=2000),
                    FieldSchema(name="tags", dtype=DataType.VARCHAR, max_length=512),
                    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=dim),
                ],
                description="mall product rag",
            )
            client.create_collection(collection_name=name, schema=schema)
            logger.info("created Milvus collection %s (dim=%s)", name, dim)

        if not client.list_indexes(collection_name=name):
            client.create_index(collection_name=name, index_params=_index_params())

        client.load_collection(collection_name=name)
        return True
    except Exception as e:
        logger.warning("Milvus unavailable, using memory store: %s", e)
        _reset_client()
        return False


def _strip_html(text: Any) -> str:
    return _TAG_RE.sub(" ", str(text or "")).strip()


def _terms(text: Any) -> set[str]:
    """ASCII words + CJK bigrams; good enough for short commerce queries."""
    lowered = str(text or "").lower()
    terms = set(_ASCII_RE.findall(lowered))
    for chunk in _CJK_RE.findall(lowered):
        if len(chunk) == 1:
            terms.add(chunk)
        else:
            terms.update(chunk[i : i + 2] for i in range(len(chunk) - 1))
    return terms


def _doc_match_text(doc: dict[str, Any]) -> str:
    return " ".join(
        [
            str(doc.get("name") or ""),
            str(doc.get("brand") or ""),
            str(doc.get("category_name") or ""),
            " ".join(doc.get("tags") or []),
            str(doc.get("description") or ""),
        ]
    )


def keyword_search(
    query: str,
    docs: list[dict[str, Any]],
    *,
    min_ratio: float = 0.5,
    limit: int = 5,
) -> list[tuple[float, dict[str, Any]]]:
    q_terms = _terms(query)
    if not q_terms:
        return []
    scored: list[tuple[float, dict[str, Any]]] = []
    for doc in docs:
        d_terms = _terms(_doc_match_text(doc))
        if not d_terms:
            continue
        overlap = len(q_terms & d_terms) / len(q_terms)
        if overlap < min_ratio:
            continue
        bonus = 0.0
        if query and query in str(doc.get("name") or ""):
            bonus += 0.3
        if query in (doc.get("tags") or []):
            bonus += 0.3
        scored.append((min(1.0, overlap + bonus), doc))
    scored.sort(key=lambda x: x[0], reverse=True)
    return scored[:limit]


async def upsert_products(
    products: list[dict[str, Any]],
    *,
    category_names: Optional[dict[int, str]] = None,
    tags_by_name: Optional[dict[str, list[str]]] = None,
) -> int:
    """
    products items: id, name, detail/description, categoryId, price, brand, ...
    category_names: {category_id: name}
    tags_by_name: {name_hash: [tag]}
    """
    if not products:
        return 0
    category_names = category_names or {}
    tags_by_name = tags_by_name or {}

    docs: list[dict[str, Any]] = []
    texts: list[str] = []
    for p in products:
        pid = int(p.get("id") or p.get("productId"))
        name = p.get("name") or ""
        # 商品表的字段是 detail（且多为空），旧代码只读 description 导致索引文本只剩标题
        desc = _strip_html(p.get("detail") or p.get("description") or "")
        brand = p.get("brand") or ""
        try:
            category_id = int(p.get("categoryId") or p.get("category_id") or 0)
        except (TypeError, ValueError):
            category_id = 0
        category_name = category_names.get(category_id, "")
        tags = tags_by_name.get(_name_key(name), [])
        if not tags:
            tags = tags_by_name.get(name, [])

        text = (
            f"{name}。分类：{category_name}。品牌：{brand}。"
            f"标签：{'/'.join(tags)}。卖点：{desc}。价格：{p.get('price')}"
        )
        texts.append(text[:1800])
        docs.append(
            {
                "id": pid,
                "product_id": pid,
                "category_id": category_id,
                "category_name": category_name,
                "price": float(p.get("price") or 0),
                "text": text[:1800],
                "name": name,
                "description": desc,
                "brand": brand,
                "tags": tags,
            }
        )
    vectors = await embed_texts(texts)

    dim = await get_embedding_dim()
    vector_stored = False
    if ensure_collection(dim):
        try:
            settings = get_settings()
            client = _client()
            rows = [
                {
                    "id": d["id"],
                    "product_id": d["product_id"],
                    "category_id": d["category_id"],
                    "price": d["price"],
                    "text": d["text"],
                    "tags": "|".join(d.get("tags") or []),
                    "embedding": vec,
                }
                for d, vec in zip(docs, vectors)
            ]
            # 用 upsert 而不是 delete+insert：Milvus 的 delete 是软删除，
            # 每次重建索引都会留下墓碑，row_count 会一路涨上去。
            client.upsert(collection_name=settings.milvus_collection, data=rows)
            client.flush(collection_name=settings.milvus_collection)
            logger.info("upserted %s products into Milvus", len(docs))
            vector_stored = True
        except Exception as e:
            logger.warning("Milvus upsert failed, memory fallback: %s", e)
            _reset_client()

    # 内存里始终保留一份关键词索引：混合召回的关键词那一路要按名称/品牌/标签匹配，
    # 光有 Milvus 里的向量是做不到的（Milvus 的 like 只支持前缀匹配）。
    # 向量已经落到 Milvus 时就不再占内存存 embedding，只有 Milvus 不可用才连向量一起留作兜底。
    global _MEMORY_DOCS
    if vector_stored:
        incoming = {d["id"]: dict(d) for d in docs}
    else:
        incoming = {d["id"]: {**d, "embedding": vec} for d, vec in zip(docs, vectors)}
    kept = [d for d in _MEMORY_DOCS if d["id"] not in incoming]
    _MEMORY_DOCS = kept + list(incoming.values())
    global _LAST_STORE
    _LAST_STORE = "milvus" if vector_stored else "memory"
    logger.info(
        "indexed %s products (store=%s, memory_docs=%s)",
        len(docs),
        _LAST_STORE,
        len(_MEMORY_DOCS),
    )
    return len(docs)


_LAST_STORE = "none"


def last_store() -> str:
    """上一次索引落在了哪里：milvus / memory。"""
    return _LAST_STORE


async def load_keyword_index_from_milvus() -> int:
    """从 Milvus 回填内存关键词索引。

    启动时若语料没变会跳过重建，此时进程内存里的关键词索引是空的，
    混合召回的关键词那一路就废了；这里把轻量文档捞回来（不含向量）。
    """
    global _MEMORY_DOCS
    try:
        settings = get_settings()
        client = _client()
        name = settings.milvus_collection
        if not client.has_collection(name):
            return 0
        rows = client.query(
            collection_name=name,
            filter="id >= 0",
            output_fields=["id", "product_id", "category_id", "price", "text", "tags"],
            limit=16384,
        )
    except Exception as e:
        logger.warning("keyword index reload failed: %s", e)
        return 0

    docs: list[dict[str, Any]] = []
    for r in rows:
        text = r.get("text") or ""
        docs.append(
            {
                "id": r.get("id"),
                "product_id": r.get("product_id"),
                "category_id": r.get("category_id"),
                "category_name": "",
                "price": float(r.get("price") or 0),
                "text": text,
                "name": text.split("。")[0],
                "description": text,
                "brand": "",
                "tags": [t for t in (r.get("tags") or "").split("|") if t],
            }
        )
    _MEMORY_DOCS = docs
    logger.info("reloaded %s docs into keyword index from Milvus", len(docs))
    return len(docs)


def milvus_collection_ready() -> bool:
    """Milvus 里是否已有可查询的集合（用于判断能不能跳过重建索引）。"""
    try:
        settings = get_settings()
        client = _client()
        name = settings.milvus_collection
        if not client.has_collection(name):
            return False
        _, fields = _collection_schema_info(client, name)
        missing = REQUIRED_FIELDS - fields
        if missing:
            logger.info("milvus collection schema stale (missing %s)", missing)
            return False
        stats = client.get_collection_stats(name)
        return int(stats.get("row_count") or 0) > 0
    except Exception as e:
        logger.debug("milvus readiness check failed: %s", e)
        return False


def _name_key(name: str) -> str:
    import hashlib

    return hashlib.sha1((name or "").strip().lower().encode("utf-8")).hexdigest()[:16]


def _to_hit(doc: dict[str, Any], score: float, match: str) -> dict[str, Any]:
    return {
        "id": doc["product_id"],
        "productId": doc["product_id"],
        "categoryId": doc["category_id"],
        "price": doc["price"],
        "name": doc.get("name") or doc["text"].split("。")[0],
        "description": doc.get("description") or doc["text"],
        "brand": doc.get("brand"),
        "tags": doc.get("tags") or [],
        "score": score,
        "match": match,
    }


async def _vector_search(
    qvec: list[float],
    limit: int,
    score_threshold: float,
) -> list[dict[str, Any]]:
    """Milvus first, then the in-process fallback store. Ordered by similarity."""
    hits: list[dict[str, Any]] = []
    try:
        settings = get_settings()
        client = _client()
        if client.has_collection(settings.milvus_collection):
            raw = client.search(
                collection_name=settings.milvus_collection,
                data=[qvec],
                anns_field="embedding",
                limit=limit,
                search_params={"metric_type": "IP", "params": {"nprobe": 16}},
                output_fields=["product_id", "category_id", "price", "text"],
            )
            for hit in (raw[0] if raw else []):
                score = float(hit.get("distance") or 0.0)
                if score < score_threshold:
                    continue
                entity = hit.get("entity") or {}
                pid = entity.get("product_id", hit.get("id"))
                text = entity.get("text") or ""
                hits.append(
                    {
                        "id": pid,
                        "productId": pid,
                        "categoryId": entity.get("category_id"),
                        "price": entity.get("price"),
                        "name": text.split("。")[0],
                        "description": text,
                        "tags": [],
                        "score": score,
                    }
                )
    except Exception as e:
        logger.debug("Milvus search skipped: %s", e)
        _reset_client()
        hits = []

    if hits:
        return hits[:limit]

    from app.rag.embedder import cosine_sim

    scored = sorted(
        (
            (cosine_sim(qvec, doc["embedding"]), doc)
            for doc in _MEMORY_DOCS
            if "embedding" in doc
        ),
        key=lambda x: x[0],
        reverse=True,
    )
    return [
        _to_hit(doc, score, "vector")
        for score, doc in scored[:limit]
        if score >= score_threshold
    ]


def _passes_filters(
    hit: dict[str, Any],
    max_price: Optional[float],
    min_price: Optional[float],
    category_id: Optional[int],
) -> bool:
    price = hit.get("price")
    try:
        price_f = float(price) if price is not None else None
    except (TypeError, ValueError):
        price_f = None
    if max_price is not None and price_f is not None and price_f > max_price:
        return False
    if min_price is not None and price_f is not None and price_f < min_price:
        return False
    if category_id is not None:
        cat = hit.get("categoryId")
        try:
            if int(cat) != int(category_id):
                return False
        except (TypeError, ValueError):
            return False
    return True


# 两路召回的分数量纲不同：向量是 cosine（bge 下相关项 0.41~0.59），
# 关键词是词覆盖率（0~1）。直接比大小会让"覆盖率 0.667 的手机"压过
# "cosine 0.555 的手表"，把正确结果挤出 top-k。
# 因此按名次做 RRF（Reciprocal Rank Fusion）融合，而不是比较原始分数。
RRF_K = 60


async def search_vectors(
    query: str,
    *,
    top_k: Optional[int] = None,
    score_threshold: Optional[float] = None,
    max_price: Optional[float] = None,
    min_price: Optional[float] = None,
    category_id: Optional[int] = None,
) -> list[dict[str, Any]]:
    settings = get_settings()
    top_k = top_k or settings.rag_top_k
    score_threshold = (
        settings.rag_score_threshold if score_threshold is None else score_threshold
    )
    depth = top_k * 3
    qvec = await embed_query(query)

    fused: dict[Any, dict[str, Any]] = {}
    order: list[Any] = []

    def _fuse(hit: dict[str, Any], source: str, rank: int) -> None:
        pid = hit["id"]
        weight = 1.0 / (RRF_K + rank)
        existing = fused.get(pid)
        if existing is not None:
            existing["rank_score"] += weight
            if source not in existing["match"]:
                existing["match"] = f"{existing['match']}+{source}"
            return
        item = dict(hit)
        item["match"] = source
        item["rank_score"] = weight
        fused[pid] = item
        order.append(pid)

    for rank, hit in enumerate(
        await _vector_search(qvec, depth, score_threshold), 1
    ):
        _fuse(hit, "vector", rank)

    for rank, (score, doc) in enumerate(
        keyword_search(query, _MEMORY_DOCS, limit=depth), 1
    ):
        _fuse(_to_hit(doc, score, "keyword"), "keyword", rank)

    results = [
        fused[pid]
        for pid in order
        if _passes_filters(fused[pid], max_price, min_price, category_id)
    ]
    results.sort(key=lambda h: h["rank_score"], reverse=True)
    return results[:top_k]


def memory_doc_count() -> int:
    return len(_MEMORY_DOCS)
