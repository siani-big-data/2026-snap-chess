export type BookCategory = 'APERTURAS' | 'FINALES' | 'TACTICA' | 'ESTRATEGIA' | 'GENERAL'

export const CATEGORY_LABELS: Record<BookCategory, string> = {
    APERTURAS: 'Aperturas',
    FINALES: 'Finales',
    TACTICA: 'Táctica',
    ESTRATEGIA: 'Estrategia',
    GENERAL: 'General'
}

export interface BoundingBox {
    x: number
    y: number
    width: number
    height: number
}
export interface ChessBoard {
    id: string
    page: number
    bbox: BoundingBox
    fen: string
    analysis: AnalysisNode | null
}
export interface ChessFile {
    id: string
    title: string
    originalFilename: string
    totalPages: number
    category: BookCategory
    boards: ChessBoard[]
}
export interface Book {
    id: string
    title: string
    originalFilename: string
    totalPages: number
    category: BookCategory
}
export interface AnalysisNode {
    move: string | null
    comment: string | null
    evalCp: number | null
    children: AnalysisNode[] | null
}

export interface AddMoveRequest {
    path: string[]
    move: string
}
export interface User {
    id: string
    username: string
}
export interface AuthToken {
    token: string
}