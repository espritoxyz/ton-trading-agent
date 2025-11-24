<script setup lang="ts">
import { ref, provide, computed } from 'vue'

const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const active = ref(props.modelValue || '')
provide('tabsActive', active)
provide('tabsSet', (id: string) => {
  active.value = id
  emit('update:modelValue', id)
})

const is = (id: string) => computed(() => active.value === id)
provide('tabsIs', is)
</script>

<template>
  <div class="rounded-2xl bg-white ring-1 ring-gray-200">
    <div class="flex gap-2 border-b border-gray-200 p-2">
      <slot name="labels" />
    </div>
    <div class="p-3">
      <slot />
    </div>
  </div>
</template>
