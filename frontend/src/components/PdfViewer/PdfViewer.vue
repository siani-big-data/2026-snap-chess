<template>
  <div class="pdf-viewer">

    <div class="controls">
      <button @click="prevPage" :disabled="currentPage === 1">←</button>
      <input
          v-model="pageInput"
          type="number"
          :min="1"
          :max="chessFile?.totalPages"
          @keydown.enter="goToPage"
          @blur="goToPage"
          class="page-input"
      />
      <span class="page-separator">/ {{ chessFile?.totalPages }}</span>
      <button @click="nextPage" :disabled="currentPage === chessFile?.totalPages">→</button>

      <div class="zoom-controls">
        <button @click="zoomOut" :disabled="zoomLevel <= 0.5">−</button>
        <span class="zoom-label" @click="resetZoom" title="Click para resetear">
      {{ Math.round(zoomLevel * 100) }}%
    </span>
        <button @click="zoomIn" :disabled="zoomLevel >= 2.5">+</button>
      </div>
    </div>
    <div class="analyze-controls">
      <button
          class="btn-analyze"
          :disabled="isAnalyzing"
          @click="onAnalyzePage">
        <font-awesome-icon :icon="isAnalyzing ? 'rotate' : 'chess-board'" :spin="isAnalyzing" />
        {{ isAnalyzing ? 'Analizando...' : 'Analizar página' }}
      </button>
      <span v-if="analyzeError" class="analyze-error">{{ analyzeError }}</span>
    </div>


    <div class="page-container" :style="containerStyle">
      <div v-if="!currentPageUrl" class="page-loading">
        <p v-if="pageLoadError" class="page-load-error">{{ pageLoadError }}</p>
        <template v-else>
          <font-awesome-icon icon="rotate" spin />
          <span>Cargando página…</span>
        </template>
      </div>

      <template v-else>
        <p v-if="pageLoadError" class="page-load-error-banner">{{ pageLoadError }}</p>
        <img
            ref="pageImageRef"
            :src="currentPageUrl"
            @load="onImageLoad"
            style="display: block; width: 100%"
            alt="Imagen página completa"/>
        <BoardOverlay
            v-for="board in currentPageBoards"
            :key="board.id"
            :board="board"
            :pageWidthPt="pageWidthPt"
            :pageHeightPt="pageHeightPt"
            @boardClicked="onBoardClicked(board)"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import type { ChessBoard, ChessFile } from "../../types/chess.types.ts";
import { getChessFile, getPageImageBlob, analyzePage } from "../../api/bookApi.ts";
import BoardOverlay from "./BoardOverlay.vue";

const isAnalyzing = ref(false)
const analyzeError = ref<string | null>(null)
const chessFile = ref<ChessFile | null>(null)
const currentPage = ref(1)
const pageWidthPt = ref(595)
const pageHeightPt = ref(842)
const pageImageRef = ref<HTMLImageElement | null>(null)
const zoomLevel = ref(1)
const MIN_ZOOM = 0.5
const MAX_ZOOM = 2.5
const ZOOM_STEP = 0.15
const pageInput = ref(1)
const currentPageUrl = ref<string | undefined>(undefined)
const pageLoadError = ref<string | null>(null)
let activeObjectUrl: string | null = null
const isLoadingPage = ref(true)

const props = defineProps<{
  bookId: string
}>()

const emit = defineEmits<{
  boardSelected: [board: ChessBoard]
}>()


const zoomIn  = () => { if (zoomLevel.value < MAX_ZOOM) zoomLevel.value = +(zoomLevel.value + ZOOM_STEP).toFixed(2) }
const zoomOut = () => { if (zoomLevel.value > MIN_ZOOM) zoomLevel.value = +(zoomLevel.value - ZOOM_STEP).toFixed(2) }
const resetZoom = () => { zoomLevel.value = 1 }


const baseMaxWidth = 800  // ancho base en px

