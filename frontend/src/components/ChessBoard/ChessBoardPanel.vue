<template>
  <div class="chess-panel">

    <div v-if="!board" class="empty-state">
      <p>Haz clic sobre un tablero del PDF para analizarlo</p>
    </div>

    <template v-else>

      <div class="board-area">
        <div class="eval-bar" v-if="evalCp !== null || isAnalyzing">
          <div class="eval-bar__white" :style="{ height: evalBarWhiteHeight }"/>
          <div class="eval-bar__black" :style="{ height: evalBarBlackHeight }"/>
        </div>

        <div class="board-wrapper" ref="boardWrapperRef">
          <TheChessboard
              :board-config="boardConfig"
              :key="boardKey"
              @move="onMove"
              @board-created="onBoardCreated"
          />
        </div>
      </div>


      <div class="eval-info" v-if="evalCp !== null || isAnalyzing">
        <template v-if="isAnalyzing">
          <span class="eval-score">
            <font-awesome-icon icon="spinner" spin /> Analizando...
          </span>
        </template>
        <template v-else>
          <span class="eval-score" :class="evalCp! >= 0 ? 'eval-score--white' : 'eval-score--black'">
            {{ formattedEval }}
          </span>
          <span v-if="bestMove && showBestMove" class="eval-bestmove">
            Mejor: {{ bestMove }}
          </span>
        </template>
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
        <button
            class="btn"
            :class="{ 'btn--active': showBestMove }"
            title="Mostrar/ocultar mejor jugada"
            @click="showBestMove = !showBestMove">
          <font-awesome-icon icon="eye" />
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
import {ref, computed, watch, onMounted, onUnmounted, nextTick} from 'vue'
import {BoardApi, TheChessboard} from 'vue3-chessboard'
import 'vue3-chessboard/style.css'
import type { BoardConfig } from 'vue3-chessboard'
import type { ChessBoard } from '../../types/chess.types.ts'
import {addMove, analyzeFen, analyzePosition} from '../../api/analysisApi'
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
const evalCp      = ref<number | null>(null)
const bestMove    = ref<string | null>(null)
const isAnalyzing = ref(false)
const showBestMove = ref(false)
let analysisAbortController: AbortController | null = null
let boardAPI: BoardApi | null = null
let moveAnalysisController: AbortController | null = null

const FEN_REGEX = /^([rnbqkpRNBQKP1-8]{1,8}\/){7}[rnbqkpRNBQKP1-8]{1,8}\s[wb]\s/

const isValidFen = (fen: string): boolean => {
  if (!FEN_REGEX.test(fen)) return false
  return fen.includes('k') && fen.includes('K')
}
const onBoardCreated = (api: BoardApi) => {
  boardAPI = api
  updateArrow()
}
const updateArrow = () => {
  if (!boardAPI || !bestMove.value) return
  if (showBestMove.value) {

    const orig = bestMove.value.slice(0, 2) as any
    const dest = bestMove.value.slice(2, 4) as any
    boardAPI.setConfig({
      drawable: { autoShapes: [{ orig, dest, brush: 'green' }] }
    })
  } else {
    boardAPI.setConfig({ drawable: { autoShapes: [] } })
  }
}


// COMPUTED
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

const evalBarWhiteHeight = computed(() => {
  if (evalCp.value === null) return '50%'
  if (evalCp.value >= 30000) return '100%'
  if (evalCp.value <= -30000) return '0%'
  const clamped = Math.max(-1000, Math.min(1000, evalCp.value))
  const pct = 50 + (clamped / 1000) * 50
  return `${pct}%`
})

const evalBarBlackHeight = computed(() => {
  return `${100 - parseFloat(evalBarWhiteHeight.value)}%`
})

const formattedEval = computed(() => {
  if (evalCp.value === null) return ''
  if (evalCp.value >= 30000) return '#'   // mate para blancas
  if (evalCp.value <= -30000) return '#'  // mate para negras
  const pawns = evalCp.value / 100
  return pawns >= 0 ? `+${pawns.toFixed(2)}` : pawns.toFixed(2)
})

const runEngineAnalysis = async () => {
  if (!props.bookId || !props.board?.id) return

  // Cancelar análisis anterior si existe
  if (analysisAbortController) {
    analysisAbortController.abort()
  }
  analysisAbortController = new AbortController()

  const cached = props.board?.analysis?.evalCp ?? null
  if (cached !== null) {
    evalCp.value = cached
  } else {
    isAnalyzing.value = true
  }

  try {
    const result = await analyzePosition(
        props.bookId,
        props.board.id,
        1000,
        analysisAbortController.signal
    )
    evalCp.value   = result.evalCp
    bestMove.value = result.bestMove
  } catch (e: any) {
    if (e.name === 'AbortError') return  // cancelado intencionalmente, no es error
    console.error('Error analizando posición:', e)
  } finally {
    isAnalyzing.value = false
  }
}

