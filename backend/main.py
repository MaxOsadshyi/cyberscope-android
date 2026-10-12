from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="CyberScope API")

# Дозволяємо запити з мобільного застосунку (для розробки)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Hardcoded статистика команд CS2 (заглушка замість реального API) ---
TEAMS_DB = {
    "Natus Vincere": {"win_rate": 0.65, "rating": 1.15},
    "FaZe Clan": {"win_rate": 0.58, "rating": 1.05},
    "Vitality": {"win_rate": 0.70, "rating": 1.20},
    "G2 Esports": {"win_rate": 0.60, "rating": 1.08},
    "MOUZ": {"win_rate": 0.55, "rating": 1.02},
    "Spirit": {"win_rate": 0.62, "rating": 1.10},
}


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


def calculate_prediction(
    team_a_name: str,
    team_a: dict[str, float],
    team_b_name: str,
    team_b: dict[str, float],
) -> PredictResponse:
    team_a_win_rate_contribution = team_a["win_rate"] * 0.6
    team_a_rating_contribution = team_a["rating"] * 0.4
    team_b_win_rate_contribution = team_b["win_rate"] * 0.6
    team_b_rating_contribution = team_b["rating"] * 0.4

    score_a = team_a_win_rate_contribution + team_a_rating_contribution
    score_b = team_b_win_rate_contribution + team_b_rating_contribution
    total = score_a + score_b
    prob_a = round(score_a / total, 2)
    prob_b = round(1 - prob_a, 2)

    if prob_a == prob_b:
        explanation = (
            "Шанси команд рівні за округленим прогнозом. "
            "Статистика команд демонстраційна."
        )
    else:
        leader = team_a_name if prob_a > prob_b else team_b_name
        explanation = (
            f"{leader} має вищий розрахунковий бал за формулою "
            "win_rate × 0.6 + rating × 0.4. "
            "Статистика команд демонстраційна."
        )

    return PredictResponse(
        team_a_name=team_a_name,
        team_b_name=team_b_name,
        team_a_win_probability=prob_a,
        team_b_win_probability=prob_b,
        explanation=explanation,
        team_a_win_rate=team_a["win_rate"],
        team_a_rating=team_a["rating"],
        team_a_win_rate_contribution=round(team_a_win_rate_contribution, 3),
        team_a_rating_contribution=round(team_a_rating_contribution, 3),
        team_a_score=round(score_a, 3),
        team_b_win_rate=team_b["win_rate"],
        team_b_rating=team_b["rating"],
        team_b_win_rate_contribution=round(team_b_win_rate_contribution, 3),
        team_b_rating_contribution=round(team_b_rating_contribution, 3),
        team_b_score=round(score_b, 3),
    )


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/teams", response_model=list[TeamInfo])
def get_teams():
    """Список команд, доступних для вибору у мобільному додатку."""
    return [
        TeamInfo(name=name, **stats) for name, stats in TEAMS_DB.items()
    ]


@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    if request.team_a_name == request.team_b_name:
        raise HTTPException(status_code=400, detail="Оберіть дві різні команди")

    team_a = TEAMS_DB.get(request.team_a_name)
    team_b = TEAMS_DB.get(request.team_b_name)

    if team_a is None:
        raise HTTPException(status_code=404, detail=f"Команду '{request.team_a_name}' не знайдено")
    if team_b is None:
        raise HTTPException(status_code=404, detail=f"Команду '{request.team_b_name}' не знайдено")

    return calculate_prediction(
        request.team_a_name,
        team_a,
        request.team_b_name,
        team_b,
    )