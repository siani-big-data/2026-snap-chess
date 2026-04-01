<template>
  <div class="app-layout" :style="layoutStyle">

    <aside class="sidebar-left">
      <div class="sidebar-header">
        <span class="app-logo"><font-awesome-icon icon="chess-knight" /></span>
        <span class="app-title">ChessPDF</span>
      </div>
      <BookList @bookSelected="onBookSelected" />
    </aside>

    <main class="main-content">
      <PdfViewer
          v-if="selectedBookId"
          :bookId="selectedBookId"
          :key="selectedBookId"
          @boardSelected="onSelectedBoard"
      />
      <div v-else class="empty-state">
        <p><font-awesome-icon icon="chess-knight" /> Selecciona un libro para empezar</p>
      </div>
    </main>

    <div class="resizer" @mousedown="startResize" />

    <aside class="sidebar-right">
      <div class="sidebar-header">
        <span>Análisis</span>
      </div>
      <ChessBoardPanel :board="selectedBoard" />
    </aside>

  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import BookList from './components/BookList/BookList.vue'
import PdfViewer from './components/PdfViewer/PdfViewer.vue'
import ChessBoardPanel from './components/ChessBoard/ChessBoardPanel.vue'
import type { ChessBoard } from './types/chess.types.ts'

const selectedBookId  = ref<string | null>(null)
const selectedBoard   = ref<ChessBoard | null>(null)

const MIN_SIDEBAR_WIDTH = 260
const MAX_SIDEBAR_WIDTH = 750
const sidebarWidth = ref(300)

const layoutStyle = computed(() => ({
  gridTemplateColumns: `260px 1fr 4px ${sidebarWidth.value}px`
}))

const onBookSelected = (bookId: string) => {
  selectedBookId.value = bookId
  selectedBoard.value = null
  sidebarWidth.value = 300
}

const onSelectedBoard = (board: ChessBoard): void => {
  selectedBoard.value = board
  sidebarWidth.value = 500
}

const startResize = (event: MouseEvent) => {
  event.preventDefault()

  const onMouseMove = (e: MouseEvent) => {
    const newWidth = window.innerWidth - e.clientX
    if (newWidth >= MIN_SIDEBAR_WIDTH && newWidth <= MAX_SIDEBAR_WIDTH) {
      sidebarWidth.value = newWidth
    }
  }

  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}
</script>

<style scoped>
.app-layout {
  display: grid;
  height: 100vh;
  overflow: hidden;
}

.sidebar-left, .sidebar-right {
  background: #1a1f2e;
  color: #e0e0e0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #2d3447;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ffffff;
}

.app-logo {
  font-size: 22px;
}

.main-content {
  background: #f0f2f5;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
}

.empty-state {
  margin: auto;
  color: #888;
  font-size: 16px;
}

.resizer {
  background: #2d3447;
  cursor: col-resize;
  transition: background 0.15s ease;
}

.resizer:hover {
  background: #4a90d9;
}
</style>