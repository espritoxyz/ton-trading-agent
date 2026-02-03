<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import { accessToken, userId } from '../composables/useAuth.ts'
import { balanceUsd, loadingBalance, refreshBalance, balanceError } from '../composables/useBalance.ts'
import { Wallet, Lock, Loader, AlertTriangle, RefreshCw, Plus } from 'lucide-vue-next'
import TopUpModal from './TopUpModal.vue'

const loggedIn = computed(() => !!accessToken.value)
const showTopUpModal = ref(false)

onMounted(async () => {
  if (loggedIn.value) await refreshBalance()
})

function handleDepositCompleted() {
  showTopUpModal.value = false
  refreshBalance()
}
</script>

<template>
  <div class="glass-card p-6 transition-all duration-300 hover:shadow-lg hover:shadow-cosmic-500/20">
    <div class="flex items-center gap-3 mb-5">
      <div class="w-12 h-12 rounded-full bg-gradient-to-br from-emerald-500 to-cyan-600 flex items-center justify-center shadow-lg">
        <Wallet :size="24" class="text-white" />
      </div>
      <div>
        <div class="text-lg font-semibold gradient-text">Wallet</div>
        <div class="text-xs text-gray-500 dark:text-gray-400">Total Balance</div>
      </div>
    </div>

    <div v-if="!loggedIn" class="flex items-center gap-3 p-4 rounded-xl bg-amber-100 dark:bg-amber-500/10 border border-amber-300 dark:border-amber-500/30">
      <Lock :size="24" class="text-amber-600 dark:text-amber-400" />
      <div>
        <div class="text-sm text-amber-800 dark:text-amber-200 font-medium">Locked</div>
        <div class="text-xs text-amber-700 dark:text-amber-300/70">Login to view balance</div>
      </div>
    </div>

    <div v-else class="space-y-5">
      <div class="p-6 rounded-xl bg-gradient-to-br from-cosmic-100 to-purple-100 dark:from-cosmic-500/20 dark:to-purple-600/20 border border-cosmic-300 dark:border-cosmic-500/30">
        <div class="text-xs text-gray-600 dark:text-gray-400 mb-3">Available Balance</div>
        <div class="flex items-baseline gap-3">
          <template v-if="loadingBalance">
            <div class="flex items-center gap-2">
              <Loader :size="24" class="animate-spin text-cosmic-500" />
              <span class="text-2xl text-gray-500 dark:text-gray-400">Loading...</span>
            </div>
          </template>
          <template v-else>
            <div class="text-4xl font-bold gradient-text">
              ${{ balanceUsd ?? '0.00' }}
            </div>
            <div class="text-lg text-gray-600 dark:text-gray-400 font-medium">USD</div>
          </template>
        </div>

        <div class="mt-4 pt-4 border-t border-gray-300 dark:border-white/10 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <div class="w-2 h-2 rounded-full bg-emerald-500 dark:bg-emerald-400 animate-pulse"></div>
            <span class="text-xs text-gray-600 dark:text-gray-400">Live</span>
          </div>
          <div class="text-xs text-gray-600 dark:text-gray-400">
            Synced with blockchain
          </div>
        </div>
      </div>

      <div v-if="balanceError" class="flex items-center gap-3 p-3 rounded-xl bg-red-100 dark:bg-red-500/10 border border-red-300 dark:border-red-500/30">
        <AlertTriangle :size="20" class="text-red-600 dark:text-red-400" />
        <div>
          <div class="text-xs font-medium text-red-700 dark:text-red-300">Error</div>
          <p class="text-xs text-red-600 dark:text-red-400/80">{{ balanceError }}</p>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3">
        <button
          class="rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-600 px-4 py-3 text-sm font-semibold text-white hover:shadow-lg hover:shadow-emerald-500/30 transition flex items-center justify-center gap-2 group"
          @click="showTopUpModal = true"
        >
          <Plus :size="16" />
          <span>Top Up</span>
        </button>
        <button
          class="rounded-xl bg-gray-100 dark:bg-white/10 px-4 py-3 text-sm font-medium text-gray-900 dark:text-white hover:bg-gray-200 dark:hover:bg-white/20 transition border border-gray-300 dark:border-white/20 flex items-center justify-center gap-2 group"
          @click="refreshBalance"
          :disabled="loadingBalance"
        >
          <component :is="loadingBalance ? Loader : RefreshCw" :size="16" :class="{ 'animate-spin': loadingBalance }" />
          <span>{{ loadingBalance ? 'Updating...' : 'Refresh' }}</span>
        </button>
      </div>
    </div>

    <TopUpModal
      v-if="showTopUpModal"
      @close="showTopUpModal = false"
      @completed="handleDepositCompleted"
    />
  </div>
</template>
