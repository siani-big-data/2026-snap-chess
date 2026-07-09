import { getToken, handleSessionExpired } from '../auth/authState'

export async function authFetch(url: string, options: RequestInit = {}): Promise<Response> {
    const token = getToken()
    const headers = new Headers(options.headers)
    let wasAuthenticatedRequest = false

    if (token) {
        headers.set('Authorization', `Bearer ${token}`)
        wasAuthenticatedRequest = true
    }

    const response = await fetch(url, { ...options, headers })

    if (response.status === 401 && wasAuthenticatedRequest) {
        handleSessionExpired()
    }

    return response
}