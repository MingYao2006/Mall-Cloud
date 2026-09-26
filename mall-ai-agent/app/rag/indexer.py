"""Index mall products into the vector store, enriching them on the way in."""
from __future__ import annotations

import hashlib
import json
import logging
from pathlib import Path
from typing import Any

from app.config import get_settings
from app.rag.store import (
    last_store,
    load_keyword_index_from_milvus,
    milvus_collection_ready,
    upsert_products,
)
from app.rag.tagger import tag_products
from app.tools.mall_api import mall_request

logger = logging.getLogger(__name__)

MANIFEST_PATH = Path(__file__).with_name("index_manifest.json")


def _corpus_hash(products: list[dict[str, Any]], category_names: dict[int, str]) -> str:
    """语料指纹：商品字段 + 分类名。变了才需要重建索引。"""
    parts: list[str] = []
    for p in sorted(products, key=lambda x: int(x.get("id") or 0)):
        parts.append(
            "|".join(
                str(x)
                for x in (
                    p.get("id"),
                    p.get("name"),
                    p.get("price"),
                    p.get("categoryId") or p.get("category_id"),
                    p.get("brand") or "",
                    p.get("detail") or p.get("description") or "",
                )
            )
        )
    for cid in sorted(category_names):
        parts.append(f"cat:{cid}:{category_names[cid]}")
    return hashlib.sha1("\n".join(parts).encode("utf-8")).hexdigest()[:16]


def _load_manifest() -> dict[str, Any]:
    if not MANIFEST_PATH.exists():
        return {}
    try:
        data = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {}
    except Exception as e:
        logger.warning("index manifest unreadable, ignoring: %s", e)
        return {}


def _save_manifest(payload: dict[str, Any]) -> None:
    try:
        MANIFEST_PATH.write_text(
            json.dumps(payload, ensure_ascii=False, indent=0), encoding="utf-8"
        )
    except Exception as e:
        logger.warning("index manifest not saved: %s", e)


async def _load_category_names() -> dict[int, str]:
    try:
        data = await mall_request("GET", "/category/list")
    except Exception as e:
        logger.warning("category list unavailable: %s", e)
        return {}
    if not isinstance(data, list):
        return {}
    names: dict[int, str] = {}
    for c in data:
        try:
            names[int(c["id"])] = c.get("name") or ""
        except (KeyError, TypeError, ValueError):
            continue
    return names


async def index_all_products(*, force: bool = False) -> dict[str, Any]:
    """Pull product list from mall gateway and upsert into vector store.

    force=False 时，如果语料没变且上次已落到 Milvus，就直接跳过：
    启动自动索引因此不会每次重启都重新调一遍 embedding 接口。
    """
    try:
        data = await mall_request("GET", "/product/list")
    except Exception as e:
        return {"ok": False, "error": str(e), "count": 0}
    products = data if isinstance(data, list) else []

    category_names = await _load_category_names()
    corpus = _corpus_hash(products, category_names)

    if not force:
        manifest = _load_manifest()
        settings = get_settings()
        embedding_same = (
            manifest.get("embedding_model") == (settings.embedding_model or "")
            and manifest.get("embedding_base_url") == (settings.embedding_base_url or "")
        )
        if (
            manifest.get("corpus_hash") == corpus
            and manifest.get("vector_store") == "milvus"
            and embedding_same
            and milvus_collection_ready()
        ):
            logger.info("corpus unchanged (%s), skip reindexing", corpus)
            # 内存关键词索引在重启后是空的，必须从 Milvus 回填，否则混合召回退化
            reloaded = await load_keyword_index_from_milvus()
            return {
                "ok": True,
                "skipped": True,
                "reason": "corpus unchanged",
                "count": manifest.get("count", 0),
                "keyword_docs": reloaded,
                "corpus_hash": corpus,
            }

    # 商品表里几乎没有描述文本，靠 LLM 离线补检索标签，否则口语化查询无法召回
    tags_by_name: dict[str, list[str]] = {}
    if products and get_settings().rag_auto_tag:
        try:
            tags_by_name = await tag_products(products)
        except Exception as e:
            logger.warning("auto tagging skipped: %s", e)

    n = await upsert_products(
        products,
        category_names=category_names,
        tags_by_name=tags_by_name,
    )
    store = last_store()
    _save_manifest(
        {
            "corpus_hash": corpus,
            "vector_store": store,
            "count": n,
            "products": len(products),
            # 换 embedding 模型/服务后向量空间不同，必须重建
            "embedding_model": get_settings().embedding_model or "",
            "embedding_base_url": get_settings().embedding_base_url or "",
        }
    )
    return {
        "ok": True,
        "count": n,
        "store": store,
        "categories": len(category_names),
        "tagged": sum(1 for t in tags_by_name.values() if t),
        "corpus_hash": corpus,
    }
