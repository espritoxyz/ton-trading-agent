<script setup lang="ts">
import {computed, inject, onMounted, onUnmounted, provide, ref, watch} from 'vue'
import BalanceCard from '../components/BalanceCard.vue'
import ChatPanel from '../components/ChatPanel.vue'
import AssetsList from '../components/AssetsList.vue'
import TransactionHistory from '../components/TransactionHistory.vue'
import OrdersList from '../components/OrdersList.vue'
import TopUpModal from '../components/TopUpModal.vue'
import {ClipboardList, MessageSquare, Plus, Receipt, Wallet} from 'lucide-vue-next'
import {accessToken, userId} from '../composables/useAuth.ts'
import {useWalletState} from '../composables/useWalletState.ts'
import {lastIncomingNotification} from '../composables/useNotifications.ts'

type Tab = 'overview' | 'assets' | 'transactions' | 'orders'

const DATA_REFRESH_TYPES = new Set(['BALANCE_CHANGE', 'TRANSACTION_COMPLETE', 'SWAP_EXECUTED', 'ORDER_FILLED'])

const activeTab = ref<Tab>('overview')
const loggedIn = computed(() => !!accessToken.value)

// Create wallet state instance for Assets and Transactions tabs
const walletStateInstance = useWalletState()
const {loadWalletState, refreshWalletState} = walletStateInstance

// Provide to child components
provide('walletState', walletStateInstance)

// Auto-load wallet state when userId becomes available
watch(userId, async (newUserId) => {
  if (newUserId && loggedIn.value) {
    await loadWalletState(newUserId)
  }
}, {immediate: true})

onMounted(async () => {
  if (loggedIn.value && userId.value) {
    await loadWalletState(userId.value)
  }
})

// Auto-refresh wallet data when a data-changing notification arrives via WebSocket
let refreshDebounceTimer: ReturnType<typeof setTimeout> | null = null
const stopNotificationWatch = watch(lastIncomingNotification, (notification) => {
  if (!notification || !userId.value) return
  if (!DATA_REFRESH_TYPES.has(notification.type)) return

  if (refreshDebounceTimer) clearTimeout(refreshDebounceTimer)
  refreshDebounceTimer = setTimeout(() => {
    refreshDebounceTimer = null
    refreshWalletState(userId.value!)
  }, 1000)
})

onUnmounted(() => {
  stopNotificationWatch()
  if (refreshDebounceTimer) clearTimeout(refreshDebounceTimer)
})

const tabs = [
  {id: 'overview' as const, label: 'Overview', icon: MessageSquare},
  {id: 'assets' as const, label: 'Assets', icon: Wallet},
  {id: 'transactions' as const, label: 'Transactions', icon: Receipt},
  {id: 'orders' as const, label: 'Orders', icon: ClipboardList}
]

// Provide tabs to AppLayout
const setNavigationTabs = inject<any>('setNavigationTabs', null)
if (setNavigationTabs) {
  setNavigationTabs(tabs, activeTab)
}

// Mobile wallet strip
const showMobileDeposit = ref(false)
const { balanceUsd, loadingWalletState: walletLoading } = walletStateInstance
const mobileBalance = computed(() => {
  if (walletLoading.value) return '...'
  const b = balanceUsd.value
  if (!b || isNaN(b)) return '$0.00'
  if (b >= 1_000_000) return `$${(b / 1_000_000).toFixed(2)}M`
  if (b >= 1_000) return `$${(b / 1_000).toFixed(2)}K`
  return `$${b.toFixed(2)}`
})
</script>

