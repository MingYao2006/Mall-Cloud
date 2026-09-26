from __future__ import annotations

import json
import logging
import time
import uuid
from typing import Any, Optional

logger = logging.getLogger("mall_ai.metrics")


def new_request_id() -> str:
    return uuid.uuid4().hex[:12]


def log_request(
    *,
    request_id: str,
    user_id: Optional[str],
    path: str,
    extra: Optional[dict[str, Any]] = None,
) -> None:
    payload = {
        "request_id": request_id,
        "user_id": user_id,
        "path": path,
        **(extra or {}),
    }
    logger.info("request %s", json.dumps(payload, ensure_ascii=False))


def log_tool_call(
    *,
    request_id: str,
    tool_name: str,
    ok: bool,
    elapsed_ms: float,
    detail: str = "",
) -> None:
    logger.info(
        "tool request_id=%s name=%s ok=%s elapsed_ms=%.1f detail=%s",
        request_id,
        tool_name,
        ok,
        elapsed_ms,
        detail[:200],
    )


class Timer:
    def __init__(self) -> None:
        self._start = time.perf_counter()

    def ms(self) -> float:
        return (time.perf_counter() - self._start) * 1000
