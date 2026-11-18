"""FastAPI Depends: фабрики постачальника статистики, AI-аналітика та прикладного сервісу."""
from fastapi import Depends

from app.core.config import get_settings
from app.domain.interfaces import TeamStatsProvider
from app.infrastructure.ai.gemini_analyst import GeminiMatchAnalyst, MatchAnalyst
from app.infrastructure.providers.fake import FakeTeamStatsProvider
from app.infrastructure.providers.in_memory import InMemoryTeamStatsProvider
from app.services.prediction_service import PredictionService


def get_team_stats_provider() -> TeamStatsProvider:
    settings = get_settings()
    if settings.stats_provider == "in_memory":
        return InMemoryTeamStatsProvider()
    if settings.stats_provider == "fake":
        return FakeTeamStatsProvider()
    raise ValueError(f"Невідомий постачальник статистики: {settings.stats_provider}")


def get_match_analyst() -> MatchAnalyst:
    settings = get_settings()
    return GeminiMatchAnalyst(
        api_key=settings.gemini_api_key,
        model_name=settings.gemini_model,
        timeout_seconds=settings.gemini_timeout_seconds,
    )


def get_prediction_service(
    provider: TeamStatsProvider = Depends(get_team_stats_provider),
    analyst: MatchAnalyst = Depends(get_match_analyst),
) -> PredictionService:
    settings = get_settings()
    return PredictionService(
        provider=provider,
        weights=settings.weights,
        analyst=analyst,
    )
