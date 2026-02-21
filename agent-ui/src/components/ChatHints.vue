<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { chatHints, type ChatHint } from '../data/chatHints'

const emit = defineEmits<{ (e: 'select', hint: ChatHint): void }>()

const shuffled = ref<ChatHint[]>([])

onMounted(() => {
  shuffled.value = [...chatHints].sort(() => Math.random() - 0.5)
})
</script>

<template>
  <div class="flex gap-2 px-6 pb-3 overflow-hidden flex-nowrap">
    <button
      v-for="hint in shuffled"
      :key="hint.label"
      class="flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium border border-cosmic-500/40 text-cosmic-300 bg-cosmic-500/10 hover:bg-cosmic-500/20 hover:border-cosmic-500/70 hover:text-cosmic-200 transition-colors whitespace-nowrap"
      @click="emit('select', hint)"
    >
      {{ hint.label }}
    </button>
  </div>
</template>
