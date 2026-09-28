CyberScope: CS2 Match Predictor

Мобільний додаток для фанатів кіберспорту (CS2). У Лабораторній роботі 2 реалізовано MVP з одним завершеним сценарієм: користувач обирає дві команди, backend розраховує ймовірність перемоги, результат показується на екрані.

Ідея

На відміну від HLTV, який надає лише сирі дані, додаток пропонує аналітику: прогноз ймовірності перемоги команди на основі статистики.

Повна концепція (ЛР1): розклад турнірів, профілі команд і гравців, історія матчів, пошук та фільтрація. У ЛР2 реалізовано лише прогнозування матчу.

Архітектура

Android (Kotlin, Jetpack Compose) → HTTP/JSON → FastAPI backend → алгоритм прогнозування → відповідь у застосунок.

Структура репозиторію
mobile/: Android Studio проєкт (Kotlin, Jetpack Compose)
backend/: FastAPI сервіс (main.py, requirements.txt)
docs/screenshots/: скріншоти
Вимоги
Python 3.10+
Android Studio з емулятором (або телефон)
Git
Запуск backend
cd backend
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload

Swagger UI: http://127.0.0.1:8000/docs

Запуск mobile
Відкрити папку mobile/ в Android Studio, дочекатись Gradle sync.
Запустити емулятор (Device Manager).
Прокинути порт backend на емулятор (повторювати після кожного перезапуску емулятора):
   adb reverse tcp:8000 tcp:8000

Якщо adb не знайдено (PowerShell):

   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
Натиснути Run ▶.

Адреса backend у застосунку: http://127.0.0.1:8000 (файл ApiClient.kt). 

API
Endpoint	Метод	Призначення
/health	GET	Перевірка доступності
/teams	GET	Список команд зі статистикою
/predict	POST	Прогноз результату матчу

Приклад запиту:

json
POST /predict
{ "team_a_name": "Natus Vincere", "team_b_name": "FaZe Clan" }

Приклад відповіді (200):

json
{
  "team_a_name": "Natus Vincere",
  "team_b_name": "FaZe Clan",
  "team_a_win_probability": 0.53,
  "team_b_win_probability": 0.47,
  "explanation": "Natus Vincere має вищий win rate та рейтинг за останні матчі"
}

Помилки:

Код	Причина	Відповідь
400	Однакові команди	{"detail": "Оберіть дві різні команди"}
404	Команду не знайдено	{"detail": "Команду '...' не знайдено"}
422	Порожнє поле	Стандартна валідація Pydantic
Інтелектуальний компонент

Базовий алгоритм (допускається методичкою):

score = win_rate * 0.6 + rating * 0.4
P(A) = score_A / (score_A + score_B)

Статистика 6 команд зберігається в TEAMS_DB (hardcoded).

Приклади використання:

№	Команда A	Команда B	Результат A / B	Висновок
1	Natus Vincere	FaZe Clan	53% / 47%	NAVI фаворит
2	Vitality	MOUZ	55% / 45%	Vitality фаворит
3	G2 Esports	Spirit	49% / 51%	Майже рівні шанси
4	Vitality	Natus Vincere	51% / 49%	Майже рівні шанси
5	MOUZ	FaZe Clan	49% / 51%	Майже рівні шанси
Стани інтерфейсу

Початковий → Завантаження → Успіх / Помилка. Повторний запит працює без перезапуску застосунку.

Скріншоти

Show Image Show Image Show Image Show Image

Безпека
API keys у коді відсутні
Передаються лише назви команд, персональних даних немає
Помилки повертаються як {"detail": ...} без stack trace
Android permissions: лише INTERNET
Обмеження поточної версії
Дані команд hardcoded, реальні джерела (HLTV, Liquipedia) не підключені
Прогноз базується на простій формулі, а не на навченій ML-моделі
HTTP без шифрування (usesCleartextTraffic) і відкритий CORS: лише для розробки
Конфігурація не винесена в environment variables
Адреса backend прописана в коді