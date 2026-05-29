<template>
  <div class="analysis-panel" v-if="tree && tree.children && tree.children.length > 0">
    <div class="analysis-title">Análisis</div>
    <div class="analysis-content">
      <AnalysisLine
          v-if="tree.children && tree.children.length > 0"
          :nodes="[tree.children[0]]"
          :current-path="currentPath"
          :move-number="1"
          :initial-white-turn="initialWhiteTurn"
          :root-path="[]"
          @navigate="onNavigate"
      />

      <!-- Variantes en la raíz (alternativas a la primera jugada) -->
      <div
          v-for="(variant, vIndex) in tree.children.slice(1)"
          :key="variant.move ?? vIndex"
          class="variation-block"
      >
        (<AnalysisLine
          :nodes="[variant]"
          :current-path="currentPath"
          :move-number="1"
          :initial-white-turn="initialWhiteTurn"
          :root-path="[]"
          @navigate="onNavigate"
      />)
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import type { AnalysisNode } from '../../types/chess.types'
import AnalysisLine from "./AnalysisLine.vue";

const props = defineProps<{
  tree: AnalysisNode | null
  currentPath: string[]
  initialFen?: string
}>()

const emit = defineEmits<{
  navigate: [path: string[]]
}>()

// Extrae si es turno de blancas del FEN (el segundo campo separado por espacios)
const initialWhiteTurn = computed((): boolean => {
  if (!props.initialFen || props.initialFen === 'start') return true
  const parts = props.initialFen.split(' ')
  return parts[1] !== 'b'
})

const onNavigate = (path: string[]) => emit('navigate', path)
</script>
<style scoped>
.analysis-panel {
  background: #12161f;
  border-radius: 6px;
  padding: 10px;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.analysis-title {
  font-size: 11px;
  font-weight: 600;
  color: #6b7a99;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}

.analysis-content {
  font-size: 13px;
  color: #c8d0e7;
  font-family: 'Courier New', monospace;
  line-height: 1.8;
}
</style>

