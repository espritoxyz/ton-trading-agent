<script setup lang="ts">
import { ref } from 'vue'
const props = defineProps<{ disabled?: boolean }>()
const emit = defineEmits<{ (e: 'send', text: string): void }>()
const text = ref('')
function onSend() {
  if (props.disabled) return
  const t = text.value.trim()
  if (!t) return
  emit('send', t)
  text.value = ''
}
</script>

<template>
  <div class="flex items-center gap-2 border-t border-gray-200 p-3">
    <input
        v-model="text"
        :disabled="disabled"
        type="text"
        placeholder="Type a message…"
        class="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
        @keydown.enter="onSend"
    />
    <button
        class="rounded-xl bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
        :disabled="disabled"
        @click="onSend">
      Send
    </button>
  </div>
</template>
