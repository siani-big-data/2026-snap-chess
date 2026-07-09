<template>
  <Teleport to="body">
    <div class="popup" :style="popupStyle">

      <div class="popup-header">
        <span class="popup-title">{{ isEditing ? 'Editar comentario' : 'Comentario' }}</span>
        <button class="popup-close" @click="emit('close')">✕</button>
      </div>

      <div class="popup-body">
        <textarea
            v-if="isEditing"
            v-model="editText"
            class="popup-textarea"
            placeholder="Escribe un comentario para esta jugada..."
            rows="4"
            autofocus
        />
        <p v-else class="popup-text">{{ comment }}</p>
      </div>

      <div class="popup-footer">
        <template v-if="isEditing">
          <button class="btn-cancel" @click="emit('close')">Cancelar</button>
          <button class="btn-save" @click="onSave">Guardar</button>
        </template>
        <template v-else>
          <button class="btn-edit" @click="isEditing = true">Editar</button>
        </template>
      </div>

    </div>
  </Teleport>
</template>

<script setup lang="ts">

import { ref, watch, computed } from 'vue'

const popupStyle = computed(() => ({
  right: `${props.anchorRight + 12}px`,
  top: '80px'
}))
const props = defineProps<{
  comment: string
  anchorRight: number
  isEditMode: boolean
}>()

const emit = defineEmits<{
  close: []
  save: [comment: string]
}>()

const isEditing = ref(props.isEditMode)
const editText = ref(props.comment)


watch(() => props.comment, (val) => {
  editText.value = val
  isEditing.value = props.isEditMode
})


const onSave = () => {
  emit('save', editText.value.trim())
}
</script>

<style>

.popup {
  position: fixed;
  width: 280px;
  background: #1a1f2e;
  border: 1px solid #2d3447;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  z-index: 101;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.popup-title {
  font-size: 12px;
  font-weight: 600;
  color: #6b7a99;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.popup-close {
  background: none;
  border: none;
  color: #6b7a99;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  line-height: 1;
}

.popup-close:hover { color: #e0e0e0; }

.popup-body { display: flex; flex-direction: column; }

.popup-text {
  color: #c8d0e7;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
}

.popup-textarea {
  background: #12161f;
  border: 1px solid #2d3447;
  border-radius: 6px;
  color: #c8d0e7;
  font-size: 13px;
  line-height: 1.6;
  padding: 8px;
  resize: vertical;
  width: 100%;
  box-sizing: border-box;
  font-family: inherit;
}

.popup-textarea:focus {
  outline: none;
  border-color: #4a7ac8;
}

.popup-footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.btn-save, .btn-edit {
  background: #2d5a9e;
  border: none;
  border-radius: 6px;
  color: white;
  cursor: pointer;
  font-size: 12px;
  padding: 6px 14px;
}

.btn-save:hover, .btn-edit:hover { background: #3d6ab0; }

.btn-cancel {
  background: #2d3447;
  border: none;
  border-radius: 6px;
  color: #e0e0e0;
  cursor: pointer;
  font-size: 12px;
  padding: 6px 14px;
}

.btn-cancel:hover { background: #3d4560; }
</style>