"""Pydantic-схеми API (контракт з ЛР3, 15 полів) і перетворення з доменних моделей."""
from pydantic import BaseModel, Field

from app.domain.models import Prediction, TeamStats

DISCLAIMER = "Статистика команд демонстраційна."


class TeamInfo(BaseModel):
    name: str
    win_rate: float
    rating: float


class PredictRequest(BaseModel):
    team_a_name: str = Field(..., min_length=1)
    team_b_name: str = Field(..., min_length=1)


class PredictResponse(BaseModel):
    team_a_name: str
    team_b_name: str
    team_a_win_probability: float
    team_b_win_probability: float
    explanation: str
    team_a_win_rate: float
    team_a_rating: float
    team_a_win_rate_contribution: float
    team_a_rating_contribution: float
    team_a_score: float
    team_b_win_rate: float
    team_b_rating: float
    team_b_win_rate_contribution: float
    team_b_rating_contribution: float
    team_b_score: float


def to_team_info(team: TeamStats) -> TeamInfo:
    return TeamInfo(name=team.name, win_rate=team.win_rate, rating=team.rating)


def build_explanation(prediction: Prediction) -> str:
    leader = prediction.leader
    if leader is None:
        return f"Шанси команд рівні за округленим прогнозом. {DISCLAIMER}"
    weights = prediction.weights
    formula = f"win_rate × {weights.win_rate} + rating × {weights.rating}"
    return f"{leader.name} має вищий розрахунковий бал за формулою {formula}. {DISCLAIMER}"


def to_predict_response(prediction: Prediction) -> PredictResponse:
    a = prediction.team_a
    b = prediction.team_b
    return PredictResponse(
        team_a_name=a.team.name,
        team_b_name=b.team.name,
        team_a_win_probability=prediction.probability_a,
        team_b_win_probability=prediction.probability_b,
        explanation=build_explanation(prediction),
        team_a_win_rate=a.team.win_rate,
        team_a_rating=a.team.rating,
        team_a_win_rate_contribution=round(a.win_rate_contribution, 3),
        team_a_rating_contribution=round(a.rating_contribution, 3),
        team_a_score=round(a.score, 3),
        team_b_win_rate=b.team.win_rate,
        team_b_rating=b.team.rating,
        team_b_win_rate_contribution=round(b.win_rate_contribution, 3),
        team_b_rating_contribution=round(b.rating_contribution, 3),
        team_b_score=round(b.score, 3),
    )
