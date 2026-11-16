"""Регресія: нова структура має давати ті самі відповіді, що й код до рефакторингу.

Еталон tests/golden_predictions.json створено зі старого main.py (ЛР4) до змін.
"""
import json
import unittest
from pathlib import Path

from app.domain.errors import SameTeamsError, TeamNotFoundError
from app.infrastructure.providers.in_memory import InMemoryTeamStatsProvider
from app.schemas import to_predict_response, to_team_info
from app.services.prediction_service import PredictionService

GOLDEN = json.loads(
    (Path(__file__).parent / "golden_predictions.json").read_text(encoding="utf-8")
)


class GoldenRegressionTests(unittest.TestCase):
    def setUp(self):
        self.service = PredictionService(InMemoryTeamStatsProvider())

    def test_team_list_matches_snapshot(self):
        teams = [to_team_info(team).model_dump() for team in self.service.list_teams()]
        self.assertEqual(teams, GOLDEN["teams"])

    def test_all_predictions_match_snapshot(self):
        self.assertEqual(len(GOLDEN["predictions"]), 30)
        for key, expected in GOLDEN["predictions"].items():
            team_a, team_b = key.split("|")
            with self.subTest(pair=key):
                actual = to_predict_response(self.service.predict(team_a, team_b)).model_dump()
                self.assertEqual(actual, expected)

    def test_error_scenarios_match_snapshot(self):
        error_types = {400: SameTeamsError, 404: TeamNotFoundError}
        for key, case in GOLDEN["errors"].items():
            with self.subTest(case=key):
                expected_type = error_types[case["status_code"]]
                with self.assertRaises(expected_type) as context:
                    self.service.predict(case["team_a_name"], case["team_b_name"])
                self.assertEqual(str(context.exception), case["detail"])


if __name__ == "__main__":
    unittest.main()
