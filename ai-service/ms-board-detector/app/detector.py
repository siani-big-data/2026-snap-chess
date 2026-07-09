from ultralytics import YOLO
from PIL import Image


class BoardDetector:

    def __init__(self, model_path: str):
        self.model = YOLO(model_path)

    def detect(self, image: Image.Image) -> list[dict]:
        results = self.model(image, verbose=False)
        detections = []

        for result in results:
            for box in result.boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                detections.append({
                    "x": round(x1),
                    "y": round(y1),
                    "w": round(x2 - x1),
                    "h": round(y2 - y1),
                    "confidence": round(float(box.conf[0]), 4)
                })

        return detections