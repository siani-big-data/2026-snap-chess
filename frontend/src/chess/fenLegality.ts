export interface FenValidationResult {
    valid: boolean
    errors: string[]
}

const MAX_PIECES_PER_SIDE = 16
const MAX_PAWNS_PER_SIDE = 8

/**
 * Valida la legalidad ajedrecística de un FEN, más allá de su corrección estructural
 * (que ya garantiza chess.js al construir el tablero). Replica exactamente las mismas
 * reglas que FenLegalityValidator.java en el backend — ver esa clase para la versión
 * autoritativa. Deliberadamente NO valida el turno real (ver decisión de diseño: el
 * campo de turno generado por el pipeline de detección es siempre 'w').
 */
export function validateFenLegality(fen: string): FenValidationResult {
    const placement = fen.split(' ')[0]
    const rows = placement.split('/')

    if (rows.length !== 8) {
        return { valid: false, errors: ['El FEN no tiene 8 filas válidas'] }
    }

    const board = parseBoard(rows)
    const errors: string[] = [
        ...checkKingCounts(board),
        ...checkPieceCounts(board),
        ...checkPawnsOnBackRanks(rows),
        ...checkKingsNotAdjacent(board),
        ...checkNoDoubleCheck(board)
    ]

    return errors.length === 0 ? { valid: true, errors: [] } : { valid: false, errors }
}

function parseBoard(rows: string[]): string[][] {
    return rows.map(row => {
        const cells: string[] = []
        for (const c of row) {
            if (/\d/.test(c)) {
                for (let i = 0; i < Number(c); i++) cells.push('.')
            } else {
                cells.push(c)
            }
        }
        return cells
    })
}

function countOccurrences(board: string[][], piece: string): number {
    return board.flat().filter(c => c === piece).length
}

function countSide(board: string[][], white: boolean): number {
    return board.flat().filter(c => {
        if (c === '.') return false
        const isWhitePiece = c === c.toUpperCase()
        return isWhitePiece === white
    }).length
}

function findPiece(board: string[][], piece: string): [number, number] | null {
    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            if (board[r][c] === piece) return [r, c]
        }
    }
    return null
}

function checkKingCounts(board: string[][]): string[] {
    const errors: string[] = []
    const whiteKings = countOccurrences(board, 'K')
    const blackKings = countOccurrences(board, 'k')
    if (whiteKings !== 1) errors.push(`Se esperaba exactamente 1 rey blanco, se detectaron ${whiteKings}`)
    if (blackKings !== 1) errors.push(`Se esperaba exactamente 1 rey negro, se detectaron ${blackKings}`)
    return errors
}

function checkPieceCounts(board: string[][]): string[] {
    const errors: string[] = []
    for (const white of [true, false]) {
        const total = countSide(board, white)
        const pawns = countOccurrences(board, white ? 'P' : 'p')
        const color = white ? 'blancas' : 'negras'
        if (total > MAX_PIECES_PER_SIDE) {
            errors.push(`Las ${color} tienen ${total} piezas, el máximo legal es ${MAX_PIECES_PER_SIDE}`)
        }
        if (pawns > MAX_PAWNS_PER_SIDE) {
            errors.push(`Las ${color} tienen ${pawns} peones, el máximo legal es ${MAX_PAWNS_PER_SIDE}`)
        }
    }
    return errors
}

function checkPawnsOnBackRanks(rows: string[]): string[] {
    const firstRow = rows[0].toLowerCase()
    const lastRow = rows[7].toLowerCase()
    if (firstRow.includes('p') || lastRow.includes('p')) {
        return ['Hay peones detectados en la primera o última fila, lo cual es imposible']
    }
    return []
}

function checkKingsNotAdjacent(board: string[][]): string[] {
    const whiteKing = findPiece(board, 'K')
    const blackKing = findPiece(board, 'k')
    if (!whiteKing || !blackKing) return []
    const rowDiff = Math.abs(whiteKing[0] - blackKing[0])
    const colDiff = Math.abs(whiteKing[1] - blackKing[1])
    if (rowDiff <= 1 && colDiff <= 1) {
        return ['Los dos reyes están en casillas adyacentes, posición imposible']
    }
    return []
}

function checkNoDoubleCheck(board: string[][]): string[] {
    const whiteKing = findPiece(board, 'K')
    const blackKing = findPiece(board, 'k')
    if (!whiteKing || !blackKing) return []
    const whiteInCheck = isAttacked(board, whiteKing, false)
    const blackInCheck = isAttacked(board, blackKing, true)
    if (whiteInCheck && blackInCheck) {
        return ['Ambos reyes están en jaque simultáneamente, posición imposible']
    }
    return []
}

function inBounds([r, c]: [number, number]): boolean {
    return r >= 0 && r < 8 && c >= 0 && c < 8
}

