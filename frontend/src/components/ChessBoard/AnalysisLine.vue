<template>
  <span class="line">

    <!-- Solo renderizamos el primer nodo como línea principal -->
    <template v-if="nodes.length > 0">
      <span v-if="true" class="move-number">{{ moveNumberAt(0) }}.{{ !isWhiteTurnAt(0) ? '..' : '' }}</span>

      <span
          class="move"
          :class="{ 'move--active': isActive(fullPathTo(0)) }"
          @click="emit('navigate', fullPathTo(0))"
      >{{ nodes[0].move }}</span>

      <!-- Variantes del nodo principal (hijos secundarios) -->
      <template v-if="nodes[0].children && nodes[0].children.length > 1">
        <div
            v-for="(variant, vIndex) in nodes[0].children.slice(1)"
            :key="variant.move ?? vIndex"
            class="variation-block"
        >
          (<AnalysisLine
            :nodes="[variant]"
            :current-path="currentPath"
            :move-number="moveNumberAt(0)"
            :initial-white-turn="initialWhiteTurn"
            :root-path="fullPathTo(0)"
            @navigate="emit('navigate', $event)"
        />)
        </div>
      </template>

      <!-- Línea principal continúa por el primer hijo -->
      <template v-if="nodes[0].children && nodes[0].children.length > 0">
        <AnalysisLine
            :nodes="[nodes[0].children[0]]"
            :current-path="currentPath"
            :move-number="moveNumberAt(0)"
            :initial-white-turn="initialWhiteTurn"
            :root-path="fullPathTo(0)"
            @navigate="emit('navigate', $event)"
        />
      </template>
    </template>

  </span>
</template>

<script setup lang="ts">
import type { AnalysisNode } from '../../types/chess.types'

const props = defineProps<{
  nodes: AnalysisNode[]
  currentPath: string[]
  moveNumber: number
  initialWhiteTurn: boolean
  rootPath: string[]          // path completo desde la raíz hasta el padre de estos nodos
}>()

const emit = defineEmits<{
  navigate: [path: string[]]
}>()

const fullPathTo = (index: number): string[] => [
  ...props.rootPath,
  ...props.nodes.slice(0, index + 1).map(n => n.move as string)
]

// El turno se calcula siempre desde el turno inicial + profundidad total en el árbol
const isWhiteTurnAt = (index: number): boolean => {
  const depth = props.rootPath.length + index
  return props.initialWhiteTurn ? depth % 2 === 0 : depth % 2 !== 0
}

const moveNumberAt = (index: number): number => {
  const depth = props.rootPath.length + index
  const baseMove = props.initialWhiteTurn ? Math.floor(depth / 2) + 1 : Math.floor((depth + 1) / 2) + 1
  return baseMove
}



const isActive = (path: string[]): boolean =>
    JSON.stringify(path) === JSON.stringify(props.currentPath)
</script>

<style scoped>
.move {
  cursor: pointer;
  padding: 1px 4px;
  border-radius: 3px;
  transition: background 0.1s;
}

.move:hover { background: #2d3447; }

.move--active {
  background: #2d5a9e;
  color: white;
}

.move-number {
  color: #6b7a99;
  margin-right: 2px;
}

.variation-block {
  color: #8899bb;
  margin: 4px 0 4px 16px;
  display: block;
}
</style>