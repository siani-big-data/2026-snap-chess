<template>
  <div class="book-list">
    <button class="btn-add" @click="showModal = true">
      + Añadir libro
    </button>

    <div class="category-filter">
      <select v-model="categoryFilter">
        <option value="ALL">Todas las categorías</option>
        <option v-for="(label, value) in CATEGORY_LABELS" :key="value" :value="value">
          {{ label }}
        </option>
      </select>
    </div>

    <ul>
      <li
          v-for="book in filteredBooks"
          :key="book.id"
          @click="selectBook(book.id)"
          :class="{ active: selectedId === book.id }"
          class="book-item"
      >
        <span class="book-icon"><font-awesome-icon icon="chess-pawn" /></span>
        <div class="book-info" @click="selectBook(book.id)">
          <div
              v-if="editingBookId === book.id"
              class="book-edit-block"
              :ref="bindEditRowRef"
              @focusout="handleEditFocusOut(book.id, $event)"
          >
            <div class="book-title-row">
              <input
                  :ref="bindTitleInputRef"
                  v-model="editingTitle"
                  class="title-input"
                  @keydown.enter="saveRename(book.id)"
                  @keydown.esc="cancelRename"
                  @click.stop
              />
            </div>
            <div class="book-meta-row">
              <span class="book-pages">{{ book.totalPages }} páginas</span>
              <select
                  v-model="editingCategory"
                  class="category-edit-select"
                  @click.stop
                  @mousedown.stop
              >
                <option v-for="(label, value) in CATEGORY_LABELS" :key="value" :value="value">
                  {{ label }}
                </option>
              </select>
            </div>
          </div>
          <template v-else>
            <div class="book-title-row">
              <span class="book-title">{{ book.title }}</span>
            </div>
            <div class="book-meta-row">
              <span class="book-pages">{{ book.totalPages }} páginas</span>
              <span class="book-category">{{ CATEGORY_LABELS[book.category] }}</span>
            </div>
          </template>
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
import {ref, onMounted, nextTick, computed} from 'vue'
import type { Book, BookCategory } from '../../types/chess.types.ts'
import { CATEGORY_LABELS } from '../../types/chess.types.ts'
import { deleteBook, getBooks, renameBook, updateBookCategory } from '../../api/bookApi.ts'
import ImportBookModal from "./ImportBookModal.vue";

const books = ref<Book[]>([])
const selectedId = ref<string | null>(null)
const showModal = ref(false)
const editingBookId = ref<string | null>(null)
const editingTitle = ref('')
const editingCategory = ref<BookCategory>('GENERAL')
let titleInputRef: HTMLInputElement | null = null
let editRowRef: HTMLDivElement | null = null

const bindEditRowRef = (el: unknown) => {
  editRowRef = el ? (el as HTMLDivElement) : null
}

const bindTitleInputRef = (el: unknown) => {
  titleInputRef = el ? (el as HTMLInputElement) : null
}

const categoryFilter = ref<'ALL' | BookCategory>('ALL')

const emit = defineEmits<{
  bookSelected: [bookId: string]
}>()

const filteredBooks = computed(() =>
    categoryFilter.value === 'ALL'
        ? books.value
        : books.value.filter(b => b.category === categoryFilter.value)
)



const reloadBooks = async () => {
  books.value = await getBooks()
}

const startRename = async (book: Book) => {
  editingBookId.value = book.id
  editingTitle.value = book.title
  editingCategory.value = book.category
  await nextTick()
  titleInputRef?.focus()
}

const cancelRename = () => {
  editingBookId.value = null
  editingTitle.value = ''
}

const handleEditFocusOut = (bookId: string, event: FocusEvent) => {
  const container = editRowRef ?? (event.currentTarget as HTMLElement)
  const relatedTarget = event.relatedTarget as Node | null
  if (relatedTarget && container.contains(relatedTarget)) {
    return
  }
  requestAnimationFrame(() => {
    const active = document.activeElement
    if (active && container.contains(active)) {
      return
    }
    saveRename(bookId)
  })
}

const saveRename = async (bookId: string) => {
  const book = books.value.find(b => b.id === bookId)
  if (!book) {
    cancelRename()
    return
  }

  const trimmed = editingTitle.value.trim()
  const titleChanged = trimmed !== book.title
  const categoryChanged = editingCategory.value !== book.category

  if (!titleChanged && !categoryChanged) {
    cancelRename()
    return
  }

  let saved = false

  if (titleChanged && trimmed) {
    await renameBook(bookId, trimmed)
    saved = true
  }

  if (categoryChanged) {
    await updateBookCategory(bookId, editingCategory.value)
    saved = true
  }

  if (saved) {
    await reloadBooks()
  }

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
  flex: 1;
  min-width: 0;
}

.book-edit-block {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
}

.book-title-row {
  display: block;
  width: 100%;
  min-width: 0;
}

.book-title {
  display: block;
  font-size: 13px;
  color: #e0e0e0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.book-category {
  flex-shrink: 0;
  font-size: 11px;
  color: #888;
  border: 1px solid #3a4156;
  border-radius: 4px;
  padding: 1px 6px;
  white-space: nowrap;
}

.book-pages {
  font-size: 11px;
  color: #888;
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

.book-meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  width: 100%;
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

.category-edit-select {
  background: #2d3447;
  border: 1px solid #2d5a9e;
  border-radius: 4px;
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  flex-shrink: 0;
  cursor: pointer;
  outline: none;
  box-sizing: border-box;
  line-height: 1.2;
}

.book-item:hover .btn-delete { opacity: 1; }

.category-filter {
  padding: 0 8px 8px;
}

.category-filter select {
  width: 100%;
  background: #2d3447;
  color: #e0e0e0;
  border: 1px solid #3a4156;
  border-radius: 4px;
  font-size: 12px;
  padding: 4px 6px;
}

</style>