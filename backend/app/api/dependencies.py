"""FastAPI Depends: фабрики постачальника статистики та прикладного сервісу."""
from fastapi import Depends

from app.core.config import get_settings
from app.domain.interfaces import TeamStatsProvider
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


def get_prediction_service(
    provider: TeamStatsProvider = Depends(get_team_stats_provider),
) -> PredictionService:
    settings = get_settings()
    return PredictionService(provider=provider, weights=settings.weights)
