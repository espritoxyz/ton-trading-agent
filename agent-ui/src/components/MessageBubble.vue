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
  <div class="max-w-[85%] rounded-2xl px-4 py-2 text-sm leading-6"
       :class="role==='USER' ? 'self-end bg-indigo-600 text-white'
                             : 'self-start bg-white text-gray-900 shadow-sm ring-1 ring-gray-200'">
    <template v-if="utilityKind === 'CONFIRM_SEND_TON'">
      <div class="space-y-2">
        <div>{{ text }}</div>
        <div v-if="acted === null" class="flex gap-2">
          <button @click="handleApprove" class="rounded-md bg-emerald-600 px-2 py-1 text-white hover:bg-emerald-700">✔
          </button>
          <button @click="handleDecline" class="rounded-md bg-rose-600 px-2 py-1 text-white hover:bg-rose-700">✖
          </button>
        </div>
        <div v-else-if="acted==='approved'" class="text-emerald-700">Approved</div>
        <div v-else class="text-rose-700">Declined</div>
      </div>
    </template>
    <template v-else>
      <pre class="whitespace-pre-wrap font-sans">{{ text }}</pre>
    </template>
  </div>
</template>
