"""Доменні моделі без залежностей від HTTP та інфраструктури."""
from dataclasses import dataclass, field

CS2_MAPS: tuple[str, ...] = (
    "Mirage",
    "Inferno",
    "Nuke",
    "Ancient",
    "Anubis",
    "Dust2",
    "Train",
)


@dataclass(frozen=True)
class PlayerStats:
    nickname: str
    rating: float


@dataclass(frozen=True)
class TeamStats:
    name: str
    win_rate: float
    rating: float
    map_win_rates: dict[str, float] = field(default_factory=dict)
    points: int = 0
    roster: tuple[PlayerStats, ...] = ()

    def effective_win_rate(self, map_name: str | None = None) -> float:
        if map_name and map_name in self.map_win_rates:
            return self.map_win_rates[map_name]
        return self.win_rate


@dataclass(frozen=True)
class PredictionWeights:
    win_rate: float = 0.6
    rating: float = 0.4


@dataclass(frozen=True)
class TeamBreakdown:
    team: TeamStats
    effective_win_rate: float
    win_rate_contribution: float
    rating_contribution: float
    score: float


@dataclass(frozen=True)
class Prediction:
    team_a: TeamBreakdown
    team_b: TeamBreakdown
    probability_a: float
    probability_b: float
    weights: PredictionWeights
    map_name: str | None = None

    @property
    def leader(self) -> TeamStats | None:
        if self.probability_a > self.probability_b:
            return self.team_a.team
        if self.probability_b > self.probability_a:
            return self.team_b.team
        return None

    @property
    def is_tie(self) -> bool:
        return self.leader is None