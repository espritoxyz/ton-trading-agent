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
  <div class="glass-card p-6 transition-all duration-300 hover:shadow-lg hover:shadow-cosmic-500/20">
    <div class="flex items-center gap-2 mb-4">
      <div class="text-2xl">💰</div>
      <div class="text-lg font-semibold gradient-text">Balance</div>
    </div>

    <div v-if="!loggedIn" class="flex items-center gap-3 p-4 rounded-xl bg-amber-500/10 border border-amber-500/30">
      <div class="text-2xl">🔒</div>
      <div class="text-sm text-amber-200">Login to view your balance</div>
    </div>

    <div v-else class="space-y-4">
      <div class="space-y-1">
        <div class="text-xs text-gray-400">User ID</div>
        <div class="text-xs font-mono text-gray-300 bg-white/5 px-2 py-1 rounded">{{ userId ?? '—' }}</div>
      </div>

      <div class="space-y-2">
        <div class="text-xs text-gray-400">Total Balance</div>
        <div class="flex items-baseline gap-2">
          <div class="text-3xl font-bold gradient-text">
            <template v-if="loadingBalance">
              <span class="animate-pulse">...</span>
            </template>
            <template v-else>${{ balanceUsd ?? '0.00' }}</template>
          </div>
          <div class="text-sm text-gray-400">USD</div>
        </div>
      </div>

      <div v-if="balanceError" class="flex items-center gap-2 p-3 rounded-xl bg-red-500/10 border border-red-500/30">
        <div class="text-lg">⚠️</div>
        <p class="text-xs text-red-300">{{ balanceError }}</p>
      </div>

      <div class="pt-2">
        <button
          class="w-full rounded-xl bg-white/10 px-4 py-3 text-sm font-medium text-white hover:bg-white/20 transition border border-white/20 flex items-center justify-center gap-2"
          @click="refreshBalance"
          :disabled="loadingBalance"
        >
          <span v-if="loadingBalance" class="animate-spin">⟳</span>
          <span v-else>🔄</span>
          <span>{{ loadingBalance ? 'Refreshing...' : 'Refresh Balance' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>
