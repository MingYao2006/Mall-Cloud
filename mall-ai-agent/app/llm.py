from __future__ import annotations

import logging
from typing import Any, AsyncIterator, Optional

from openai import AsyncOpenAI

from app.config import get_settings

logger = logging.getLogger(__name__)


def get_async_client() -> AsyncOpenAI:
    settings = get_settings()
    if not settings.deepseek_api_key:
        raise RuntimeError("DEEPSEEK_API_KEY is not set")
    return AsyncOpenAI(
        api_key=settings.deepseek_api_key,
        base_url=settings.deepseek_base_url,
    )


async def chat_completion(
    messages: list[dict[str, Any]],
    *,
    tools: Optional[list[dict[str, Any]]] = None,
    tool_choice: Optional[str] = None,
    temperature: float = 0.3,
    json_mode: bool = False,
) -> Any:
    settings = get_settings()
    client = get_async_client()
    kwargs: dict[str, Any] = {
        "model": settings.deepseek_model,
        "messages": messages,
        "temperature": temperature,
    }
    if tools:
        kwargs["tools"] = tools
        if tool_choice:
            kwargs["tool_choice"] = tool_choice
    if json_mode:
        kwargs["response_format"] = {"type": "json_object"}
    resp = await client.chat.completions.create(**kwargs)
    return resp


async def chat_completion_stream(
    messages: list[dict[str, Any]],
    *,
    temperature: float = 0.3,
) -> AsyncIterator[str]:
    settings = get_settings()
    client = get_async_client()
    stream = await client.chat.completions.create(
        model=settings.deepseek_model,
        messages=messages,
        temperature=temperature,
        stream=True,
    )
    async for chunk in stream:
        delta = chunk.choices[0].delta if chunk.choices else None
        if delta and delta.content:
            yield delta.content


async def simple_chat(user_message: str, system: Optional[str] = None) -> str:
    """Non-streaming Q&A (phase 0)."""
    messages: list[dict[str, Any]] = []
    if system:
        messages.append({"role": "system", "content": system})
    messages.append({"role": "user", "content": user_message})
    resp = await chat_completion(messages)
    content = resp.choices[0].message.content or ""
    usage = getattr(resp, "usage", None)
    if usage:
        logger.info(
            "llm_usage prompt=%s completion=%s total=%s",
            usage.prompt_tokens,
            usage.completion_tokens,
            usage.total_tokens,
        )
    return content
