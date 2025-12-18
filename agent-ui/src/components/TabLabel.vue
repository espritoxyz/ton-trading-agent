<script setup lang="ts">
import { inject } from 'vue'

const props = defineProps<{ id: string; label: string }>()
const tabsSet = inject<(id: string) => void>('tabsSet')!
const tabsIs = inject<(id: string) => { value: boolean }>('tabsIs')!

const isActive = tabsIs(props.id)
</script>

<template>
  <button
    class="rounded-xl px-3 py-1.5 text-sm font-medium transition-all duration-200"
    :class="isActive.value 
      ? 'bg-indigo-600 text-white shadow-md ring-2 ring-indigo-300 dark:bg-indigo-500 dark:text-white dark:ring-indigo-400' 
      : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700'"
    @click="() => tabsSet(props.id)">
    {{ props.label }}
  </button>
</template>

