# Архітектура

## Загальна схема

```
Телефон / емулятор                      Комп'ютер
┌──────────────────────┐   HTTP + JSON  ┌──────────────────────────┐
│ MainActivity.kt      │ ─────────────► │ Uvicorn → FastAPI        │
│  (екран, стани)      │                │  main.py                 │
│ ApiClient.kt         │ ◄───────────── │  Pydantic-моделі         │
│ ApiModels.kt         │                │  TEAMS_DB (словник)      │
└──────────────────────┘                │  формула у predict()     │
        127.0.0.1:8000                  └──────────────────────────┘
        └──── adb reverse tcp:8000 tcp:8000 ────┘
```

## Компоненти

### Mobile (`mobile/app/src/main/java/com/example/helloworld/`)

| Файл | Відповідальність |
|---|---|
| `MainActivity.kt` | Екран `PredictionScreen`, випадні списки `TeamDropdown`, картка `ResultCard`, стани `UiState` (`Idle`, `Loading`, `Success`, `Error`) |
| `ApiClient.kt` | Об'єкт з двома функціями: `getTeams()` і `predictMatch()`. Використовує `HttpURLConnection`, таймаути 5 с, виконується у `Dispatchers.IO`. JSON розбирається вручну через `org.json` |
| `ApiModels.kt` | Класи `PredictRequest`, `PredictResponse`, `TeamInfo` |

Адреса backend: константа `BASE_URL = "http://127.0.0.1:8000"` у `ApiClient.kt`.
У `AndroidManifest.xml`: дозвіл `INTERNET` і `usesCleartextTraffic="true"`.

### Backend (`backend/main.py`)

| Елемент | Призначення |
|---|---|
| `TEAMS_DB` | Словник із 6 командами: `win_rate`, `rating` |
| `TeamInfo`, `PredictRequest`, `PredictResponse` | Pydantic-моделі |
| `GET /health` | Перевірка доступності |
| `GET /teams` | Список команд зі статистикою |
| `POST /predict` | Валідація, розрахунок, відповідь |
| CORS | `allow_origins=["*"]` (лише для розробки) |

## Потік даних основного сценарію

1. `PredictionScreen` при відкритті викликає `ApiClient.getTeams()` → `GET /teams` → отримує 6 команд.
2. Користувач обирає дві команди. Застосунок перевіряє лише те, що обрано обидві.
3. Кнопка ставить `UiState.Loading` і викликає `ApiClient.predictMatch(a, b)` → `POST /predict` з JSON `{"team_a_name": ..., "team_b_name": ...}`.
4. `predict()` перевіряє: однакові команди → 400, команда не знайдена → 404.
5. `predict()` рахує бали й ймовірності (див. `ai-component.md`).
6. Backend повертає `PredictResponse` у JSON.
7. `ApiClient` розбирає JSON у `PredictResponse`, екран ставить `UiState.Success` і малює `ResultCard`. При помилці: `UiState.Error` з текстом із поля `detail`.

## Відмінності від архітектури ЛР1

| ЛР1 (концепція) | ЛР2 (факт) |
|---|---|
| База даних команд, гравців, матчів | Словник `TEAMS_DB` у коді |
| Зовнішні джерела даних, що оновлюють БД | Немає, дані вручну |
| Окремий AI-модуль, який читає історичні дані з БД | Формула всередині `predict()`, модуля немає |
| Backend з кількома сервісами | Один файл `main.py` |
| Адреса `10.0.2.2` для емулятора | `127.0.0.1` + `adb reverse` (`10.0.2.2` давала timeout) |
