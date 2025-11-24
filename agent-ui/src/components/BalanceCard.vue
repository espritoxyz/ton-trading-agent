<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { accessToken, userId } from '../composables/useAuth.ts'
import { balanceUsd, loadingBalance, refreshBalance, balanceError } from '../composables/useBalance.ts'

const loggedIn = computed(() => !!accessToken.value)
onMounted(async () => {
  if (loggedIn.value) await refreshBalance()
})
</script>

<template>
  <div class="rounded-2xl bg-white p-4 ring-1 ring-gray-200">
    <div class="text-sm font-semibold text-gray-900">Balance</div>

    <div v-if="!loggedIn" class="mt-2 text-sm text-gray-500">
      Login to see your balance.
    </div>

    <div v-else class="mt-3">
      <div class="text-xs text-gray-500">User ID</div>
      <div class="text-sm font-mono text-gray-900">{{ userId ?? '—' }}</div>

      <div class="mt-3 text-xs text-gray-500">USD Total</div>
      <div class="text-lg text-gray-900">
        <template v-if="loadingBalance">Loading…</template>
        <template v-else>{{ balanceUsd ?? 0 }}</template>
      </div>

      <p v-if="balanceError" class="mt-2 text-xs text-red-600">{{ balanceError }}</p>

      <div class="mt-4">
        <button class="rounded-lg bg-gray-100 px-3 py-2 text-sm ring-1 ring-gray-200 hover:bg-gray-200"
                @click="refreshBalance">
          Refresh Balance
        </button>
      </div>
    </div>
  </div>
</template>
