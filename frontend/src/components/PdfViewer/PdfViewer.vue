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

    
    <div class="page-container" :style="containerStyle">
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
          @boardClicked="emit('boardSelected', board)"

      />
    </div>

  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import type {ChessBoard, ChessFile} from "../../types/chess.types.ts";
import {getChessFile, getPageImageUrl} from "../../api/bookApi.ts";
import BoardOverlay from "./BoardOverlay.vue";

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


const props = defineProps<{
  bookId: string
}>()

const emit = defineEmits<{
  boardSelected: [board: ChessBoard]
}>()


const zoomIn  = () => { if (zoomLevel.value < MAX_ZOOM) zoomLevel.value = +(zoomLevel.value + ZOOM_STEP).toFixed(2) }
const zoomOut = () => { if (zoomLevel.value > MIN_ZOOM) zoomLevel.value = +(zoomLevel.value - ZOOM_STEP).toFixed(2) }
const resetZoom = () => { zoomLevel.value = 1 }

const currentPageUrl = computed(() =>
    getPageImageUrl(props.bookId, currentPage.value)
)


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


onMounted(async () => {
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
</style>