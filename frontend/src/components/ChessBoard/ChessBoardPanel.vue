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
        <button class="btn" @click="flipBoard" title="Girar tablero">
          <font-awesome-icon icon="rotate" />
        </button>
        <button class="btn" @click="resetBoard" title="Reiniciar">
          <font-awesome-icon icon="arrow-rotate-left" />
        </button>
        <button class="btn" :class="{ 'btn--active': freeMode }"
                @click="freeMode = !freeMode; boardKey++" title="Modo libre / Con reglas">
          <font-awesome-icon icon="chess-board" />
        </button>
        <button
            class="btn"
            title="Añadir comentario"
            @click="popupEditMode = true; showpopup = true">
          <font-awesome-icon icon="comment" />
        </button>
      </div>

      <CommentPopUp
          v-if="showpopup"
          :comment="currentComment"
          :anchor-right="props.sidebarWidth"
          :is-edit-mode="popupEditMode"
          @close="showpopup = false"
          @save="onSaveComment"
      />

      <AnalysisPanel
          :tree="analysisTree"
          :current-path="currentPath"
          :initial-fen="props.board?.fen"
          @navigate="navigateTo"
      />

    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { TheChessboard } from 'vue3-chessboard'
import 'vue3-chessboard/style.css'
import type { BoardConfig } from 'vue3-chessboard'
import type { ChessBoard } from '../../types/chess.types.ts'
import { addMove } from '../../api/analysisApi'
import type { AnalysisNode } from '../../types/chess.types'
import { Chess } from 'chess.js'
import AnalysisPanel from './AnalysisPanel.vue'
import CommentPopUp from "./CommentPopUp.vue";
import { updateComment } from '../../api/analysisApi'

const showpopup = ref(false)
const popupEditMode = ref(false)


const props = defineProps<{
  board: ChessBoard | null
  bookId: string | null
  sidebarWidth: number
}>()

const isFlipped = ref(false)
const moveHistory = ref<string[]>([])
const boardKey = ref(0)
const boardWrapperRef = ref<HTMLElement | null>(null)
const boardSize = ref(0)
let ro: ResizeObserver | null = null
const freeMode = ref(false)
const analysisTree = ref<AnalysisNode | null>(props.board?.analysis ?? null)
const currentPath = ref<string[]>([])
const navigatedFen = ref<string | null>(null)

const FEN_REGEX = /^([rnbqkpRNBQKP1-8]{1,8}\/){7}[rnbqkpRNBQKP1-8]{1,8}\s[wb]\s/

const isValidFen = (fen: string): boolean => {
  if (!FEN_REGEX.test(fen)) return false
  return fen.includes('k') && fen.includes('K')
}

const boardConfig = computed<BoardConfig>(() => ({
  fen: navigatedFen.value
      ?? (props.board?.fen && isValidFen(props.board.fen) ? props.board.fen : 'start'),
  orientation: isFlipped.value ? 'black' : 'white',
  movable: {
    free: freeMode.value,
    color: 'both',
    showDests: true
  }
}))

const currentComment = computed((): string => {
  if (!analysisTree.value || currentPath.value.length === 0) return ''
  let node = analysisTree.value
  for (const move of currentPath.value) {
    const child = node.children?.find(c => c.move === move)
    if (!child) return ''
    node = child
  }
  return node.comment ?? ''
})

const onSaveComment = async (text: string) => {
  if (!props.bookId || !props.board?.id) return
  try {
    analysisTree.value = await updateComment(
        props.bookId, props.board.id, currentPath.value, text)
  } catch (e) {
    console.error('Error guardando comentario:', e)
  }
  showpopup.value = false
}

const navigateTo = (path: string[]) => {
  currentPath.value = path
  moveHistory.value = [...path]

  const baseFen = props.board?.fen ?? 'start'
  const chess = new Chess(baseFen === 'start' ? undefined : baseFen)

  for (const san of path) {
    chess.move(san)
  }

  navigatedFen.value = chess.fen()
  boardKey.value++
}

const onMove = async (move: any) => {
  if (!move?.san || !props.bookId || !props.board?.id) return

  moveHistory.value.push(move.san)
  currentPath.value = [...moveHistory.value]

  try {
    const path = moveHistory.value.slice(0, -1)
    analysisTree.value = await addMove(props.bookId, props.board.id, {
      path,
      move: move.san
    })
  } catch (e) {
    console.error('Error guardando jugada en el análisis:', e)
  }
}
const flipBoard  = () => { isFlipped.value = !isFlipped.value; boardKey.value++ }
const resetBoard = () => {
  moveHistory.value = []
  currentPath.value = []
  navigatedFen.value = null
  boardKey.value++
}
watch(() => props.board, () => {
  moveHistory.value = []
  analysisTree.value = props.board?.analysis ?? null
  currentPath.value = []
  navigatedFen.value = null
  showpopup.value = false
  boardKey.value++
})
watch(currentPath, () => {
  if (currentComment.value) {
    popupEditMode.value = false
    showpopup.value = true
  } else {
    showpopup.value = false
  }
})


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
  overflow: hidden;       /* antes: overflow-y: auto — el panel ya no hace scroll */
  box-sizing: border-box;
}

.board-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;
  padding: 12px;
  flex-shrink: 0;         /* evita que el tablero se comprima cuando el árbol crece */
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
  flex-shrink: 0;         /* los botones tampoco se comprimen */
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