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
    <div class="flex items-center gap-3 mb-5">
      <div class="w-12 h-12 rounded-full bg-gradient-to-br from-emerald-500 to-cyan-600 flex items-center justify-center text-xl">
        💰
      </div>
      <div>
        <div class="text-lg font-semibold gradient-text">Wallet</div>
        <div class="text-xs text-gray-400">Total Balance</div>
      </div>
    </div>

    <div v-if="!loggedIn" class="flex items-center gap-3 p-4 rounded-xl bg-amber-500/10 border border-amber-500/30">
      <div class="text-2xl">🔒</div>
      <div>
        <div class="text-sm text-amber-200 font-medium">Locked</div>
        <div class="text-xs text-amber-300/70">Login to view balance</div>
      </div>
    </div>

    <div v-else class="space-y-5">
      <div class="p-6 rounded-xl bg-gradient-to-br from-cosmic-500/20 to-purple-600/20 border border-cosmic-500/30">
        <div class="text-xs text-gray-400 mb-3">Available Balance</div>
        <div class="flex items-baseline gap-3">
          <template v-if="loadingBalance">
            <div class="flex items-center gap-2">
              <span class="animate-spin text-2xl">⟳</span>
              <span class="text-2xl text-gray-400">Loading...</span>
            </div>
          </template>
          <template v-else>
            <div class="text-4xl font-bold gradient-text">
              ${{ balanceUsd ?? '0.00' }}
            </div>
            <div class="text-lg text-gray-400 font-medium">USD</div>
          </template>
        </div>

        <div class="mt-4 pt-4 border-t border-white/10 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <div class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></div>
            <span class="text-xs text-gray-400">Live</span>
          </div>
          <div class="text-xs text-gray-400">
            Synced with blockchain
          </div>
        </div>
      </div>

      <div v-if="balanceError" class="flex items-center gap-3 p-3 rounded-xl bg-red-500/10 border border-red-500/30">
        <div class="text-xl">⚠️</div>
        <div>
          <div class="text-xs font-medium text-red-300">Error</div>
          <p class="text-xs text-red-400/80">{{ balanceError }}</p>
        </div>
      </div>

      <button
        class="w-full rounded-xl bg-white/10 px-4 py-3 text-sm font-medium text-white hover:bg-white/20 transition border border-white/20 flex items-center justify-center gap-2 group"
        @click="refreshBalance"
        :disabled="loadingBalance"
      >
        <span class="text-base" :class="{ 'animate-spin': loadingBalance }">
          {{ loadingBalance ? '⟳' : '🔄' }}
        </span>
        <span>{{ loadingBalance ? 'Updating...' : 'Refresh Balance' }}</span>
      </button>
    </div>
  </div>
</template>
