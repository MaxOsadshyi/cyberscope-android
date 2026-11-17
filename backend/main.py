"""Тонка точка входу: `uvicorn main:app` запускає застосунок із `app.main`."""
from app.main import app

__all__ = ["app"]
