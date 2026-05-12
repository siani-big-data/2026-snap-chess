import io
from contextlib import asynccontextmanager

from fastapi import FastAPI, File, UploadFile, HTTPException
from PIL import Image

from app.detector import BoardDetector

MODEL_PATH = "models/board_detector.pt"

detector: BoardDetector = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global detector
    detector = BoardDetector(MODEL_PATH)
    print("Modelo cargado")
    yield


app = FastAPI(
    title="ChessReader — Board Detector",
    version="1.0.0",
    lifespan=lifespan
)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/detect")
async def detect(file: UploadFile = File(...)):
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="El fichero debe ser una imagen")

    contents = await file.read()
    image = Image.open(io.BytesIO(contents)).convert("RGB")

    detections = detector.detect(image)

    return {"detections": detections}