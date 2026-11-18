"""Доменні моделі. Не залежать від FastAPI, Pydantic чи HTTP."""
from dataclasses import dataclass, field

CS2_MAPS: tuple[str, ...] = (
    "Mirage",
    "Inferno",
    "Nuke",
    "Ancient",
    "Dust2",
    "Anubis",
    "Train",
)


@dataclass(frozen=True)
class TeamStats:
    """Статистика команди, яку надає джерело даних."""

    name: str
    win_rate: float
    rating: float
    map_win_rates: dict[str, float] = field(default_factory=dict)

    def effective_win_rate(self, map_name: str | None = None) -> float:
        """Повертає вінрейт на обраній карті або загальний вінрейт команди."""
        if map_name is None:
            return self.win_rate
        return self.map_win_rates.get(map_name, self.win_rate)


@dataclass(frozen=True)
class PredictionWeights:
    """Ваги формули прогнозу. Значення за замовчуванням збігаються з ЛР2-ЛР4."""

    win_rate: float = 0.6
    rating: float = 0.4


@dataclass(frozen=True)
class TeamBreakdown:
    """Розрахунок однієї команди. Значення без округлення."""

    team: TeamStats
    effective_win_rate: float
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
    map_name: str | None = None

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
