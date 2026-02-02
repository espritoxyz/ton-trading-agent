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
  <div class="flex items-center gap-3 border-t border-white/10 p-4 bg-white/5">
    <input
        v-model="text"
        :disabled="disabled"
        type="text"
        placeholder="Type your message..."
        class="flex-1 rounded-xl bg-white/10 border border-white/20 px-4 py-3 text-sm text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-transparent transition disabled:opacity-50 disabled:cursor-not-allowed"
        @keydown.enter="onSend"
    />
    <button
        class="cosmic-button rounded-xl px-6 py-3 text-sm font-semibold text-white disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
        :disabled="disabled || !text.trim()"
        @click="onSend">
      <span>Send</span>
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"></path>
      </svg>
    </button>
  </div>
</template>
