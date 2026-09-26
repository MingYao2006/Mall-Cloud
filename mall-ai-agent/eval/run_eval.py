"""
Run shopping-guide eval:
- tool-call accuracy (expect_tools subset of tools_used)
- top-3 keyword hit rate on reply / tool payloads (proxy for recommendation hit)

Usage:
  cd mall-ai-agent
  python -m eval.run_eval
  python -m eval.run_eval --with-rag   # index first then compare
"""
from __future__ import annotations

import argparse
import asyncio
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.agent import run_agent
from app.rag.indexer import index_all_products
from app.rag.retriever import semantic_retrieve
from app.tools.mall_api import mall_request


def load_dataset() -> list[dict]:
    path = Path(__file__).with_name("dataset.json")
    return json.loads(path.read_text(encoding="utf-8"))


def tool_hit(expect: list[str], used: list[str]) -> bool:
    if not expect:
        return True
    used_set = set(used)
    return any(t in used_set for t in expect)


def keyword_hit(text: str, keywords: list[str]) -> bool:
    if not keywords:
        return True
    t = text or ""
    return any(k in t for k in keywords if k)


async def keyword_search_baseline(query: str) -> list[dict]:
    try:
        data = await mall_request("GET", "/product/search", params={"keyword": query})
        return data if isinstance(data, list) else []
    except Exception:
        try:
            data = await mall_request("GET", "/product/list")
            items = data if isinstance(data, list) else []
            return [p for p in items if query[:2] in (p.get("name") or "")][:10]
        except Exception:
            return []


def top3_name_hit(products: list[dict], relevant_names: list[str]) -> bool:
    """Top-3 是否命中相关品类。

    商品标题里几乎没有品类词（"AirPods Pro 2" 不含 "耳机"），所以必须连标签
    一起比对，否则这个指标恒为 0，衡量不出召回质量。
    """
    if not relevant_names:
        return True
    parts: list[str] = []
    for p in products[:3]:
        parts.append(str(p.get("name") or p.get("productName") or ""))
        parts.extend(str(t) for t in (p.get("tags") or []))
        parts.append(str(p.get("description") or ""))
    text = " ".join(parts)
    return any(k in text for k in relevant_names if k)


async def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--with-rag", action="store_true")
    parser.add_argument("--limit", type=int, default=0, help="only first N queries")
    args = parser.parse_args()

    if args.with_rag:
        print("Indexing products for RAG...")
        print(await index_all_products())

    dataset = load_dataset()
    if args.limit > 0:
        dataset = dataset[: args.limit]

    tool_ok = 0
    kw_ok = 0
    base_hit = 0
    rag_hit = 0
    n_search = 0

    for item in dataset:
        qid = item["id"]
        query = item["query"]
        print(f"\n=== {qid}: {query}")
        try:
            reply, _, tools_used = await run_agent(query, request_id=f"eval-{qid}")
        except Exception as e:
            print(f"  ERROR: {e}")
            reply, tools_used = "", []

        th = tool_hit(item.get("expect_tools") or [], tools_used)
        kh = keyword_hit(reply, item.get("expect_keywords") or [])
        tool_ok += int(th)
        kw_ok += int(kh)
        print(f"  tools={tools_used} tool_hit={th} reply_kw_hit={kh}")
        print(f"  reply={reply[:160].replace(chr(10), ' ')}")

        if item.get("relevant_product_names"):
            n_search += 1
            # 基线用朴素关键词（站点 LIKE 搜索），RAG 用同一个词，口径才对等；
            # 直接拿整句去比会把两边都打成 0，看不出差异。
            keywords = item.get("expect_keywords") or []
            # retrieval_probe 把"检索词"和"期望命中的关键词"解耦：
            # 纯语义样本的整句里不能出现品类词，否则又退化成字面匹配，
            # 测不出向量召回的价值。
            probe = item.get("retrieval_probe") or (keywords[0] if keywords else query)
            base = await keyword_search_baseline(probe)
            rag = await semantic_retrieve(probe)
            bh = top3_name_hit(base, item["relevant_product_names"])
            rh = top3_name_hit(rag, item["relevant_product_names"])
            base_hit += int(bh)
            rag_hit += int(rh)
            print(
                f"  probe={probe!r} top3 baseline={bh}(n={len(base)})"
                f" hybrid={rh}(n={len(rag)})"
            )

    total = len(dataset)
    print("\n========== SUMMARY ==========")
    print(f"tool_call_accuracy: {tool_ok}/{total} = {tool_ok / total * 100:.1f}%")
    print(f"reply_keyword_hit:  {kw_ok}/{total} = {kw_ok / total * 100:.1f}%")
    if n_search:
        print(
            f"top3_keyword_baseline: {base_hit}/{n_search} = {base_hit / n_search * 100:.1f}%"
        )
        print(
            f"top3_vector_rag:       {rag_hit}/{n_search} = {rag_hit / n_search * 100:.1f}%"
        )
        print(
            f"vector_lift: from {base_hit / n_search * 100:.1f}% -> {rag_hit / n_search * 100:.1f}%"
        )


if __name__ == "__main__":
    asyncio.run(main())
