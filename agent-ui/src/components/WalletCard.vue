<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../composables/useApi.ts'
import type { WalletBalances, UserProfile } from '../types.ts'

const profile = ref<UserProfile | null>(null)
const wallet = ref<WalletBalances | null>(null)
const loading = ref(true)

async function load() {
  loading.value = true
  const [p, w] = await Promise.all([
    api.get('/user/profile'),
    api.get('/wallet/balances')
  ])
  profile.value = p.data
  wallet.value = w.data
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="rounded-2xl bg-white p-4 ring-1 ring-gray-200">
    <div class="text-sm text-gray-500">Agent Wallet</div>
    <div class="mt-1 text-xs text-gray-500">
      <div v-if="profile">Email: <span class="font-medium text-gray-900">{{ profile.email }}</span></div>
    </div>

    <div v-if="loading" class="mt-3 text-sm text-gray-500">Loading…</div>

    <template v-else>
      <div class="mt-3 text-xs text-gray-500">Address</div>
      <div class="truncate text-sm font-mono">{{ wallet?.address }}</div>

      <div class="mt-3 text-xs text-gray-500">Balances</div>
      <ul class="mt-1 space-y-1">
        <li v-for="b in wallet?.balances" :key="b.symbol" class="flex justify-between rounded-lg bg-gray-50 px-3 py-2">
          <span class="font-medium text-gray-900">{{ b.symbol }}</span>
          <span class="text-gray-700">{{ b.amount }}</span>
        </li>
      </ul>

      <div class="mt-4 flex gap-2">
        <button class="rounded-lg bg-gray-100 px-3 py-2 text-sm ring-1 ring-gray-200 hover:bg-gray-200"
                @click="load">
          Refresh Balances
        </button>
      </div>
    </template>
  </div>
</template>
