<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal">

      <div class="modal-header">
        <h3>Añadir libro</h3>
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

        <div
            class="drop-zone"
            :class="{ 'drop-zone--active': isDragging }"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="onDrop"
            @click="triggerFileInput"
        >
          <input
              ref="fileInputRef"
              type="file"
              accept=".pdf"
              style="display: none"
              @change="onFileChange"
          />
          <div class="drop-zone__icon"><font-awesome-icon icon="cloud-arrow-up" /></div>
          <p class="drop-zone__text">
            {{ selectedFile ? selectedFile.name : 'Arrastra tu PDF aquí' }}
          </p>
          <p class="drop-zone__subtext" v-if="!selectedFile">
            o haz click para seleccionar
          </p>
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
          {{ isLoading ? 'Añadiendo...' : 'Añadir' }}
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
const fileInputRef = ref<HTMLInputElement | null>(null)
const isDragging = ref(false)

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'bookImported'): void;
}>()

const triggerFileInput = () => {
  fileInputRef.value?.click()
}
const onDrop = (event: DragEvent) => {
  isDragging.value = false
  const file = event.dataTransfer?.files[0]
  if (file && file.type === 'application/pdf') {
    selectedFile.value = file
    if (!title.value) {
      title.value = file.name.replace('.pdf', '')
    }
  }
}

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
.drop-zone {
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 32px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.drop-zone:hover {
  border-color: #2d5a9e;
  background: #f0f4ff;
}

.drop-zone--active {
  border-color: #2d5a9e;
  background: #e8f0ff;
  transform: scale(1.01);
}

.drop-zone__icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.drop-zone__text {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 4px;
  word-break: break-all;
}

.drop-zone__subtext {
  font-size: 12px;
  color: #888;
}
</style>