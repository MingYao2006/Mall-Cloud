from __future__ import annotations

import hashlib
import logging
import math
from typing import Optional

import httpx
import numpy as np

from app.config import get_settings

logger = logging.getLogger(__name__)


def _hash_embed(text: str, dim: int) -> list[float]:
    """Deterministic pseudo-embedding for local/dev without external API."""
    vec = np.zeros(dim, dtype=np.float32)
    tokens = text.lower().replace("\n", " ").split()
    if not tokens:
        tokens = [text]
    for tok in tokens:
        h = hashlib.sha256(tok.encode("utf-8")).digest()
        for i in range(0, min(len(h), 32)):
            idx = (h[i] + i * 17) % dim
            vec[idx] += 1.0 if h[i] % 2 == 0 else -1.0
        # char bigrams for Chinese
        raw = tok if len(tok) > 1 else text
        for j in range(max(0, len(raw) - 1)):
            bg = raw[j : j + 2]
            hb = hashlib.md5(bg.encode("utf-8")).digest()
            idx = int.from_bytes(hb[:2], "little") % dim
            vec[idx] += 0.5
    norm = float(np.linalg.norm(vec))
    if norm < 1e-9:
        vec[0] = 1.0
        norm = 1.0
    vec = vec / norm
    return vec.tolist()


async def embed_texts(texts: list[str]) -> list[list[float]]:
    settings = get_settings()
    if settings.embedding_base_url and settings.embedding_api_key:
        return await _remote_embed(texts)
    return [_hash_embed(t, settings.embedding_dim) for t in texts]


async def embed_query(text: str) -> list[float]:
    return (await embed_texts([text]))[0]


_DIM_CACHE: Optional[int] = None


async def get_embedding_dim() -> int:
    """返回实际生效的向量维度。

    远程 embedding 以接口返回的向量长度为准，避免 EMBEDDING_DIM 与所选模型不一致时
    向量被截断/补齐导致检索质量下降；本地哈希伪向量使用配置值。
    """
    global _DIM_CACHE
    if _DIM_CACHE is not None:
        return _DIM_CACHE

    settings = get_settings()
    if settings.embedding_base_url and settings.embedding_api_key:
        probe = await _remote_embed(["维度探测"])
        _DIM_CACHE = len(probe[0])
        if _DIM_CACHE != settings.embedding_dim:
            logger.warning(
                "EMBEDDING_DIM=%s 与模型实际维度 %s 不一致，已按实际维度使用",
                settings.embedding_dim,
                _DIM_CACHE,
            )
    else:
        _DIM_CACHE = settings.embedding_dim
    return _DIM_CACHE


# 单次请求条数上限：整库 146 条一次提交会被服务端拒绝
REMOTE_BATCH_SIZE = 32
# bge-large-zh-v1.5 上下文 512 token，中文约 500 字，超长直接截断
REMOTE_MAX_CHARS = 480


async def _embed_batch(
    client: httpx.AsyncClient,
    url: str,
    headers: dict[str, str],
    model: str,
    batch: list[str],
) -> list[list[float]]:
    body = {"model": model, "input": [t[:REMOTE_MAX_CHARS] for t in batch]}
    resp = await client.post(url, headers=headers, json=body)
    resp.raise_for_status()
    items = sorted(resp.json().get("data") or [], key=lambda x: x.get("index", 0))
    out = []
    for it in items:
        arr = np.array(it["embedding"], dtype=np.float32)
        n = float(np.linalg.norm(arr))
        if n > 1e-9:
            arr = arr / n
        out.append(arr.tolist())
    return out


async def _remote_embed(texts: list[str]) -> list[list[float]]:
    settings = get_settings()
    url = settings.embedding_base_url.rstrip("/") + "/embeddings"
    headers = {
        "Authorization": f"Bearer {settings.embedding_api_key}",
        "Content-Type": "application/json",
    }
    model = settings.embedding_model or "text-embedding"
    out: list[list[float]] = []
    async with httpx.AsyncClient(timeout=120.0) as client:
        for i in range(0, len(texts), REMOTE_BATCH_SIZE):
            out.extend(
                await _embed_batch(
                    client, url, headers, model, texts[i : i + REMOTE_BATCH_SIZE]
                )
            )
    if len(out) != len(texts):
        raise RuntimeError(f"embedding 返回条数不匹配: {len(out)} != {len(texts)}")
    return out


def cosine_sim(a: list[float], b: list[float]) -> float:
    va = np.array(a, dtype=np.float32)
    vb = np.array(b, dtype=np.float32)
    denom = float(np.linalg.norm(va) * np.linalg.norm(vb))
    if denom < 1e-9:
        return 0.0
    return float(np.dot(va, vb) / denom)
