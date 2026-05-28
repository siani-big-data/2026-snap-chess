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
    boards: ChessBoard[]
}
export interface Book {
    id: string
    title: string
    originalFilename: string
    totalPages: number
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