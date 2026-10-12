# API-контракт

Базова адреса: `http://127.0.0.1:8000`. Swagger UI: `/docs`. Формат: JSON.

Зміна цього контракту вимагає синхронного оновлення: `backend/main.py` (Pydantic), `mobile/.../ApiModels.kt`, розбору JSON в `ApiClient.kt` і цього файлу.

## GET /health

Відповідь `200`:

```json
{ "status": "ok" }
```

## GET /teams

Відповідь `200` (масив `TeamInfo`):

```json
[
  { "name": "Natus Vincere", "win_rate": 0.65, "rating": 1.15 },
  { "name": "FaZe Clan", "win_rate": 0.58, "rating": 1.05 }
]
```

Усього 6 команд: Natus Vincere, FaZe Clan, Vitality, G2 Esports, MOUZ, Spirit.

## POST /predict

### Запит (`PredictRequest`)

| Поле | Тип | Обов'язкове | Обмеження |
|---|---|---|---|
| `team_a_name` | string | так | мінімум 1 символ |
| `team_b_name` | string | так | мінімум 1 символ |

```json
{ "team_a_name": "Natus Vincere", "team_b_name": "FaZe Clan" }
```

### Відповідь `200` (`PredictResponse`)

| Поле | Тип | Опис |
|---|---|---|
| `team_a_name` | string | назва команди A з запиту |
| `team_b_name` | string | назва команди B з запиту |
| `team_a_win_probability` | float | ймовірність перемоги A, 0..1, округлена до 2 знаків |
| `team_b_win_probability` | float | `1 − P(A)`, округлена до 2 знаків |
| `explanation` | string | текстове пояснення |

```json
{
  "team_a_name": "Natus Vincere",
  "team_b_name": "FaZe Clan",
  "team_a_win_probability": 0.53,
  "team_b_win_probability": 0.47,
  "explanation": "Natus Vincere має вищий win rate та рейтинг за останні матчі"
}
```

### Помилки

| Код | Коли | Тіло |
|---|---|---|
| 400 | Однакові команди | `{"detail": "Оберіть дві різні команди"}` |
| 404 | Команди A немає в `TEAMS_DB` | `{"detail": "Команду '<назва>' не знайдено"}` |
| 404 | Команди B немає в `TEAMS_DB` | `{"detail": "Команду '<назва>' не знайдено"}` |
| 422 | Відсутнє або порожнє поле | Стандартна відповідь Pydantic: `detail` це масив об'єктів |

Перевірка команд іде в порядку: однакові → A не знайдена → B не знайдена. Якщо відсутні обидві, у повідомленні буде назва команди A.

### Як клієнт читає помилку

`ApiClient.predictMatch()` при коді не 200 читає `errorStream`, бере рядок `detail` і кидає з нього виняток. Для 422 `detail` є масивом, тому текст у застосунку буде технічним (див. `known-limitations.md`).

## Стабільність формату

- Успішна відповідь завжди містить п'ять полів з таблиці вище.
- Stack trace назовні не віддається.
