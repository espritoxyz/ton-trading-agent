<script setup lang="ts">
import { inject, onMounted, computed } from 'vue'

const props = defineProps<{ id: string; label: string; default?: boolean }>()
const tabsSet = inject<(id: string) => void>('tabsSet')!
const tabsActive = inject<{ value: string }>('tabsActive')!
const tabsSetDefault = inject<((id: string) => void) | undefined>('tabsSetDefault')

onMounted(() => {
  if (props.default) {
    if (tabsSetDefault) {
      tabsSetDefault(props.id)
    } else if (!tabsActive.value) {
      tabsSet(props.id)
    }
  }
})

const isActive = computed(() => tabsActive.value === props.id)
</script>

<template>
  <div v-show="isActive">
    <slot />
  </div>
</template>
