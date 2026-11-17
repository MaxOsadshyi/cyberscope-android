import os
import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient

from app.api.dependencies import get_team_stats_provider
from app.infrastructure.providers.fake import FakeTeamStatsProvider
from main import app


class PredictionApiTests(unittest.TestCase):
    def setUp(self) -> None:
        app.dependency_overrides.clear()
        self.client = TestClient(app)

    def tearDown(self) -> None:
        app.dependency_overrides.clear()

    def test_natus_vincere_vs_faze_clan_returns_expected_contract(self) -> None:
        response = self.client.post(
            "/predict",
            json={"team_a_name": "Natus Vincere", "team_b_name": "FaZe Clan"},
        )
        self.assertEqual(response.status_code, 200)
        data = response.json()

        self.assertEqual(
            set(data),
            {
                "team_a_name",
                "team_b_name",
                "team_a_win_probability",
                "team_b_win_probability",
                "explanation",
                "team_a_win_rate",
                "team_a_rating",
                "team_a_win_rate_contribution",
                "team_a_rating_contribution",
                "team_a_score",
                "team_b_win_rate",
                "team_b_rating",
                "team_b_win_rate_contribution",
                "team_b_rating_contribution",
                "team_b_score",
            },
        )
        self.assertEqual(len(data), 15)
        self.assertAlmostEqual(data["team_a_win_probability"], 0.53)
        self.assertAlmostEqual(data["team_b_win_probability"], 0.47)
        self.assertAlmostEqual(data["team_a_score"], 0.850)
        self.assertAlmostEqual(data["team_b_score"], 0.768)
        self.assertIn("Статистика команд демонстраційна.", data["explanation"])

    def test_dependency_override_with_fake_provider_and_tie_explanation(self) -> None:
        app.dependency_overrides[get_team_stats_provider] = lambda: FakeTeamStatsProvider()

        teams_resp = self.client.get("/teams")
        self.assertEqual(teams_resp.status_code, 200)
        team_names = [t["name"] for t in teams_resp.json()]
        self.assertEqual(team_names, ["Fake Alpha", "Fake Beta", "Fake Gamma"])

        pred_resp = self.client.post(
            "/predict",
            json={"team_a_name": "Fake Beta", "team_b_name": "Fake Gamma"},
        )
        self.assertEqual(pred_resp.status_code, 200)
        data = pred_resp.json()
        self.assertAlmostEqual(data["team_a_win_probability"], 0.50)
        self.assertAlmostEqual(data["team_b_win_probability"], 0.50)
        self.assertIn("Шанси команд рівні", data["explanation"])

    def test_env_var_switches_provider_to_fake(self) -> None:
        with patch.dict(os.environ, {"STATS_PROVIDER": "fake"}):
            response = self.client.get("/teams")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            [t["name"] for t in response.json()],
            ["Fake Alpha", "Fake Beta", "Fake Gamma"],
        )

    def test_same_teams_return_400(self) -> None:
        response = self.client.post(
            "/predict",
            json={"team_a_name": "Natus Vincere", "team_b_name": "Natus Vincere"},
        )
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json()["detail"], "Оберіть дві різні команди")

    def test_unknown_team_return_404(self) -> None:
        response = self.client.post(
            "/predict",
            json={"team_a_name": "Unknown", "team_b_name": "FaZe Clan"},
        )
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.json()["detail"], "Команду 'Unknown' не знайдено")

    def test_unavailable_provider_returns_503(self) -> None:
        app.dependency_overrides[get_team_stats_provider] = (
            lambda: FakeTeamStatsProvider(fail=True)
        )
        response = self.client.post(
            "/predict",
            json={"team_a_name": "Fake Alpha", "team_b_name": "Fake Beta"},
        )
        self.assertEqual(response.status_code, 503)
        self.assertEqual(
            response.json()["detail"],
            "Джерело статистики тимчасово недоступне",
        )


if __name__ == "__main__":
    unittest.main()
