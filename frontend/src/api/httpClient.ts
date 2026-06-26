import axios from 'axios'
import { getToken, handleSessionExpired } from '../auth/authState'

export const apiClient = axios.create({
    baseURL: '/api',
    headers: { 'Content-Type': 'application/json' }
})

apiClient.interceptors.request.use((config) => {
    const token = getToken()
    if (token) {
        config.headers.set('Authorization', `Bearer ${token}`)
    }
    return config
})

apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        const wasAuthenticatedRequest = Boolean(error.config?.headers?.get?.('Authorization'))
        if (error.response?.status === 401 && wasAuthenticatedRequest) {
            handleSessionExpired()
        }
        return Promise.reject(error)
    }
)