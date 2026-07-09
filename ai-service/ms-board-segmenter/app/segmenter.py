import cv2
import numpy as np
from dataclasses import dataclass

GAUSSIAN_BLUR_KERNEL = (5, 5)
SOBEL_THRESHOLD      = 80
HOUGH_VOTE_THRESHOLD = 120
HOUGH_MIN_LINE_RATIO = 0.4
HOUGH_MAX_LINE_GAP   = 20
CLUSTER_DIVISOR      = 16
BORDER_SIZE          = 10
GRID_LINES           = 9
BOARD_SIZE           = 8

@dataclass
class SquareResult:
    square: str        # "a8", "h1", etc
    row: int
    col: int
    image_bytes: bytes


def segment_board(board_image: np.ndarray) -> tuple[list[SquareResult], str]:
    board_image = _crop_white_margin(board_image)
    result = _segment_with_hough(board_image)

    if result is not None:
        return result, "hough"

    return _segment_fallback(board_image), "fallback"


def _add_synthetic_border(image: np.ndarray) -> np.ndarray:
    return cv2.copyMakeBorder(
        image,
        top=BORDER_SIZE, bottom=BORDER_SIZE,
        left=BORDER_SIZE, right=BORDER_SIZE,
        borderType=cv2.BORDER_CONSTANT,
        value=(0, 0, 0)
    )


def _detect_edges(image: np.ndarray) -> np.ndarray:
    gray      = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    blurred   = cv2.GaussianBlur(gray, GAUSSIAN_BLUR_KERNEL, sigmaX=0)
    sobel_x   = cv2.Sobel(blurred, cv2.CV_64F, dx=1, dy=0, ksize=3)
    sobel_y   = cv2.Sobel(blurred, cv2.CV_64F, dx=0, dy=1, ksize=3)
    magnitude = cv2.magnitude(sobel_x, sobel_y)
    magnitude = np.uint8(np.clip(magnitude, 0, 255))
    _, edges  = cv2.threshold(magnitude, SOBEL_THRESHOLD, 255, cv2.THRESH_BINARY)
    return edges


def _detect_lines(edges: np.ndarray, image_size: tuple) -> np.ndarray | None:
    h, w       = image_size
    min_length = int(min(h, w) * HOUGH_MIN_LINE_RATIO)
    return cv2.HoughLinesP(
        edges,
        rho=1,
        theta=np.pi / 180,
        threshold=HOUGH_VOTE_THRESHOLD,
        minLineLength=min_length,
        maxLineGap=HOUGH_MAX_LINE_GAP
    )


def _classify_lines(lines: np.ndarray) -> tuple[list, list]:
    horizontal, vertical = [], []
    for line in lines:
        x1, y1, x2, y2 = line[0]
        angle = abs(np.degrees(np.arctan2(y2 - y1, x2 - x1)))
        if angle < 20:
            horizontal.append((x1, y1, x2, y2))
        elif angle > 70:
            vertical.append((x1, y1, x2, y2))
    return horizontal, vertical

#agrupar_posiciones
def _cluster_lines(lines: list, axis: str, image_size: int) -> list[float]:
    """
        Agrupa líneas cercanas (detecciones duplicadas de la misma línea real)
        y devuelve la posición media de cada grupo.
        """
    if not lines:
        return []
    positions = []
    for x1, y1, x2, y2 in lines:
        pos = (y1 + y2) / 2 if axis == "y" else (x1 + x2) / 2
        positions.append(pos)
    positions.sort()

    gap_threshold = image_size / CLUSTER_DIVISOR
    groups: list[list[float]] = []
    current_group = [positions[0]]

    for pos in positions[1:]:
        if pos - current_group[-1] < gap_threshold:
            current_group.append(pos)
        else:
            groups.append(current_group)
            current_group = [pos]
    groups.append(current_group)

    return [float(np.mean(g)) for g in groups]


def _crop_squares(
    image: np.ndarray,
    h_positions: list[float],
    v_positions: list[float]
) -> list[SquareResult]:
    results = []
    for row in range(BOARD_SIZE):
        for col in range(BOARD_SIZE):
            y1 = int(h_positions[row])
            y2 = int(h_positions[row + 1])
            x1 = int(v_positions[col])
            x2 = int(v_positions[col + 1])

            cell_img = image[y1:y2, x1:x2]
            square_name = chr(ord('a') + col) + str(BOARD_SIZE - row)
            _, encoded = cv2.imencode('.png', cell_img)

            results.append(SquareResult(
                square=square_name,
                row=row,
                col=col,
                image_bytes=encoded.tobytes()
            ))
    return results


