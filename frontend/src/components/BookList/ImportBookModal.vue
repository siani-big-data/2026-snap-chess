<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal">

      <div class="modal-header">
        <h3>Importar libro</h3>
        <button class="close-btn" @click="emit('close')">✕</button>
      </div>

      <div class="modal-body">

        <div class="form-group">
          <label>Título del libro</label>
          <input
              v-model="title"
              type="text"
              placeholder="Ej: My System - Aron Nimzowitsch "
          />
        </div>

        <div class="form-group">
          <label>Archivo PDF</label>
          <input
              type="file"
              accept=".pdf"
              @change="onFileChange"
          />
        </div>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      </div>

      <div class="modal-footer">
        <button class="btn-cancel" @click="emit('close')">Cancelar</button>
        <button
            class="btn-import"
            @click="handleImport"
            :disabled="!title || !selectedFile || isLoading"
        >
          {{ isLoading ? 'Importando...' : 'Importar' }}
        </button>
      </div>

    </div>
  </div>
</template>
<script setup lang="ts">

import {ref} from "vue";
import {importBook} from "../../api/bookApi.ts";

const title = ref('')
const selectedFile = ref<File | null>(null)
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'bookImported'): void;
}>()

const onFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
}

const handleImport = async () => {
  if (!selectedFile.value || !title.value) return
  isLoading.value = true
  errorMessage.value = null
  try {
    await importBook(selectedFile.value, title.value)
    emit('bookImported')
    emit('close')
  } catch (e) {
    errorMessage.value = 'Error al importar el libro. Inténtalo de nuevo.'
  } finally {
    isLoading.value = false
  }
}
</script>
<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 10px;
  width: 420px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.3);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #1a1f2e;
  color: white;
}

.modal-header h3 {
  font-size: 15px;
  font-weight: 600;
}

.close-btn {
  background: none;
  color: #aaa;
  font-size: 16px;
  cursor: pointer;
}

.close-btn:hover { color: white; }

.modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 12px;
  font-weight: 600;
  color: #555;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.form-group input[type="text"] {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  color: #2c3e50;
}

.form-group input[type="text"]:focus {
  outline: none;
  border-color: #2d5a9e;
}

.error {
  color: #e74c3c;
  font-size: 13px;
}

.modal-footer {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid #eee;
}

.btn-cancel {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  color: #555;
  background: #f0f0f0;
  cursor: pointer;
}

.btn-import {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  color: white;
  background: #2d5a9e;
  cursor: pointer;
}

.btn-import:disabled {
  background: #aaa;
  cursor: not-allowed;
}
</style>