<template>
  <div class="orders-list">
    <!-- Header with Filters -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Orders</h3>

      <!-- Filter Pills -->
      <div class="flex gap-2">
        <button
            @click="filterStatus = 'all'"
            :class="[
              'px-3 py-1.5 text-sm rounded-lg transition-colors',
              filterStatus === 'all'
                ? 'bg-cyan-500 text-white'
                : 'bg-gray-200 dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-700'
            ]"
        >
          All ({{ orders.length }})
        </button>
        <button
            @click="filterStatus = 'active'"
            :class="[
              'px-3 py-1.5 text-sm rounded-lg transition-colors',
              filterStatus === 'active'
                ? 'bg-cyan-500 text-white'
                : 'bg-gray-200 dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-700'
            ]"
        >
          Active ({{ activeOrders.length }})
        </button>
        <button
            @click="filterStatus = 'fulfilled'"
            :class="[
              'px-3 py-1.5 text-sm rounded-lg transition-colors',
              filterStatus === 'fulfilled'
                ? 'bg-cyan-500 text-white'
                : 'bg-gray-200 dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-700'
            ]"
        >
          Fulfilled ({{ fulfilledOrders.length }})
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loadingOrders" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-cyan-500"></div>
    </div>

    <!-- Error State -->
    <div v-else-if="ordersError"
         class="text-red-600 dark:text-red-400 text-sm p-4 bg-red-100 dark:bg-red-900/20 rounded-lg">
      {{ ordersError }}
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredOrders.length === 0" class="text-gray-600 dark:text-gray-400 text-sm text-center py-8">
      <div class="flex flex-col items-center gap-2">
        <svg class="w-12 h-12 text-gray-400 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
        </svg>
        <p>No {{ filterStatus === 'all' ? '' : filterStatus }} orders found</p>
      </div>
    </div>

    <!-- Orders List -->
    <div v-else class="space-y-2">
      <div
          v-for="order in paginatedOrders"
          :key="order.id"
          class="order-item p-3 rounded-lg bg-gray-100 dark:bg-gray-800/50 hover:bg-gray-200 dark:hover:bg-gray-700/50 transition-colors"
      >
        <div class="flex items-start gap-3">
          <!-- Action Icon -->
          <div class="flex-shrink-0 mt-1">
            <div
                :class="[
                  'w-8 h-8 rounded-full flex items-center justify-center',
                  order.action === 'buy' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-orange-500/20 text-orange-400'
                ]"
            >
              <svg v-if="order.action === 'buy'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/>
              </svg>
            </div>
          </div>

          <!-- Order Details -->
          <div class="flex-1 min-w-0">
            <!-- Action and Amount -->
            <div class="flex items-baseline gap-2 mb-1">
              <span class="font-semibold text-gray-900 dark:text-white uppercase">
                {{ order.action }}
              </span>
              <span class="font-mono text-gray-900 dark:text-white">
                {{ formatAmount(order.amount) }}
              </span>
              <span class="text-sm text-gray-600 dark:text-gray-400">
                {{ order.symbol || 'Token' }}
              </span>
            </div>

            <!-- Jetton Master Address -->
            <div class="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400 mb-1">
              <span
                  @click="copyAddress(order.jettonMaster, order.id)"
                  class="truncate cursor-pointer hover:text-cyan-400 transition-colors"
                  title="Click to copy jetton master address"
              >
                {{ formatAddress(order.jettonMaster) }}
              </span>
              <button
                  @click="copyAddress(order.jettonMaster, order.id)"
                  :class="{'copied': copiedOrderId === order.id}"
                  class="copy-btn p-0.5 hover:text-cyan-400 transition-all duration-200"
                  title="Copy address"
              >
                <svg v-if="copiedOrderId !== order.id" class="w-3.5 h-3.5" fill="none" stroke="currentColor"
                     viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                </svg>
                <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
              </button>
            </div>

            <!-- Target Price Info (if available) -->
            <div v-if="order.targetPrice && order.direction" class="flex items-center gap-1.5 text-xs mb-1">
              <span class="text-gray-500 dark:text-gray-500">Triggers at:</span>
              <span class="font-mono text-cyan-600 dark:text-cyan-400">${{ formatTargetPrice(order.targetPrice) }}</span>
              <span
                  :class="[
                    'px-1.5 py-0.5 rounded text-xs font-medium',
                    order.direction === 'UP'
                      ? 'bg-emerald-500/20 text-emerald-400'
                      : 'bg-orange-500/20 text-orange-400'
                  ]"
              >
                {{ order.direction }}
              </span>
            </div>

            <!-- Date and Status -->
            <div class="flex items-center gap-3 text-xs">
              <span class="text-gray-500 dark:text-gray-500">{{ formatDate(order.createdAt) }}</span>
              <span
                  :class="[
                    'px-2 py-0.5 rounded-full font-medium',
                    order.fulfilled
                      ? 'bg-green-500/20 text-green-400'
                      : 'bg-yellow-500/20 text-yellow-400'
                  ]"
              >
                {{ order.fulfilled ? 'Fulfilled' : 'Active' }}
              </span>
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
import {computed, inject, ref} from 'vue'
import type {OrderData} from '../types'

