"""Централізоване перетворення доменних помилок на HTTP-відповіді."""
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse

from app.domain.errors import (
    DomainError,
    SameTeamsError,
    StatsUnavailableError,
    TeamNotFoundError,
)


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(SameTeamsError)
    async def handle_same_teams(_: Request, exc: SameTeamsError) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={"detail": str(exc)},
        )

    @app.exception_handler(TeamNotFoundError)
    async def handle_team_not_found(_: Request, exc: TeamNotFoundError) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={"detail": str(exc)},
        )

    @app.exception_handler(StatsUnavailableError)
    async def handle_stats_unavailable(
        _: Request, exc: StatsUnavailableError
    ) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            content={"detail": str(exc)},
        )

    @app.exception_handler(DomainError)
    async def handle_domain_error(_: Request, exc: DomainError) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={"detail": str(exc)},
        )
