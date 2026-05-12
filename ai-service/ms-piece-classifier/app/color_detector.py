import cv2
import numpy as np
from PIL import Image


def detect_color(image: Image.Image) -> str:
    img_array = np.array(image.convert('RGB'))
    img_bgr = cv2.cvtColor(img_array, cv2.COLOR_RGB2BGR)

    height, width = img_bgr.shape[:2]
    margin = int(min(height, width) * 0.2)
    center_region = img_bgr[margin:height - margin, margin:width - margin]

    gray = cv2.cvtColor(center_region, cv2.COLOR_BGR2GRAY)
    mean_brightness = np.mean(gray)

    return "white" if mean_brightness > 128 else "black"