// Inject wallet state from parent
const walletState = inject<any>('walletState')
if (!walletState) {
  throw new Error('WalletState not provided')
}

const {
  orders,
  loadingWalletState: loadingOrders,
  walletStateError: ordersError
} = walletState

// Filter state
const filterStatus = ref<'all' | 'active' | 'fulfilled'>('all')
const copiedOrderId = ref<number | null>(null)

// Pagination
const currentPage = ref(1)
const pageSize = 20

// Computed order lists
const activeOrders = computed(() => orders.value.filter((o: OrderData) => !o.fulfilled))
const fulfilledOrders = computed(() => orders.value.filter((o: OrderData) => o.fulfilled))

// Filtered orders
const filteredOrders = computed(() => {
  // Sort by createdAt DESC (newest first)
  const sorted = [...orders.value].sort((a, b) => {
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  })

  if (filterStatus.value === 'active') {
    return sorted.filter(o => !o.fulfilled)
  } else if (filterStatus.value === 'fulfilled') {
    return sorted.filter(o => o.fulfilled)
  }
  return sorted
})

// Paginated orders
const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return filteredOrders.value.slice(start, end)
})

// Total pages
const totalPages = computed(() => {
  return Math.ceil(filteredOrders.value.length / pageSize)
})

// Pagination controls
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

// Format helpers
const formatAmount = (amount: number): string => {
  if (amount >= 1) {
    return amount.toFixed(2)
  } else if (amount >= 0.01) {
    return amount.toFixed(4).replace(/\.?0+$/, '')
  } else {
    return amount.toFixed(8).replace(/\.?0+$/, '')
  }
}

const formatAddress = (address: string): string => {
  if (address.length <= 12) return address
  return `${address.substring(0, 6)}...${address.substring(address.length - 6)}`
}

const formatDate = (dateString: string): string => {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatTargetPrice = (price: number): string => {
  if (price >= 1) {
    return price.toFixed(2)
  } else if (price >= 0.001) {
    return price.toFixed(4)
  } else {
    return price.toFixed(8).replace(/\.?0+$/, '')
  }
}

const copyAddress = async (address: string, orderId: number) => {
  try {
    await navigator.clipboard.writeText(address)
    copiedOrderId.value = orderId
    // Reset after 2 seconds
    setTimeout(() => {
      copiedOrderId.value = null
    }, 2000)
  } catch (err) {
    console.error('Failed to copy address:', err)
  }
}

// Reset pagination when filter changes
const resetPagination = () => {
  currentPage.value = 1
}

// Watch filter changes
const unwatchFilter = ref(filterStatus.value)
computed(() => {
  if (unwatchFilter.value !== filterStatus.value) {
    resetPagination()
    unwatchFilter.value = filterStatus.value
  }
  return null
})
</script>

<style scoped>
.order-item {
  border: 1px solid rgba(99, 102, 241, 0.15);
}

:global(.dark) .order-item {
  border-color: rgba(99, 102, 241, 0.1);
}

.order-item:hover {
  border-color: rgba(99, 102, 241, 0.4);
}

:global(.dark) .order-item:hover {
  border-color: rgba(99, 102, 241, 0.3);
}

.copy-btn.copied {
  color: #22c55e;
  animation: copy-success 0.3s ease-in-out;
}

@keyframes copy-success {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}
</style>
