"""Eight shopping-guide tools calling mall gateway APIs."""
from __future__ import annotations

import logging
from typing import Any, Optional

from app.config import get_settings
from app.tools.mall_api import MallApiError, mall_request, tool_result_err, tool_result_ok

logger = logging.getLogger(__name__)


def _apply_filters(
    items: list[dict[str, Any]],
    max_price: Optional[float],
    min_price: Optional[float],
    category_id: Optional[int],
) -> list[dict[str, Any]]:
    filtered: list[dict[str, Any]] = []
    for item in items:
        price = item.get("price")
        try:
            price_f = float(price) if price is not None else None
        except (TypeError, ValueError):
            price_f = None
        if max_price is not None and price_f is not None and price_f > max_price:
            continue
        if min_price is not None and price_f is not None and price_f < min_price:
            continue
        if category_id is not None and item.get("categoryId") not in (category_id, str(category_id)):
            if item.get("category_id") not in (category_id, str(category_id)):
                continue
        filtered.append(item)
    return filtered


async def _rag_search(
    keyword: str,
    *,
    max_price: Optional[float] = None,
    min_price: Optional[float] = None,
    category_id: Optional[int] = None,
) -> list[dict[str, Any]]:
    """向量 + 关键词标签混合召回（见 app/rag/store.py）。"""
    try:
        from app.rag.retriever import semantic_retrieve

        return await semantic_retrieve(
            keyword,
            max_price=max_price,
            min_price=min_price,
            category_id=category_id,
        )
    except Exception as e:
        logger.warning("semantic_retrieve skipped: %s", e)
        return []


async def _gateway_search(keyword: str) -> list[dict[str, Any]]:
    """站点搜索接口兜底：/product/search 走 DB（C 端白名单，免登录）。

    不要再往 `/search/products`（ES）退：本项目没有 ES 搜索服务，那条路径
    未鉴权返回 401、带 token 返回 500，白白给每次空结果打一条误导日志。
    """
    try:
        data = await mall_request("GET", "/product/search", params={"keyword": keyword})
    except MallApiError as e:
        logger.warning("gateway search failed: %s", e)
        return []
    return data if isinstance(data, list) else []


async def _search_candidates(
    keyword: str,
    *,
    max_price: Optional[float] = None,
    min_price: Optional[float] = None,
    category_id: Optional[int] = None,
) -> list[dict[str, Any]]:
    """RAG 优先，未命中再回落站点搜索接口。"""
    hits = await _rag_search(
        keyword,
        max_price=max_price,
        min_price=min_price,
        category_id=category_id,
    )
    if hits:
        return hits
    return _apply_filters(await _gateway_search(keyword), max_price, min_price, category_id)


def _slim(items: list[dict[str, Any]], limit: int = 15) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for it in items[:limit]:
        out.append(
            {
                "id": it.get("id") or it.get("productId"),
                "name": it.get("name") or it.get("productName"),
                "price": it.get("price"),
                "categoryId": it.get("categoryId") or it.get("category_id"),
                "brand": it.get("brand"),
                "description": (it.get("description") or "")[:120],
                "score": it.get("score"),
                "match": it.get("match"),
            }
        )
    return out


async def search_products(
    keyword: str,
    max_price: Optional[float] = None,
    min_price: Optional[float] = None,
    category_id: Optional[int] = None,
) -> str:
    if not keyword or not str(keyword).strip():
        return tool_result_err("参数缺失: keyword 必填")
    kw = str(keyword).strip()
    try:
        items = await _search_candidates(
            kw,
            max_price=max_price,
            min_price=min_price,
            category_id=category_id,
        )
        payload: dict[str, Any] = {"count": len(items), "products": _slim(items)}
        if not items and any(v is not None for v in (max_price, min_price)):
            # 放宽价格再探一次，区分「站内没有」和「有但超预算」
            broad = await _search_candidates(kw)
            if broad:
                prices = sorted(
                    float(b["price"]) for b in broad if b.get("price") is not None
                )
                payload["hint"] = (
                    f"关键字「{kw}」站内有 {len(broad)} 件商品，但都不满足价格区间"
                    f"（最低 {prices[0] if prices else '未知'} 元）。"
                    "请如实告知用户价格超出预算，不要虚构低价商品。"
                )
                payload["out_of_budget"] = _slim(broad, 3)
        if not items and "hint" not in payload:
            payload["hint"] = (
                f"站内没有匹配「{kw}」的商品。可改用更通用的品类词，"
                "或先调用 list_categories 看有哪些分类。"
            )
        return tool_result_ok(payload)
    except MallApiError as e:
        return tool_result_err(str(e))
    except Exception as e:
        return tool_result_err(f"search_products 失败: {e}")


