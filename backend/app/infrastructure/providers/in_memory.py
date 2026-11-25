"""Постачальник статистики з даних у пам'яті (базові команди + вінрейти по картах + склади)."""
from collections.abc import Iterable

from app.domain.models import PlayerStats, TeamStats

DEMO_TEAMS = (
    TeamStats(
        "Natus Vincere",
        0.65,
        1.15,
        {
            "Mirage": 0.72,
            "Inferno": 0.61,
            "Nuke": 0.54,
            "Ancient": 0.70,
            "Dust2": 0.64,
            "Anubis": 0.66,
            "Train": 0.68,
        },
        points=940,
        roster=(
            PlayerStats("b1t", 1.18),
            PlayerStats("w0nderful", 1.16),
            PlayerStats("jL", 1.15),
            PlayerStats("iM", 1.14),
            PlayerStats("Aleksib", 1.02),
        ),
    ),
    TeamStats(
        "FaZe Clan",
        0.58,
        1.05,
        {
            "Mirage": 0.55,
            "Inferno": 0.66,
            "Nuke": 0.71,
            "Ancient": 0.59,
            "Dust2": 0.52,
            "Anubis": 0.49,
            "Train": 0.54,
        },
        points=825,
        roster=(
            PlayerStats("frozen", 1.16),
            PlayerStats("EliGE", 1.13),
            PlayerStats("broky", 1.11),
            PlayerStats("rain", 1.03),
            PlayerStats("karrigan", 0.91),
        ),
    ),
    TeamStats(
        "Vitality",
        0.70,
        1.20,
        {
            "Mirage": 0.68,
            "Inferno": 0.73,
            "Nuke": 0.72,
            "Ancient": 0.62,
            "Dust2": 0.75,
            "Anubis": 0.74,
            "Train": 0.66,
        },
        points=985,
        roster=(
            PlayerStats("ZywOo", 1.32),
            PlayerStats("ropz", 1.19),
            PlayerStats("flameZ", 1.17),
            PlayerStats("mezii", 1.12),
            PlayerStats("apEX", 1.05),
        ),
    ),
    TeamStats(
        "G2 Esports",
        0.60,
        1.08,
        {
            "Mirage": 0.63,
            "Inferno": 0.67,
            "Nuke": 0.56,
            "Ancient": 0.58,
            "Dust2": 0.65,
            "Anubis": 0.55,
            "Train": 0.56,
        },
        points=860,
        roster=(
            PlayerStats("m0NESY", 1.27),
            PlayerStats("malbsMd", 1.14),
            PlayerStats("HeavyGod", 1.09),
            PlayerStats("huNter-", 1.04),
            PlayerStats("Snax", 0.94),
        ),
    ),
    TeamStats(
        "MOUZ",
        0.55,
        1.02,
        {
            "Mirage": 0.60,
            "Inferno": 0.52,
            "Nuke": 0.64,
            "Ancient": 0.57,
            "Dust2": 0.48,
            "Anubis": 0.51,
            "Train": 0.53,
        },
        points=790,
        roster=(
            PlayerStats("torzsi", 1.12),
            PlayerStats("xertioN", 1.10),
            PlayerStats("Spinx", 1.09),
            PlayerStats("Jimpphat", 1.07),
            PlayerStats("Brollan", 1.01),
        ),
    ),
    TeamStats(
        "Spirit",
        0.62,
        1.10,
        {
            "Mirage": 0.61,
            "Inferno": 0.50,
            "Nuke": 0.65,
            "Ancient": 0.69,
            "Dust2": 0.71,
            "Anubis": 0.63,
            "Train": 0.55,
        },
        points=895,
        roster=(
            PlayerStats("donk", 1.34),
            PlayerStats("sh1ro", 1.21),
            PlayerStats("zont1x", 1.08),
            PlayerStats("magixx", 1.02),
            PlayerStats("chopper", 0.96),
        ),
    ),
)


class InMemoryTeamStatsProvider:
    def __init__(self, teams: Iterable[TeamStats] = DEMO_TEAMS) -> None:
        self._teams = {team.name: team for team in teams}

    def list_teams(self) -> list[TeamStats]:
        return list(self._teams.values())

    def get_team(self, name: str) -> TeamStats | None:
        return self._teams.get(name)