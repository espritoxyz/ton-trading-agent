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
  <div class="flex items-center gap-2 border-t border-gray-200 p-3 dark:border-gray-700">
    <input
        v-model="text"
        :disabled="disabled"
        type="text"
        placeholder="Type a message…"
        class="flex-1 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100 disabled:text-gray-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100 dark:placeholder-gray-400 dark:disabled:bg-gray-800 dark:disabled:text-gray-500"
        @keydown.enter="onSend"
    />
    <button
        class="rounded-xl bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 dark:bg-indigo-500 dark:hover:bg-indigo-600"
        :disabled="disabled"
        @click="onSend">
      Send
    </button>
  </div>
</template>
