"""Модуль AI-аналітики матчів CS2 на базі Google Gemini API з безпечним fallback."""
from dataclasses import dataclass
from typing import Protocol

from app.domain.models import Prediction


@dataclass(frozen=True)
class AiAnalysisResult:
    """Результат роботи AI-компонента."""

    summary: str
    ai_status: str  # "disabled" | "generated" | "fallback"
    model: str | None = None


class MatchAnalyst(Protocol):
    """Контракт AI-аналітика прогнозу (дозволяє підміняти у тестах)."""

    def analyze(self, prediction: Prediction) -> AiAnalysisResult: ...


def build_match_prompt(prediction: Prediction) -> str:
    """1. Підготовка даних: формує структурований промпт із порахованих метрик."""
    a = prediction.team_a
    b = prediction.team_b
    map_line = (
        f"Обрана карта: {prediction.map_name} "
        f"(вінрейт {a.team.name} на карті: {a.effective_win_rate:.0%}, "
        f"вінрейт {b.team.name} на карті: {b.effective_win_rate:.0%})."
        if prediction.map_name
        else "Карту не обрано (оцінка за загальним вінрейтом команд)."
    )
    return (
        "Ти — аналітик кіберспортивних матчів Counter-Strike 2. "
        "На основі вже обчислених математичних показників дай стислий тактичний розбір українською мовою (3–4 речення, без markdown-списків і заголовків).\n"
        f"Команда A: {a.team.name} — вінрейт {a.effective_win_rate:.0%}, рейтинг {a.team.rating:.2f}, бал {a.score:.3f}, шанс перемоги {prediction.probability_a:.0%}.\n"
        f"Команда B: {b.team.name} — вінрейт {b.effective_win_rate:.0%}, рейтинг {b.team.rating:.2f}, бал {b.score:.3f}, шанс перемоги {prediction.probability_b:.0%}.\n"
        f"{map_line}\n"
        "Поясни: 1) головну перевагу фаворита (або рівність сил), 2) вплив обраної карти (якщо вказана), 3) головний ризик для фаворита. Не вигадуй склад гравців чи вигадані рахунки."
    )


class GeminiMatchAnalyst:
    """Реалізація AI-аналітики через Google GenAI SDK з таймаутом та fallback."""

    def __init__(
        self,
        api_key: str | None,
        model_name: str = "gemini-3.5-flash-lite",
        timeout_seconds: float = 25.0,
    ) -> None:
        self._api_key = (api_key or "").strip()
        self._model_name = model_name
        self._timeout_seconds = timeout_seconds

    def analyze(self, prediction: Prediction) -> AiAnalysisResult:
        if not self._api_key:
            return AiAnalysisResult(
                summary="AI-аналіз недоступний: не задано GEMINI_API_KEY. Показано базовий розрахунок.",
                ai_status="fallback",
                model=self._model_name,
            )

        prompt = build_match_prompt(prediction)
        try:
            raw_text = self._call_gemini(prompt)
            cleaned = self._normalize_response(raw_text)
            if not cleaned:
                return AiAnalysisResult(
                    summary="Модель повернула порожню відповідь. Показано базовий розрахунок.",
                    ai_status="fallback",
                    model=self._model_name,
                )
            return AiAnalysisResult(
                summary=cleaned,
                ai_status="generated",
                model=self._model_name,
            )
        except Exception:
            return AiAnalysisResult(
                summary="Не вдалося отримати відповідь від Gemini (таймаут або помилка мережі). Показано базовий розрахунок.",
                ai_status="fallback",
                model=self._model_name,
            )

    def _call_gemini(self, prompt: str) -> str:
        """2. AI-обробка: мережевий виклик моделі з обмеженим часом очікування."""
        from google import genai
        from google.genai import types

        client = genai.Client(
            api_key=self._api_key,
            http_options=types.HttpOptions(timeout=int(self._timeout_seconds * 1000)),
        )
        response = client.models.generate_content(
            model=self._model_name,
            contents=prompt,
            config=types.GenerateContentConfig(
                temperature=0.4,
                max_output_tokens=512,
            ),
        )
        return response.text or ""

    @staticmethod
    def _normalize_response(text: str) -> str:
        """3. Обробка результату: очищення зайвих переносів та маркерів."""
        lines = [line.strip(" -*•\t") for line in text.splitlines() if line.strip()]
        return " ".join(lines).strip()




