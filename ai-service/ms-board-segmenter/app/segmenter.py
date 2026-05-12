import cv2
import numpy as np
from dataclasses import dataclass

GAUSSIAN_BLUR_KERNEL = (5, 5)
SOBEL_THRESHOLD      = 50
HOUGH_VOTE_THRESHOLD = 80
HOUGH_MIN_LINE_RATIO = 0.4
HOUGH_MAX_LINE_GAP   = 10
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
    bordered = _add_synthetic_border(board_image)
    result   = _segment_with_hough(bordered)

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

    if len(h_positions) != GRID_LINES or len(v_positions) != GRID_LINES:
        return None

    return _crop_squares(bordered, h_positions, v_positions)


def _segment_fallback(image: np.ndarray) -> list[SquareResult]:
    h, w = image.shape[:2]
    h_positions = [row * (h / BOARD_SIZE) for row in range(GRID_LINES)]
    v_positions = [col * (w / BOARD_SIZE) for col in range(GRID_LINES)]
    return _crop_squares(image, h_positions, v_positions)