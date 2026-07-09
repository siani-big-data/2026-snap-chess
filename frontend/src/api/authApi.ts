import { apiClient } from './httpClient.ts'
import type { User, AuthToken } from '../types/chess.types.ts'

export const registerUser = async (username: string, password: string): Promise<User> => {
    const { data } = await apiClient.post('/auth/register', { username, password })
    return data
}

export const loginUser = async (username: string, password: string): Promise<AuthToken> => {
    const { data } = await apiClient.post('/auth/login', { username, password })
    return data
}