def _segment_with_hough(bordered: np.ndarray) -> list[SquareResult] | None:
    h, w = bordered.shape[:2]
    edges = _detect_edges(bordered)
    lines = _detect_lines(edges, (h, w))

    if lines is None:
        return None

    h_lines, v_lines = _classify_lines(lines)
    h_positions = sorted(_cluster_lines(h_lines, axis="y", image_size=h))
    v_positions = sorted(_cluster_lines(v_lines, axis="x", image_size=w))

    h_positions = _regularize_lines(h_positions, image_size=h)
    v_positions = _regularize_lines(v_positions, image_size=w)

    if len(h_positions) != GRID_LINES or len(v_positions) != GRID_LINES:
        return None

    # Forzar que la primera línea sea 0 y la última sea el borde de la imagen
    h_step = h_positions[-1] - h_positions[-2]
    v_step = v_positions[-1] - v_positions[-2]
    h_positions = [h_positions[0] + i * h_step for i in range(GRID_LINES)]
    v_positions = [v_positions[0] + i * v_step for i in range(GRID_LINES)]

    # Anclar al borde real de la imagen
    h_positions[0]  = 0.0
    h_positions[-1] = float(h)
    v_positions[0]  = 0.0
    v_positions[-1] = float(w)

    _save_debug_image(bordered, h_positions, v_positions)
    return _crop_squares(bordered, h_positions, v_positions)


def _force_border_lines(positions: list[float], image_size: int) -> list[float]:
    """
    Si hay 8 líneas en vez de 9, encuentra el gap más grande
    y añade una línea interpolada en su punto medio.
    """
    if len(positions) != GRID_LINES - 1:
        return positions

    # Encuentra el gap más grande entre líneas consecutivas
    gaps = [positions[i+1] - positions[i] for i in range(len(positions)-1)]
    max_gap_idx = int(np.argmax(gaps))

    # Interpola una línea en el centro del gap más grande
    interpolated = (positions[max_gap_idx] + positions[max_gap_idx + 1]) / 2
    new_positions = positions[:max_gap_idx + 1] + [interpolated] + positions[max_gap_idx + 1:]

    return new_positions

def _regularize_lines(positions: list[float], image_size: int) -> list[float]:
    """
    Genera una cuadrícula uniforme centrada en la imagen,
    usando la mediana del paso detectado como tamaño de casilla.
    """
    if len(positions) < 2:
        return positions

    gaps = [positions[i+1] - positions[i] for i in range(len(positions)-1)]
    median_gap = float(np.median(gaps))

    # Centro real de la cuadrícula detectada
    detected_center = (positions[0] + positions[-1]) / 2
    # Centro de la cuadrícula completa de 9 líneas
    grid_span = median_gap * (GRID_LINES - 1)
    start = detected_center - grid_span / 2

    print(f"[REG] detected_center: {detected_center:.1f}")
    print(f"[REG] grid_span: {grid_span:.1f}")
    print(f"[REG] start: {start:.1f}")
    print(f"[REG] end: {start + grid_span:.1f}")
    print(f"[REG] image_size: {image_size}")

    return [start + i * median_gap for i in range(GRID_LINES)]

def _segment_fallback(image: np.ndarray) -> list[SquareResult]:
    h, w = image.shape[:2]
    h_positions = [row * (h / BOARD_SIZE) for row in range(GRID_LINES)]
    v_positions = [col * (w / BOARD_SIZE) for col in range(GRID_LINES)]
    _save_debug_image(image, h_positions, v_positions)
    return _crop_squares(image, h_positions, v_positions)

def _crop_white_margin(image: np.ndarray) -> np.ndarray:
    """
    Elimina márgenes exteriores (blancos o negros) para quedarse
    solo con el área jugable del tablero.
    """
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Detecta contenido que no sea blanco puro ni negro puro
    # Las casillas del tablero están entre 30 y 200 de intensidad
    binary = cv2.inRange(gray, 30, 200)

    coords = cv2.findNonZero(binary)
    if coords is None:
        return image

    x, y, w, h = cv2.boundingRect(coords)
    padding = 2
    x = max(0, x - padding)
    y = max(0, y - padding)
    w = min(image.shape[1] - x, w + padding * 2)
    h = min(image.shape[0] - y, h + padding * 2)
    return image[y:y+h, x:x+w]

def _save_debug_image(
    image: np.ndarray,
    h_positions: list[float],
    v_positions: list[float]
) -> None:
    debug = image.copy()
    bh, bw = debug.shape[:2]
    for y in h_positions:
        cv2.line(debug, (0, int(y)), (bw, int(y)), (0, 0, 255), 2)
    for x in v_positions:
        cv2.line(debug, (int(x), 0), (int(x), bh), (0, 0, 255), 2)
    for y in h_positions:
        for x in v_positions:
            cv2.circle(debug, (int(x), int(y)), 4, (255, 100, 0), -1)
    cv2.imwrite("debug_lines.png", debug)