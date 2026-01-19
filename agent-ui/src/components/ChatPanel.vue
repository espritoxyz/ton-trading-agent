<script setup lang="ts">
import { computed, nextTick, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as chatModule from '../composables/useChat.ts'
import { accessToken } from '../composables/useAuth.ts'
import MessageBubble from './MessageBubble.vue'
import InputBar from './InputBar.vue'

const chat = chatModule.useChat()
const messages = chat.messages
const sending  = chat.sending

const scroller = ref<HTMLDivElement | null>(null)
const ready = computed(() => !!accessToken.value)

const autoScroll = ref(true)

function scrollToBottom(smooth = true) {
  const el = scroller.value
  if (!el) return

  const items = el.querySelectorAll('[data-message-id]')
  const last = items.length ? (items[items.length - 1] as HTMLElement) : null

  if (last) {
    try {
      last.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto', block: 'end' })
      return
    } catch {}
  }

  try {
    el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
  } catch {}
}

watch(() => messages.length, async () => {
  await nextTick()
  requestAnimationFrame(() => requestAnimationFrame(() => {
    if (autoScroll.value) scrollToBottom(true)
  }))
})

let resizeObserver: ResizeObserver | null = null
let onScrollListener: ((e: Event) => void) | null = null
let wheelListener: ((e: WheelEvent) => void) | null = null
let windowWheelListener: ((e: WheelEvent) => void) | null = null
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
    }
    scroller.value.addEventListener('scroll', onScrollListener)

    scroller.value.addEventListener('pointerenter', handlePointerEnter)
    scroller.value.addEventListener('pointerleave', handlePointerLeave)

    try { scroller.value.style.pointerEvents = 'auto' } catch(e){}
    try { scroller.value.style.position = scroller.value.style.position || 'relative' } catch(e){}

    wheelListener = (e: WheelEvent) => {
      const el = scroller.value!
      if (!el) return
      const delta = e.deltaY
      const canScrollDown = el.scrollTop + el.clientHeight < el.scrollHeight
      const canScrollUp = el.scrollTop > 0
      if ((delta > 0 && canScrollDown) || (delta < 0 && canScrollUp)) {
        el.scrollTop += delta
        e.preventDefault()
      }
    }
    scroller.value.addEventListener('wheel', wheelListener as EventListener, { passive: false, capture: true })

    windowWheelListener = (e: WheelEvent) => {
      if (!isHover.value) return
      const el = scroller.value!
      if (!el) return
      const delta = e.deltaY
      const canScrollDown = el.scrollTop + el.clientHeight < el.scrollHeight
      const canScrollUp = el.scrollTop > 0
      if ((delta > 0 && canScrollDown) || (delta < 0 && canScrollUp)) {
        el.scrollTop += delta
        e.preventDefault()
      }
    }
    window.addEventListener('wheel', windowWheelListener as EventListener, { passive: false, capture: true })

    resizeObserver = new ResizeObserver(() => {
      if (autoScroll.value) requestAnimationFrame(() => scrollToBottom(false))
    })
    resizeObserver.observe(scroller.value)
  })
})

onBeforeUnmount(() => {
  if (onScrollListener && scroller.value) scroller.value.removeEventListener('scroll', onScrollListener)
  if (wheelListener && scroller.value) scroller.value.removeEventListener('wheel', wheelListener as EventListener)
  if (scroller.value) {
    scroller.value.removeEventListener('pointerenter', handlePointerEnter)
    scroller.value.removeEventListener('pointerleave', handlePointerLeave)
  }
  if (windowWheelListener) window.removeEventListener('wheel', windowWheelListener as EventListener)
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

    <div class="flex-1 min-h-0 p-2 pr-3 w-full rounded-2xl overflow-hidden">
      <div ref="scroller" class="h-full min-h-0 space-y-3 overflow-y-auto overscroll-contain p-4 pr-12 w-full chat-scroller">
        <MessageBubble
          v-for="(m, i) in messages"
          :key="m.id + i"
          :data-message-id="m.id"
          :local-id="m.id"
          :role="m.role"
          :text="m.content"
          :utilityKind="m.utilityKind"
          :utilityMeta="m.utilityMeta"
          @dismiss="(id) => { if (!id) return; const idx = messages.findIndex(x => x.id === id); if (idx !== -1) messages.splice(idx, 1) }"
        />
        <div v-if="sending" class="text-xs text-gray-500 dark:text-gray-400">Sending…</div>
      </div>
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
