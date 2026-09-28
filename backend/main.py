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

    # --- Проста AI-функція: вагова формула на основі статистики ---
    score_a = team_a["win_rate"] * 0.6 + team_a["rating"] * 0.4
    score_b = team_b["win_rate"] * 0.6 + team_b["rating"] * 0.4

    total = score_a + score_b
    prob_a = round(score_a / total, 2)
    prob_b = round(1 - prob_a, 2)

    leader = request.team_a_name if prob_a > prob_b else request.team_b_name
    explanation = f"{leader} має вищий win rate та рейтинг за останні матчі"

    return PredictResponse(
        team_a_name=request.team_a_name,
        team_b_name=request.team_b_name,
        team_a_win_probability=prob_a,
        team_b_win_probability=prob_b,
        explanation=explanation,
    )