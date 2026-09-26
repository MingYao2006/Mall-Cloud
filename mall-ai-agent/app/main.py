from __future__ import annotations

import asyncio
import json
import logging
from typing import Any, Optional

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app.agent import run_agent, stream_agent_reply
from app.config import get_settings
from app.context import reset_request_context, set_request_context
from app.llm import simple_chat
from app.logging_metrics import log_request, new_request_id, Timer
from app.rag.indexer import index_all_products
from app.rag.store import memory_doc_count
from app.session.store import load_history, maybe_summarize, save_history

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger(__name__)

app = FastAPI(title="Mall AI Shopping Guide", version="0.5.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def _warm_index() -> None:
    """Milvus 未启用时向量索引落在进程内存里，重启即失效，这里后台重建一次。"""
    if not get_settings().rag_auto_index_on_startup:
        return

    async def _run() -> None:
        try:
            result = await index_all_products()
            logger.info("startup index finished: %s", result)
        except Exception:
            logger.exception("startup index failed, keyword fallback still works")

    asyncio.create_task(_run())


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1)
    session_id: Optional[str] = None
    # phase 0: simple=True bypasses tools (plain DeepSeek)
    simple: bool = False


class ChatResponse(BaseModel):
    reply: str
    session_id: Optional[str] = None
    tools_used: list[str] = []
    request_id: str
    elapsed_ms: float


@app.get("/health")
async def health() -> dict[str, Any]:
    settings = get_settings()
    return {
        "status": "ok",
        "service": "mall-ai-agent",
        "model": settings.deepseek_model,
        "rag_memory_docs": memory_doc_count(),
    }


@app.post("/chat", response_model=ChatResponse)
async def chat(
    body: ChatRequest,
    authorization: Optional[str] = Header(default=None),
    x_user_id: Optional[str] = Header(default=None, alias="X-User-Id"),
) -> ChatResponse:
    request_id = new_request_id()
    timer = Timer()
    log_request(request_id=request_id, user_id=x_user_id, path="/chat")
# set_request_context 将 token 和用户 ID 注入到请求上下文
    tokens = set_request_context(token=authorization, user_id=x_user_id)
    try:
        if body.simple:
            reply = await simple_chat(
                body.message,
                system="你是友善的商城导购助手，用中文简洁回答。",
            )
            return ChatResponse(
                reply=reply,
                session_id=body.session_id,
                tools_used=[],
                request_id=request_id,
                elapsed_ms=timer.ms(),
            )

        history = load_history(body.session_id or "")
#         可能做摘要压缩：maybe_summarize 在历史过长时将其压缩成摘要，避免超出 token 上限；
        history = await maybe_summarize(body.session_id or "", history)
        reply, messages, tools_used = await run_agent(
            body.message,
            history=history,
            request_id=request_id,
        )
        if body.session_id:
            # persist compact turns
            save_history(
                body.session_id,
                history
                + [
                    {"role": "user", "content": body.message},
                    {"role": "assistant", "content": reply},
                ],
            )
        return ChatResponse(
            reply=reply,
            session_id=body.session_id,
            tools_used=tools_used,
            request_id=request_id,
            elapsed_ms=timer.ms(),
        )
    except Exception as e:
        logger.exception("chat failed")
        raise HTTPException(status_code=500, detail=str(e)) from e
    finally:
        reset_request_context(tokens)


@app.post("/chat/stream")
async def chat_stream(
    body: ChatRequest,
    authorization: Optional[str] = Header(default=None),
    x_user_id: Optional[str] = Header(default=None, alias="X-User-Id"),
):
    request_id = new_request_id()
    log_request(request_id=request_id, user_id=x_user_id, path="/chat/stream")

    async def event_gen():
        # Starlette 在独立的 task context 中迭代响应体，contextvars 必须在这里设置，
        # 否则 set/reset 分属两个 context，reset 会抛 ValueError 并中断 SSE。
        tokens = set_request_context(token=authorization, user_id=x_user_id)
        try:
            history = load_history(body.session_id or "")
            history = await maybe_summarize(body.session_id or "", history)
#             累积所有分片，最后拼成完整回复（因为要存历史）
            full = []
            yield _sse({"type": "meta", "request_id": request_id})
            async for chunk in stream_agent_reply(
                body.message, history=history, request_id=request_id
            ):
                full.append(chunk)
                yield _sse({"type": "token", "content": chunk})
            reply = "".join(full)
            if body.session_id:
                save_history(
                    body.session_id,
                    history
                    + [
                        {"role": "user", "content": body.message},
                        {"role": "assistant", "content": reply},
                    ],
                )
            yield _sse({"type": "done", "reply": reply})
        except Exception as e:
            logger.exception("stream failed")
            yield _sse({"type": "error", "message": str(e)})
        finally:
            reset_request_context(tokens)

    return StreamingResponse(event_gen(), media_type="text/event-stream")


def _sse(payload: dict[str, Any]) -> str:
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


@app.post("/rag/index")
async def rag_index(
    authorization: Optional[str] = Header(default=None),
    x_user_id: Optional[str] = Header(default=None, alias="X-User-Id"),
) -> dict[str, Any]:
    """Index products from mall into vector store (public product list OK)."""
    tokens = set_request_context(token=authorization, user_id=x_user_id)
    try:
        # 手动触发一律重建；启动时的自动索引才走「语料没变就跳过」
        return await index_all_products(force=True)
    finally:
        reset_request_context(tokens)


def create_app() -> FastAPI:
    return app
