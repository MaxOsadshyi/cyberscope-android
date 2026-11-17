"""Прикладна логіка прогнозу. Не імпортує FastAPI, Pydantic чи конкретне джерело даних."""
from app.domain.errors import SameTeamsError, TeamNotFoundError
from app.domain.interfaces import TeamStatsProvider
from app.domain.models import (
    Prediction,
    PredictionWeights,
    TeamBreakdown,
    TeamStats,
)


class PredictionService:
    def __init__(
        self,
        provider: TeamStatsProvider,
        weights: PredictionWeights | None = None,
    ) -> None:
        self._provider = provider
        self._weights = weights or PredictionWeights()

    def list_teams(self) -> list[TeamStats]:
        return self._provider.list_teams()

    def check_dependencies(self) -> int:
        """Перевіряє доступність джерела статистики команд і повертає кількість записів."""
        return len(self._provider.list_teams())

    def predict(self, team_a_name: str, team_b_name: str) -> Prediction:
        # Порядок перевірок збережено з ЛР2: спочатку однакові, потім A, потім B
        if team_a_name == team_b_name:
            raise SameTeamsError()

        team_a = self._provider.get_team(team_a_name)
        team_b = self._provider.get_team(team_b_name)
        if team_a is None:
            raise TeamNotFoundError(team_a_name)
        if team_b is None:
            raise TeamNotFoundError(team_b_name)

        breakdown_a = self._breakdown(team_a)
        breakdown_b = self._breakdown(team_b)

        total = breakdown_a.score + breakdown_b.score
        probability_a = round(breakdown_a.score / total, 2)
        probability_b = round(1 - probability_a, 2)

        return Prediction(
            team_a=breakdown_a,
            team_b=breakdown_b,
            probability_a=probability_a,
            probability_b=probability_b,
            weights=self._weights,
        )

    def _breakdown(self, team: TeamStats) -> TeamBreakdown:
        win_rate_contribution = team.win_rate * self._weights.win_rate
        rating_contribution = team.rating * self._weights.rating
        return TeamBreakdown(
            team=team,
            win_rate_contribution=win_rate_contribution,
            rating_contribution=rating_contribution,
            score=win_rate_contribution + rating_contribution,
        )
