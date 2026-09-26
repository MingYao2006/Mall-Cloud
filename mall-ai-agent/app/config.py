from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"

    app_host: str = "0.0.0.0"
    app_port: int = 8095

    mall_gateway_base: str = "http://localhost:9000/api/c"
    http_timeout_seconds: float = 15.0

    redis_url: str = "redis://localhost:6379/1"
    session_max_messages: int = 20
    session_summary_threshold: int = 16

    milvus_host: str = "localhost"
    milvus_port: int = 19530
    milvus_collection: str = "mall_products"
    embedding_dim: int = 384
    embedding_base_url: str = ""
    embedding_api_key: str = ""
    embedding_model: str = ""

    rag_top_k: int = 5
    # 实测：全库无关查询最高分约 0.24，相关商品 0.41+，取 0.40 作分界
    rag_score_threshold: float = 0.40
    # 索引时用 LLM 给只有标题的商品补检索标签（离线一次性，结果落盘缓存）
    rag_auto_tag: bool = True
    # 启动时后台重建索引（Milvus 未启用时索引在进程内存里，重启会丢）
    rag_auto_index_on_startup: bool = True


@lru_cache
def get_settings() -> Settings:
    return Settings()
