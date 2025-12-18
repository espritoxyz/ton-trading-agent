<script setup lang="ts">
import { ref, provide, computed, watch } from 'vue'

const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const active = ref(props.modelValue ?? '')
provide('tabsActive', active)

// Синхронизация с modelValue извне
watch(() => props.modelValue, (newValue) => {
  if (newValue !== undefined) {
    active.value = newValue
  }
}, { immediate: true })

provide('tabsSet', (id: string) => {
  active.value = id
  emit('update:modelValue', id)
})

const is = (id: string) => computed(() => active.value === id)
provide('tabsIs', is)

// Предоставляем функцию для установки значения по умолчанию
let defaultId: string | null = null
provide('tabsSetDefault', (id: string) => {
  if (!active.value && !defaultId) {
    defaultId = id
    active.value = id
    emit('update:modelValue', id)
  }
})
</script>

<template>
  <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800">
    <div class="flex gap-2 border-b border-gray-200 p-2 dark:border-gray-700">
      <slot name="labels" />
    </div>
    <div class="p-3">
      <slot />
    </div>
  </div>
</template>
