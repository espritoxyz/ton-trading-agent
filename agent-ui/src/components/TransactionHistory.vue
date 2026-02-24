<template>
  <div class="transaction-history">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Transaction History</h3>
    </div>

    <!-- Filters -->
    <div class="filters flex flex-wrap gap-2 mb-4">
      <select
          v-model="filters.assetType"
          class="px-3 py-1.5 text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white border border-gray-200 dark:border-gray-700 rounded-lg focus:outline-none focus:border-cyan-500 transition-colors"
      >
        <option value="ALL">All Assets</option>
        <option value="TON">TON</option>
        <option value="JETTON">Tokens</option>
      </select>

      <select
          v-model="filters.kind"
          class="px-3 py-1.5 text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white border border-gray-200 dark:border-gray-700 rounded-lg focus:outline-none focus:border-cyan-500 transition-colors"
      >
        <option value="ALL">All Types</option>
        <option value="INCOMING">Incoming</option>
        <option value="OUTGOING">Outgoing</option>
        <option value="SWAP">Swaps</option>
      </select>
    </div>

    <!-- Loading State -->
    <div v-if="loadingTransactions" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-cyan-500"></div>
    </div>

    <!-- Error State -->
    <div v-else-if="transactionsError" class="text-red-600 dark:text-red-400 text-sm p-4 bg-red-100 dark:bg-red-900/20 rounded-lg">
      {{ transactionsError }}
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredActivity.length === 0" class="text-gray-600 dark:text-gray-400 text-sm text-center py-8">
      <div class="flex flex-col items-center gap-2">
        <svg class="w-12 h-12 text-gray-400 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        <p>No transactions found</p>
      </div>
    </div>

    <!-- Activity List -->
    <div v-else class="space-y-2">
      <template v-for="item in paginatedActivity" :key="item.itemType + '-' + item.id">

        <!-- Swap item -->
        <div
            v-if="item.itemType === 'swap'"
            class="transaction-item p-3 rounded-lg bg-gray-100 dark:bg-gray-800/50 hover:bg-gray-200 dark:hover:bg-gray-700/50 transition-colors"
        >
          <div class="flex items-start gap-3">
            <!-- Swap Icon -->
            <div class="flex-shrink-0 mt-1">
              <div class="w-8 h-8 rounded-full flex items-center justify-center bg-purple-500/20 text-purple-400">
                <ArrowLeftRight class="w-4 h-4" />
              </div>
            </div>

            <!-- Swap Details -->
            <div class="flex-1 min-w-0">
              <!-- Amounts -->
              <div class="flex items-baseline gap-2 mb-1">
                <span class="font-mono font-semibold text-gray-900 dark:text-white">
                  {{ item.fromAmount }} {{ item.fromAsset }}
                </span>
                <span class="text-xs text-gray-500 dark:text-gray-500">→</span>
                <span class="font-mono font-semibold text-gray-900 dark:text-white">
                  {{ item.toAmount }} {{ item.toAsset }}
                </span>
              </div>

              <!-- Label -->
              <div class="text-xs text-purple-400 mb-1">Swap</div>

              <!-- Date and Link -->
              <div class="flex items-center gap-3 text-xs">
                <span class="text-gray-500 dark:text-gray-500">{{ formatDate(item.createdAt) }}</span>
                <a
                    v-if="item.transactionId"
                    :href="getTonViewerUrl(item.transactionId)"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-cyan-400 hover:text-cyan-300 transition-colors flex items-center gap-1"
                >
                  <span>View</span>
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
                  </svg>
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- Transaction item -->
        <div
            v-else
            class="transaction-item p-3 rounded-lg bg-gray-100 dark:bg-gray-800/50 hover:bg-gray-200 dark:hover:bg-gray-700/50 transition-colors"
        >
          <div class="flex items-start gap-3">
            <!-- Direction Icon -->
            <div class="flex-shrink-0 mt-1">
              <div
                  :class="[
                    'w-8 h-8 rounded-full flex items-center justify-center',
                    item.direction === 'INCOMING' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                  ]"
              >
                <svg v-if="item.direction === 'INCOMING'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3"/>
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"/>
                </svg>
              </div>
            </div>

            <!-- Transaction Details -->
            <div class="flex-1 min-w-0">
              <!-- Amount and Asset -->
              <div class="flex items-baseline gap-2 mb-1">
                <span class="font-mono font-semibold text-gray-900 dark:text-white">
                  {{ item.direction === 'INCOMING' ? '+' : '-' }}{{ formatAmount(item.amountNano, item.jettonDecimals || 9) }}
                </span>
                <span class="text-sm text-gray-600 dark:text-gray-400">
                  {{ item.assetType === 'TON' ? 'TON' : (item.jettonSymbol || 'Token') }}
                </span>
              </div>

              <!-- Addresses -->
              <div class="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400 mb-1">
                <span class="truncate">
                  {{ item.direction === 'INCOMING' ? 'From:' : 'To:' }}
                  {{ formatAddress(item.direction === 'INCOMING' ? item.senderAddress : item.recipientAddress) }}
                </span>
              </div>

              <!-- Comment (if exists) -->
              <div v-if="item.comment" class="text-xs text-gray-500 dark:text-gray-500 italic mb-1 truncate">
                "{{ item.comment }}"
              </div>

              <!-- Date and Link -->
              <div class="flex items-center gap-3 text-xs">
                <span class="text-gray-500 dark:text-gray-500">{{ formatDate(item.createdAt) }}</span>
                <a
                    :href="getTonViewerUrl(item.transactionHash)"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-cyan-400 hover:text-cyan-300 transition-colors flex items-center gap-1"
                >
                  <span>View</span>
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
                  </svg>
                </a>
              </div>
            </div>
          </div>
        </div>

      </template>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-4 pt-4">
        <button
            @click="prevPage"
            :disabled="currentPage === 1"
            :class="[
              'px-3 py-1.5 text-sm rounded-lg transition-colors',
              currentPage === 1
                ? 'bg-gray-200 dark:bg-gray-800 text-gray-400 dark:text-gray-600 cursor-not-allowed'
                : 'bg-gray-200 dark:bg-gray-800 text-cyan-600 dark:text-cyan-400 hover:bg-gray-300 dark:hover:bg-gray-700'
            ]"
        >
          Previous
        </button>

        <span class="text-sm text-gray-600 dark:text-gray-400">
          Page {{ currentPage }} of {{ totalPages }}
        </span>

        <button
            @click="nextPage"
            :disabled="currentPage === totalPages"
            :class="[
              'px-3 py-1.5 text-sm rounded-lg transition-colors',
              currentPage === totalPages
                ? 'bg-gray-200 dark:bg-gray-800 text-gray-400 dark:text-gray-600 cursor-not-allowed'
                : 'bg-gray-200 dark:bg-gray-800 text-cyan-600 dark:text-cyan-400 hover:bg-gray-300 dark:hover:bg-gray-700'
            ]"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref, watch} from 'vue'
