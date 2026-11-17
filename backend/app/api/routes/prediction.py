"""Тонкий HTTP router: приймає запит, викликає сервіс і повертає DTO."""
from fastapi import APIRouter, Depends

from app.api.dependencies import get_prediction_service
from app.core.config import Settings, get_settings
from app.schemas import (
    DependencyHealthResponse,
    PredictRequest,
    PredictResponse,
    TeamInfo,
    to_predict_response,
    to_team_info,
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


@router.post("/predict", response_model=PredictResponse)
def predict(
    request: PredictRequest,
    service: PredictionService = Depends(get_prediction_service),
) -> PredictResponse:
    prediction = service.predict(request.team_a_name, request.team_b_name)
    return to_predict_response(prediction)
