from __future__ import annotations

from contextvars import ContextVar
from typing import Optional

user_token_var: ContextVar[Optional[str]] = ContextVar("user_token", default=None)
user_id_var: ContextVar[Optional[str]] = ContextVar("user_id", default=None)


def set_request_context(*, token: Optional[str], user_id: Optional[str]) -> tuple:
    """Set per-request context; returns tokens for reset."""
    t1 = user_token_var.set(token)
    t2 = user_id_var.set(user_id)
    return t1, t2


def reset_request_context(tokens: tuple) -> None:
    """Reset the per-request context.

    Streaming responses iterate the body generator inside a task that runs in a
    different context, so a Token created by `set_request_context` in the route
    handler cannot be `reset()` there (it raises ValueError and aborts the SSE
    stream half way through). Fall back to clearing the value instead.
    """
    for var, token in zip((user_token_var, user_id_var), tokens):
        try:
            var.reset(token)
        except (ValueError, RuntimeError):
            var.set(None)


def get_user_token() -> Optional[str]:
    return user_token_var.get()


def get_user_id() -> Optional[str]:
    return user_id_var.get()
