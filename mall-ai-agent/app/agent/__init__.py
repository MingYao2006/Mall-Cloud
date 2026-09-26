"""LangGraph-style agent loop with DeepSeek function calling."""
from __future__ import annotations

import json
import logging
from typing import Any, AsyncIterator, Optional

from app.agent.prompts import RAG_CONSTRAINT, SYSTEM_PROMPT
from app.llm import chat_completion, chat_completion_stream
from app.logging_metrics import Timer, log_tool_call
from app.tools import OPENAI_TOOLS, dispatch_tool

logger = logging.getLogger(__name__)

MAX_TOOL_ROUNDS = 8


async def run_agent(
    user_message: str,
    *,
    history: Optional[list[dict[str, Any]]] = None,
    request_id: str = "",
) -> tuple[str, list[dict[str, Any]], list[str]]:
    """
    Returns: (final_reply, updated_messages, tools_used)
    """
    messages: list[dict[str, Any]] = [
        {"role": "system", "content": SYSTEM_PROMPT + "\n" + RAG_CONSTRAINT}
    ]
    if history:
        messages.extend(history)
    messages.append({"role": "user", "content": user_message})

    tools_used: list[str] = []

    for _ in range(MAX_TOOL_ROUNDS):
        resp = await chat_completion(messages, tools=OPENAI_TOOLS, tool_choice="auto")
        msg = resp.choices[0].message
        tool_calls = msg.tool_calls or []

        if not tool_calls:
            content = msg.content or ""
            messages.append({"role": "assistant", "content": content})
            return content, messages[1:], tools_used  # drop system for session store

        # assistant message with tool_calls
        assistant_payload: dict[str, Any] = {
            "role": "assistant",
            "content": msg.content or "",
            "tool_calls": [
                {
                    "id": tc.id,
                    "type": "function",
                    "function": {
                        "name": tc.function.name,
                        "arguments": tc.function.arguments or "{}",
                    },
                }
                for tc in tool_calls
            ],
        }
        messages.append(assistant_payload)

        for tc in tool_calls:
            name = tc.function.name
            tools_used.append(name)
            try:
                args = json.loads(tc.function.arguments or "{}")
            except json.JSONDecodeError:
                args = {}
                result = json.dumps(
                    {"ok": False, "error": "工具参数 JSON 解析失败"},
                    ensure_ascii=False,
                )
            else:
                timer = Timer()
                result = await dispatch_tool(name, args)
                ok = '"ok": true' in result.lower() or '"ok":true' in result.lower()
                log_tool_call(
                    request_id=request_id,
                    tool_name=name,
                    ok=ok,
                    elapsed_ms=timer.ms(),
                    detail=result,
                )

            messages.append(
                {
                    "role": "tool",
                    "tool_call_id": tc.id,
                    "content": result,
                }
            )

    # force final text
    messages.append(
        {
            "role": "user",
            "content": "请根据已有工具结果给出最终回答，不要再调用工具。",
        }
    )
    resp = await chat_completion(messages)
    content = resp.choices[0].message.content or ""
    messages.append({"role": "assistant", "content": content})
    return content, messages[1:], tools_used


async def stream_agent_reply(
    user_message: str,
    *,
    history: Optional[list[dict[str, Any]]] = None,
    request_id: str = "",
) -> AsyncIterator[str]:
    """
    Run tool loop non-streaming, then stream the final natural-language answer.
    Yields text chunks for SSE.
    """
    # First run tools to completion (tool rounds are not streamed)
    reply, _, tools_used = await run_agent(
        user_message, history=history, request_id=request_id
    )
    # Re-stream by asking model to polish briefly — or just chunk the reply
    # For true token streaming of final answer, rebuild messages without forcing tools.
    # Simpler UX: stream the already-computed reply in small chunks + optional polish stream.

    messages: list[dict[str, Any]] = [
        {"role": "system", "content": SYSTEM_PROMPT + "\n" + RAG_CONSTRAINT},
    ]
    if history:
        messages.extend(history)
    messages.append({"role": "user", "content": user_message})
    if tools_used:
        messages.append(
            {
                "role": "assistant",
                "content": f"（已调用工具: {', '.join(tools_used)}）\n{reply}",
            }
        )
        messages.append(
            {
                "role": "user",
                "content": "请用导购口吻重新表述上述结论，不要编造新商品。直接输出最终回复。",
            }
        )
        async for chunk in chat_completion_stream(messages):
            yield chunk
    else:
        # already have reply — stream it
        step = 8
        for i in range(0, len(reply), step):
            yield reply[i : i + step]
