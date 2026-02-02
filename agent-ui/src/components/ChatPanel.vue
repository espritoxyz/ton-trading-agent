<script setup lang="ts">
import { computed, nextTick, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as chatModule from '../composables/useChat.ts'
import { accessToken } from '../composables/useAuth.ts'
import MessageBubble from './MessageBubble.vue'
import InputBar from './InputBar.vue'

const chat = chatModule.useChat()
const messages = chat.messages
const sending = chat.sending

async function clearConversation() {
  try {
    await (await import('../composables/useApi.ts')).api.post('/chat/history/clear')
  } catch {}
  chat.clearChat()
}

const scroller = ref<HTMLDivElement | null>(null)
const ready = computed(() => !!accessToken.value)

const autoScroll = ref(true)
const showTopButton = ref(false)

function animateScroll(el: HTMLElement, to: number, duration = 300) {
  const start = el.scrollTop
  const change = to - start
  const startTime = performance.now()
  const easeInOutQuad = (t: number) => t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t

  function step(now: number) {
    const elapsed = Math.min(1, (now - startTime) / duration)
    const v = easeInOutQuad(elapsed)
    el.scrollTop = Math.round(start + change * v)
    if (elapsed < 1) requestAnimationFrame(step)
  }

  requestAnimationFrame(step)
}

function scrollToBottom(smooth = true) {
  const el = scroller.value
  if (!el) return

  const items = el.querySelectorAll('[data-message-id]')
  const last = items.length ? (items[items.length - 1] as HTMLElement) : null

  if (last) {
    try {
      // compute target scrollTop so last element is aligned to bottom
      const target = Math.max(0, last.offsetTop + last.offsetHeight - el.clientHeight)
      if (smooth) {
        animateScroll(el, target)
      } else {
        el.scrollTop = el.scrollHeight
      }
      return
    } catch {}
  }

  try {
    if (smooth) animateScroll(el, el.scrollHeight)
    else el.scrollTop = el.scrollHeight
  } catch {}
}

function scrollToTop(smooth = true) {
  const el = scroller.value
  if (!el) return
  try {
    if (smooth) animateScroll(el, 0)
    else el.scrollTop = 0
  } catch {
    el.scrollTop = 0
  }
}

watch(() => messages.length, async () => {
  await nextTick()
  requestAnimationFrame(() => requestAnimationFrame(() => {
    if (autoScroll.value) scrollToBottom(true)
  }))
})

let resizeObserver: ResizeObserver | null = null
let onScrollListener: ((e: Event) => void) | null = null
let isHover = ref(false)

const handlePointerEnter = () => { isHover.value = true }
const handlePointerLeave = () => { isHover.value = false }

onMounted(() => {
  requestAnimationFrame(() => {
    if (!scroller.value) return

    // defensive styles
    try { scroller.value.style.overflowY = 'auto' } catch(e){}
    try { scroller.value.style.webkitOverflowScrolling = 'touch' } catch(e){}

    onScrollListener = () => {
      const el = scroller.value!
      autoScroll.value = el.scrollHeight - el.scrollTop - el.clientHeight <= 50
      showTopButton.value = el.scrollTop > 50
    }
    scroller.value.addEventListener('scroll', onScrollListener)

    scroller.value.addEventListener('pointerenter', handlePointerEnter)
    scroller.value.addEventListener('pointerleave', handlePointerLeave)

    try { scroller.value.style.pointerEvents = 'auto' } catch(e){}
    try { scroller.value.style.position = scroller.value.style.position || 'relative' } catch(e){}

    resizeObserver = new ResizeObserver(() => {
      if (autoScroll.value) requestAnimationFrame(() => scrollToBottom(true))
    })
    resizeObserver.observe(scroller.value)
  })
})

onBeforeUnmount(() => {
  if (onScrollListener && scroller.value) scroller.value.removeEventListener('scroll', onScrollListener)
  // wheel listeners were removed; nothing to detach here
  if (scroller.value) {
    scroller.value.removeEventListener('pointerenter', handlePointerEnter)
    scroller.value.removeEventListener('pointerleave', handlePointerLeave)
  }
  if (resizeObserver) resizeObserver.disconnect()
})

async function handleSend(text: string) {
  await chat.sendMessage(text)
  await nextTick()
  requestAnimationFrame(() => scrollToBottom(true))
}
</script>

<template>
  <div class="flex w-full h-full flex-col min-h-0 glass-card overflow-hidden cosmic-glow">
    <div class="flex items-center justify-between p-4 border-b border-gray-200 dark:border-white/10">
      <div class="flex items-center gap-2">
        <div class="text-2xl">💬</div>
        <div class="text-lg font-semibold gradient-text">AI Trading Assistant</div>
      </div>
      <button @click="clearConversation" class="flex items-center gap-2 px-3 py-2 rounded-xl bg-gray-100 dark:bg-white/10 border border-gray-300 dark:border-white/20 hover:bg-gray-200 dark:hover:bg-white/20 transition group" title="Clear chat">
        <svg class="w-4 h-4 text-gray-600 dark:text-gray-300 group-hover:text-gray-900 dark:group-hover:text-white transition" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="3 6 5 6 21 6"></polyline>
          <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path>
          <path d="M10 11v6"></path>
          <path d="M14 11v6"></path>
          <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"></path>
        </svg>
        <span class="text-xs font-medium text-gray-600 dark:text-gray-300 group-hover:text-gray-900 dark:group-hover:text-white transition hidden sm:inline">Clear</span>
      </button>
    </div>

    <div v-if="!ready" class="border-b border-amber-300 dark:border-amber-500/30 bg-amber-100 dark:bg-amber-500/10 px-4 py-3 flex items-center gap-3">
      <div class="text-xl">🔒</div>
      <div class="text-sm text-amber-800 dark:text-amber-200">Login to start chatting with AI</div>
    </div>

    <div class="flex-1 min-h-0 w-full overflow-hidden relative">
      <div ref="scroller" class="h-full min-h-0 space-y-4 overflow-y-auto overscroll-contain p-6 w-full chat-scroller">
        <MessageBubble
          v-for="(m, i) in messages"
          :key="m.id + i"
          :data-message-id="m.id"
          :local-id="m.id"
          :role="m.role"
          :text="m.content"
          :utility-kind="m.utilityKind"
          :utility-meta="m.utilityMeta"
          @dismiss="(id) => { if (!id) return; const idx = messages.findIndex(x => x.id === id); if (idx !== -1) messages.splice(idx, 1) }"
        />
      </div>

      <transition name="fade-scale">
        <button
          v-show="showTopButton"
          @click="scrollToTop(true)"
          class="absolute bottom-4 right-6 z-10 bg-cosmic-500 border border-cosmic-400 rounded-full p-3 shadow-lg hover:shadow-cosmic-500/50 transition cosmic-glow"
          aria-label="Scroll to top"
        >
          <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7"></path>
          </svg>
        </button>
      </transition>
    </div>

    <InputBar :disabled="!ready" @send="handleSend" />
  </div>
</template>

<style scoped>
.chat-scroller {
  scrollbar-width: thin;
  scrollbar-color: rgba(99, 102, 241, 0.5) transparent;
  scrollbar-gutter: stable;
}

.fade-scale-enter-active, .fade-scale-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}
.fade-scale-enter-from, .fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(6px);
}
.fade-scale-enter-to, .fade-scale-leave-from {
  opacity: 1;
  transform: scale(1) translateY(0);
}

.chat-scroller::-webkit-scrollbar {
  width: 10px;
}
.chat-scroller::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}
.chat-scroller::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #6366f1, #a855f7);
  border-radius: 10px;
  border: 2px solid transparent;
  background-clip: padding-box;
}
.chat-scroller::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #7c3aed, #d946ef);
}
.chat-scroller:focus,
.chat-scroller:focus-visible {
  outline: none;
  box-shadow: none;
}
</style>
