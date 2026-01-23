<script setup lang="ts">
import { computed, nextTick, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as chatModule from '../composables/useChat.ts'
import { accessToken } from '../composables/useAuth.ts'
import MessageBubble from './MessageBubble.vue'
import InputBar from './InputBar.vue'

const chat = chatModule.useChat()
const messages = chat.messages
const sending = chat.sending

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
  <div class="flex w-full h-full flex-col min-h-0 rounded-2xl border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800">
    <div v-if="!ready" class="border-b border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900 dark:border-amber-800 dark:bg-amber-900/30 dark:text-amber-200">
      Login to start chatting.
    </div>

    <div class="flex-1 min-h-0 p-2 pr-3 w-full rounded-2xl overflow-hidden relative">
      <div ref="scroller" class="h-full min-h-0 space-y-3 overflow-y-auto overscroll-contain p-4 pr-12 w-full chat-scroller">
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
          class="absolute bottom-4 right-6 z-10 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-full p-2 shadow hover:shadow-md transition"
          aria-label="Scroll to top"
        >
          <svg class="w-4 h-4 text-gray-700 dark:text-gray-200" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
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
  scrollbar-color: rgba(100,100,100,0.6) transparent;
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
  width: 12px;
}
.chat-scroller::-webkit-scrollbar-track {
  background: transparent;
}
.chat-scroller::-webkit-scrollbar-thumb {
  background-color: rgba(100,100,100,0.6);
  border-radius: 6px;
  border: 2px solid transparent;
  background-clip: padding-box;
}
.chat-scroller:focus,
.chat-scroller:focus-visible {
  outline: none;
  box-shadow: none;
}
</style>