function isAttacked(board: string[][], [tr, tc]: [number, number], byWhite: boolean): boolean {
    const pawn = byWhite ? 'P' : 'p'
    const pawnDir = byWhite ? 1 : -1
    for (const [r, c] of [[tr + pawnDir, tc - 1], [tr + pawnDir, tc + 1]] as [number, number][]) {
        if (inBounds([r, c]) && board[r][c] === pawn) return true
    }

    const knight = byWhite ? 'N' : 'n'
    const knightDeltas: [number, number][] = [[-2,-1],[-2,1],[-1,-2],[-1,2],[1,-2],[1,2],[2,-1],[2,1]]
    for (const [dr, dc] of knightDeltas) {
        const sq: [number, number] = [tr + dr, tc + dc]
        if (inBounds(sq) && board[sq[0]][sq[1]] === knight) return true
    }

    const king = byWhite ? 'K' : 'k'
    for (let dr = -1; dr <= 1; dr++) {
        for (let dc = -1; dc <= 1; dc++) {
            if (dr === 0 && dc === 0) continue
            const sq: [number, number] = [tr + dr, tc + dc]
            if (inBounds(sq) && board[sq[0]][sq[1]] === king) return true
        }
    }

    const rook = byWhite ? 'R' : 'r'
    const bishop = byWhite ? 'B' : 'b'
    const queen = byWhite ? 'Q' : 'q'

    const straightDirs: [number, number][] = [[-1,0],[1,0],[0,-1],[0,1]]
    for (const [dr, dc] of straightDirs) {
        if (slidingAttackHits(board, tr, tc, dr, dc, rook, queen)) return true
    }
    const diagonalDirs: [number, number][] = [[-1,-1],[-1,1],[1,-1],[1,1]]
    for (const [dr, dc] of diagonalDirs) {
        if (slidingAttackHits(board, tr, tc, dr, dc, bishop, queen)) return true
    }

    return false
}

function slidingAttackHits(
    board: string[][], r: number, c: number, dr: number, dc: number, piece1: string, piece2: string
): boolean {
    let row = r + dr, col = c + dc
    while (inBounds([row, col])) {
        const cell = board[row][col]
        if (cell !== '.') return cell === piece1 || cell === piece2
        row += dr
        col += dc
    }
    return false
}

/**
 * Sanea un FEN ilegal para que pueda cargarse en chessground sin reventar.
 * chessground exige EXACTAMENTE 1 rey de cada color para poder construir el
 * tablero (rechaza tanto el exceso como la ausencia). No rechaza, en cambio,
 * otras violaciones de legalidad (peones en fila 1/8, reyes adyacentes, jaque
 * doble, exceso de piezas), que sí puede representar visualmente sin problema.
 *
 * Estrategia:
 * - Si hay más de 1 rey de un color: se conserva el primero (orden de lectura)
 *   y se vacían los sobrantes.
 * - Si falta el rey de un color: se coloca uno PROVISIONAL en la primera
 *   casilla vacía encontrada, para que el usuario lo reubique con el popover
 *   de corrección a la casilla real.
 * No se toca ninguna otra pieza.
 */
export function sanitizeFenForLoading(fen: string): string {
    const fields = fen.split(' ')
    const board = parseBoardMutable(fields[0])

    ensureExactlyOneKing(board, 'K')
    ensureExactlyOneKing(board, 'k')

    fields[0] = board.map(rowToFenSegment).join('/')
    return fields.join(' ')
}

function parseBoardMutable(placement: string): string[][] {
    return placement.split('/').map(row => {
        const cells: string[] = []
        for (const c of row) {
            if (/\d/.test(c)) {
                for (let i = 0; i < Number(c); i++) cells.push('.')
            } else {
                cells.push(c)
            }
        }
        return cells
    })
}

function ensureExactlyOneKing(board: string[][], kingChar: string): void {
    const positions = findAllPieces(board, kingChar)

    if (positions.length > 1) {
        for (const [r, c] of positions.slice(1)) {
            board[r][c] = '.'
        }
    } else if (positions.length === 0) {
        const empty = findFirstEmptySquare(board)
        if (empty) {
            const [r, c] = empty
            board[r][c] = kingChar
        }
        // Si no hay ninguna casilla vacía (tablero completamente lleno, caso
        // extremo), no se puede insertar; el try/catch en el componente capturará
        // el fallo de carga si esto llegara a ocurrir.
    }
}

function findAllPieces(board: string[][], piece: string): [number, number][] {
    const found: [number, number][] = []
    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            if (board[r][c] === piece) found.push([r, c])
        }
    }
    return found
}

function findFirstEmptySquare(board: string[][]): [number, number] | null {
    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            if (board[r][c] === '.') return [r, c]
        }
    }
    return null
}

function rowToFenSegment(cells: string[]): string {
    let segment = ''
    let emptyCount = 0
    for (const cell of cells) {
        if (cell === '.') {
            emptyCount++
        } else {
            if (emptyCount > 0) {
                segment += emptyCount
                emptyCount = 0
            }
            segment += cell
        }
    }
    if (emptyCount > 0) segment += emptyCount
    return segment
}