//FUNCIONES

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

  bestMove.value = null
  evalCp.value   = null

  const baseFen = props.board?.fen ?? 'start'
  const chess = new Chess(baseFen === 'start' ? undefined : baseFen)
  for (const san of path) chess.move(san)

  navigatedFen.value = chess.fen()
  boardKey.value++

  const isGameOver = chess.isGameOver()

  if (isGameOver) {
    evalCp.value   = chess.isCheckmate()
        ? (chess.turn() === 'w' ? -30000 : 30000)
        : 0
    bestMove.value = null
    nextTick(() => updateArrow())
    return
  }

  // Leer evalCp cacheado del nodo del árbol si existe
  const cachedEval = getNodeEvalCp(path)
  if (cachedEval !== null) {
    evalCp.value = cachedEval
    // Aun así lanzar análisis para obtener bestMove
  }

  if (moveAnalysisController) moveAnalysisController.abort()
  moveAnalysisController = new AbortController()
  const signal = moveAnalysisController.signal

  if (cachedEval === null) isAnalyzing.value = true

  analyzeFen(chess.fen(), 1000, signal)
      .then(result => {
        evalCp.value   = result.evalCp
        bestMove.value = result.bestMove
        nextTick(() => updateArrow())
      })
      .catch(e => {
        if (e.name === 'AbortError') return
        console.error('Error analizando posición navegada:', e)
      })
      .finally(() => {
        isAnalyzing.value = false
      })
}

const getNodeEvalCp = (path: string[]): number | null => {
  if (!analysisTree.value) return null
  if (path.length === 0) return analysisTree.value.evalCp ?? null

  let node = analysisTree.value
  for (const move of path) {
    const child = node.children?.find(c => c.move === move)
    if (!child) return null
    node = child
  }
  return node.evalCp ?? null
}

const onMove = async (move: any) => {
  if (!move?.san || !props.bookId || !props.board?.id) return

  moveHistory.value.push(move.san)
  currentPath.value = [...moveHistory.value]

  const baseFen = props.board?.fen ?? 'start'
  const chess = new Chess(baseFen === 'start' ? undefined : baseFen)
  for (const san of moveHistory.value) chess.move(san)
  const currentFen = chess.fen()
  const isGameOver = chess.isGameOver()

  if (moveAnalysisController) moveAnalysisController.abort()
  moveAnalysisController = new AbortController()
  const signal = moveAnalysisController.signal

  // Mostrar spinner mientras analiza
  if (!isGameOver) isAnalyzing.value = true

  try {
    const promises: Promise<any>[] = [
      addMove(props.bookId, props.board.id, {
        path: moveHistory.value.slice(0, -1),
        move: move.san
      })
    ]

    if (!isGameOver) promises.push(analyzeFen(currentFen, 1000, signal))

    const results = await Promise.all(promises)
    analysisTree.value = results[0]

    if (!isGameOver && results[1]) {
      evalCp.value   = results[1].evalCp
      bestMove.value = results[1].bestMove
    } else if (isGameOver) {
      evalCp.value   = chess.isCheckmate()
          ? (chess.turn() === 'w' ? -30000 : 30000)
          : 0
      bestMove.value = null
    }
  } catch (e: any) {
    if (e.name === 'AbortError') return
    console.error('Error en onMove:', e)
  } finally {
    isAnalyzing.value = false  // ← siempre limpiar
  }
}
const flipBoard  = () => { isFlipped.value = !isFlipped.value; boardKey.value++ }
const resetBoard = () => {
  moveHistory.value  = []
  currentPath.value  = []
  navigatedFen.value = null
  evalCp.value       = null
  bestMove.value     = null
  boardKey.value++
  if (props.board) runEngineAnalysis()
}
watch(() => props.board, () => {
  evalCp.value       = null
  bestMove.value     = null
  moveHistory.value  = []
  analysisTree.value = props.board?.analysis ?? null
  currentPath.value  = []
  showBestMove.value = false
  navigatedFen.value = null
  showpopup.value    = false
  boardKey.value++

  if (props.board) {
    runEngineAnalysis()  // única llamada — maneja caché internamente
  }
})
watch(currentPath, () => {
  if (currentComment.value) {
    popupEditMode.value = false
    showpopup.value = true
  } else {
    showpopup.value = false
  }
})

watch([bestMove, showBestMove], () => {
  nextTick(() => updateArrow())
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

onUnmounted(() => {
  ro?.disconnect()
  analysisAbortController?.abort()
  moveAnalysisController?.abort()
})
</script>

<style scoped>

.chess-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px;
  gap: 10px;
  overflow: hidden;
  box-sizing: border-box;
}

.board-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;
  padding: 12px;
  flex-shrink: 0;
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
  flex-shrink: 0;
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

.eval-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  padding: 0 4px;
}

.eval-bar {
  width: 12px;
  align-self: stretch;    /* ← toma la altura del board-wrapper */
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  flex-direction: column-reverse;
  border: 1px solid #3d4560;
  flex-shrink: 0;
}

.eval-bar__white {
  background: #f0d9b5;
  transition: height 0.4s ease;
}

.eval-bar__black {
  background: #2d3447;
  transition: height 0.4s ease;
}

.eval-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  padding: 0 4px;
  min-height: 24px;
}

.eval-score {
  font-size: 14px;
  font-weight: 700;
  color: #e0e0e0;
}

.eval-score--white { color: #f0d9b5; }
.eval-score--black { color: #6b7a99; }

.eval-bestmove {
  font-size: 11px;
  color: #6b7a99;
}

.board-area {
  display: flex;
  flex-direction: row;
  align-items: stretch;
  gap: 6px;
  flex-shrink: 0;
  padding-right: 8px;
}
</style>