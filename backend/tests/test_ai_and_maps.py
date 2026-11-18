"""Автотести для ЛР6: вибір карти CS2, AI-аналітика Gemini та безпечний fallback."""
import unittest

from fastapi.testclient import TestClient

from app.api.dependencies import get_match_analyst
from app.domain.models import Prediction
from app.infrastructure.ai.gemini_analyst import (
    AiAnalysisResult,
    GeminiMatchAnalyst,
    build_match_prompt,
)
from app.main import app


class FakeSuccessAnalyst:
    def analyze(self, prediction: Prediction) -> AiAnalysisResult:
        return AiAnalysisResult(
            summary=f"Тестовий AI-розбір для карти {prediction.map_name or 'загальної'}.",
            ai_status="generated",
            model="fake-gemini-test",
        )


class AiAndMapsTests(unittest.TestCase):
    def tearDown(self) -> None:
        app.dependency_overrides.clear()

    def test_get_maps_returns_cs2_pool(self) -> None:
        client = TestClient(app)
        response = client.get("/maps")
        self.assertEqual(response.status_code, 200)
        maps = response.json()
        self.assertIn("Mirage", maps)
        self.assertIn("Nuke", maps)
        self.assertEqual(len(maps), 7)

    def test_map_selection_changes_win_rate_and_favorite(self) -> None:
        client = TestClient(app)
        # Без карти Natus Vincere фаворит (0.53 vs 0.47)
        base = client.post(
            "/predict",
            json={"team_a_name": "Natus Vincere", "team_b_name": "FaZe Clan"},
        ).json()
        self.assertGreater(base["team_a_win_probability"], base["team_b_win_probability"])

        # На Nuke фаворитом стає FaZe Clan (0.48 vs 0.52)
        on_nuke = client.post(
            "/predict",
            json={
                "team_a_name": "Natus Vincere",
                "team_b_name": "FaZe Clan",
                "map_name": "Nuke",
            },
        ).json()
        self.assertEqual(on_nuke["map_name"], "Nuke")
        self.assertEqual(on_nuke["team_a_win_rate"], 0.54)
        self.assertEqual(on_nuke["team_b_win_rate"], 0.71)
        self.assertLess(on_nuke["team_a_win_probability"], on_nuke["team_b_win_probability"])
        self.assertEqual(on_nuke["ai_status"], "disabled")

    def test_unknown_map_returns_400(self) -> None:
        client = TestClient(app)
        response = client.post(
            "/predict",
            json={
                "team_a_name": "Natus Vincere",
                "team_b_name": "FaZe Clan",
                "map_name": "UnknownMap",
            },
        )
        self.assertEqual(response.status_code, 400)
        self.assertIn("Невідома карта", response.json()["detail"])

    def test_use_ai_with_injected_analyst_returns_generated(self) -> None:
        app.dependency_overrides[get_match_analyst] = lambda: FakeSuccessAnalyst()
        client = TestClient(app)
        response = client.post(
            "/predict",
            json={
                "team_a_name": "Natus Vincere",
                "team_b_name": "FaZe Clan",
                "map_name": "Mirage",
                "use_ai": True,
            },
        )
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["ai_status"], "generated")
        self.assertEqual(body["ai_model"], "fake-gemini-test")
        self.assertIn("Mirage", body["ai_analysis"])

    def test_missing_api_key_triggers_graceful_fallback(self) -> None:
        app.dependency_overrides[get_match_analyst] = lambda: GeminiMatchAnalyst(api_key=None)
        client = TestClient(app)
        response = client.post(
            "/predict",
            json={
                "team_a_name": "Vitality",
                "team_b_name": "Spirit",
                "map_name": "Dust2",
                "use_ai": True,
            },
        )
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["ai_status"], "fallback")
        self.assertIn("GEMINI_API_KEY", body["ai_analysis"])

    def test_prompt_builder_includes_map_and_probabilities(self) -> None:
        from app.infrastructure.providers.in_memory import InMemoryTeamStatsProvider
        from app.services.prediction_service import PredictionService

        service = PredictionService(InMemoryTeamStatsProvider())
        pred = service.predict("Natus Vincere", "FaZe Clan", map_name="Nuke")
        prompt = build_match_prompt(pred)
        self.assertIn("Nuke", prompt)
        self.assertIn("54%", prompt)
        self.assertIn("71%", prompt)


if __name__ == "__main__":
    unittest.main()
