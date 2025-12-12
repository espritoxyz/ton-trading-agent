<script setup lang="ts">
import { inject, onMounted } from 'vue'

const props = defineProps<{ id: string; label: string; default?: boolean }>()
const tabsSet = inject<(id: string) => void>('tabsSet')!
const tabsActive = inject<any>('tabsActive')!
const tabsIs = inject<(id: string) => { value: boolean }>('tabsIs')!

onMounted(() => {
  if (props.default && !tabsActive.value) tabsSet(props.id)
})
</script>

<template>
  <template #default>
    <div v-show="tabsIs(props.id).value">
      <slot />
    </div>
  </template>

  <template #label>
    <button
        class="rounded-xl px-3 py-1 text-sm"
        :class="tabsIs(props.id).value ? 'bg-indigo-600 text-white dark:bg-indigo-500' : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700'"
        @click="$inject.tabsSet(props.id)">
      {{ props.label }}
    </button>
  </template>
</template>
