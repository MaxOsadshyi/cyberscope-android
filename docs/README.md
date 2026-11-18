# CyberScope: CS2 Match Predictor

Мобільний додаток для фанатів кіберспорту (CS2). Користувач обирає дві команди, опціонально карту матчу та тактичний AI-розбір (Gemini), backend розраховує ймовірність перемоги та формує аналітичний коментар, а застосунок зберігає успішні прогнози в локальну історію на пристрої.

- **ЛР2:** MVP «mobile → backend → результат».
- **ЛР3:** Knowledge Base (`docs/`), контрольована розробка з coding agent, блок «Чому такий прогноз?».
- **ЛР4:** локальна історія прогнозів (SQLite), перегляд, деталі, видалення, очищення.
- **ЛР5:** шарувата архітектура backend (`domain` → `services` → `infrastructure` → `api`), контракт `TeamStatsProvider`, змінні середовища, 19 автотестів і `GET /health/dependencies`.
- **ЛР6:** вибір карти CS2 (`GET /maps`, мап-пул з 7 карт із власним вінрейтом команд), інтеграція зовнішнього LLM API (**Google Gemini**, модель `gemini-3.5-flash-lite`) для тактичного аналізу матчу з відмовостійким `fallback`-режимом, 25 автотестів.

## Ідея

На відміну від HLTV, який надає лише сирі дані, додаток пропонує аналітику:
прогноз ймовірності перемоги команди на основі загальної або мап-специфічної статистики, прозоре математичне пояснення та стислий тактичний розбір від Gemini на основі порахованих показників.

Повна концепція (ЛР1): розклад турнірів, профілі команд і гравців, історія матчів, пошук та фільтрація.
Реалізовано прогнозування матчу (загальне та по картах), AI-аналіз Gemini та локальну історію прогнозів.

## Архітектура

Android (Kotlin, Jetpack Compose) → HTTP/JSON → FastAPI backend (`PredictionService` + `GeminiMatchAnalyst` через `google-genai`) → відповідь у застосунок.
Успішна відповідь (включно з обраною картою та AI-розбором) зберігається в локальну базу SQLite на телефоні.

Детальний опис: [`docs/architecture.md`](docs/architecture.md).

## Структура репозиторію

- `mobile/`: Android Studio проєкт (Kotlin, Jetpack Compose)
- `backend/`: FastAPI сервіс (`app/`, `main.py`, `requirements.txt`, `tests/`)
- `docs/`: Knowledge Base проєкту
- `docs/screenshots/`: скріншоти

## Вимоги

- Python 3.10+
- Android Studio з емулятором (або телефон)
- Git
- Безкоштовний API-ключ Google AI Studio (`GEMINI_API_KEY`) для генерації AI-аналізу (без ключа сервер працює у базовому режимі з безпечним `fallback`)

## Запуск backend

```powershell
cd backend
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt

# Задати ключ Gemini API у поточній сесії (ніколи не комітити в репозиторій!)
$env:GEMINI_API_KEY = "ваш_ключ_Gemini"

.\venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload