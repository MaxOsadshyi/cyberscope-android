"""Помилки прикладного рівня. Не знають про HTTP: код відповіді обирає API-шар."""


class DomainError(Exception):
    """Базова помилка сценарію прогнозу."""


class SameTeamsError(DomainError):
    def __init__(self) -> None:
        super().__init__("Оберіть дві різні команди")


class TeamNotFoundError(DomainError):
    def __init__(self, team_name: str) -> None:
        self.team_name = team_name
        super().__init__(f"Команду '{team_name}' не знайдено")


class StatsUnavailableError(DomainError):
    """Джерело статистики не відповіло або повернуло збій."""

    def __init__(self, message: str = "Джерело статистики тимчасово недоступне") -> None:
        super().__init__(message)
