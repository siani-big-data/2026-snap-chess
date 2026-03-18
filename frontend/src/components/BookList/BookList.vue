<template>
  <div class="book-list">
    <p class="section-label">MI BIBLIOTECA</p>
    <button class="btn-add" @click="showModal = true">
      + Importar libro
    </button>
    <ul>
      <li
          v-for="book in books"
          :key="book.id"
          @click="selectBook(book.id)"
          :class="{ active: selectedId === book.id }"
          class="book-item"
      >
        <span class="book-icon">📄</span>
        <div class="book-info" @click="selectBook(book.id)">
          <span class="book-title">{{ book.title }}</span>
          <span class="book-pages">{{ book.totalPages }} páginas</span>
        </div>
        <button class="btn-delete" @click.stop="handleDelete(book.id)">🗑</button>
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
import { ref, onMounted } from 'vue'
import type { Book } from '../../types/chess.types.ts'
import {deleteBook, getBooks} from '../../api/bookApi.ts'
import ImportBookModal from "./ImportBookModal.vue";

const books = ref<Book[]>([])
const selectedId = ref<string | null>(null)
const showModal = ref(false)

const emit = defineEmits<{
  bookSelected: [bookId: string]
}>()

const reloadBooks = async () => {
  books.value = await getBooks()
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

.section-label {
  padding: 12px 16px 6px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #666;
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
  font-size: 18px;
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

.book-item:hover .btn-delete { opacity: 1; }
</style>