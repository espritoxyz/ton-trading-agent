<script setup lang="ts">
import { ref, nextTick } from 'vue'

defineExpose({ fill })
const props = defineProps<{ disabled?: boolean }>()
const emit = defineEmits<{ (e: 'send', text: string): void }>()
const text = ref('')
const textarea = ref<HTMLTextAreaElement | null>(null)
const needsScroll = ref(false)

function adjustHeight() {
  const el = textarea.value
  if (!el) return
  el.style.height = 'auto'
  const newHeight = el.scrollHeight
  el.style.height = newHeight + 'px'

  // Проверяем, нужен ли скролл (макс. высота 128px = max-h-32)
  needsScroll.value = newHeight >= 128
}

async function onSend() {
  if (props.disabled) return
  const t = text.value.trim()
  if (!t) return
  emit('send', t)
  text.value = ''
  await nextTick()
  adjustHeight()
}

function fill(value: string) {
  text.value = value
  nextTick(() => {
    textarea.value?.focus()
    adjustHeight()
  })
}

function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    onSend()
  }
}
</script>

<template>
  <div class="flex items-end gap-3 border-t border-gray-200 dark:border-white/10 p-4 bg-gray-50 dark:bg-white/5">
    <textarea
        ref="textarea"
        v-model="text"
        :disabled="disabled"
        placeholder="Type your message..."
        rows="1"
        :class="[
          'flex-1 rounded-xl bg-white dark:bg-white/10 border border-gray-300 dark:border-white/20 px-4 py-3 text-sm text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-transparent transition disabled:opacity-50 disabled:cursor-not-allowed shadow-sm resize-none max-h-32 input-textarea',
          needsScroll ? 'overflow-y-auto' : 'overflow-y-hidden'
        ]"
        @keydown="onKeyDown"
        @input="adjustHeight"
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

<style scoped>
.input-textarea {
  scrollbar-width: thin;
  scrollbar-color: rgba(99, 102, 241, 0.5) transparent;
  scrollbar-gutter: stable;
}

.input-textarea::-webkit-scrollbar {
  width: 8px;
}

.input-textarea::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}

.input-textarea::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #6366f1, #a855f7);
  border-radius: 8px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.input-textarea::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #7c3aed, #d946ef);
}

.input-textarea:focus,
.input-textarea:focus-visible {
  outline: none;
}
</style>