<template>
  <div class="h-full min-h-0">
    <!-- Overview Tab -->
    <div v-if="activeTab === 'overview'" class="h-full min-h-0 flex flex-col gap-2 overflow-hidden">
      <!-- Mobile-only compact wallet strip -->
      <div v-if="loggedIn" class="lg:hidden shrink-0 flex items-center gap-3 px-3 py-2 glass-card rounded-xl">
        <div class="w-7 h-7 rounded-full bg-gradient-to-br from-emerald-500 to-cyan-600 flex items-center justify-center shadow-sm shrink-0">
          <Wallet :size="13" class="text-white" />
        </div>
        <span class="text-base font-bold gradient-text">{{ mobileBalance }}</span>
        <div class="flex items-center gap-1">
          <div class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></div>
          <span class="text-xs text-gray-500 dark:text-gray-400">Live</span>
        </div>
        <button
          @click="showMobileDeposit = true"
          class="ml-auto shrink-0 flex items-center gap-1 text-xs px-3 py-1.5 rounded-lg bg-gradient-to-r from-emerald-500 to-cyan-600 text-white font-semibold shadow-sm"
        >
          <Plus :size="11" />
          Deposit
        </button>
      </div>

      <!-- Content grid: ChatPanel always visible, BalanceCard only on desktop -->
      <div class="flex-1 min-h-0 grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
        <div class="flex h-full flex-col min-h-0">
          <ChatPanel/>
        </div>
        <div class="hidden lg:flex h-full flex-col gap-4">
          <BalanceCard compact/>
        </div>
      </div>

      <TopUpModal v-if="showMobileDeposit" @close="showMobileDeposit = false" @completed="showMobileDeposit = false" />
    </div>

    <!-- Assets Tab -->
    <div v-else-if="activeTab === 'assets'" class="h-full overflow-auto page-scroller">
      <div class="glass-card p-6">
        <div class="flex items-center gap-3 mb-6">
          <div
              class="w-12 h-12 rounded-full bg-gradient-to-br from-emerald-500 to-cyan-600 flex items-center justify-center shadow-lg">
            <Wallet :size="24" class="text-white"/>
          </div>
          <div>
            <h2 class="text-xl font-semibold gradient-text">Your Assets</h2>
            <p class="text-sm text-gray-500 dark:text-gray-400">Manage your tokens and balances</p>
          </div>
        </div>

        <div v-if="!loggedIn" class="text-center py-12">
          <p class="text-gray-500 dark:text-gray-400">Login to view your assets</p>
        </div>
        <div v-else>
          <AssetsList :display-limit="100"/>
        </div>
      </div>
    </div>

    <!-- Transactions Tab -->
    <div v-else-if="activeTab === 'transactions'" class="h-full overflow-auto page-scroller">
      <div class="glass-card p-6">
        <div class="flex items-center gap-3 mb-6">
          <div
              class="w-12 h-12 rounded-full bg-gradient-to-br from-purple-500 to-pink-600 flex items-center justify-center shadow-lg">
            <Receipt :size="24" class="text-white"/>
          </div>
          <div>
            <h2 class="text-xl font-semibold gradient-text">Transaction History</h2>
            <p class="text-sm text-gray-500 dark:text-gray-400">Track all your wallet activity</p>
          </div>
        </div>

        <div v-if="!loggedIn" class="text-center py-12">
          <p class="text-gray-500 dark:text-gray-400">Login to view your transactions</p>
        </div>
        <div v-else>
          <TransactionHistory/>
        </div>
      </div>
    </div>

    <!-- Orders Tab -->
    <div v-else-if="activeTab === 'orders'" class="h-full overflow-auto page-scroller">
      <div class="glass-card p-6">
        <div class="flex items-center gap-3 mb-6">
          <div
              class="w-12 h-12 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg">
            <ClipboardList :size="24" class="text-white"/>
          </div>
          <div>
            <h2 class="text-xl font-semibold gradient-text">Orders</h2>
            <p class="text-sm text-gray-500 dark:text-gray-400">Track your active and fulfilled orders</p>
          </div>
        </div>

        <div v-if="!loggedIn" class="text-center py-12">
          <p class="text-gray-500 dark:text-gray-400">Login to view your orders</p>
        </div>
        <div v-else>
          <OrdersList/>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-scroller {
  scrollbar-width: thin;
  scrollbar-color: rgba(99, 102, 241, 0.4) transparent;
  scrollbar-gutter: stable;
}

.page-scroller::-webkit-scrollbar {
  width: 6px;
}

.page-scroller::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 10px;
}

.page-scroller::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #6366f1, #a855f7);
  border-radius: 10px;
}

.page-scroller::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #7c3aed, #d946ef);
}
</style>
