<template>
  <div class="transaction-history">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-white">Transaction History</h3>
    </div>

    <!-- Filters -->
    <div class="filters flex flex-wrap gap-2 mb-4">
      <select
          v-model="filters.assetType"
          class="px-3 py-1.5 text-sm bg-gray-800 text-white border border-gray-700 rounded-lg focus:outline-none focus:border-cyan-500 transition-colors"
      >
        <option value="ALL">All Assets</option>
        <option value="TON">TON</option>
        <option value="JETTON">Tokens</option>
      </select>

      <select
          v-model="filters.direction"
          class="px-3 py-1.5 text-sm bg-gray-800 text-white border border-gray-700 rounded-lg focus:outline-none focus:border-cyan-500 transition-colors"
      >
        <option value="ALL">All Directions</option>
        <option value="INCOMING">Incoming</option>
        <option value="OUTGOING">Outgoing</option>
      </select>
    </div>

    <!-- Loading State -->
    <div v-if="loadingTransactions" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-cyan-500"></div>
    </div>

    <!-- Error State -->
    <div v-else-if="transactionsError" class="text-red-400 text-sm p-4 bg-red-900/20 rounded-lg">
      {{ transactionsError }}
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredTransactions.length === 0" class="text-gray-400 text-sm text-center py-8">
      <div class="flex flex-col items-center gap-2">
        <svg class="w-12 h-12 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        <p>No transactions found</p>
      </div>
    </div>

    <!-- Transactions List -->
    <div v-else class="space-y-2">
      <div
          v-for="tx in paginatedTransactions"
          :key="tx.id"
          class="transaction-item p-3 rounded-lg bg-gray-800/50 hover:bg-gray-700/50 transition-colors"
      >
        <div class="flex items-start gap-3">
          <!-- Direction Icon -->
          <div class="flex-shrink-0 mt-1">
            <div
                :class="[
                  'w-8 h-8 rounded-full flex items-center justify-center',
                  tx.direction === 'INCOMING' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                ]"
            >
              <svg v-if="tx.direction === 'INCOMING'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
              <span class="font-mono font-semibold text-white">
                {{ tx.direction === 'INCOMING' ? '+' : '-' }}{{ formatAmount(tx.amountNano, tx.jettonDecimals || 9) }}
              </span>
              <span class="text-sm text-gray-400">
                {{ tx.assetType === 'TON' ? 'TON' : (tx.jettonSymbol || 'Token') }}
              </span>
            </div>

            <!-- Addresses -->
            <div class="flex items-center gap-2 text-xs text-gray-400 mb-1">
              <span class="truncate">
                {{ tx.direction === 'INCOMING' ? 'From:' : 'To:' }}
                {{ formatAddress(tx.direction === 'INCOMING' ? tx.senderAddress : tx.recipientAddress) }}
              </span>
            </div>

            <!-- Comment (if exists) -->
            <div v-if="tx.comment" class="text-xs text-gray-500 italic mb-1 truncate">
              "{{ tx.comment }}"
            </div>

            <!-- Date and Link -->
            <div class="flex items-center gap-3 text-xs">
              <span class="text-gray-500">{{ formatDate(tx.createdAt) }}</span>
              <a
                  :href="getTonViewerUrl(tx.transactionHash)"
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

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-4 pt-4">
        <button
            @click="prevPage"
            :disabled="currentPage === 1"
            :class="[
              'px-3 py-1.5 text-sm rounded-lg transition-colors',
              currentPage === 1
                ? 'bg-gray-800 text-gray-600 cursor-not-allowed'
                : 'bg-gray-800 text-cyan-400 hover:bg-gray-700'
            ]"
        >
          Previous
        </button>

        <span class="text-sm text-gray-400">
          Page {{ currentPage }} of {{ totalPages }}
        </span>

        <button
            @click="nextPage"
            :disabled="currentPage === totalPages"
            :class="[
              'px-3 py-1.5 text-sm rounded-lg transition-colors',
              currentPage === totalPages
                ? 'bg-gray-800 text-gray-600 cursor-not-allowed'
                : 'bg-gray-800 text-cyan-400 hover:bg-gray-700'
            ]"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref} from 'vue'
import type {Transaction} from '../types'

// Inject wallet state from parent
const walletState = inject<any>('walletState')
if (!walletState) {
  throw new Error('WalletState not provided')
}

const {
  transactions,
  loadingWalletState: loadingTransactions,
  walletStateError: transactionsError
} = walletState

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
  direction: 'ALL' as 'ALL' | 'INCOMING' | 'OUTGOING'
})

// Pagination
const currentPage = ref(1)
const pageSize = 20

// Filtered transactions
const filteredTransactions = computed(() => {
  return transactions.value.filter(tx => {
    // Filter by asset type
    if (filters.value.assetType !== 'ALL' && tx.assetType !== filters.value.assetType) {
      return false
    }

    // Filter by direction
    if (filters.value.direction !== 'ALL' && tx.direction !== filters.value.direction) {
      return false
    }

    return true
  })
})

// Paginated transactions
const paginatedTransactions = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return filteredTransactions.value.slice(start, end)
})

// Total pages
const totalPages = computed(() => {
  return Math.ceil(filteredTransactions.value.length / pageSize)
})

// Reset to page 1 when filters change
const resetPagination = () => {
  currentPage.value = 1
}

// Watch filters
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

// Watch for filter changes
const unwatchAssetType = ref(filters.value.assetType)
const unwatchDirection = ref(filters.value.direction)

const checkFilters = () => {
  if (unwatchAssetType.value !== filters.value.assetType || unwatchDirection.value !== filters.value.direction) {
    resetPagination()
    unwatchAssetType.value = filters.value.assetType
    unwatchDirection.value = filters.value.direction
  }
}

// Simple reactive watcher using computed
computed(() => {
  checkFilters()
  return null
})
</script>

<style scoped>
.transaction-item {
  border: 1px solid rgba(99, 102, 241, 0.1);
}

.transaction-item:hover {
  border-color: rgba(99, 102, 241, 0.3);
}
</style>
