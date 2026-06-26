import { ref, computed } from 'vue'

const TOKEN_KEY = 'chessreader.token'
const USERNAME_KEY = 'chessreader.username'

const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
const username = ref<string | null>(localStorage.getItem(USERNAME_KEY))
const sessionExpiredMessage = ref<string | null>(null)

export const isAuthenticated = computed(() => token.value !== null)
export const currentUsername = computed(() => username.value)
export const sessionExpiredNotice = computed(() => sessionExpiredMessage.value)

export const getToken = (): string | null => token.value

export const setSession = (newToken: string, newUsername: string): void => {
    token.value = newToken
    username.value = newUsername
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USERNAME_KEY, newUsername)
}

export const clearSession = (): void => {
    token.value = null
    username.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
}

export const handleSessionExpired = (): void => {
    clearSession()
    sessionExpiredMessage.value = 'Tu sesión ha caducado. Inicia sesión de nuevo.'
}

export const clearSessionExpiredNotice = (): void => {
    sessionExpiredMessage.value = null
}