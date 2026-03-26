<template>
  <div class="board-overlay" :style="overlayStyle" @click="emit('boardClicked', board)"></div>
</template>

<script setup lang="ts">
import type { ChessBoard } from "../../types/chess.types.ts";
import { computed } from "vue";

const props = defineProps<{
  board: ChessBoard
  pageWidthPt: number
  pageHeightPt: number
}>();

const emit = defineEmits<{
  boardClicked: [board: ChessBoard]
}>()
const overlayStyle = computed(() => {

  const { board, pageWidthPt, pageHeightPt } = props;
  const { bbox } = board;
  return {
    position: 'absolute' as const,
    left:   `${(bbox.x / pageWidthPt) * 100}%`,
    top:    `${(bbox.y / pageHeightPt) * 100}%`,
    width:  `${(bbox.width / pageWidthPt) * 100}%`,
    height: `${(bbox.height / pageHeightPt) * 100}%`,
    border: '3px solid rgba(0, 255, 0, 0.8)',
    backgroundColor: 'rgba(0, 255, 0, 0.1)',
    cursor:          'pointer',
    transition:      'background-color 0.15s ease',
  };
});
</script>
<style scoped>
.board-overlay:hover {
  background-color: rgba(0, 255, 0, 0.25) !important;
}
</style>