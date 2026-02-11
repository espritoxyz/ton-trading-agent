<script setup lang="ts">
import {ref} from 'vue'
import type {ChatRole, ChatUtilityKind} from "../types.ts";
import { Zap, Check, X, CheckCircle, XCircle, Wallet } from 'lucide-vue-next'

const props = defineProps<{
  role: ChatRole;
  text: string;
  utilityKind?: ChatUtilityKind;
  utilityMeta?: Record<string, any>
  localId?: string;
}>()

const emit = defineEmits<{
  (e: 'dismiss', id: string | undefined): void
  (e: 'openTopUp'): void
}>()

const acted = ref<null | 'approved' | 'declined' | 'opened'>(null)

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

async function handleOpenTopUp() {
  acted.value = 'opened'
  try {
    if (props.utilityMeta?.messageId && props.utilityMeta?.confirmationId) {
      await api.post(`/chat/messages/${props.utilityMeta.messageId}/confirmations/${props.utilityMeta.confirmationId}/approve`)
    }
  } catch {}
  emit('openTopUp')
  emit('dismiss', props.localId)
}
</script>

<template>
  <div class="w-fit max-w-[85%] rounded-2xl px-5 py-3 text-sm leading-6 transition-all duration-200 hover:scale-[1.02]"
       :class="role==='USER'
         ? 'self-end bg-gradient-to-br from-cosmic-500 to-purple-600 text-white shadow-lg shadow-cosmic-500/30'
         : 'self-start glass-card text-gray-900 dark:text-white shadow-lg'">
    <template v-if="utilityKind === 'CONFIRM_SEND_TON'">
      <div class="space-y-3">
        <div class="flex items-start gap-2">
          <Zap :size="18" class="text-amber-500 flex-shrink-0 mt-0.5" />
          <div>{{ text }}</div>
        </div>
        <div v-if="acted === null" class="flex gap-2 pt-2">
          <button @click="handleApprove" class="flex-1 rounded-xl bg-emerald-500 px-4 py-2 text-white hover:bg-emerald-600 transition font-medium flex items-center justify-center gap-1 shadow-md">
            <Check :size="16" />
            <span>Approve</span>
          </button>
          <button @click="handleDecline" class="flex-1 rounded-xl bg-rose-500 px-4 py-2 text-white hover:bg-rose-600 transition font-medium flex items-center justify-center gap-1 shadow-md">
            <X :size="16" />
            <span>Decline</span>
          </button>
        </div>
        <div v-else-if="acted==='approved'" class="flex items-center gap-2 p-3 rounded-xl bg-emerald-100 dark:bg-emerald-500/20 border border-emerald-300 dark:border-emerald-500/30">
          <CheckCircle :size="18" class="text-emerald-600 dark:text-emerald-400" />
          <span class="text-emerald-700 dark:text-emerald-300 font-medium">Approved</span>
        </div>
        <div v-else class="flex items-center gap-2 p-3 rounded-xl bg-rose-100 dark:bg-rose-500/20 border border-rose-300 dark:border-rose-500/30">
          <XCircle :size="18" class="text-rose-600 dark:text-rose-400" />
          <span class="text-rose-700 dark:text-rose-300 font-medium">Declined</span>
        </div>
      </div>
    </template>
    <template v-else-if="utilityKind === 'SHOW_TOP_UP'">
      <div class="space-y-3">
        <div class="flex items-start gap-2">
          <Wallet :size="18" class="text-emerald-500 flex-shrink-0 mt-0.5" />
          <div>{{ text }}</div>
        </div>
        <div v-if="acted === null" class="pt-2">
          <button @click="handleOpenTopUp" class="w-full rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-600 px-4 py-3 text-white hover:shadow-lg transition font-semibold flex items-center justify-center gap-2">
            <Wallet :size="18" />
            <span>Deposit</span>
          </button>
        </div>
        <div v-else-if="acted==='opened'" class="flex items-center gap-2 p-3 rounded-xl bg-emerald-100 dark:bg-emerald-500/20 border border-emerald-300 dark:border-emerald-500/30">
          <CheckCircle :size="18" class="text-emerald-600 dark:text-emerald-400" />
          <span class="text-emerald-700 dark:text-emerald-300 font-medium">Deposit dialog opened</span>
        </div>
      </div>
    </template>
    <template v-else>
      <pre v-if="role==='USER'" class="whitespace-pre-wrap font-sans">{{ text }}</pre>
      <div v-else class="prose prose-sm dark:prose-invert max-w-none message-content" v-html="text"></div>
    </template>
  </div>
</template>

<style scoped>
.message-content :deep(a) {
  color: #6366f1;
  text-decoration: underline;
  word-break: break-word;
  overflow-wrap: break-word;
  hyphens: auto;
}

.message-content :deep(a:hover) {
  color: #818cf8;
}

:global(.dark) .message-content :deep(a) {
  color: #a5b4fc;
}

:global(.dark) .message-content :deep(a:hover) {
  color: #c7d2fe;
}
</style>
