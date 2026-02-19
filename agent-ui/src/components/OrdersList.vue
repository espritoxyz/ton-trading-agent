<template>
  <div class="orders-list">
    <!-- Filter Tabs -->
    <div class="flex gap-6 border-b border-gray-200 dark:border-gray-700/60 mb-4">
      <button
          @click="filterStatus = 'all'"
          :class="[
            'pb-2.5 text-sm font-medium transition-colors border-b-2 -mb-px',
            filterStatus === 'all'
              ? 'border-indigo-500 text-gray-900 dark:text-white'
              : 'border-transparent text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300'
          ]"
      >
        All
        <span class="ml-1.5 text-xs text-gray-400 dark:text-gray-500">{{ orders.length }}</span>
      </button>
      <button
          @click="filterStatus = 'active'"
          :class="[
            'pb-2.5 text-sm font-medium transition-colors border-b-2 -mb-px',
            filterStatus === 'active'
              ? 'border-indigo-500 text-gray-900 dark:text-white'
              : 'border-transparent text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300'
          ]"
      >
        Active
        <span class="ml-1.5 text-xs px-1.5 py-0.5 rounded-md bg-amber-500/15 text-amber-600 dark:text-amber-400 font-semibold">{{ activeOrders.length }}</span>
      </button>
      <button
          @click="filterStatus = 'fulfilled'"
          :class="[
            'pb-2.5 text-sm font-medium transition-colors border-b-2 -mb-px',
            filterStatus === 'fulfilled'
              ? 'border-indigo-500 text-gray-900 dark:text-white'
              : 'border-transparent text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300'
          ]"
      >
        Fulfilled
        <span class="ml-1.5 text-xs px-1.5 py-0.5 rounded-md bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 font-semibold">{{ fulfilledOrders.length }}</span>
      </button>
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

    <!-- Delete Error -->
    <div v-if="deleteError"
         class="text-red-600 dark:text-red-400 text-sm p-3 bg-red-100 dark:bg-red-900/20 rounded-lg mb-4">
      {{ deleteError }}
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
          :class="[
            'order-item p-4 rounded-xl transition-colors',
            order.fulfilled
              ? 'order-fulfilled bg-gray-50 dark:bg-gray-800/30'
              : 'order-active bg-gray-100 dark:bg-gray-800/50 hover:bg-gray-200 dark:hover:bg-gray-700/50',
          ]"
      >
        <!-- Main content row -->
        <div class="flex items-center gap-4 justify-between">

          <!-- Left Section: Action Icon and Details -->
          <div class="flex items-center gap-4 flex-1 min-w-0">

            <!-- Action Icon -->
            <div class="flex-shrink-0">
              <div
                  :class="[
                    'w-12 h-12 rounded-xl flex items-center justify-center',
                    order.fulfilled ? 'opacity-50' : '',
                    order.action === 'buy'
                      ? 'bg-emerald-500/20 text-emerald-400'
                      : 'bg-orange-500/20 text-orange-400'
                  ]"
              >
                <CirclePlus v-if="order.action === 'buy'" :size="22"/>
                <CircleMinus v-else :size="22"/>
              </div>
            </div>

            <!-- Order Details -->
            <div class="flex-1 min-w-0 space-y-1.5">
              <!-- Action and Amount -->
              <div class="flex items-baseline gap-2.5">
                <span
                    :class="[
                      'font-bold text-lg uppercase',
                      order.fulfilled ? 'text-gray-500 dark:text-gray-400' : 'text-gray-900 dark:text-white'
                    ]"
                >
                  {{ order.action }}
                </span>
                <span
                    :class="[
                      'font-mono text-lg font-semibold',
                      order.fulfilled ? 'text-gray-500 dark:text-gray-400' : 'text-gray-900 dark:text-white'
                    ]"
                >
                  {{ formatAmount(order.amount) }}
                </span>
                <span class="text-base text-gray-500 dark:text-gray-400 font-medium">
                  {{ order.symbol || 'Token' }}
                </span>
              </div>

              <!-- Jetton Master Address -->
              <div class="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
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

              <!-- Date -->
              <div class="text-xs text-gray-500 dark:text-gray-500">
                {{ formatDate(order.createdAt) }}
              </div>
            </div>
          </div>

          <!-- Right Section: Cards -->
          <div class="flex items-stretch gap-2 flex-shrink-0">

            <!-- Trigger Price Card -->
            <div v-if="order.targetPrice && order.direction" class="flex-shrink-0">
              <div
                  :class="[
                    'trigger-card h-full px-4 py-3 rounded-xl border-2',
                    order.direction === 'UP'
                      ? 'bg-emerald-500/10 border-emerald-500/30 dark:bg-emerald-500/5 dark:border-emerald-500/20'
                      : 'bg-orange-500/10 border-orange-500/30 dark:bg-orange-500/5 dark:border-orange-500/20'
                  ]"
              >
                <div class="flex flex-col items-center gap-2">
                  <!-- Direction Arrow -->
                  <div class="flex items-center gap-2">
                    <ArrowUp
                        v-if="order.direction === 'UP'"
                        :size="20"
                        class="text-emerald-400"
                        stroke-width="2.5"
                    />
                    <ArrowDown
                        v-else
                        :size="20"
                        class="text-orange-400"
                        stroke-width="2.5"
                    />
                  </div>

                  <!-- Trigger Text -->
                  <div class="text-xs font-medium text-gray-600 dark:text-gray-400 whitespace-nowrap">
                    Price {{ order.direction === 'UP' ? 'above' : 'below' }}
                  </div>

                  <!-- Target Price -->
                  <div class="font-mono font-bold text-lg text-gray-900 dark:text-white">
                    ${{ formatTargetPrice(order.targetPrice) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Action Card: Cancel (active) or Filled (fulfilled) -->
            <div class="flex-shrink-0 flex">

              <!-- Cancel card for active orders -->
              <button
                  v-if="!order.fulfilled"
                  @click.stop="deleteOrder(order.id)"
                  :disabled="deletingOrderId !== null"
                  :class="[
                    'action-card h-full px-4 py-3 rounded-xl border-2 min-w-[72px]',
                    'flex flex-col items-center justify-center gap-2',
                    'transition-all duration-200',
                    deletingOrderId === order.id
                      ? 'bg-red-500/20 border-red-500/50 dark:bg-red-500/15 dark:border-red-500/40 cursor-wait'
                      : 'bg-red-500/10 border-red-500/30 dark:bg-red-500/5 dark:border-red-500/20 hover:bg-red-500/20 hover:border-red-500/50 dark:hover:bg-red-500/15 dark:hover:border-red-500/40',
                    deletingOrderId !== null && deletingOrderId !== order.id
                      ? 'opacity-50 cursor-not-allowed'
                      : '',
                  ]"
                  title="Cancel order"
              >
                <div v-if="deletingOrderId === order.id"
                     class="animate-spin rounded-full h-4 w-4 border-b-2 border-red-400"/>
                <Trash2 v-else :size="16" class="text-red-400"/>
                <span v-show="deletingOrderId !== order.id" class="text-xs font-medium text-red-400 whitespace-nowrap">
                  Cancel
                </span>
              </button>

              <!-- Filled indicator card for fulfilled orders -->
              <div
                  v-else
                  class="h-full px-4 py-3 rounded-xl border-2 min-w-[72px] bg-emerald-500/5 border-emerald-500/20 dark:border-emerald-500/15 flex flex-col items-center justify-center gap-2"
              >
                <Check :size="16" class="text-emerald-400 opacity-70"/>
                <span class="text-xs font-medium text-emerald-500 dark:text-emerald-400 opacity-70 whitespace-nowrap">
                  Filled
                </span>
              </div>

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
import {ArrowDown, ArrowUp, Check, CircleMinus, CirclePlus, Trash2} from 'lucide-vue-next'
import type {OrderData} from '../types'
import {api} from '../composables/useApi'
import {userId} from '../composables/useAuth'

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
const deletingOrderId = ref<number | null>(null)
const deleteError = ref<string | null>(null)

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

const deleteOrder = async (orderId: number) => {
  if (!userId.value || deletingOrderId.value !== null) return

  deletingOrderId.value = orderId
  deleteError.value = null

  // Optimistic update
  const rawState = walletState.walletState
  const previousOrders = rawState.value?.orders ?? []
  if (rawState.value) {
    rawState.value = {
      ...rawState.value,
      orders: previousOrders.filter((o: OrderData) => o.id !== orderId)
    }
  }

  try {
    await api.delete(`/user/${userId.value}/orders/${orderId}`)
  } catch (err: any) {
    // Revert optimistic update on failure
    if (rawState.value) {
      rawState.value = {
        ...rawState.value,
        orders: previousOrders
      }
    }
    deleteError.value = err.response?.data?.message || 'Failed to delete order'
  } finally {
    deletingOrderId.value = null
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

/* Active orders: amber left accent */
.order-active {
  border-left: 3px solid rgba(251, 191, 36, 0.6);
}

.order-active:hover {
  border-color: rgba(99, 102, 241, 0.4);
  border-left-color: rgba(251, 191, 36, 0.85);
}

:global(.dark) .order-active:hover {
  border-color: rgba(99, 102, 241, 0.3);
  border-left-color: rgba(251, 191, 36, 0.7);
}

/* Fulfilled orders: emerald left accent, dimmed */
.order-fulfilled {
  border-left: 3px solid rgba(52, 211, 153, 0.4);
  opacity: 0.75;
}

:global(.dark) .order-fulfilled {
  border-left-color: rgba(52, 211, 153, 0.3);
}

.trigger-card {
  transition: all 0.2s ease-in-out;
  min-width: 120px;
}

.trigger-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:global(.dark) .trigger-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.action-card {
  transition: all 0.2s ease-in-out;
}

.action-card:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.15);
}

:global(.dark) .action-card:not(:disabled):hover {
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.1);
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
