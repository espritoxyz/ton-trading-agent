<template>
  <div class="transaction-history">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Transaction History</h3>
    </div>

    <!-- Filters -->
    <div class="filters flex flex-wrap gap-3 mb-4">
      <!-- Asset Type -->
      <div class="flex items-center gap-0.5 bg-gray-100 dark:bg-gray-800/70 rounded-lg p-1">
        <button
            v-for="opt in assetOptions"
            :key="opt.value"
            @click="filters.assetType = opt.value"
            :class="[
              'px-3 py-1 text-sm font-medium rounded-md transition-all duration-200',
              filters.assetType === opt.value
                ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
            ]"
        >{{ opt.label }}</button>
      </div>

      <!-- Kind -->
      <div class="flex items-center gap-0.5 bg-gray-100 dark:bg-gray-800/70 rounded-lg p-1">
        <button
            v-for="opt in kindOptions"
            :key="opt.value"
            @click="filters.kind = opt.value"
            :class="[
              'px-3 py-1 text-sm font-medium rounded-md transition-all duration-200',
              filters.kind === opt.value
                ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
            ]"
        >{{ opt.label }}</button>
      </div>
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
    <div v-else class="space-y-1">
      <!-- Desktop column headers -->
      <div class="hidden lg:flex items-center gap-2.5 px-4 pb-1 text-xs text-gray-400 dark:text-gray-600 font-medium select-none">
        <div class="w-28 flex-shrink-0">Date</div>
        <div class="w-7 flex-shrink-0" />
        <div class="w-36 flex-shrink-0">Type</div>
        <div class="flex-1 text-right">Details</div>
        <div class="w-24 flex-shrink-0 text-right">Fee</div>
        <div class="w-3.5 flex-shrink-0 ml-1" />
      </div>

      <template v-for="item in paginatedActivity" :key="item.itemType + '-' + item.id">

        <!-- Swap item -->
        <div
            v-if="item.itemType === 'swap'"
            class="transaction-item rounded-xl bg-gray-50 dark:bg-gray-800/40 hover:bg-gray-100 dark:hover:bg-gray-700/40 transition-colors px-3 py-3 lg:px-4"
        >
          <!-- Row 1: icon + label + amounts + link -->
          <div class="flex items-center gap-2.5">
            <!-- Desktop-only date -->
            <div class="hidden lg:block w-28 flex-shrink-0 text-xs text-gray-500 dark:text-gray-500 tabular-nums">
              {{ formatDate(item.createdAt) }}
            </div>
            <!-- Icon -->
            <div class="w-7 h-7 rounded-lg flex items-center justify-center bg-purple-500/20 text-purple-400 flex-shrink-0">
              <ArrowLeftRight class="w-3.5 h-3.5" />
            </div>
            <!-- Label -->
            <span class="text-sm font-semibold text-gray-900 dark:text-white truncate flex-1 lg:flex-none lg:w-36">Swap tokens</span>
            <!-- Desktop: counterparty + amounts in one flex-1 block -->
            <div class="hidden lg:flex flex-1 min-w-0 items-center gap-2">
              <span class="text-sm text-cyan-400 font-medium">Ston.fi</span>
              <div class="ml-auto flex items-center gap-1 font-mono text-xs lg:text-sm flex-shrink-0">
                <span class="text-gray-400 dark:text-gray-400">–{{ formatSwapAmount(item.fromAmount) }} {{ item.fromAsset }}</span>
                <span class="text-gray-600 dark:text-gray-600">›</span>
                <span class="text-cyan-400">+{{ formatSwapAmount(item.toAmount) }} {{ item.toAsset }}</span>
              </div>
            </div>
            <!-- Mobile: amounts only -->
            <div class="lg:hidden flex-shrink-0 flex items-center gap-1 font-mono text-xs">
              <span class="text-gray-400 dark:text-gray-400">–{{ formatSwapAmount(item.fromAmount) }} {{ item.fromAsset }}</span>
              <span class="text-gray-600 dark:text-gray-600">›</span>
              <span class="text-cyan-400">+{{ formatSwapAmount(item.toAmount) }} {{ item.toAsset }}</span>
            </div>
            <!-- Fee -->
            <div class="hidden lg:block w-24 flex-shrink-0 text-right font-mono text-xs tabular-nums">
              <span v-if="item.feeNano" class="text-gray-500 dark:text-gray-400">
                –{{ formatFee(item.feeNano) }} TON
              </span>
              <span v-else class="text-gray-400 dark:text-gray-600">—</span>
            </div>
            <!-- View link -->
            <a
                v-if="item.transactionId"
                :href="getTonViewerUrl(item.transactionId)"
                target="_blank"
                rel="noopener noreferrer"
                class="flex-shrink-0 ml-1 text-gray-500 hover:text-cyan-400 transition-colors"
                title="View on explorer"
            >
              <ExternalLink :size="13" />
            </a>
            <div v-else class="flex-shrink-0 ml-1 w-3.5" />
          </div>
          <!-- Row 2 (mobile only): date + counterparty -->
          <div class="flex items-center gap-2 mt-1.5 pl-9 lg:hidden">
            <span class="text-xs text-gray-500 dark:text-gray-500 tabular-nums">{{ formatDate(item.createdAt) }}</span>
            <span class="text-xs text-cyan-400 font-medium ml-2">Ston.fi</span>
          </div>
        </div>

        <!-- Transaction item -->
        <div
            v-else
            class="transaction-item rounded-xl bg-gray-50 dark:bg-gray-800/40 hover:bg-gray-100 dark:hover:bg-gray-700/40 transition-colors px-3 py-3 lg:px-4"
        >
          <!-- Row 1: icon + label + amount + link -->
          <div class="flex items-center gap-2.5">
            <!-- Desktop-only date -->
            <div class="hidden lg:block w-28 flex-shrink-0 text-xs text-gray-500 dark:text-gray-500 tabular-nums">
              {{ formatDate(item.createdAt) }}
            </div>
            <!-- Icon -->
            <div
                :class="[
                  'w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0',
                  item.direction === 'INCOMING' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                ]"
            >
              <svg v-if="item.direction === 'INCOMING'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 14l-7 7m0 0l-7-7m7 7V3"/>
              </svg>
              <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 10l7-7m0 0l7 7m-7-7v18"/>
              </svg>
            </div>
            <!-- Label -->
            <span class="text-sm font-semibold text-gray-900 dark:text-white truncate flex-1 lg:flex-none lg:w-36">
              {{ item.direction === 'INCOMING' ? 'Received' : 'Sent' }}
              {{ item.assetType === 'TON' ? 'TON' : (item.jettonSymbol || 'Token') }}
            </span>
            <!-- Desktop: address + amount in one flex-1 block -->
            <div class="hidden lg:flex flex-1 min-w-0 items-center gap-2">
              <div class="min-w-0">
                <div class="text-sm text-cyan-400 font-medium truncate">
                  {{ formatAddress(item.direction === 'INCOMING' ? item.senderAddress : item.recipientAddress) }}
                </div>
                <div v-if="item.comment" class="text-xs text-gray-500 dark:text-gray-500 italic truncate mt-0.5">
                  "{{ item.comment }}"
                </div>
              </div>
              <div class="ml-auto flex-shrink-0 font-mono text-sm">
                <span :class="item.direction === 'INCOMING' ? 'text-green-400' : 'text-gray-300 dark:text-gray-300'">
                  {{ item.direction === 'INCOMING' ? '+' : '–' }}{{ formatAmount(item.amountNano, item.jettonDecimals || 9) }}
                  {{ item.assetType === 'TON' ? 'TON' : (item.jettonSymbol || 'Token') }}
                </span>
              </div>
            </div>
            <!-- Mobile: amount only -->
            <div class="lg:hidden flex-shrink-0 font-mono text-xs">
              <span :class="item.direction === 'INCOMING' ? 'text-green-400' : 'text-gray-300 dark:text-gray-300'">
                {{ item.direction === 'INCOMING' ? '+' : '–' }}{{ formatAmount(item.amountNano, item.jettonDecimals || 9) }}
                {{ item.assetType === 'TON' ? 'TON' : (item.jettonSymbol || 'Token') }}
              </span>
            </div>
            <!-- Fee column: fixed width so numbers align across all rows -->
            <div class="hidden lg:block w-24 flex-shrink-0 text-right font-mono text-xs tabular-nums">
              <span v-if="item.feeNano" class="text-gray-400 dark:text-gray-500">
                –{{ formatFee(item.feeNano) }} TON
              </span>
              <span v-else class="text-gray-500 dark:text-gray-600">—</span>
            </div>
            <!-- View link -->
            <a
                :href="getTonViewerUrl(item.transactionHash)"
                target="_blank"
                rel="noopener noreferrer"
                class="flex-shrink-0 ml-1 text-gray-500 hover:text-cyan-400 transition-colors"
                title="View on explorer"
            >
              <ExternalLink :size="13" />
            </a>
          </div>
          <!-- Row 2 (mobile only): date + address + fee -->
          <div class="flex items-center gap-2 mt-1.5 pl-9 lg:hidden">
            <span class="text-xs text-gray-500 dark:text-gray-500 tabular-nums">{{ formatDate(item.createdAt) }}</span>
            <span class="text-xs text-cyan-400 font-medium ml-2 truncate">
              {{ formatAddress(item.direction === 'INCOMING' ? item.senderAddress : item.recipientAddress) }}
            </span>
            <span v-if="item.feeNano" class="text-xs font-mono tabular-nums text-gray-400 dark:text-gray-500 ml-auto flex-shrink-0">
              fee: –{{ formatFee(item.feeNano) }} TON
            </span>
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
import {ArrowLeftRight, ExternalLink} from 'lucide-vue-next'
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
  // Always show 4 decimal places for amounts
  return amount.toFixed(4)
}

