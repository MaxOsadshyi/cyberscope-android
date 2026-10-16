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

Запит не змінювався порівняно з ЛР2.

### Відповідь `200` (`PredictResponse`, 15 полів)

Перші п'ять полів лишились без змін з ЛР2. Десять нових полів додано в кінець, щоб не ламати наявний клієнт.

| Поле | Тип | Опис |
|---|---|---|
| `team_a_name` | string | назва команди A з запиту |
| `team_b_name` | string | назва команди B з запиту |
| `team_a_win_probability` | float | ймовірність перемоги A, 0..1, округлена до 2 знаків |
| `team_b_win_probability` | float | `1 − P(A)`, округлена до 2 знаків |
| `explanation` | string | текстове пояснення (див. нижче) |
| `team_a_win_rate` | float | вихідний `win_rate` команди A |
| `team_a_rating` | float | вихідний `rating` команди A |
| `team_a_win_rate_contribution` | float | `win_rate × 0.6`, округлено до 3 знаків |
| `team_a_rating_contribution` | float | `rating × 0.4`, округлено до 3 знаків |
| `team_a_score` | float | підсумковий бал A, округлено до 3 знаків |
| `team_b_win_rate` | float | вихідний `win_rate` команди B |
| `team_b_rating` | float | вихідний `rating` команди B |
| `team_b_win_rate_contribution` | float | `win_rate × 0.6`, округлено до 3 знаків |
| `team_b_rating_contribution` | float | `rating × 0.4`, округлено до 3 знаків |
| `team_b_score` | float | підсумковий бал B, округлено до 3 знаків |

Ймовірності рахуються з **незаокруглених** балів, а потім округлюються. Округлені лише значення внесків і балів у відповіді.

Приклад для Natus Vincere проти FaZe Clan (реальна відповідь backend):

```json
{
  "team_a_name": "Natus Vincere",
  "team_b_name": "FaZe Clan",
  "team_a_win_probability": 0.53,
  "team_b_win_probability": 0.47,
  "explanation": "Natus Vincere має вищий розрахунковий бал за формулою win_rate × 0.6 + rating × 0.4. Статистика команд демонстраційна.",
  "team_a_win_rate": 0.65,
  "team_a_rating": 1.15,
  "team_a_win_rate_contribution": 0.39,
  "team_a_rating_contribution": 0.46,
  "team_a_score": 0.85,
  "team_b_win_rate": 0.58,
  "team_b_rating": 1.05,
  "team_b_win_rate_contribution": 0.348,
  "team_b_rating_contribution": 0.42,
  "team_b_score": 0.768
}
```

### Поле `explanation`

- Якщо округлені ймовірності різні: `"<команда> має вищий розрахунковий бал за формулою win_rate × 0.6 + rating × 0.4. Статистика команд демонстраційна."`
- Якщо округлені ймовірності рівні: `"Шанси команд рівні за округленим прогнозом. Статистика команд демонстраційна."`

### Помилки

| Код | Коли | Тіло |
|---|---|---|
| 400 | Однакові команди | `{"detail": "Оберіть дві різні команди"}` |
| 404 | Команди A немає в `TEAMS_DB` | `{"detail": "Команду '<назва>' не знайдено"}` |
| 404 | Команди B немає в `TEAMS_DB` | `{"detail": "Команду '<назва>' не знайдено"}` |
| 422 | Відсутнє або порожнє поле | Стандартна відповідь Pydantic: `detail` це масив об'єктів |

Перевірка команд іде в порядку: однакові → A не знайдена → B не знайдена. Якщо відсутні обидві, у повідомленні буде назва команди A. Помилки не змінювались порівняно з ЛР2.

### Як клієнт читає відповідь і помилку

`ApiClient.predictMatch()` розбирає успішну відповідь вручну через `org.json` (`getString`, `getDouble`) і тепер читає також десять нових полів. При коді не 200 читає `errorStream`, бере рядок `detail` і кидає з нього виняток. Для 422 `detail` є масивом, тому текст у застосунку буде технічним (див. `known-limitations.md`).

## Сумісність

- П'ять полів ЛР2 не змінено і не видалено: старий клієнт, який їх читає, продовжить працювати (зайві поля в JSON він ігнорує).
- Stack trace назовні не віддається.
