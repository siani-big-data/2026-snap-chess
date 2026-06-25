import cv2
import numpy as np
from PIL import Image

# Umbral de brillo (0-255) del núcleo de la pieza por debajo del cual se considera negra.
# Calibrado sobre el dataset de diagramas de libro: separa blancas (núcleo claro,
# interior hueco) de negras (núcleo oscuro, masa sólida) con ~2.7% de error,
# frente al ~12% del método anterior basado en densidad global de píxeles oscuros.
CORE_DARK_THRESHOLD = 135

# Proporción del lado de la casilla recortada como margen exterior (descarta bordes
# de tablero y restos del recorte de MS2).
ROI_MARGIN = 0.15

# Proporción del lado de la ROI recortada como margen interior para aislar el núcleo
# de la pieza (su centro), donde mejor se distingue relleno sólido de interior hueco.
CORE_MARGIN = 0.30


def detect_color(image: Image.Image) -> str:
    """Determina el color de una pieza ('white' | 'black') a partir de la casilla recortada.

    Mide el brillo medio del núcleo central de la pieza en lugar de la densidad global
    de píxeles oscuros: el centro de una pieza blanca es claro (contorno con interior
    hueco) mientras que el de una negra es oscuro (silueta rellena). Esto evita que el
    sombreado de las casillas oscuras del tablero contamine la medición.
    """
    img_array = np.array(image.convert('RGB'))
    gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY).astype(np.float32)

    height, width = gray.shape

    margin = int(min(height, width) * ROI_MARGIN)
    roi = gray[margin:height - margin, margin:width - margin]

    roi_h, roi_w = roi.shape
    core_margin = int(min(roi_h, roi_w) * CORE_MARGIN)
    core = roi[core_margin:roi_h - core_margin, core_margin:roi_w - core_margin]

    core_brightness = float(core.mean())

    return "black" if core_brightness < CORE_DARK_THRESHOLD else "white"