const containerStyle = computed(() => ({
  width: `${baseMaxWidth * zoomLevel.value}px`,
  maxWidth: 'none'
}))

const currentPageBoards = computed(() =>
    chessFile.value?.boards.filter(board => board.page === currentPage.value) ?? []
)
const goToPage = () => {
  const target = Number(pageInput.value)
  const total = chessFile.value?.totalPages ?? 1
  if (!isNaN(target) && target >= 1 && target <= total) {
    currentPage.value = target
  } else {
    pageInput.value = currentPage.value
  }
}

const loadPageImage = async (pageNumber: number) => {
  isLoadingPage.value = true
  pageLoadError.value = null
  try {
    const blob = await getPageImageBlob(props.bookId, pageNumber)
    const objectUrl = URL.createObjectURL(blob)

    if (activeObjectUrl) {
      URL.revokeObjectURL(activeObjectUrl)
    }
    activeObjectUrl = objectUrl
    currentPageUrl.value = objectUrl
  } catch (e) {
    pageLoadError.value = 'No se pudo cargar la página.'
  } finally {
    isLoadingPage.value = false
  }
}

const onImageLoad = () => {
  const img = pageImageRef.value
  if (!img) return
  const DPI = 150
  const factor = DPI / 72
  pageWidthPt.value  = img.naturalWidth  / factor
  pageHeightPt.value = img.naturalHeight / factor
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    pageInput.value = currentPage.value
  }
}

const nextPage = () => {
  if (currentPage.value < (chessFile.value?.totalPages ?? 1)) {
    currentPage.value++
    pageInput.value = currentPage.value
  }
}
const onAnalyzePage = async () => {
  isAnalyzing.value = true
  analyzeError.value = null
  try {
    chessFile.value = await analyzePage(props.bookId, currentPage.value)
  } catch (e) {
    analyzeError.value = 'Error al analizar la página'
  } finally {
    isAnalyzing.value = false
  }
}

watch(currentPage, (newPage) => {
  analyzeError.value = null
  loadPageImage(newPage)
})

const onBoardClicked = (board: ChessBoard) => {
  analyzeError.value = null
  emit('boardSelected', board)
}
onUnmounted(() => {
  if (activeObjectUrl) {
    URL.revokeObjectURL(activeObjectUrl)
  }
})
onMounted(async () => {
  await loadPageImage(currentPage.value)
  chessFile.value = await getChessFile(props.bookId)
})
</script>
<style scoped>
.pdf-viewer {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.page-container {
  position: relative;
  width: 100%;
  max-width: 900px;
}

.page-container img {
  width: 100%;
  height: auto;
  display: block;
  box-shadow: 0 2px 10px rgba(0,0,0,0.15);
  border-radius: 4px;
}

.controls {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

.controls button {
  padding: 6px 12px;
  border: none;
  background: #2d5a9e;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.controls button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.zoom-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
  padding-left: 16px;
  border-left: 1px solid #eee;
}

.zoom-label {
  min-width: 48px;
  text-align: center;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  user-select: none;
}

.zoom-label:hover {
  color: #2d5a9e;
}
.page-input {
  width: 52px;
  text-align: center;
  padding: 4px 6px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 13px;
  color: #2c3e50;
  background: white;
}

.page-input:focus {
  outline: none;
  border-color: #2d5a9e;
}

.page-input::-webkit-inner-spin-button,
.page-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
}

.page-separator {
  font-size: 13px;
  color: #555;
}

.analyze-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
  padding-left: 16px;
  border-left: 1px solid #eee;
}

.btn-analyze {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: none;
  background: #2d5a9e;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
}

.btn-analyze:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.analyze-error {
  font-size: 12px;
  color: #e74c3c;
}

.page-load-error {
  color: #e74c3c;
  font-size: 14px;
  padding: 40px 0;
}
.page-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: #888;
  font-size: 14px;
}
.page-load-error-banner {
  color: #e74c3c;
  font-size: 13px;
  margin: 0 0 8px;
}
</style>