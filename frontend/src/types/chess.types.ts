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
    analysis: unknown
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