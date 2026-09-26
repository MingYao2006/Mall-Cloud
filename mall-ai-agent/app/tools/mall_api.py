from __future__ import annotations

import json
import logging
from typing import Any, Optional

import httpx

from app.config import get_settings
from app.context import get_user_token

logger = logging.getLogger(__name__)


class MallApiError(Exception):
    def __init__(self, message: str, *, status: Optional[int] = None) -> None:
        super().__init__(message)
        self.status = status


def _auth_headers() -> dict[str, str]:
    token = get_user_token()
    headers = {"Accept": "application/json"}
    if token:
        if not token.startswith("Bearer "):
            token = f"Bearer {token}"
        headers["Authorization"] = token
    return headers


async def mall_request(
    method: str,
    path: str,
    *,
    params: Optional[dict[str, Any]] = None,
    json_body: Optional[Any] = None,
) -> Any:
    settings = get_settings()
    url = f"{settings.mall_gateway_base.rstrip('/')}/{path.lstrip('/')}"
    timeout = httpx.Timeout(settings.http_timeout_seconds)
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            resp = await client.request(
                method,
                url,
                params=params,
                json=json_body,
                headers=_auth_headers(),
            )
    except httpx.TimeoutException:
        raise MallApiError(f"请求超时: {method} {path}") from None
    except httpx.HTTPError as e:
        raise MallApiError(f"网络错误: {e}") from e

    if resp.status_code >= 400:
        raise MallApiError(
            f"HTTP {resp.status_code}: {resp.text[:300]}",
            status=resp.status_code,
        )

    try:
        data = resp.json()
    except Exception:
        return {"raw": resp.text}

    if isinstance(data, dict) and "code" in data and data.get("code") != 200:
        raise MallApiError(
            f"业务错误 code={data.get('code')}: {data.get('message')}",
            status=data.get("code"),
        )
    return data.get("data") if isinstance(data, dict) and "data" in data else data


def tool_result_ok(payload: Any) -> str:
    return json.dumps({"ok": True, "data": payload}, ensure_ascii=False, default=str)


def tool_result_err(message: str) -> str:
    return json.dumps({"ok": False, "error": message}, ensure_ascii=False)
