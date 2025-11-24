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
        :class="tabsIs(props.id).value ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        @click="$inject.tabsSet(props.id)">
      {{ props.label }}
    </button>
  </template>
</template>
