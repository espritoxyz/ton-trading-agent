<script setup lang="ts">
import { ref, computed } from 'vue'
import { StarIcon, MagnifyingGlassIcon, FunnelIcon, ChevronUpDownIcon } from '@heroicons/vue/24/outline'
import { StarIcon as StarIconSolid } from '@heroicons/vue/24/solid'

// Market tabs
const marketTabs = ['Market', 'USD-M', 'COIN-M', 'Options']
const selectedMarketTab = ref('Market')

// Sub tabs
const subTabs = ['Overview', 'Rating']
const selectedSubTab = ref('Overview')

// Filter tabs
const filterTabs = ['Favorites', 'Futures USD-M', 'Futures COIN-M']
const selectedFilterTab = ref('Futures USD-M')

// Open Interest data
const openInterestSymbol = ref('BTCUSDT')
const openInterestType = ref('Perpetual')
const openInterestValue = ref(6971951801.27)
const openInterestChange = ref(2.36)
const openInterestCurrency = ref('USDT')

// Long/Short ratio data
const longShortSymbol = ref('BTCUSDT')
const shortPercent = ref(31.14)
const longPercent = ref(68.86)
const longShortRatio = ref(2.21)

// Altcoin index
const altcoinIndexValue = ref(6)
const altcoinIndexText = ref("It's Bitcoin week!")

// News data
const news = ref([
  { time: '11:34 AM', title: 'Gnosis Co-Founder Criticizes Crypto Industry\'s Shift Towards ...' },
  { time: '11:24 AM', title: 'Dubai Diamond Inventory Tokenized on XRP Ledger' },
])

// Filter options
const categoryFilter = ref('All')
const volume24hFilter = ref('All')
const change24hFilter = ref('All')
const periodFilter = ref('All')
const fundingRateFilter = ref('All')

// Market data
interface MarketItem {
  symbol: string
  type: string
  price: number
  priceUSD: number
  change24h: number
  high24h: number
  low24h: number
  volume24h: string
  fundingRate: string
  isFavorite: boolean
  icon: string
  iconColor: string
}

const marketData = ref<MarketItem[]>([
  {
    symbol: 'BTCUSDT',
    type: 'Perpetual',
    price: 78974.9,
    priceUSD: 78974.90,
    change24h: 2.78,
    high24h: 79311.0,
    low24h: 76610.9,
    volume24h: '13.78B',
    fundingRate: '0.0027%',
    isFavorite: true,
    icon: 'B',
    iconColor: 'from-orange-400 to-orange-600'
  },
  {
    symbol: 'BNBUSDT',
    type: 'Perpetual',
    price: 782.39,
    priceUSD: 782.39,
    change24h: 3.04,
    high24h: 783.78,
    low24h: 755.45,
    volume24h: '527.51M',
    fundingRate: '0.0000%',
    isFavorite: true,
    icon: 'B',
    iconColor: 'from-yellow-400 to-yellow-600'
  },
  {
    symbol: 'ETHUSDT',
    type: 'Perpetual',
    price: 2328.40,
    priceUSD: 2328.40,
    change24h: 3.76,
    high24h: 2395.40,
    low24h: 2236.43,
    volume24h: '15.92B',
    fundingRate: '0.0016%',
    isFavorite: true,
    icon: 'E',
    iconColor: 'from-purple-400 to-purple-600'
  },
  {
    symbol: 'SOLUSDT',
    type: 'Perpetual',
    price: 198.45,
    priceUSD: 198.45,
    change24h: 4.12,
    high24h: 201.30,
    low24h: 189.20,
    volume24h: '8.45B',
    fundingRate: '0.0018%',
    isFavorite: false,
    icon: 'S',
    iconColor: 'from-teal-400 to-teal-600'
  },
  {
    symbol: 'XRPUSDT',
    type: 'Perpetual',
    price: 2.85,
    priceUSD: 2.85,
    change24h: -1.23,
    high24h: 2.92,
    low24h: 2.78,
    volume24h: '2.31B',
    fundingRate: '0.0012%',
    isFavorite: false,
    icon: 'X',
    iconColor: 'from-gray-400 to-gray-600'
  },
])

