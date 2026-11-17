# CyberScope: CS2 Match Predictor

Мобільний додаток для фанатів кіберспорту (CS2). Користувач обирає дві команди, backend розраховує ймовірність перемоги, а застосунок показує результат, пояснення розрахунку та зберігає успішні прогнози в локальну історію на пристрої.

- **ЛР2:** MVP «mobile → backend → результат».
- **ЛР3:** Knowledge Base (`docs/`), контрольована розробка з coding agent, блок «Чому такий прогноз?».
- **ЛР4:** локальна історія прогнозів (SQLite), перегляд, деталі, видалення, очищення.
- **ЛР5:** шарувата архітектура backend (`domain` → `services` → `infrastructure` → `api`), заміна джерела статистики через `TeamStatsProvider`, конфігурація через змінні середовища, 19 автотестів і `GET /health/dependencies`.

## Структура репозиторію

- `mobile/`: Android Studio проєкт (Kotlin, Jetpack Compose, SQLite)
- `backend/`: FastAPI сервіс (`main.py`, `app/`, `requirements.txt`, `requirements-dev.txt`, `tests/`)
- `docs/`: Knowledge Base проєкту
- `docs/screenshots/`: скріншоти

## Запуск backend

```powershell
cd backend
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload
```

Запуск із альтернативним постачальником статистики (`FakeTeamStatsProvider`):

```powershell
$env:STATS_PROVIDER = "fake"
uvicorn main:app --reload
```

Swagger UI: http://127.0.0.1:8000/docs

## Запуск mobile

1. Відкрити папку `mobile/` в Android Studio, дочекатись Gradle sync.
2. Запустити емулятор і прокинути порт: `adb reverse tcp:8000 tcp:8000`
3. Натиснути Run ▶. Адреса backend у застосунку: `http://127.0.0.1:8000` (`ApiClient.kt`).

## Тести

```powershell
cd backend
venv\Scripts\activate
pip install -r requirements-dev.txt
python -m unittest discover -s tests -v
```

Очікуваний результат: 19 тестів, у кінці `OK`.

## API

| Endpoint | Метод | Призначення |
|---|---|---|
| `/health` | GET | Перевірка доступності сервісу |
| `/health/dependencies` | GET | Перевірка доступності постачальника статистики (`200` або `503`) |
| `/teams` | GET | Список команд зі статистикою |
| `/predict` | POST | Прогноз результату матчу |

Повний контракт: [`docs/api-contract.md`](docs/api-contract.md).

## Knowledge Base

- [`ai-instructions.md`](docs/ai-instructions.md) — правила роботи coding agent
- [`project-overview.md`](docs/project-overview.md) — проблема, користувач, межі MVP
- [`requirements.md`](docs/requirements.md) — вимоги та їхній статус
- [`architecture.md`](docs/architecture.md) — компоненти та потік даних
- [`api-contract.md`](docs/api-contract.md) — endpoints, моделі, помилки
- [`ai-component.md`](docs/ai-component.md) — алгоритм прогнозу
- [`testing.md`](docs/testing.md) — тести та ручні сценарії
- [`known-limitations.md`](docs/known-limitations.md) — відомі обмеження
- [`decisions.md`](docs/decisions.md) — технічні рішення
