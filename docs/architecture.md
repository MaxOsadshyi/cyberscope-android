# Архітектура

## Загальна схема
Android (Kotlin, Compose, SQLite) ──HTTP/JSON──► FastAPI (backend/main.py)
├─ app/api/ (routes, Depends, error_handlers)
├─ app/services/ (PredictionService)
├─ app/domain/ (models, errors, TeamStatsProvider)
└─ app/infrastructure/providers/
├─ InMemoryTeamStatsProvider
└─ FakeTeamStatsProvider
## Компоненти Backend (`backend/`)

- `main.py` — тонка точка входу (`from app.main import app`).
- `app/main.py` — фабрика `create_app()`, налаштування CORS, реєстрація обробників помилок і роутера.
- `app/core/config.py` — `Settings` і `get_settings()` (зчитує `STATS_PROVIDER`, `WEIGHT_WIN_RATE`, `WEIGHT_RATING`, `CORS_ALLOW_ORIGINS`).
- `app/domain/` — чисті доменні моделі (`TeamStats`, `PredictionWeights`, `TeamBreakdown`, `Prediction`), винятки (`SameTeamsError`, `TeamNotFoundError`, `StatsUnavailableError`) та контракт `TeamStatsProvider` (`Protocol`).
- `app/services/prediction_service.py` — `PredictionService` (`list_teams`, `check_dependencies`, `predict`). Не залежить від FastAPI чи конкретної реалізації провайдера.
- `app/infrastructure/providers/` — реалізації `TeamStatsProvider`: `InMemoryTeamStatsProvider` (6 команд) та `FakeTeamStatsProvider` (3 тестові команди, режим `fail=True`).
- `app/schemas.py` — Pydantic DTO (`TeamInfo`, `PredictRequest`, `PredictResponse`, `DependencyHealthResponse`) та мапери.
- `app/api/` — `dependencies.py` (FastAPI `Depends`), `error_handlers.py` (переклад доменних помилок у HTTP `400`/`404`/`503`) та `routes/prediction.py`.

## Правило напрямку залежностей

`app/api` → `app/services` → `app/domain` ← `app/infrastructure`.
Модуль `app/domain` не містить імпортів з FastAPI, Pydantic або сусідніх шарів.

## Компоненти Mobile (`mobile/`)

- `MainActivity.kt` — екрани (`Predict`, `History`, `Details`), стани `UiState`, картка результату `ResultCard`.
- `ApiClient.kt`, `ApiModels.kt` — запити `GET /teams` і `POST /predict` через `HttpURLConnection` (`http://127.0.0.1:8000` + `adb reverse`).
- `HistoryStore.kt`, `HistoryModels.kt`, `HistoryScreens.kt` — локальна база SQLite (`cyberscope_history.db`) для збереження успішних прогнозів.