// Sorting
type SortKey = 'symbol' | 'price' | 'change24h' | 'high24h' | 'low24h' | 'volume24h' | 'fundingRate'
const sortKey = ref<SortKey>('volume24h')
const sortDirection = ref<'asc' | 'desc'>('desc')

function toggleSort(key: SortKey) {
  if (sortKey.value === key) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDirection.value = 'desc'
  }
}

const sortedMarketData = computed(() => {
  return [...marketData.value].sort((a, b) => {
    let aVal = a[sortKey.value]
    let bVal = b[sortKey.value]

    if (typeof aVal === 'string' && sortKey.value === 'volume24h') {
      aVal = parseFloat(aVal.replace(/[BMK]/g, ''))
      bVal = parseFloat((bVal as string).replace(/[BMK]/g, ''))
    }

    if (sortDirection.value === 'asc') {
      return aVal > bVal ? 1 : -1
    } else {
      return aVal < bVal ? 1 : -1
    }
  })
})

function toggleFavorite(item: MarketItem) {
  item.isFavorite = !item.isFavorite
}

function formatNumber(num: number, decimals = 2): string {
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

// Donut chart calculation
const donutRadius = 40
const donutStrokeWidth = 12
const donutCircumference = 2 * Math.PI * donutRadius
const longOffset = computed(() => (1 - longPercent.value / 100) * donutCircumference)
</script>

<template>
  <div class="h-full flex flex-col gap-4 overflow-hidden">
    <!-- Page Title -->
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Cryptocurrency Futures Market</h1>

    <!-- Market Tabs -->
    <div class="flex gap-6 border-b border-gray-200 dark:border-white/10">
      <button
        v-for="tab in marketTabs"
        :key="tab"
        @click="selectedMarketTab = tab"
        :class="[
          'pb-3 text-sm font-medium transition-colors relative',
          selectedMarketTab === tab
            ? 'text-cosmic-600 dark:text-cosmic-400'
            : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
        ]"
      >
        {{ tab }}
        <div
          v-if="selectedMarketTab === tab"
          class="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-cosmic-500 to-purple-500"
        ></div>
      </button>
    </div>

    <!-- Stats Cards Row -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Open Interest Card -->
      <div class="glass-card p-4">
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm text-gray-500 dark:text-gray-400">Open Interest</span>
          <select class="bg-transparent text-xs text-gray-500 dark:text-gray-400 border-none outline-none cursor-pointer">
            <option>{{ openInterestCurrency }}</option>
          </select>
        </div>
        <div class="flex items-center gap-2 mb-2">
          <span class="text-xs text-gray-500 dark:text-gray-400">{{ openInterestSymbol }} {{ openInterestType }}</span>
          <ChevronUpDownIcon class="w-3 h-3 text-gray-400" />
        </div>
        <div class="text-xl font-bold text-gray-900 dark:text-white mb-1">
          {{ formatNumber(openInterestValue) }} {{ openInterestCurrency }}
        </div>
        <div class="flex items-center gap-1">
          <span class="text-xs text-gray-500 dark:text-gray-400">24h change</span>
          <span class="text-xs text-green-500">+{{ openInterestChange }}%</span>
        </div>
        <!-- Mini chart placeholder -->
        <div class="mt-3 h-12 relative">
          <svg class="w-full h-full">
            <polyline
              points="0,40 20,35 40,38 60,30 80,25 100,28 120,20 140,15 160,18 180,10 200,12 220,8"
              fill="none"
              class="stroke-yellow-500"
              stroke-width="2"
            />
          </svg>
        </div>
      </div>

      <!-- Long/Short Ratio Card -->
      <div class="glass-card p-4">
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm text-gray-500 dark:text-gray-400">1h Long/Short Ratio</span>
          <span class="text-gray-400">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" stroke-width="2"/>
              <path d="M12 16v-4M12 8h.01" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </span>
        </div>
        <div class="flex items-center gap-2 mb-4">
          <span class="text-xs text-gray-500 dark:text-gray-400">{{ longShortSymbol }} {{ openInterestType }}</span>
          <ChevronUpDownIcon class="w-3 h-3 text-gray-400" />
        </div>
        <div class="flex items-center gap-4">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <div class="w-2 h-2 bg-red-500 rounded-sm"></div>
              <span class="text-xs text-gray-500 dark:text-gray-400">Short %</span>
            </div>
            <div class="text-lg font-semibold text-gray-900 dark:text-white">{{ shortPercent }}%</div>
            <div class="flex items-center gap-2 mt-2">
              <div class="w-2 h-2 bg-green-500 rounded-sm"></div>
              <span class="text-xs text-gray-500 dark:text-gray-400">Long %</span>
            </div>
            <div class="text-lg font-semibold text-gray-900 dark:text-white">{{ longPercent }}%</div>
            <div class="mt-2 text-xs text-gray-500 dark:text-gray-400">
              Long/Short ratio
            </div>
            <div class="text-sm font-medium text-gray-900 dark:text-white">{{ longShortRatio }}</div>
          </div>
          <!-- Donut Chart -->
          <div class="relative w-24 h-24">
            <svg class="w-full h-full -rotate-90" viewBox="0 0 100 100">
              <circle
                cx="50"
                cy="50"
                :r="donutRadius"
                fill="none"
                class="stroke-red-500"
                :stroke-width="donutStrokeWidth"
              />
              <circle
                cx="50"
                cy="50"
                :r="donutRadius"
                fill="none"
                class="stroke-green-500"
                :stroke-width="donutStrokeWidth"
                :stroke-dasharray="donutCircumference"
                :stroke-dashoffset="longOffset"
              />
            </svg>
            <div class="absolute inset-0 flex items-center justify-center">
              <span class="text-lg font-bold text-gray-900 dark:text-white">{{ longShortRatio }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Altcoin Index Card -->
      <div class="glass-card p-4">
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm text-gray-500 dark:text-gray-400">Altcoin Week Index</span>
          <span class="text-gray-400">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" stroke-width="2"/>
              <path d="M12 16v-4M12 8h.01" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </span>
        </div>
        <div class="text-sm text-gray-900 dark:text-white mb-4">{{ altcoinIndexText }}</div>
        <!-- Gauge -->
        <div class="relative h-20 flex items-end justify-center">
          <svg class="w-32 h-16" viewBox="0 0 100 50">
            <defs>
              <linearGradient id="gaugeGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" style="stop-color:#ef4444"/>
                <stop offset="50%" style="stop-color:#eab308"/>
                <stop offset="100%" style="stop-color:#22c55e"/>
              </linearGradient>
            </defs>
            <path
              d="M 10 50 A 40 40 0 0 1 90 50"
              fill="none"
              stroke="url(#gaugeGradient)"
              stroke-width="8"
              stroke-linecap="round"
            />
            <!-- Needle -->
            <line
              x1="50"
              y1="50"
              :x2="50 + 30 * Math.cos(Math.PI * (1 - altcoinIndexValue / 10))"
              :y2="50 - 30 * Math.sin(Math.PI * (1 - altcoinIndexValue / 10))"
              stroke="white"
              stroke-width="2"
            />
            <circle cx="50" cy="50" r="4" fill="white"/>
          </svg>
          <div class="absolute bottom-0 left-1/2 -translate-x-1/2 flex items-center gap-4 text-xs">
            <span class="text-gray-500 dark:text-gray-400">Bitcoin</span>
            <span class="text-gray-500 dark:text-gray-400">Altcoin</span>
          </div>
        </div>
        <div class="text-center mt-2 text-2xl font-bold text-gray-900 dark:text-white">{{ altcoinIndexValue }}</div>
      </div>

      <!-- News Card -->
      <div class="glass-card p-4">
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm text-gray-500 dark:text-gray-400">News</span>
        </div>
        <div class="space-y-3">
          <div v-for="item in news" :key="item.time" class="flex gap-3">
            <span class="text-xs text-gray-400 whitespace-nowrap">{{ item.time }}</span>
            <p class="text-sm text-gray-900 dark:text-white leading-tight line-clamp-2">{{ item.title }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Overview / Rating Tabs -->
    <div class="flex gap-6 border-b border-gray-200 dark:border-white/10">
      <button
        v-for="tab in subTabs"
        :key="tab"
        @click="selectedSubTab = tab"
        :class="[
          'pb-3 text-sm font-medium transition-colors relative',
          selectedSubTab === tab
            ? 'text-gray-900 dark:text-white'
            : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
        ]"
      >
        {{ tab }}
        <div
          v-if="selectedSubTab === tab"
          class="absolute bottom-0 left-0 right-0 h-0.5 bg-yellow-500"
        ></div>
      </button>
    </div>

    <!-- Filter Tabs & Search -->
    <div class="flex items-center justify-between gap-4 flex-wrap">
      <div class="flex gap-2">
        <button
          v-for="tab in filterTabs"
          :key="tab"
          @click="selectedFilterTab = tab"
          :class="[
            'px-4 py-2 text-sm rounded-lg transition-colors',
            selectedFilterTab === tab
              ? 'bg-gray-200 dark:bg-white/10 text-gray-900 dark:text-white'
              : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
          ]"
        >
          {{ tab }}
        </button>
      </div>
      <div class="flex items-center gap-2">
        <button class="p-2 text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white">
          <MagnifyingGlassIcon class="w-5 h-5" />
        </button>
        <button class="p-2 text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white">
          <FunnelIcon class="w-5 h-5" />
        </button>
      </div>
    </div>

    <!-- Filters Row -->
    <div class="flex gap-3 flex-wrap">
      <button class="px-4 py-2 text-sm rounded-lg bg-gray-100 dark:bg-white/5 text-gray-700 dark:text-gray-300 flex items-center gap-2">
        Category <span class="text-gray-500">All</span> <ChevronUpDownIcon class="w-4 h-4" />
      </button>
      <button class="px-4 py-2 text-sm rounded-lg bg-gray-100 dark:bg-white/5 text-gray-700 dark:text-gray-300 flex items-center gap-2">
        24h Volume <span class="text-gray-500">All</span> <ChevronUpDownIcon class="w-4 h-4" />
      </button>
      <button class="px-4 py-2 text-sm rounded-lg bg-gray-100 dark:bg-white/5 text-gray-700 dark:text-gray-300 flex items-center gap-2">
        24h Change <span class="text-gray-500">All</span> <ChevronUpDownIcon class="w-4 h-4" />
      </button>
      <button class="px-4 py-2 text-sm rounded-lg bg-gray-100 dark:bg-white/5 text-gray-700 dark:text-gray-300 flex items-center gap-2">
        Period <span class="text-gray-500">All</span> <ChevronUpDownIcon class="w-4 h-4" />
      </button>
      <button class="px-4 py-2 text-sm rounded-lg bg-yellow-500/20 text-yellow-600 dark:text-yellow-400 flex items-center gap-2">
        Funding Rate <ChevronUpDownIcon class="w-4 h-4" />
      </button>
    </div>

    <!-- Market Table -->
    <div class="flex-1 glass-card overflow-hidden min-h-0">
      <div class="overflow-auto h-full">
        <table class="w-full text-sm">
          <thead class="sticky top-0 bg-gray-50 dark:bg-space-dark/90 backdrop-blur-sm">
            <tr class="text-left text-gray-500 dark:text-gray-400">
              <th class="px-4 py-3 font-medium">
                <button @click="toggleSort('symbol')" class="flex items-center gap-1 hover:text-gray-900 dark:hover:text-white">
                  Name <ChevronUpDownIcon class="w-4 h-4" />
                </button>
              </th>
              <th class="px-4 py-3 font-medium">
                <button @click="toggleSort('price')" class="flex items-center gap-1 hover:text-gray-900 dark:hover:text-white">
                  Price <ChevronUpDownIcon class="w-4 h-4" />
                </button>
              </th>
              <th class="px-4 py-3 font-medium">
                <button @click="toggleSort('change24h')" class="flex items-center gap-1 hover:text-gray-900 dark:hover:text-white">
                  24h Change <ChevronUpDownIcon class="w-4 h-4" />
                </button>
              </th>
              <th class="px-4 py-3 font-medium">
                <button @click="toggleSort('high24h')" class="flex items-center gap-1 hover:text-gray-900 dark:hover:text-white">
                  24h High <ChevronUpDownIcon class="w-4 h-4" />
                </button>
              </th>
              <th class="px-4 py-3 font-medium">
                <button @click="toggleSort('low24h')" class="flex items-center gap-1 hover:text-gray-900 dark:hover:text-white">
                  24h Low <ChevronUpDownIcon class="w-4 h-4" />
                </button>
              </th>
              <th class="px-4 py-3 font-medium">
                <button @click="toggleSort('volume24h')" class="flex items-center gap-1 hover:text-gray-900 dark:hover:text-white">
                  24h Volume (USD) <ChevronUpDownIcon class="w-4 h-4" />
                </button>
              </th>
              <th class="px-4 py-3 font-medium">Funding Rate</th>
              <th class="px-4 py-3 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100 dark:divide-white/5">
            <tr
              v-for="item in sortedMarketData"
              :key="item.symbol"
              class="hover:bg-gray-50 dark:hover:bg-white/5 transition-colors"
            >
              <td class="px-4 py-4">
                <div class="flex items-center gap-3">
                  <button @click="toggleFavorite(item)" class="text-yellow-500">
                    <StarIconSolid v-if="item.isFavorite" class="w-4 h-4" />
                    <StarIcon v-else class="w-4 h-4" />
                  </button>
                  <div :class="['w-8 h-8 rounded-full bg-gradient-to-br flex items-center justify-center', item.iconColor]">
                    <span class="text-white text-xs font-bold">{{ item.icon }}</span>
                  </div>
                  <div>
                    <div class="font-medium text-gray-900 dark:text-white">{{ item.symbol }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">{{ item.type }}</div>
                  </div>
                </div>
              </td>
              <td class="px-4 py-4">
                <div class="font-medium text-gray-900 dark:text-white">{{ formatNumber(item.price) }}</div>
                <div class="text-xs text-gray-500 dark:text-gray-400">${{ formatNumber(item.priceUSD) }}</div>
              </td>
              <td class="px-4 py-4">
                <span :class="item.change24h >= 0 ? 'text-green-500' : 'text-red-500'">
                  {{ item.change24h >= 0 ? '+' : '' }}{{ item.change24h }}%
                </span>
              </td>
              <td class="px-4 py-4 text-gray-900 dark:text-white">{{ formatNumber(item.high24h) }}</td>
              <td class="px-4 py-4 text-gray-900 dark:text-white">{{ formatNumber(item.low24h) }}</td>
              <td class="px-4 py-4 text-gray-900 dark:text-white">{{ item.volume24h }}</td>
              <td class="px-4 py-4 text-gray-900 dark:text-white">{{ item.fundingRate }}</td>
              <td class="px-4 py-4">
                <button class="text-yellow-500 hover:text-yellow-400 font-medium">Trade</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
