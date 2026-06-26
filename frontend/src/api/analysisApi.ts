import type { AnalysisNode, AddMoveRequest } from '../types/chess.types'
import { authFetch } from './authFetch.ts'

const BASE_URL = 'http://localhost:8080/api'

export async function addMove(
    bookId: string,
    boardId: string,
    request: AddMoveRequest
): Promise<AnalysisNode> {
    const response = await authFetch(
        `${BASE_URL}/books/${bookId}/boards/${boardId}/analysis/moves`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        }
    )
    if (!response.ok) throw new Error(`Error añadiendo jugada: ${response.status}`)
    return response.json()
}

export async function updateComment(
    bookId: string,
    boardId: string,
    path: string[],
    comment: string
): Promise<AnalysisNode> {
    const response = await authFetch(
        `${BASE_URL}/books/${bookId}/boards/${boardId}/analysis/comment`,
        {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path, comment })
        }
    )
    if (!response.ok) throw new Error(`Error actualizando comentario: ${response.status}`)
    return response.json()
}

export async function analyzePosition(
    bookId: string,
    boardId: string,
    moveTimeMs: number = 1000,
    signal?: AbortSignal
): Promise<{ evalCp: number; formattedEval: string; bestMove: string }> {
    const response = await authFetch(
        `${BASE_URL}/books/${bookId}/boards/${boardId}/engine/analyze?moveTimeMs=${moveTimeMs}`,
        { method: 'POST', signal }
    )
    if (!response.ok) throw new Error(`Error analizando posición: ${response.status}`)
    return response.json()
}

export async function analyzeFen(
    fen: string,
    moveTimeMs: number = 1000,
    signal?: AbortSignal
): Promise<{ evalCp: number; formattedEval: string; bestMove: string }> {
    const response = await authFetch(
        `${BASE_URL}/engine/analyze-fen?moveTimeMs=${moveTimeMs}`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fen }),
            signal
        }
    )
    if (!response.ok) throw new Error(`Error analizando FEN: ${response.status}`)
    return response.json()
}