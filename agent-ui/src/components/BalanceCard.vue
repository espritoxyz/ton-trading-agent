<script setup lang="ts">
import {computed, onMounted, ref, provide, watch} from 'vue'
import {accessToken, userId} from '../composables/useAuth.ts'
import {useWalletState} from '../composables/useWalletState.ts'
import {AlertTriangle, Loader, Lock, Plus, RefreshCw, Wallet} from 'lucide-vue-next'
import TopUpModal from './TopUpModal.vue'
import AssetsList from './AssetsList.vue'
import TransactionHistory from './TransactionHistory.vue'

const loggedIn = computed(() => !!accessToken.value)
const showTopUpModal = ref(false)

// Create single wallet state instance and provide to children
const walletStateInstance = useWalletState()
const {
  balanceUsd,
  loadingWalletState,
  walletStateError,
  loadWalletState,
  refreshWalletState
} = walletStateInstance

// Provide to child components
provide('walletState', walletStateInstance)

const formattedBalance = computed(() => {
  if (!balanceUsd.value) return '0.00'

  const balance = balanceUsd.value

  if (isNaN(balance)) return '0.00'

  // Large numbers with abbreviations
  if (balance >= 1_000_000_000) {
    return (balance / 1_000_000_000).toFixed(2) + 'B'
  } else if (balance >= 1_000_000) {
    return (balance / 1_000_000).toFixed(2) + 'M'
  } else if (balance >= 1_000) {
    return (balance / 1_000).toFixed(2) + 'K'
  }

  // Normal numbers - limit to 2 decimal places
  if (balance >= 0.01) {
    return balance.toFixed(2)
  }

  // Very small numbers
  if (balance > 0) {
    return '< 0.01'
  }

  return '0.00'
})

// Auto-load wallet state when userId becomes available
watch(userId, async (newUserId) => {
  if (newUserId && loggedIn.value) {
    await loadWalletState(newUserId)
  }
}, { immediate: true })

onMounted(async () => {
  if (loggedIn.value && userId.value) {
    await loadWalletState(userId.value)
  }
})

async function handleDepositCompleted() {
  showTopUpModal.value = false
  if (userId.value) {
    await refreshWalletState(userId.value)
  }
}

async function handleRefresh() {
  if (userId.value) {
    await refreshWalletState(userId.value)
  }
}
</script>

<template>
  <div class="glass-card p-6 transition-all duration-300 hover:shadow-lg hover:shadow-cosmic-500/20">
    <div class="flex items-center gap-3 mb-5">
      <div
          class="w-12 h-12 rounded-full bg-gradient-to-br from-emerald-500 to-cyan-600 flex items-center justify-center shadow-lg">
        <Wallet :size="24" class="text-white"/>
      </div>
      <div>
        <div class="text-lg font-semibold gradient-text">Wallet</div>
        <div class="text-xs text-gray-500 dark:text-gray-400">Total Balance</div>
      </div>
    </div>

    <div v-if="!loggedIn"
         class="flex items-center gap-3 p-4 rounded-xl bg-amber-100 dark:bg-amber-500/10 border border-amber-300 dark:border-amber-500/30">
      <Lock :size="24" class="text-amber-600 dark:text-amber-400"/>
      <div>
        <div class="text-sm text-amber-800 dark:text-amber-200 font-medium">Locked</div>
        <div class="text-xs text-amber-700 dark:text-amber-300/70">Login to view balance</div>
      </div>
    </div>

    <div v-else class="space-y-5">
      <div
          class="p-6 rounded-xl bg-gradient-to-br from-cosmic-100 to-purple-100 dark:from-cosmic-500/20 dark:to-purple-600/20 border border-cosmic-300 dark:border-cosmic-500/30">
        <div class="text-xs text-gray-600 dark:text-gray-400 mb-3">Available Balance</div>
        <div class="flex items-baseline gap-3">
          <template v-if="loadingWalletState">
            <div class="flex items-center gap-2">
              <Loader :size="24" class="animate-spin text-cosmic-500"/>
              <span class="text-2xl text-gray-500 dark:text-gray-400">Loading...</span>
            </div>
          </template>
          <template v-else>
            <div class="text-4xl font-bold gradient-text">
              ${{ formattedBalance }}
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

      <div v-if="walletStateError"
           class="flex items-center gap-3 p-3 rounded-xl bg-red-100 dark:bg-red-500/10 border border-red-300 dark:border-red-500/30">
        <AlertTriangle :size="20" class="text-red-600 dark:text-red-400"/>
        <div>
          <div class="text-xs font-medium text-red-700 dark:text-red-300">Error</div>
          <p class="text-xs text-red-600 dark:text-red-400/80">{{ walletStateError }}</p>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3">
        <button
            class="rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-600 px-4 py-3 text-sm font-semibold text-white hover:shadow-lg hover:shadow-emerald-500/30 transition flex items-center justify-center gap-2 group"
            @click="showTopUpModal = true"
        >
          <Plus :size="16"/>
          <span>Deposit</span>
        </button>
        <button
            class="rounded-xl bg-gray-100 dark:bg-white/10 px-4 py-3 text-sm font-medium text-gray-900 dark:text-white hover:bg-gray-200 dark:hover:bg-white/20 transition border border-gray-300 dark:border-white/20 flex items-center justify-center gap-2 group"
            @click="handleRefresh"
            :disabled="loadingWalletState"
        >
          <component :is="loadingWalletState ? Loader : RefreshCw" :size="16" :class="{ 'animate-spin': loadingWalletState }"/>
          <span>{{ loadingWalletState ? 'Updating...' : 'Refresh' }}</span>
        </button>
      </div>

      <!-- Assets List Section -->
      <div class="mt-6 pt-6 border-t border-gray-200 dark:border-white/10">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-gray-900 dark:text-white">Your Assets</h3>
        </div>
        <AssetsList :display-limit="5" />
      </div>

      <!-- Transaction History Section -->
      <div class="mt-6 pt-6 border-t border-gray-200 dark:border-white/10">
        <TransactionHistory />
      </div>
    </div>

    <TopUpModal
        v-if="showTopUpModal"
        @close="showTopUpModal = false"
        @completed="handleDepositCompleted"
    />
  </div>
</template>
