"""Постачальник статистики з даних у пам'яті (поточний TEAMS_DB з ЛР2-ЛР4)."""
from collections.abc import Iterable

from app.domain.models import TeamStats

# Демонстраційна статистика, не реальні дані
DEMO_TEAMS = (
    TeamStats("Natus Vincere", 0.65, 1.15),
    TeamStats("FaZe Clan", 0.58, 1.05),
    TeamStats("Vitality", 0.70, 1.20),
    TeamStats("G2 Esports", 0.60, 1.08),
    TeamStats("MOUZ", 0.55, 1.02),
    TeamStats("Spirit", 0.62, 1.10),
)


class InMemoryTeamStatsProvider:
    def __init__(self, teams: Iterable[TeamStats] = DEMO_TEAMS) -> None:
        self._teams = {team.name: team for team in teams}

    def list_teams(self) -> list[TeamStats]:
        return list(self._teams.values())

    def get_team(self, name: str) -> TeamStats | None:
        return self._teams.get(name)
