import unittest

from fastapi import HTTPException

from main import PredictRequest, TEAMS_DB, calculate_prediction, predict


class PredictionTests(unittest.TestCase):
    def test_natus_vincere_vs_faze_clan_returns_expected_details(self):
        result = calculate_prediction(
            "Natus Vincere",
            TEAMS_DB["Natus Vincere"],
            "FaZe Clan",
            TEAMS_DB["FaZe Clan"],
        )
        fields = result.model_dump()

        self.assertEqual(
            set(fields),
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
        self.assertEqual(len(fields), 15)
        self.assertAlmostEqual(result.team_a_win_probability, 0.53)
        self.assertAlmostEqual(result.team_b_win_probability, 0.47)
        self.assertAlmostEqual(result.team_a_win_rate_contribution, 0.390)
        self.assertAlmostEqual(result.team_a_rating_contribution, 0.460)
        self.assertAlmostEqual(result.team_a_score, 0.850)
        self.assertAlmostEqual(result.team_b_win_rate_contribution, 0.348)
        self.assertAlmostEqual(result.team_b_rating_contribution, 0.420)
        self.assertAlmostEqual(result.team_b_score, 0.768)
        self.assertAlmostEqual(
            result.team_a_win_probability + result.team_b_win_probability,
            1.0,
        )
        self.assertIn("Статистика команд демонстраційна.", result.explanation)

    def test_equal_rounded_probabilities_have_neutral_explanation(self):
        team_a = {"win_rate": 0.6, "rating": 1.1}
        team_b = {"win_rate": 0.6, "rating": 1.1025}

        result = calculate_prediction("Team A", team_a, "Team B", team_b)

        self.assertAlmostEqual(result.team_a_win_probability, 0.50)
        self.assertAlmostEqual(result.team_b_win_probability, 0.50)
        self.assertIn("Шанси команд рівні", result.explanation)
        self.assertIn("Статистика команд демонстраційна.", result.explanation)
        self.assertNotIn("Team A має", result.explanation)
        self.assertNotIn("Team B має", result.explanation)

    def test_same_teams_return_400(self):
        with self.assertRaises(HTTPException) as context:
            predict(PredictRequest(team_a_name="Natus Vincere", team_b_name="Natus Vincere"))

        self.assertEqual(context.exception.status_code, 400)

    def test_unknown_team_returns_404(self):
        with self.assertRaises(HTTPException) as context:
            predict(PredictRequest(team_a_name="Unknown", team_b_name="FaZe Clan"))

        self.assertEqual(context.exception.status_code, 404)


if __name__ == "__main__":
    unittest.main()
