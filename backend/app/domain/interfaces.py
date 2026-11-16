"""Інтерфейси зовнішніх залежностей."""
from typing import Protocol

from app.domain.models import TeamStats


class TeamStatsProvider(Protocol):
    """Джерело статистики команд.

    get_team повертає None, якщо команди немає. Якщо джерело недоступне,
    реалізація має кинути StatsUnavailableError.
    """

    def list_teams(self) -> list[TeamStats]:
        ...

    def get_team(self, name: str) -> TeamStats | None:
        ...
