"""Тести прикладної логіки на fake-постачальнику: без реальних даних і мережі."""
import unittest

from app.domain.errors import (
    SameTeamsError,
    StatsUnavailableError,
    TeamNotFoundError,
)
from app.domain.models import PredictionWeights
from app.infrastructure.providers.fake import FakeTeamStatsProvider
from app.schemas import to_predict_response
from app.services.prediction_service import PredictionService


class PredictionServiceTests(unittest.TestCase):
    def test_probabilities_are_calculated_from_provider_data(self):
        service = PredictionService(FakeTeamStatsProvider())

        prediction = service.predict("Fake Alpha", "Fake Beta")

        # Alpha: 0.6*0.6 + 1.1*0.4 = 0.80; Beta: 0.5*0.6 + 1.0*0.4 = 0.70
        self.assertAlmostEqual(prediction.team_a.score, 0.80)
        self.assertAlmostEqual(prediction.team_b.score, 0.70)
        self.assertAlmostEqual(prediction.probability_a, 0.53)
        self.assertAlmostEqual(prediction.probability_b, 0.47)
        self.assertAlmostEqual(prediction.probability_a + prediction.probability_b, 1.0)
        self.assertEqual(prediction.leader.name, "Fake Alpha")

    def test_equal_stats_give_tie_without_favorite(self):
        service = PredictionService(FakeTeamStatsProvider())

        prediction = service.predict("Fake Beta", "Fake Gamma")
        explanation = to_predict_response(prediction).explanation

        self.assertTrue(prediction.is_tie)
        self.assertIsNone(prediction.leader)
        self.assertNotIn("Fake Beta", explanation)
        self.assertNotIn("Fake Gamma", explanation)
        self.assertIn("Шанси команд рівні", explanation)

    def test_unknown_team_raises_domain_error_with_name(self):
        service = PredictionService(FakeTeamStatsProvider())

        with self.assertRaises(TeamNotFoundError) as context:
            service.predict("Fake Alpha", "Navi")

        self.assertEqual(context.exception.team_name, "Navi")
        self.assertEqual(str(context.exception), "Команду 'Navi' не знайдено")

    def test_provider_failure_becomes_domain_error(self):
        service = PredictionService(FakeTeamStatsProvider(fail=True))

        with self.assertRaises(StatsUnavailableError):
            service.predict("Fake Alpha", "Fake Beta")
        with self.assertRaises(StatsUnavailableError):
            service.list_teams()

    def test_same_teams_are_rejected_before_provider_is_queried(self):
        provider = FakeTeamStatsProvider(fail=True)
        service = PredictionService(provider)

        with self.assertRaises(SameTeamsError):
            service.predict("Fake Alpha", "Fake Alpha")

        self.assertEqual(provider.calls, [])

    def test_weights_come_from_injection_not_from_code(self):
        service = PredictionService(
            FakeTeamStatsProvider(),
            PredictionWeights(win_rate=0.5, rating=0.5),
        )

        prediction = service.predict("Fake Alpha", "Fake Beta")
        explanation = to_predict_response(prediction).explanation

        self.assertAlmostEqual(prediction.team_a.win_rate_contribution, 0.30)
        self.assertAlmostEqual(prediction.team_a.rating_contribution, 0.55)
        self.assertAlmostEqual(prediction.team_b.score, 0.75)
        self.assertIn("win_rate × 0.5 + rating × 0.5", explanation)


if __name__ == "__main__":
    unittest.main()