/**
 * Format a human-readable swap amount to 4 decimal places.
 */
const formatSwapAmount = (value: string | number): string => {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(4) : String(value)
}

/**
 * Format fee from nanotons to readable TON string (2 decimal places)
 */
const formatFee = (feeNano: number): string => {
  const fee = feeNano / 1_000_000_000
  return fee.toFixed(2)
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
 * Format date to "18 Feb 14:57" style
 */
const formatDate = (dateString: string): string => {
  const date = new Date(dateString)
  const day = date.getDate()
  const month = date.toLocaleString('en-US', {month: 'short'})
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${day} ${month} ${hh}:${mm}`
}

// Filter options
const assetOptions = [
  {value: 'ALL', label: 'All'},
  {value: 'TON', label: 'TON'},
  {value: 'JETTON', label: 'Tokens'},
] as const

const kindOptions = [
  {value: 'ALL', label: 'All'},
  {value: 'INCOMING', label: 'Incoming'},
  {value: 'OUTGOING', label: 'Outgoing'},
  {value: 'SWAP', label: 'Swaps'},
] as const

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
  border: 1px solid transparent;
}

.transaction-item:hover {
  border-color: rgba(99, 102, 241, 0.2);
}

:global(.dark) .transaction-item:hover {
  border-color: rgba(99, 102, 241, 0.15);
}
</style>
