"""Прикладна логіка прогнозу. Не імпортує FastAPI, Pydantic чи конкретне джерело даних."""
from app.domain.errors import SameTeamsError, TeamNotFoundError, UnknownMapError
from app.domain.interfaces import TeamStatsProvider
from app.domain.models import (
    CS2_MAPS,
    Prediction,
    PredictionWeights,
    TeamBreakdown,
    TeamStats,
)
from app.infrastructure.ai.gemini_analyst import AiAnalysisResult, MatchAnalyst


class PredictionService:
    def __init__(
        self,
        provider: TeamStatsProvider,
        weights: PredictionWeights | None = None,
        analyst: MatchAnalyst | None = None,
    ) -> None:
        self._provider = provider
        self._weights = weights or PredictionWeights()
        self._analyst = analyst

    def list_teams(self) -> list[TeamStats]:
        return self._provider.list_teams()

    def list_maps(self) -> list[str]:
        return list(CS2_MAPS)

    def check_dependencies(self) -> int:
        """Перевіряє доступність джерела статистики команд і повертає кількість записів."""
        return len(self._provider.list_teams())

    def predict(
        self,
        team_a_name: str,
        team_b_name: str,
        map_name: str | None = None,
    ) -> Prediction:
        normalized_map = map_name.strip() if map_name and map_name.strip() else None
        if normalized_map is not None and normalized_map not in CS2_MAPS:
            raise UnknownMapError(normalized_map)

        # Порядок перевірок збережено з ЛР2: спочатку однакові, потім A, потім B
        if team_a_name == team_b_name:
            raise SameTeamsError()

        team_a = self._provider.get_team(team_a_name)
        team_b = self._provider.get_team(team_b_name)
        if team_a is None:
            raise TeamNotFoundError(team_a_name)
        if team_b is None:
            raise TeamNotFoundError(team_b_name)

        breakdown_a = self._breakdown(team_a, normalized_map)
        breakdown_b = self._breakdown(team_b, normalized_map)

        total = breakdown_a.score + breakdown_b.score
        probability_a = round(breakdown_a.score / total, 2)
        probability_b = round(1 - probability_a, 2)

        return Prediction(
            team_a=breakdown_a,
            team_b=breakdown_b,
            probability_a=probability_a,
            probability_b=probability_b,
            weights=self._weights,
            map_name=normalized_map,
        )

    def analyze_with_ai(self, prediction: Prediction, use_ai: bool) -> AiAnalysisResult:
        if not use_ai:
            return AiAnalysisResult(
                summary="Стандартний математичний режим (AI-аналіз не запитувався).",
                ai_status="disabled",
                model=None,
            )
        if self._analyst is None:
            return AiAnalysisResult(
                summary="AI-аналітик не налаштований. Показано базовий розрахунок.",
                ai_status="fallback",
                model=None,
            )
        return self._analyst.analyze(prediction)

    def _breakdown(self, team: TeamStats, map_name: str | None) -> TeamBreakdown:
        eff_win_rate = team.effective_win_rate(map_name)
        win_rate_contribution = eff_win_rate * self._weights.win_rate
        rating_contribution = team.rating * self._weights.rating
        return TeamBreakdown(
            team=team,
            effective_win_rate=eff_win_rate,
            win_rate_contribution=win_rate_contribution,
            rating_contribution=rating_contribution,
            score=win_rate_contribution + rating_contribution,
        )
