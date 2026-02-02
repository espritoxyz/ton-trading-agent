<script setup lang="ts">
import {ref} from 'vue'
import type {ChatRole, ChatUtilityKind} from "../types.ts";

const props = defineProps<{
  role: ChatRole;
  text: string;
  utilityKind?: ChatUtilityKind;
  utilityMeta?: Record<string, any>
  localId?: string;
}>()

const emit = defineEmits<{
  (e: 'dismiss', id: string | undefined): void
}>()

const acted = ref<null | 'approved' | 'declined'>(null)

import { api } from '../composables/useApi.ts'

async function handleApprove() {
  acted.value = 'approved'
  try {
    if (props.utilityMeta?.messageId && props.utilityMeta?.confirmationId) {
      await api.post(`/chat/messages/${props.utilityMeta.messageId}/confirmations/${props.utilityMeta.confirmationId}/approve`)
    }
  } catch {}
  // remove this utility bubble after action
  emit('dismiss', props.localId)
}

async function handleDecline() {
  acted.value = 'declined'
  try {
    if (props.utilityMeta?.messageId && props.utilityMeta?.confirmationId) {
      await api.post(`/chat/messages/${props.utilityMeta.messageId}/confirmations/${props.utilityMeta.confirmationId}/decline`)
    }
  } catch {}
  emit('dismiss', props.localId)
}
</script>

<template>
  <div class="w-fit max-w-[85%] rounded-2xl px-5 py-3 text-sm leading-6 transition-all duration-200 hover:scale-[1.02]"
       :class="role==='USER'
         ? 'self-end bg-gradient-to-br from-cosmic-500 to-purple-600 text-white shadow-lg shadow-cosmic-500/30'
         : 'self-start glass-card text-white shadow-lg'">
    <template v-if="utilityKind === 'CONFIRM_SEND_TON'">
      <div class="space-y-3">
        <div class="flex items-start gap-2">
          <div class="text-lg">⚡</div>
          <div>{{ text }}</div>
        </div>
        <div v-if="acted === null" class="flex gap-2 pt-2">
          <button @click="handleApprove" class="flex-1 rounded-xl bg-emerald-500 px-4 py-2 text-white hover:bg-emerald-600 transition font-medium flex items-center justify-center gap-1">
            <span>✓</span>
            <span>Approve</span>
          </button>
          <button @click="handleDecline" class="flex-1 rounded-xl bg-rose-500 px-4 py-2 text-white hover:bg-rose-600 transition font-medium flex items-center justify-center gap-1">
            <span>✕</span>
            <span>Decline</span>
          </button>
        </div>
        <div v-else-if="acted==='approved'" class="flex items-center gap-2 p-3 rounded-xl bg-emerald-500/20 border border-emerald-500/30">
          <span>✅</span>
          <span class="text-emerald-300 font-medium">Approved</span>
        </div>
        <div v-else class="flex items-center gap-2 p-3 rounded-xl bg-rose-500/20 border border-rose-500/30">
          <span>❌</span>
          <span class="text-rose-300 font-medium">Declined</span>
        </div>
      </div>
    </template>
    <template v-else>
      <pre v-if="role==='USER'" class="whitespace-pre-wrap font-sans">{{ text }}</pre>
      <div v-else class="prose prose-sm prose-invert max-w-none" v-html="text"></div>
    </template>
  </div>
</template>