import {ArrowLeftRight} from 'lucide-vue-next'
import type {SwapData, Transaction} from '../types'

// Inject wallet state from parent
const walletState = inject<any>('walletState')
if (!walletState) {
  throw new Error('WalletState not provided')
}

const {
  transactions,
  swaps,
  loadingWalletState: loadingTransactions,
  walletStateError: transactionsError
} = walletState

// Discriminated union for combined activity items
type TransactionActivity = (Transaction & { itemType: 'transaction' })
type SwapActivity = (SwapData & { itemType: 'swap' })
type ActivityItem = TransactionActivity | SwapActivity

/**
 * Format amount from nano to readable format
 */
const formatAmount = (amountNano: string | number, decimals: number = 9): string => {
  const amount = Number(amountNano) / Math.pow(10, decimals)
  if (decimals === 9) {
    // TON - show 4 decimals
    return amount.toFixed(4)
  } else {
    // Jetton - show 2 decimals
    return amount.toFixed(2)
  }
}

/**
 * Format address to short form
 */
const formatAddress = (address?: string): string => {
  if (!address) return 'Unknown'
  if (address.length <= 12) return address
  return `${address.substring(0, 6)}...${address.substring(address.length - 6)}`
}

/**
 * Get TON Viewer URL for transaction
 */
const getTonViewerUrl = (hash: string): string => {
  return `https://tonviewer.com/transaction/${hash}`
}

/**
 * Format date to readable format
 */
const formatDate = (dateString: string): string => {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Filters
const filters = ref({
  assetType: 'ALL' as 'ALL' | 'TON' | 'JETTON',
  kind: 'ALL' as 'ALL' | 'INCOMING' | 'OUTGOING' | 'SWAP'
})

// Pagination
const currentPage = ref(1)
const pageSize = 20

// Combined and sorted activity list
const allActivity = computed<ActivityItem[]>(() => {
  const txItems: ActivityItem[] = (transactions.value as Transaction[]).map(
      (tx) => ({...tx, itemType: 'transaction' as const})
  )
  const swapItems: ActivityItem[] = (swaps.value as SwapData[]).map(
      (sw) => ({...sw, itemType: 'swap' as const})
  )
  return [...txItems, ...swapItems].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  )
})

// Filtered activity
const filteredActivity = computed<ActivityItem[]>(() => {
  return allActivity.value.filter((item) => {
    if (item.itemType === 'swap') {
      // Swaps are excluded when filtering for INCOMING or OUTGOING only
      if (filters.value.kind === 'INCOMING' || filters.value.kind === 'OUTGOING') return false

      // Asset type filter for swaps: match if either leg involves the asset type
      if (filters.value.assetType === 'TON') {
        return item.fromAsset === 'TON' || item.toAsset === 'TON'
      }
      if (filters.value.assetType === 'JETTON') {
        return item.fromAsset !== 'TON' || item.toAsset !== 'TON'
      }

      return true
    }

    // Transaction item
    // Exclude when SWAP filter is active
    if (filters.value.kind === 'SWAP') return false

    if (filters.value.assetType !== 'ALL' && item.assetType !== filters.value.assetType) {
      return false
    }
    if (filters.value.kind !== 'ALL' && item.direction !== filters.value.kind) {
      return false
    }

    return true
  })
})

// Paginated activity
const paginatedActivity = computed<ActivityItem[]>(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return filteredActivity.value.slice(start, end)
})

// Total pages
const totalPages = computed(() => {
  return Math.ceil(filteredActivity.value.length / pageSize)
})

// Reset to page 1 when filters change
watch(filters, () => { currentPage.value = 1 }, {deep: true})

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}
</script>

<style scoped>
.transaction-item {
  border: 1px solid rgba(99, 102, 241, 0.15);
}

:global(.dark) .transaction-item {
  border-color: rgba(99, 102, 241, 0.1);
}

.transaction-item:hover {
  border-color: rgba(99, 102, 241, 0.4);
}

:global(.dark) .transaction-item:hover {
  border-color: rgba(99, 102, 241, 0.3);
}
</style>
