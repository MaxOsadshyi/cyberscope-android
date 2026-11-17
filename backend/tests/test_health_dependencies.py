import unittest

from fastapi.testclient import TestClient

from app.api.dependencies import get_team_stats_provider
from app.domain.errors import StatsUnavailableError
from app.infrastructure.providers.fake import FakeTeamStatsProvider
from app.services.prediction_service import PredictionService
from main import app


class DependencyHealthTests(unittest.TestCase):
    def setUp(self) -> None:
        app.dependency_overrides.clear()
        self.client = TestClient(app)

    def tearDown(self) -> None:
        app.dependency_overrides.clear()

    def test_service_check_dependencies_returns_count_and_propagates_failure(self) -> None:
        healthy_service = PredictionService(provider=FakeTeamStatsProvider())
        self.assertEqual(healthy_service.check_dependencies(), 3)

        failing_service = PredictionService(provider=FakeTeamStatsProvider(fail=True))
        with self.assertRaises(StatsUnavailableError):
            failing_service.check_dependencies()

    def test_health_dependencies_returns_200_for_in_memory_provider(self) -> None:
        response = self.client.get("/health/dependencies")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            {"status": "ok", "provider": "in_memory", "teams_count": 6},
        )

    def test_health_dependencies_returns_200_for_fake_provider(self) -> None:
        app.dependency_overrides[get_team_stats_provider] = lambda: FakeTeamStatsProvider()
        response = self.client.get("/health/dependencies")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "ok")
        self.assertEqual(response.json()["teams_count"], 3)

    def test_health_dependencies_returns_503_when_provider_is_unavailable(self) -> None:
        app.dependency_overrides[get_team_stats_provider] = (
            lambda: FakeTeamStatsProvider(fail=True)
        )
        response = self.client.get("/health/dependencies")
        self.assertEqual(response.status_code, 503)
        self.assertEqual(
            response.json()["detail"],
            "Джерело статистики тимчасово недоступне",
        )


if __name__ == "__main__":
    unittest.main()
