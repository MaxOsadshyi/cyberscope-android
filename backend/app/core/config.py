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
    cors_allow_credentials: bool = True
    cors_allow_methods: tuple[str, ...] = ("*",)
    cors_allow_headers: tuple[str, ...] = ("*",)


def get_settings() -> Settings:
    provider = os.getenv("STATS_PROVIDER", "in_memory").strip().lower()
    win_rate_weight = float(os.getenv("WEIGHT_WIN_RATE", "0.6"))
    rating_weight = float(os.getenv("WEIGHT_RATING", "0.4"))
    origins = _parse_origins(os.getenv("CORS_ALLOW_ORIGINS", "*"))

    return Settings(
        stats_provider=provider,
        weights=PredictionWeights(win_rate=win_rate_weight, rating=rating_weight),
        cors_allow_origins=origins,
    )
