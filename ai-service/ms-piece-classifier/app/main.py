from contextlib import asynccontextmanager
from fastapi import FastAPI, File, UploadFile, HTTPException
from PIL import Image
import io

from app.classifier import PieceClassifier
from app.color_detector import detect_color

MODEL_PATH = "models/RESNET18_SCRATCH_V3.pth"

classifier: PieceClassifier = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global classifier
    classifier = PieceClassifier(MODEL_PATH)
    print("Modelo cargado")
    yield


app = FastAPI(
    title="ChessReader — Piece Classifier",
    version="1.0.0",
    lifespan=lifespan
)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/classify")
async def classify(file: UploadFile = File(...)):
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="El fichero debe ser una imagen")

    contents = await file.read()
    image = Image.open(io.BytesIO(contents)).convert("RGB")

    result = classifier.predict(image)

    if result["piece"] == "empty":
        color = None
    else:
        color = detect_color(image)

    return {
        "piece": result["piece"],
        "color": color,
        "confidence": result["confidence"]
    }