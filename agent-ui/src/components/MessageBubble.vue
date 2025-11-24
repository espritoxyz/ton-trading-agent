<script setup lang="ts">
import type {ChatRole} from "../types.ts";

defineProps<{ role: ChatRole; text: string }>()
</script>

<template>
  <div class="max-w-[85%] rounded-2xl px-4 py-2 text-sm leading-6"
       :class="role==='USER' ? 'self-end bg-indigo-600 text-white'
                             : 'self-start bg-white text-gray-900 shadow-sm ring-1 ring-gray-200'">
    <pre class="whitespace-pre-wrap font-sans">{{ text }}</pre>
  </div>
</template>
