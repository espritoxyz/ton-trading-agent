<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import * as chatModule from '../composables/useChat.ts'
import { accessToken } from '../composables/useAuth.ts'
import MessageBubble from './MessageBubble.vue'
import InputBar from './InputBar.vue'

const chat = chatModule.useChat()
const messages = chat.messages
const sending  = chat.sending

const scroller = ref<HTMLDivElement | null>(null)
const ready = computed(() => !!accessToken.value)

async function handleSend(text: string) {
  await chat.sendMessage(text)
  await nextTick()
  scroller.value?.scrollTo({ top: scroller.value.scrollHeight, behavior: 'smooth' })
}
</script>

<template>
  <div class="flex h-full flex-col rounded-2xl bg-white ring-1 ring-gray-200">
    <div v-if="!ready" class="border-b border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900">
      Login to start chatting.
    </div>

    <div ref="scroller" class="flex-1 space-y-3 overflow-y-auto p-4">
      <MessageBubble
        v-for="(m, i) in messages"
        :key="m.id + i"
        :local-id="m.id"
        :role="m.role"
        :text="m.content"
        :utilityKind="m.utilityKind"
        :utilityMeta="m.utilityMeta"
        @dismiss="(id) => { if (!id) return; const idx = messages.findIndex(x => x.id === id); if (idx !== -1) messages.splice(idx, 1) }"
      />
      <div v-if="sending" class="text-xs text-gray-500">Sending…</div>
    </div>

    <InputBar :disabled="!ready" @send="handleSend" />
  </div>
</template>
