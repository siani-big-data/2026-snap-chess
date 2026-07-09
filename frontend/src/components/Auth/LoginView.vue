<template>
  <div class="auth-screen">
    <form class="auth-card" @submit.prevent="handleSubmit">
      <div class="auth-header">
        <span class="app-logo"><font-awesome-icon icon="chess-knight" /></span>
        <h1>ChessPDF</h1>
      </div>

      <label class="field-label" for="login-username">Usuario</label>
      <input
          id="login-username"
          v-model="username"
          type="text"
          autocomplete="username"
          :disabled="isSubmitting"
      />

      <label class="field-label" for="login-password">Contraseña</label>
      <input
          id="login-password"
          v-model="password"
          type="password"
          autocomplete="current-password"
          :disabled="isSubmitting"
      />
      <p v-if="infoMessage" class="info-message">{{ infoMessage }}</p>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <button type="submit" class="btn-primary" :disabled="isSubmitting">
        {{ isSubmitting ? 'Entrando…' : 'Iniciar sesión' }}
      </button>

      <button type="button" class="btn-link" @click="emit('switchToRegister')">
        ¿No tienes cuenta? Regístrate
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { loginUser } from '../../api/authApi.ts'
import { setSession, sessionExpiredNotice, clearSessionExpiredNotice } from '../../auth/authState.ts'

const emit = defineEmits<{
  (e: 'loginSuccess'): void
  (e: 'switchToRegister'): void
}>()

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)
const infoMessage = ref('')


const handleSubmit = async () => {
  errorMessage.value = ''
  const trimmedUsername = username.value.trim()

  if (!trimmedUsername || !password.value) {
    errorMessage.value = 'Introduce usuario y contraseña.'
    return
  }

  isSubmitting.value = true
  try {
    const { token } = await loginUser(trimmedUsername, password.value)
    setSession(token, trimmedUsername)
    emit('loginSuccess')
  } catch (err: any) {
    errorMessage.value = err.response?.status === 401
        ? 'Usuario o contraseña incorrectos.'
        : 'No se pudo iniciar sesión. Inténtalo de nuevo.'
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  if (sessionExpiredNotice.value) {
    infoMessage.value = sessionExpiredNotice.value
    clearSessionExpiredNotice()
  }
})
</script>

<style scoped>
.auth-screen {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #0d1117;
}

.auth-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 320px;
  padding: 32px;
  background: #1a1f2e;
  border: 1px solid #2d3447;
  border-radius: 8px;
}

.auth-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: #ffffff;
}

.auth-header h1 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.app-logo {
  font-size: 22px;
  color: #4a90d9;
}

.field-label {
  font-size: 13px;
  color: #888;
  margin-top: 12px;
}

input {
  background: #0f1420;
  border: 1px solid #2d3447;
  border-radius: 4px;
  color: #e0e0e0;
  padding: 8px 10px;
  font-size: 14px;
}

input:focus {
  outline: none;
  border-color: #4a90d9;
}

.error-message {
  color: #e57373;
  font-size: 13px;
  margin: 10px 0 0;
}
.info-message {
  color: #4a90d9;
  font-size: 13px;
  margin: 10px 0 0;
}
.btn-primary {
  margin-top: 20px;
  background: #2d5a9e;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  padding: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-primary:hover:not(:disabled) {
  background: #4a90d9;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-link {
  margin-top: 12px;
  background: none;
  border: none;
  color: #888;
  font-size: 13px;
  cursor: pointer;
}

.btn-link:hover {
  color: #4a90d9;
}
</style>