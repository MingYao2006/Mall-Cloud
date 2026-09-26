"""Offline LLM tagging: enrich title-only products with searchable Chinese tags.

The mall catalogue has almost no description text (145/146 products have a null
`detail`), so neither vector nor keyword retrieval can map a colloquial query
like "耳机" onto a model name like "AirPods Pro 2". We close that gap once, at
index time, by asking the LLM for category/synonym tags per product and caching
the result on disk.
"""
from __future__ import annotations

import hashlib
import json
import logging
from pathlib import Path
from typing import Any, Iterable

from app.llm import chat_completion

logger = logging.getLogger(__name__)

CACHE_PATH = Path(__file__).with_name("tags_cache.json")
BATCH_SIZE = 25

_PROMPT = """你在为电商站内搜索做商品打标。下面每个商品只有标题、品牌和价格。
请为每个商品补充 3-6 个中文检索标签，覆盖：品类词、同义口语词、品牌、典型用途。
硬性要求：
1. 只补充你能从标题或品牌明确判断的信息，禁止编造参数（内存/尺寸/型号等）；
2. 标签要能让中文口语搜索命中，例如 "AirPods Pro 2" -> ["耳机","蓝牙耳机","无线耳机","苹果"];
3. "男士T恤" 这类已经是品类词的标题，标签写 ["T恤","男装","上衣"]；
4. 只输出 JSON，不要解释：{"items":[{"id":95,"tags":["耳机","蓝牙耳机"]}]}
"""


def _key(name: str) -> str:
    return hashlib.sha1(name.strip().lower().encode("utf-8")).hexdigest()[:16]


def load_cache() -> dict[str, list[str]]:
    if not CACHE_PATH.exists():
        return {}
    try:
        data = json.loads(CACHE_PATH.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {}
    except Exception as e:  # corrupted cache should never break indexing
        logger.warning("tags cache unreadable, ignoring: %s", e)
        return {}


def save_cache(cache: dict[str, list[str]]) -> None:
    try:
        CACHE_PATH.write_text(
            json.dumps(cache, ensure_ascii=False, indent=0), encoding="utf-8"
        )
    except Exception as e:
        logger.warning("tags cache not saved: %s", e)


def _batched(items: list[Any], size: int) -> Iterable[list[Any]]:
    for i in range(0, len(items), size):
        yield items[i : i + size]


async def _tag_batch(items: list[dict[str, Any]]) -> dict[str, list[str]]:
    """items: [{'id':1,'name':'..','brand':'..','price':1.0}]"""
    lines = [
        {"id": it["id"], "name": it["name"], "brand": it.get("brand") or "", "price": it.get("price")}
        for it in items
    ]
    user = json.dumps({"products": lines}, ensure_ascii=False)
    resp = await chat_completion(
        [
            {"role": "system", "content": _PROMPT},
            {"role": "user", "content": user},
        ],
        temperature=0.1,
        json_mode=True,
    )
    raw = resp.choices[0].message.content or "{}"
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        logger.warning("tagger returned non-JSON, skipping batch")
        return {}
    out: dict[str, list[str]] = {}
    for row in data.get("items") or []:
        pid = row.get("id")
        tags = row.get("tags") or []
        if pid is None:
            continue
        clean = [str(t).strip() for t in tags if str(t).strip()]
        if clean:
            out[str(pid)] = clean[:8]
    return out


async def tag_products(products: list[dict[str, Any]]) -> dict[str, list[str]]:
    """Return {'<name-hash>': [tags]} for every product, using cache when possible."""
    if not products:
        return {}
    cache = load_cache()
    pending: list[dict[str, Any]] = []
    for p in products:
        name = p.get("name") or ""
        if not name:
            continue
        if _key(name) not in cache:
            pending.append(p)

    if pending:
        logger.info("tagging %s new products via LLM", len(pending))
        for batch in _batched(pending, BATCH_SIZE):
            by_id = {str(p.get("id") or p.get("productId")): p for p in batch}
            try:
                tagged = await _tag_batch(batch)
            except Exception as e:
                logger.warning("tag batch failed: %s", e)
                continue
            for pid, tags in tagged.items():
                product = by_id.get(pid)
                if product:
                    cache[_key(product.get("name") or "")] = tags
        save_cache(cache)

    return {_key(p.get("name") or ""): cache.get(_key(p.get("name") or ""), []) for p in products if p.get("name")}
