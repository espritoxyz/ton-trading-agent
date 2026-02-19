<script setup lang="ts">
import {computed, inject, onMounted, provide, ref, watch} from 'vue'
import BalanceCard from '../components/BalanceCard.vue'
import ChatPanel from '../components/ChatPanel.vue'
import AssetsList from '../components/AssetsList.vue'
import TransactionHistory from '../components/TransactionHistory.vue'
import OrdersList from '../components/OrdersList.vue'
import {ClipboardList, MessageSquare, Receipt, Wallet} from 'lucide-vue-next'
import {accessToken, userId} from '../composables/useAuth.ts'
import {useWalletState} from '../composables/useWalletState.ts'

type Tab = 'overview' | 'assets' | 'transactions' | 'orders'

const activeTab = ref<Tab>('overview')
const loggedIn = computed(() => !!accessToken.value)

// Create wallet state instance for Assets and Transactions tabs
const walletStateInstance = useWalletState()
const {loadWalletState} = walletStateInstance

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
</script>

<template>
  <div class="h-full min-h-0">
    <!-- Overview Tab -->
    <div v-if="activeTab === 'overview'" class="grid h-full gap-4 lg:grid-cols-[1.2fr_0.8fr]">
      <div class="flex h-full min-h-0">
        <ChatPanel/>
      </div>
      <div class="flex h-full flex-col gap-4">
        <BalanceCard compact/>
      </div>
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
