"""Pydantic-схеми API (зворотна сумісність з ЛР3 + контекст карти та AI-аналіз ЛР6 + рейтинг команд)."""
from typing import Any
from pydantic import BaseModel, Field

from app.domain.models import Prediction, TeamStats
from app.infrastructure.ai.gemini_analyst import AiAnalysisResult

DISCLAIMER = "Статистика команд демонстраційна."


class DependencyHealthResponse(BaseModel):
    status: str
    provider: str
    teams_count: int


class PlayerInfo(BaseModel):
    nickname: str
    rating: float


class TeamInfo(BaseModel):
    name: str
    win_rate: float
    rating: float


class TeamRankingInfo(BaseModel):
    name: str
    win_rate: float
    rating: float
    points: int
    roster: list[PlayerInfo]


class PredictRequest(BaseModel):
    team_a_name: str = Field(..., min_length=1)
    team_b_name: str = Field(..., min_length=1)
    map_name: str | None = None
    use_ai: bool = False


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
    map_name: str | None = None
    ai_status: str | None = None
    ai_analysis: str | None = None
    ai_model: str | None = None

    def model_dump(self, *args: Any, **kwargs: Any) -> dict[str, Any]:
        data = super().model_dump(*args, **kwargs)
        if data.get("map_name") is None and data.get("ai_status") == "disabled":
            for key in ("map_name", "ai_status", "ai_analysis", "ai_model"):
                data.pop(key, None)
        return data


def to_team_info(team: TeamStats) -> TeamInfo:
    return TeamInfo(name=team.name, win_rate=team.win_rate, rating=team.rating)


def to_team_ranking_info(team: TeamStats) -> TeamRankingInfo:
    return TeamRankingInfo(
        name=team.name,
        win_rate=team.win_rate,
        rating=team.rating,
        points=team.points,
        roster=[PlayerInfo(nickname=p.nickname, rating=p.rating) for p in team.roster],
    )


def build_explanation(prediction: Prediction) -> str:
    leader = prediction.leader
    map_suffix = f" на карті {prediction.map_name}" if prediction.map_name else ""
    if leader is None:
        return f"Шанси команд рівні за округленим прогнозом{map_suffix}. {DISCLAIMER}"
    weights = prediction.weights
    formula = f"win_rate × {weights.win_rate} + rating × {weights.rating}"
    return f"{leader.name} має вищий розрахунковий бал{map_suffix} за формулою {formula}. {DISCLAIMER}"


def to_predict_response(
    prediction: Prediction,
    ai_result: AiAnalysisResult | None = None,
) -> PredictResponse:
    a = prediction.team_a
    b = prediction.team_b
    ai = ai_result or AiAnalysisResult(
        summary="Стандартний математичний режим (AI-аналіз не запитувався).",
        ai_status="disabled",
        model=None,
    )
    return PredictResponse(
        team_a_name=a.team.name,
        team_b_name=b.team.name,
        team_a_win_probability=prediction.probability_a,
        team_b_win_probability=prediction.probability_b,
        explanation=build_explanation(prediction),
        team_a_win_rate=round(a.effective_win_rate, 4),
        team_a_rating=a.team.rating,
        team_a_win_rate_contribution=round(a.win_rate_contribution, 3),
        team_a_rating_contribution=round(a.rating_contribution, 3),
        team_a_score=round(a.score, 3),
        team_b_win_rate=round(b.effective_win_rate, 4),
        team_b_rating=b.team.rating,
        team_b_win_rate_contribution=round(b.win_rate_contribution, 3),
        team_b_rating_contribution=round(b.rating_contribution, 3),
        team_b_score=round(b.score, 3),
        map_name=prediction.map_name,
        ai_status=ai.ai_status,
        ai_analysis=ai.summary,
        ai_model=ai.model,
    )