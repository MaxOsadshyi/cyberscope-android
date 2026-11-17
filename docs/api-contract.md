# API-контракт

Базова адреса: `http://127.0.0.1:8000`. Swagger UI: `/docs`. Формат: JSON.
Зміна контракту вимагає синхронного оновлення `backend/app/schemas.py`, `ApiModels.kt`, `ApiClient.kt` та цього файлу.

## GET /health

Відповідь `200`: `{ "status": "ok" }`

## GET /health/dependencies

Перевіряє доступність активного постачальника статистики (`TeamStatsProvider`).

- Відповідь `200` (`DependencyHealthResponse`):
  `{ "status": "ok", "provider": "in_memory", "teams_count": 6 }` (або `"provider": "fake", "teams_count": 3` при `STATS_PROVIDER=fake`).
- Відповідь `503`:
  `{ "detail": "Джерело статистики тимчасово недоступне" }`

## GET /teams

Відповідь `200` (масив `TeamInfo`): список команд (`name`, `win_rate`, `rating`).
За замовчуванням (`STATS_PROVIDER=in_memory`) — 6 команд; при `STATS_PROVIDER=fake` — 3 команди (`Fake Alpha`, `Fake Beta`, `Fake Gamma`).

## POST /predict

Запит (`PredictRequest`): `{ "team_a_name": "Natus Vincere", "team_b_name": "FaZe Clan" }` (обидва поля обов'язкові, `min_length=1`).

Відповідь `200` (`PredictResponse`, 15 полів):
- 5 полів ЛР2: `team_a_name`, `team_b_name`, `team_a_win_probability`, `team_b_win_probability`, `explanation`.
- 10 полів розбивки ЛР3: `team_a_win_rate`, `team_a_rating`, `team_a_win_rate_contribution`, `team_a_rating_contribution`, `team_a_score` (і аналогічно для `team_b_*`).

Помилки:
- `400` — однакові команди: `{"detail": "Оберіть дві різні команди"}`
- `404` — команду не знайдено: `{"detail": "Команду '<назва>' не знайдено"}`
- `422` — порожнє або відсутнє поле (масив помилок Pydantic)
- `503` — джерело статистики недоступне: `{"detail": "Джерело статистики тимчасово недоступне"}`
