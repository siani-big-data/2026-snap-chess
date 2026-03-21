<template>
  <div class="book-list">
    <button class="btn-add" @click="showModal = true">
      + Añadir libro
    </button>
    <ul>
      <li
          v-for="book in books"
          :key="book.id"
          @click="selectBook(book.id)"
          :class="{ active: selectedId === book.id }"
          class="book-item"
      >
        <span class="book-icon"><font-awesome-icon icon="chess-pawn" /></span>
        <div class="book-info" @click="selectBook(book.id)">
          <input
              :ref="el => { if (el) titleInputRef = el as HTMLInputElement }"
              v-if="editingBookId === book.id"
              v-model="editingTitle"
              class="title-input"
              @keydown.enter="saveRename(book.id)"
              @keydown.esc="cancelRename"
              @blur="saveRename(book.id)"
              @click.stop
          />
          <span v-else class="book-title">{{ book.title }}</span>
          <span class="book-pages">{{ book.totalPages }} páginas</span>
        </div>
        <div class="book-actions">
          <button class="btn-action" @click.stop="startRename(book)" title="Renombrar"><font-awesome-icon icon="pen" /></button>
          <button class="btn-action" @click.stop="handleDelete(book.id)" title="Eliminar"><font-awesome-icon icon="trash" /></button>
        </div>
      </li>
    </ul>
  </div>
  <ImportBookModal
      v-if="showModal"
      @close="showModal = false"
      @bookImported="reloadBooks"
  />
</template>

<script setup lang="ts">
import {ref, onMounted, nextTick} from 'vue'
import type { Book } from '../../types/chess.types.ts'
import {deleteBook, getBooks, renameBook} from '../../api/bookApi.ts'
import ImportBookModal from "./ImportBookModal.vue";

const books = ref<Book[]>([])
const selectedId = ref<string | null>(null)
const showModal = ref(false)
const editingBookId = ref<string | null>(null)
const editingTitle = ref('')
let titleInputRef = ref<HTMLInputElement | null>(null)

const emit = defineEmits<{
  bookSelected: [bookId: string]
}>()



const reloadBooks = async () => {
  books.value = await getBooks()
}

const startRename = async (book: Book) => {
  editingBookId.value = book.id
  editingTitle.value = book.title
  await nextTick()
  titleInputRef.value?.focus()
}

const cancelRename = () => {
  editingBookId.value = null
  editingTitle.value = ''
}

const saveRename = async (bookId: string) => {
  const trimmed = editingTitle.value.trim()

  const original = books.value.find(b => b.id === bookId)?.title
  if (!trimmed || trimmed === original) {
    cancelRename()
    return
  }

  await renameBook(bookId, trimmed)
  await reloadBooks()
  cancelRename()
}

const selectBook = (id: string) => {
  selectedId.value = id
  emit('bookSelected', id)
}

const handleDelete = async (bookId: string) => {
  if (!confirm('¿Eliminar este libro y todos sus datos?')) return
  await deleteBook(bookId)
  await reloadBooks()
}

onMounted(async () => {
  books.value = await getBooks()
})
</script>

<style scoped>
.book-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}
.book-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  border-radius: 6px;
  margin: 2px 8px;
  transition: background 0.15s;
  list-style: none;
}

.book-item:hover {
  background: #2d3447;
}

.book-item.active {
  background: #2d5a9e;
}

.book-icon {
  font-size: 13px;
  flex-shrink: 0;
}

.book-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.book-title {
  font-size: 13px;
  color: #e0e0e0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-pages {
  font-size: 11px;
  color: #888;
  margin-top: 2px;
}

.btn-add {
  width: calc(100% - 16px);
  margin: 8px;
  padding: 8px;
  background: #2d5a9e;
  color: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  text-align: center;
}

.btn-add:hover { background: #3a6bbf; }

.btn-delete {
  margin-left: auto;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.book-actions {
  display: flex;
  gap: 4px;
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.book-item:hover .book-actions { opacity: 1; }

.btn-action {
  padding: 4px 6px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.title-input {
  background: #2d3447;
  border: 1px solid #2d5a9e;
  border-radius: 4px;
  color: white;
  font-size: 13px;
  padding: 2px 6px;
  width: 100%;
  outline: none;
}

.book-item:hover .btn-delete { opacity: 1; }
</style>