<template>
  <div class="chess-panel">

    <div v-if="!board" class="empty-state">
      <p>Haz clic sobre un tablero del PDF para analizarlo</p>
    </div>

    <template v-else>

      <div class="board-wrapper" ref="boardWrapperRef">
        <TheChessboard
            :board-config="boardConfig"
            :key="boardKey"
            @move="onMove"
        />
      </div>

      <div class="controls">
        <button class="btn" @click="flipBoard"><font-awesome-icon icon="rotate" /> Girar</button>
        <button class="btn" @click="resetBoard"><font-awesome-icon icon="arrow-rotate-left" /> Reiniciar</button>
        <button class="btn" :class="{ 'btn--active': freeMode }" @click="freeMode = !freeMode; boardKey++">
          <font-awesome-icon icon="chess-board" />{{ freeMode ? 'Modo libre' : 'Con reglas' }}
        </button>
      </div>

      <div class="move-history" v-if="moveHistory.length > 0">
        <div class="move-history-title">Movimientos</div>
        <div class="move-list">
          <span v-for="(move, index) in moveHistory" :key="index" class="move-item">
            <span v-if="index % 2 === 0" class="move-number">{{ Math.floor(index / 2) + 1 }}.</span>
            {{ move }}
          </span>
        </div>
      </div>

    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { TheChessboard } from 'vue3-chessboard'
import 'vue3-chessboard/style.css'
import type { BoardConfig } from 'vue3-chessboard'
import type { ChessBoard } from '../../types/chess.types.ts'

const props = defineProps<{
  board: ChessBoard | null
}>()

const isFlipped = ref(false)
const moveHistory = ref<string[]>([])
const boardKey = ref(0)
const boardWrapperRef = ref<HTMLElement | null>(null)
const boardSize = ref(0)
let ro: ResizeObserver | null = null
const freeMode = ref(false)

const FEN_REGEX = /^([rnbqkpRNBQKP1-8]{1,8}\/){7}[rnbqkpRNBQKP1-8]{1,8}\s[wb]\s/

const isValidFen = (fen: string): boolean => {
  if (!FEN_REGEX.test(fen)) return false
  return fen.includes('k') && fen.includes('K')
}

const boardConfig = computed<BoardConfig>(() => ({
  fen: props.board?.fen && isValidFen(props.board.fen) ? props.board.fen : 'start',
  orientation: isFlipped.value ? 'black' : 'white',
  movable: {
    free: freeMode.value,
    color: 'both',
    showDests: true
  }
}))

const onMove    = (move: any) => { if (move?.san) moveHistory.value.push(move.san) }
const flipBoard  = () => { isFlipped.value = !isFlipped.value; boardKey.value++ }
const resetBoard = () => { moveHistory.value = []; boardKey.value++ }
watch(() => props.board, () => { moveHistory.value = []; boardKey.value++ })


onMounted(() => {
  if (!boardWrapperRef.value) return
  ro = new ResizeObserver((entries) => {
    const w = Math.floor(entries[0].contentRect.width)
    if (w > 0 && w !== boardSize.value) {
      boardSize.value = w
    }
  })
  ro.observe(boardWrapperRef.value)
})

onUnmounted(() => ro?.disconnect())
</script>

<style scoped>

.chess-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px;
  gap: 10px;
  overflow-y: auto;
  box-sizing: border-box;
}


.board-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;
  padding: 12px;
}

.board-wrapper :deep(> *) {
  width: 100% !important;
  height: 100% !important;
}

.board-wrapper :deep(.cg-wrap) {
  width: 100% !important;
  height: 100% !important;
}

.board-wrapper :deep(.cg-wrap cg-board) {
  width: 100% !important;
  height: 100% !important;
}

.controls {
  display: flex;
  gap: 8px;
}

.btn {
  flex: 1;
  padding: 8px;
  background: #2d3447;
  color: #e0e0e0;
  border: 1px solid #3d4560;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.15s ease;
}

.btn:hover { background: #3d4560; }

.move-history {
  background: #12161f;
  border-radius: 6px;
  padding: 10px;
  flex: 1;
  overflow-y: auto;
  min-height: 80px;
}

.move-history-title {
  font-size: 11px;
  font-weight: 600;
  color: #6b7a99;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}

.move-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 13px;
  color: #c8d0e7;
  font-family: 'Courier New', monospace;
}

.move-number { color: #6b7a99; margin-right: 2px; }

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6b7a99;
  font-size: 13px;
  text-align: center;
  padding: 24px;
}

.btn--active {
  background: #2d5a9e;
  border-color: #4a7ac8;
  color: white;
}
</style>