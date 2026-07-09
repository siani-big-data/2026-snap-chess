"""Pruebas ligeras del segmentador (sin modelo opcional pesado)."""
from http import HTTPStatus

from fastapi.testclient import TestClient

from app.main import app


def test_health_returns_ok():
    with TestClient(app) as client:
        response = client.get("/health")
    assert response.status_code == HTTPStatus.OK
    payload = response.json()
    assert payload.get("status") == "ok"
    assert payload.get("service") == "ms-board-segmenter"
