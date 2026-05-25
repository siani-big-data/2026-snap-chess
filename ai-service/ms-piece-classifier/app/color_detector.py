import cv2
import numpy as np
from PIL import Image


def detect_color(image: Image.Image) -> str:
    img_array = np.array(image.convert('RGB'))
    gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)

    height, width = gray.shape

    margin = int(min(height, width) * 0.15)
    roi = gray[margin:height - margin, margin:width - margin]

    _, binary = cv2.threshold(roi, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)

    kernel = np.ones((2, 2), np.uint8)
    binary_clean = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel)

    total_pixels = binary_clean.size
    dark_pixels = np.count_nonzero(binary_clean)
    density = dark_pixels / total_pixels

    return "black" if density > 0.35 else "white"