<template>
  <div class="app-layout">

    <aside class="sidebar-left">
      <div class="sidebar-header">
        <span class="app-logo"><font-awesome-icon icon="chess-knight" /></span>
        <span class="app-title">ChessPDF</span>
      </div>
      <BookList @bookSelected="selectedBookId = $event" />
    </aside>

    <main class="main-content">
      <PdfViewer
          v-if="selectedBookId"
          :bookId="selectedBookId"
          :key="selectedBookId"
      />
      <div v-else class="empty-state">
        <p><font-awesome-icon icon="chess-knight" /> Selecciona un libro para empezar</p>
      </div>
    </main>

    <aside class="sidebar-right">
      <div class="sidebar-header">
        <span>Análisis</span>
      </div>
      <div class="placeholder">
        <p>Analisis de las jugadas, arbol y demás</p>
      </div>
    </aside>

  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BookList from './components/BookList/BookList.vue'
import PdfViewer from './components/PdfViewer/PdfViewer.vue'

const selectedBookId = ref<string | null>(null)
</script>

<style scoped>
.app-layout {
  display: grid;
  grid-template-columns: 260px 1fr 300px;
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

.placeholder {
  padding: 16px;
  color: #666;
  font-size: 12px;
  text-align: center;
  margin-top: 24px;
}
</style>