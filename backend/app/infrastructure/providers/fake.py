"""Fake-постачальник для тестів і демонстрації: вигадані команди, можна імітувати збій."""
from collections.abc import Iterable

from app.domain.errors import StatsUnavailableError
from app.domain.models import TeamStats

# Fake Beta і Fake Gamma мають однакову статистику: пара дає рівні шанси
FAKE_TEAMS = (
    TeamStats("Fake Alpha", 0.60, 1.10),
    TeamStats("Fake Beta", 0.50, 1.00),
    TeamStats("Fake Gamma", 0.50, 1.00),
)


class FakeTeamStatsProvider:
    def __init__(self, teams: Iterable[TeamStats] = FAKE_TEAMS, fail: bool = False) -> None:
        self._teams = {team.name: team for team in teams}
        self.fail = fail
        self.calls: list[str] = []

    def list_teams(self) -> list[TeamStats]:
        self._raise_if_failing()
        return list(self._teams.values())

    def get_team(self, name: str) -> TeamStats | None:
        self.calls.append(name)
        self._raise_if_failing()
        return self._teams.get(name)

    def _raise_if_failing(self) -> None:
        if self.fail:
            raise StatsUnavailableError()
