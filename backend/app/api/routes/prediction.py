"""Тонкий HTTP router: приймає запит, викликає сервіс і повертає DTO."""
from app.api.dependencies import get_team_stats_provider
from typing import Any
from fastapi import APIRouter, Depends

from app.api.dependencies import get_prediction_service
from app.core.config import Settings, get_settings
from app.schemas import (
    DependencyHealthResponse,
    PredictRequest,
    PredictResponse,
    TeamInfo,
    TeamRankingInfo,
    to_predict_response,
    to_team_info,
    to_team_ranking_info,
)
from app.services.prediction_service import PredictionService

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@router.get("/health/dependencies", response_model=DependencyHealthResponse)
def health_dependencies(
    service: PredictionService = Depends(get_prediction_service),
    settings: Settings = Depends(get_settings),
) -> DependencyHealthResponse:
    teams_count = service.check_dependencies()
    return DependencyHealthResponse(
        status="ok",
        provider=settings.stats_provider,
        teams_count=teams_count,
    )


@router.get("/teams", response_model=list[TeamInfo])
def get_teams(
    service: PredictionService = Depends(get_prediction_service),
) -> list[TeamInfo]:
    return [to_team_info(team) for team in service.list_teams()]


@router.get("/maps", response_model=list[str])
def get_maps(
    service: PredictionService = Depends(get_prediction_service),
) -> list[str]:
    return service.list_maps()


@router.post("/predict", response_model=PredictResponse, response_model_exclude_none=True)
def predict(
    request: PredictRequest,
    service: PredictionService = Depends(get_prediction_service),
) -> dict[str, Any]:
    prediction = service.predict(
        request.team_a_name,
        request.team_b_name,
        map_name=request.map_name,
    )
    ai_result = service.analyze_with_ai(prediction, use_ai=request.use_ai)
    return to_predict_response(prediction, ai_result=ai_result).model_dump()

@router.get("/teams/ranking", response_model=list[TeamRankingInfo])
def get_teams_ranking(
    provider=Depends(get_team_stats_provider),
) -> list[TeamRankingInfo]:
    ranked = sorted(provider.list_teams(), key=lambda t: t.points, reverse=True)
    return [to_team_ranking_info(team) for team in ranked]