"""Централізована конфігурація застосунку."""
from dataclasses import dataclass
import os

from app.domain.models import PredictionWeights


def _parse_origins(raw: str) -> list[str]:
    items = [item.strip() for item in raw.split(",") if item.strip()]
    return items or ["*"]


@dataclass(frozen=True)
class Settings:
    stats_provider: str
    weights: PredictionWeights
    cors_allow_origins: list[str]
    gemini_api_key: str | None = None
    gemini_model: str = "gemini-3.5-flash-lite"
    gemini_timeout_seconds: float = 25.0
    cors_allow_credentials: bool = True
    cors_allow_methods: tuple[str, ...] = ("*",)
    cors_allow_headers: tuple[str, ...] = ("*",)


def get_settings() -> Settings:
    provider = os.getenv("STATS_PROVIDER", "in_memory").strip().lower()
    win_rate_weight = float(os.getenv("WEIGHT_WIN_RATE", "0.6"))
    rating_weight = float(os.getenv("WEIGHT_RATING", "0.4"))
    origins = _parse_origins(os.getenv("CORS_ALLOW_ORIGINS", "*"))
    gemini_key = os.getenv("GEMINI_API_KEY", "").strip() or None
    gemini_model = os.getenv("GEMINI_MODEL", "gemini-3.5-flash-lite").strip()
    gemini_timeout = float(os.getenv("GEMINI_TIMEOUT", "25.0"))

    return Settings(
        stats_provider=provider,
        weights=PredictionWeights(win_rate=win_rate_weight, rating=rating_weight),
        cors_allow_origins=origins,
        gemini_api_key=gemini_key,
        gemini_model=gemini_model,
        gemini_timeout_seconds=gemini_timeout,
    )




