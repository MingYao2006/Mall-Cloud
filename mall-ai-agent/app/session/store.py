from __future__ import annotations

import json
import logging
from typing import Any, Optional

from app.config import get_settings
from app.llm import simple_chat

logger = logging.getLogger(__name__)

_redis = None
_memory_sessions: dict[str, list[dict[str, Any]]] = {}


def _get_redis():
    global _redis
    if _redis is not False and _redis is not None:
        return _redis
    try:
        import redis

        settings = get_settings()
        client = redis.from_url(settings.redis_url, decode_responses=True)
        client.ping()
        _redis = client
        return _redis
    except Exception as e:
        logger.warning("Redis unavailable, using memory sessions: %s", e)
        _redis = False
        return None


def _key(session_id: str) -> str:
    return f"mall:ai:session:{session_id}"


def load_history(session_id: str) -> list[dict[str, Any]]:
    if not session_id:
        return []
    r = _get_redis()
    if r:
        raw = r.get(_key(session_id))
        if not raw:
            return []
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return []
    return list(_memory_sessions.get(session_id, []))


def save_history(session_id: str, messages: list[dict[str, Any]]) -> None:
    if not session_id:
        return
    settings = get_settings()
    # Keep only user/assistant text turns for compactness (drop heavy tool payloads)
    compact: list[dict[str, Any]] = []
    for m in messages:
        role = m.get("role")
        if role in ("user", "assistant") and m.get("content") and not m.get("tool_calls"):
            compact.append({"role": role, "content": m["content"]})
    compact = compact[-settings.session_max_messages :]

    r = _get_redis()
    if r:
        r.setex(_key(session_id), 60 * 60 * 24 * 7, json.dumps(compact, ensure_ascii=False))
    else:
        _memory_sessions[session_id] = compact


async def maybe_summarize(session_id: str, history: list[dict[str, Any]]) -> list[dict[str, Any]]:
    settings = get_settings()
    if len(history) < settings.session_summary_threshold:
        return history
    transcript = "\n".join(f"{m['role']}: {m['content']}" for m in history[:-4])
    try:
        summary = await simple_chat(
            f"请将以下导购对话压缩为不超过200字的中文摘要，保留用户偏好与已推荐商品：\n{transcript}",
            system="你是会话摘要助手。",
        )
    except Exception as e:
        logger.warning("summary failed: %s", e)
        return history[-settings.session_max_messages :]
    new_hist = [{"role": "assistant", "content": f"【历史摘要】{summary}"}] + history[-4:]
    save_history(session_id, new_hist)
    return new_hist
