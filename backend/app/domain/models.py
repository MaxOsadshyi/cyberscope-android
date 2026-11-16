"""Доменні моделі. Не залежать від FastAPI, Pydantic чи HTTP."""
from dataclasses import dataclass


@dataclass(frozen=True)
class TeamStats:
    """Статистика команди, яку надає джерело даних."""

    name: str
    win_rate: float
    rating: float


@dataclass(frozen=True)
class PredictionWeights:
    """Ваги формули прогнозу. Значення за замовчуванням збігаються з ЛР2-ЛР4."""

    win_rate: float = 0.6
    rating: float = 0.4


@dataclass(frozen=True)
class TeamBreakdown:
    """Розрахунок однієї команди. Значення без округлення."""

    team: TeamStats
    win_rate_contribution: float
    rating_contribution: float
    score: float


@dataclass(frozen=True)
class Prediction:
    """Результат прогнозу. Ймовірності вже округлені до двох знаків."""

    team_a: TeamBreakdown
    team_b: TeamBreakdown
    probability_a: float
    probability_b: float
    weights: PredictionWeights

    @property
    def is_tie(self) -> bool:
        """Шанси рівні за округленими значеннями, фаворита немає."""
        return self.probability_a == self.probability_b

    @property
    def leader(self) -> TeamStats | None:
        if self.is_tie:
            return None
        if self.probability_a > self.probability_b:
            return self.team_a.team
        return self.team_b.team
