import io
import base64

import numpy as np
from fastapi import FastAPI, File, UploadFile, HTTPException
from PIL import Image
import cv2

from app.segmenter import segment_board


app = FastAPI(title="ChessReader — Board Segmenter MS2")


@app.get("/health")
def health():
    return {"status": "ok", "service": "ms-board-segmenter"}


@app.post("/segment")
async def segment(file: UploadFile = File(...)):
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="El archivo debe ser una imagen")

    try:
        contents  = await file.read()
        pil_image = Image.open(io.BytesIO(contents)).convert("RGB")
        image     = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"No se pudo leer la imagen: {e}")

    squares, method = segment_board(image)

    return {
        "method": method,
        "cells": [
            {
                "square": sq.square,
                "row":    sq.row,
                "col":    sq.col,
                "image_base64": base64.b64encode(sq.image_bytes).decode("utf-8")
            }
            for sq in squares
        ]
    }