async def get_product_detail(product_id: int) -> str:
    if product_id is None:
        return tool_result_err("参数缺失: product_id 必填")
    try:
        data = await mall_request("GET", f"/product/{product_id}")
        return tool_result_ok(data)
    except MallApiError as e:
        return tool_result_err(str(e))


async def list_categories() -> str:
    try:
        data = await mall_request("GET", "/category/list")
        return tool_result_ok(data)
    except MallApiError as e:
        return tool_result_err(str(e))


async def add_to_cart(product_id: int, quantity: int = 1) -> str:
    if product_id is None:
        return tool_result_err("参数缺失: product_id 必填")
    qty = quantity if quantity and quantity > 0 else 1
    try:
        await mall_request(
            "POST",
            "/cart/add",
            params={"productId": product_id, "quantity": qty},
        )
        return tool_result_ok({"productId": product_id, "quantity": qty, "message": "已加入购物车"})
    except MallApiError as e:
        return tool_result_err(str(e))


async def get_cart() -> str:
    try:
        data = await mall_request("GET", "/cart/list")
        return tool_result_ok(data)
    except MallApiError as e:
        return tool_result_err(str(e))


async def update_cart(
    product_id: int,
    quantity: Optional[int] = None,
    delete: bool = False,
) -> str:
    if product_id is None:
        return tool_result_err("参数缺失: product_id 必填")
    try:
        if delete:
            await mall_request("DELETE", "/cart/delete", params={"productId": product_id})
            return tool_result_ok({"productId": product_id, "message": "已从购物车删除"})
        if quantity is None:
            return tool_result_err("参数缺失: 改数量时 quantity 必填，或设置 delete=true")
        await mall_request(
            "PUT",
            "/cart/quantity",
            params={"productId": product_id, "quantity": quantity},
        )
        return tool_result_ok({"productId": product_id, "quantity": quantity})
    except MallApiError as e:
        return tool_result_err(str(e))


async def list_coupons(mine: bool = False) -> str:
    try:
        if mine:
            data = await mall_request("GET", "/coupon/my")
        else:
            data = await mall_request("GET", "/coupon/list")
        return tool_result_ok(data)
    except MallApiError as e:
        return tool_result_err(str(e))


async def list_orders() -> str:
    try:
        data = await mall_request("GET", "/orders")
        return tool_result_ok(data)
    except MallApiError as e:
        return tool_result_err(str(e))


TOOL_HANDLERS = {
    "search_products": search_products,
    "get_product_detail": get_product_detail,
    "list_categories": list_categories,
    "add_to_cart": add_to_cart,
    "get_cart": get_cart,
    "update_cart": update_cart,
    "list_coupons": list_coupons,
    "list_orders": list_orders,
}


OPENAI_TOOLS: list[dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "search_products",
            "description": "按关键词搜索商品，可按价格区间与分类过滤。推荐商品前必须调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "keyword": {"type": "string", "description": "搜索关键词，如耳机、香水"},
                    "max_price": {"type": "number", "description": "最高价格"},
                    "min_price": {"type": "number", "description": "最低价格"},
                    "category_id": {"type": "integer", "description": "分类 ID"},
                },
                "required": ["keyword"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_product_detail",
            "description": "获取单个商品详情",
            "parameters": {
                "type": "object",
                "properties": {
                    "product_id": {"type": "integer"},
                },
                "required": ["product_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_categories",
            "description": "列出商品分类",
            "parameters": {"type": "object", "properties": {}},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "add_to_cart",
            "description": "将商品加入当前用户购物车（需要登录）",
            "parameters": {
                "type": "object",
                "properties": {
                    "product_id": {"type": "integer"},
                    "quantity": {"type": "integer", "default": 1},
                },
                "required": ["product_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_cart",
            "description": "查看当前用户购物车",
            "parameters": {"type": "object", "properties": {}},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "update_cart",
            "description": "修改购物车数量或删除商品",
            "parameters": {
                "type": "object",
                "properties": {
                    "product_id": {"type": "integer"},
                    "quantity": {"type": "integer"},
                    "delete": {"type": "boolean", "default": False},
                },
                "required": ["product_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_coupons",
            "description": "查看商城优惠券；mine=true 时查看我的券",
            "parameters": {
                "type": "object",
                "properties": {
                    "mine": {"type": "boolean", "default": False},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_orders",
            "description": "查看当前用户历史订单",
            "parameters": {"type": "object", "properties": {}},
        },
    },
]


async def dispatch_tool(name: str, arguments: dict[str, Any]) -> str:
    handler = TOOL_HANDLERS.get(name)
    if not handler:
        return tool_result_err(f"未知工具: {name}")
    # filter unexpected keys
    import inspect

    sig = inspect.signature(handler)
    allowed = set(sig.parameters.keys())
    kwargs = {k: v for k, v in (arguments or {}).items() if k in allowed}
    return await handler(**kwargs)


# silence unused import warning path for settings in module
_ = get_settings
