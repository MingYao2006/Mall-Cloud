from __future__ import annotations

from typing import Any, Optional

from app.rag.store import search_vectors


async def semantic_retrieve(
    query: str,
    *,
    max_price: Optional[float] = None,
    min_price: Optional[float] = None,
    category_id: Optional[int] = None,
) -> list[dict[str, Any]]:
    return await search_vectors(
        query,
        max_price=max_price,
        min_price=min_price,
        category_id=category_id,
    )
