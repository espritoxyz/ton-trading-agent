<script setup lang="ts">
import { ref, computed } from 'vue'
import { StarIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'
import { StarIcon as StarIconSolid } from '@heroicons/vue/24/solid'

// Mock data for the trading pair
const selectedPair = ref('BTCUSDT')
const currentPrice = ref(78690.7)
const priceChange = ref(1744.39)
const priceChangePercent = ref(2.26)
const high24h = ref(79311.0)
const low24h = ref(76610.9)
const volume24hBTC = ref(13778253510.06)
const volumeUSDT = ref(7000528559.60)
const markPrice = ref(78690.7)
const indexPrice = ref(78730.0)
const fundingRate = ref(0.00237)
const fundingCountdown = ref('07:00:17')
const openInterest = ref(175910073)

const timeframes = ['1c', '15m', '1H', '4H', '1D', '1H']
const selectedTimeframe = ref('15m')

const isFavorite = ref(true)

// Mock candlestick data
const candlesticks = ref([
  { time: '21:00', open: 77800, high: 78100, low: 77600, close: 77900, volume: 1200 },
  { time: '23:00', open: 77900, high: 78200, low: 77700, close: 78000, volume: 1100 },
  { time: '01:00', open: 78000, high: 78100, low: 77500, close: 77600, volume: 1300 },
  { time: '02/02', open: 77600, high: 77800, low: 74555, close: 74800, volume: 2500 },
  { time: '05:00', open: 74800, high: 75200, low: 74600, close: 75100, volume: 1800 },
  { time: '07:00', open: 75100, high: 77500, low: 75000, close: 77200, volume: 2200 },
  { time: '09:00', open: 77200, high: 78200, low: 77100, close: 78000, volume: 1600 },
  { time: '11:00', open: 78000, high: 78400, low: 77800, close: 78300, volume: 1400 },
  { time: '13:00', open: 78300, high: 78600, low: 78200, close: 78500, volume: 1200 },
  { time: '15:00', open: 78500, high: 79000, low: 78400, close: 78800, volume: 1500 },
  { time: '17:00', open: 78800, high: 79311, low: 78700, close: 79100, volume: 1700 },
  { time: '19:00', open: 79100, high: 79200, low: 78600, close: 78700, volume: 1300 },
  { time: '21:00', open: 78700, high: 78900, low: 78500, close: 78800, volume: 1100 },
  { time: '23:00', open: 78800, high: 79000, low: 78400, close: 78600, volume: 1200 },
  { time: '02/03', open: 78600, high: 78800, low: 78300, close: 78500, volume: 1000 },
  { time: '05:00', open: 78500, high: 78700, low: 78200, close: 78400, volume: 900 },
  { time: '07:00', open: 78400, high: 79100, low: 78300, close: 79000, volume: 1400 },
  { time: '09:00', open: 79000, high: 79200, low: 78700, close: 78900, volume: 1300 },
  { time: '11:00', open: 78900, high: 79100, low: 78600, close: 78690, volume: 1100 },
])

// Calculate chart dimensions
const chartHeight = 400
const chartWidth = computed(() => candlesticks.value.length * 50)
const priceMin = computed(() => Math.min(...candlesticks.value.map(c => c.low)) - 500)
const priceMax = computed(() => Math.max(...candlesticks.value.map(c => c.high)) + 500)
const priceRange = computed(() => priceMax.value - priceMin.value)

function priceToY(price: number): number {
  return chartHeight - ((price - priceMin.value) / priceRange.value) * chartHeight
}

// Moving averages (simplified mock)
const ma7 = ref(78755.1)
const ma25 = ref(78524.0)
const ma99 = ref(78342.5)

const tabs = ['Graph', 'Info', 'Trading data']
const selectedTab = ref('Graph')

const chartTypes = ['Base version', 'Trading View', 'Depth charts']
const selectedChartType = ref('Base version')

function formatNumber(num: number, decimals = 2): string {
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

function formatVolume(num: number): string {
  if (num >= 1e9) return (num / 1e9).toFixed(2) + 'B'
  if (num >= 1e6) return (num / 1e6).toFixed(2) + 'M'
  if (num >= 1e3) return (num / 1e3).toFixed(2) + 'K'
  return num.toString()
}
</script>

<template>
  <div class="h-full flex flex-col gap-3 overflow-hidden">
    <!-- Top Ticker Bar -->
    <div class="glass-card px-4 py-2 flex items-center gap-6 text-xs overflow-x-auto">
      <span class="text-green-400">BTCUSDT +2.26%</span>
      <span class="text-green-400">ETHUSDT +2.75%</span>
      <span class="text-green-400">BNBUSDT +2.63%</span>
    </div>

    <!-- Main Chart Area -->
    <div class="flex-1 glass-card flex flex-col min-h-0 overflow-hidden">
      <!-- Trading Pair Header -->
      <div class="px-4 py-3 border-b border-gray-200 dark:border-white/10 flex items-center gap-4 flex-wrap">
        <div class="flex items-center gap-2">
          <button @click="isFavorite = !isFavorite" class="text-yellow-500">
            <StarIconSolid v-if="isFavorite" class="w-5 h-5" />
            <StarIcon v-else class="w-5 h-5" />
          </button>
          <div class="flex items-center gap-2">
            <div class="w-6 h-6 rounded-full bg-gradient-to-br from-orange-400 to-orange-600 flex items-center justify-center">
              <span class="text-white text-xs font-bold">B</span>
            </div>
            <span class="font-semibold text-gray-900 dark:text-white">{{ selectedPair }}</span>
            <span class="text-xs text-gray-500 dark:text-gray-400 px-1.5 py-0.5 bg-gray-100 dark:bg-white/10 rounded">Perpetual</span>
          </div>
        </div>

        <div class="flex items-center gap-6 text-sm flex-wrap">
          <div>
            <span class="text-2xl font-bold" :class="priceChangePercent >= 0 ? 'text-green-500' : 'text-red-500'">
              {{ formatNumber(currentPrice, 1) }}
            </span>
            <span class="text-xs text-gray-500 dark:text-gray-400 ml-1">
              {{ priceChangePercent >= 0 ? '+' : '' }}{{ formatNumber(priceChange) }} {{ priceChangePercent >= 0 ? '+' : '' }}{{ priceChangePercent }}%
            </span>
          </div>

          <div class="flex gap-4 text-xs">
            <div>
              <div class="text-gray-500 dark:text-gray-400">Mark price</div>
              <div class="text-gray-900 dark:text-white">{{ formatNumber(markPrice, 1) }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">Index</div>
              <div class="text-gray-900 dark:text-white">{{ formatNumber(indexPrice, 1) }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">Funding (8h) / Countdown</div>
              <div class="text-green-500">{{ fundingRate }}% / {{ fundingCountdown }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">24h High</div>
              <div class="text-gray-900 dark:text-white">{{ formatNumber(high24h, 1) }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">24h Low</div>
              <div class="text-gray-900 dark:text-white">{{ formatNumber(low24h, 1) }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">Volume 24h(BTC)</div>
              <div class="text-gray-900 dark:text-white">{{ formatVolume(volume24hBTC) }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">Volume 24h(USDT)</div>
              <div class="text-gray-900 dark:text-white">{{ formatVolume(volumeUSDT) }}</div>
            </div>
            <div>
              <div class="text-gray-500 dark:text-gray-400">Open Interest(USDT)</div>
              <div class="text-gray-900 dark:text-white">{{ formatVolume(openInterest) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Navigation -->
      <div class="px-4 py-2 border-b border-gray-200 dark:border-white/10 flex items-center justify-between">
        <div class="flex gap-4">
          <button
            v-for="tab in tabs"
            :key="tab"
            @click="selectedTab = tab"
            :class="[
              'text-sm py-1 transition-colors',
              selectedTab === tab
                ? 'text-cosmic-600 dark:text-cosmic-400 font-medium'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
            ]"
          >
            {{ tab }}
          </button>
        </div>
        <div class="flex gap-2 text-xs">
          <button
            v-for="type in chartTypes"
            :key="type"
            @click="selectedChartType = type"
            :class="[
              'px-3 py-1 rounded transition-colors',
              selectedChartType === type
                ? 'bg-cosmic-500/20 text-cosmic-600 dark:text-cosmic-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
            ]"
          >
            {{ type }}
          </button>
        </div>
      </div>

      <!-- Timeframe & Tools -->
      <div class="px-4 py-2 border-b border-gray-200 dark:border-white/10 flex items-center gap-4 text-xs">
        <div class="flex gap-1">
          <button
            v-for="tf in timeframes"
            :key="tf"
            @click="selectedTimeframe = tf"
            :class="[
              'px-2 py-1 rounded transition-colors',
              selectedTimeframe === tf
                ? 'bg-cosmic-500/20 text-cosmic-600 dark:text-cosmic-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
            ]"
          >
            {{ tf }}
          </button>
        </div>
        <div class="h-4 w-px bg-gray-200 dark:bg-white/10"></div>
        <span class="text-gray-500 dark:text-gray-400">Last price</span>
      </div>

      <!-- OHLC Info -->
      <div class="px-4 py-2 border-b border-gray-200 dark:border-white/10 flex items-center gap-6 text-xs">
        <span class="text-gray-500 dark:text-gray-400">2026/02/03 11:45</span>
        <span><span class="text-gray-500 dark:text-gray-400">Open</span> <span class="text-gray-900 dark:text-white">78 969.8</span></span>
        <span><span class="text-gray-500 dark:text-gray-400">High</span> <span class="text-green-500">78 975.0</span></span>
        <span><span class="text-gray-500 dark:text-gray-400">Low</span> <span class="text-red-500">78 642.1</span></span>
        <span><span class="text-gray-500 dark:text-gray-400">Close</span> <span class="text-gray-900 dark:text-white">78 690.7</span></span>
        <span><span class="text-gray-500 dark:text-gray-400">Change</span> <span class="text-red-500">-0.35%</span></span>
        <span><span class="text-gray-500 dark:text-gray-400">Range</span> <span class="text-gray-900 dark:text-white">0.42%</span></span>
      </div>

      <!-- MA Indicators -->
      <div class="px-4 py-2 border-b border-gray-200 dark:border-white/10 flex items-center gap-6 text-xs">
        <span><span class="text-yellow-500">MA(7)</span> <span class="text-yellow-500">{{ formatNumber(ma7, 1) }}</span></span>
        <span><span class="text-pink-500">MA(25)</span> <span class="text-pink-500">{{ formatNumber(ma25, 1) }}</span></span>
        <span><span class="text-purple-500">MA(99)</span> <span class="text-purple-500">{{ formatNumber(ma99, 1) }}</span></span>
      </div>

      <!-- Chart Area -->
      <div class="flex-1 relative overflow-hidden p-4">
        <div class="absolute inset-0 flex items-center justify-center text-gray-400 dark:text-gray-600 text-6xl font-bold opacity-10">
          ESPRITO
        </div>

        <!-- Simplified Candlestick Chart -->
        <div class="h-full w-full overflow-x-auto">
          <svg :width="chartWidth" :height="chartHeight" class="min-w-full">
            <!-- Grid lines -->
            <g class="grid-lines">
              <line
                v-for="i in 5"
                :key="'h-' + i"
                x1="0"
                :y1="(chartHeight / 5) * i"
                :x2="chartWidth"
                :y2="(chartHeight / 5) * i"
                class="stroke-gray-200 dark:stroke-white/5"
                stroke-dasharray="4"
              />
            </g>

            <!-- Candlesticks -->
            <g v-for="(candle, index) in candlesticks" :key="index">
              <!-- Wick -->
              <line
                :x1="index * 50 + 25"
                :y1="priceToY(candle.high)"
                :x2="index * 50 + 25"
                :y2="priceToY(candle.low)"
                :class="candle.close >= candle.open ? 'stroke-green-500' : 'stroke-red-500'"
                stroke-width="1"
              />
              <!-- Body -->
              <rect
                :x="index * 50 + 10"
                :y="priceToY(Math.max(candle.open, candle.close))"
                width="30"
                :height="Math.abs(priceToY(candle.open) - priceToY(candle.close)) || 2"
                :class="candle.close >= candle.open ? 'fill-green-500' : 'fill-red-500'"
              />
            </g>

            <!-- Moving Average Lines (simplified) -->
            <polyline
              :points="candlesticks.map((_, i) => `${i * 50 + 25},${priceToY(ma7 + (Math.random() - 0.5) * 500)}`).join(' ')"
              fill="none"
              class="stroke-yellow-500"
              stroke-width="1"
            />
            <polyline
              :points="candlesticks.map((_, i) => `${i * 50 + 25},${priceToY(ma25 + (Math.random() - 0.5) * 800)}`).join(' ')"
              fill="none"
              class="stroke-pink-500"
              stroke-width="1"
            />

            <!-- Current price indicator -->
            <g>
              <line
                x1="0"
                :y1="priceToY(currentPrice)"
                :x2="chartWidth"
                :y2="priceToY(currentPrice)"
                class="stroke-cosmic-500"
                stroke-dasharray="4"
                stroke-width="1"
              />
              <rect
                :x="chartWidth - 80"
                :y="priceToY(currentPrice) - 10"
                width="70"
                height="20"
                rx="4"
                class="fill-cosmic-500"
              />
              <text
                :x="chartWidth - 45"
                :y="priceToY(currentPrice) + 4"
                class="fill-white text-xs"
                text-anchor="middle"
              >
                {{ formatNumber(currentPrice, 1) }}
              </text>
            </g>
          </svg>
        </div>
      </div>

      <!-- Volume Bar -->
      <div class="px-4 py-2 border-t border-gray-200 dark:border-white/10 h-20">
        <div class="flex items-center gap-4 text-xs mb-2">
          <span class="text-gray-500 dark:text-gray-400">Vol(BTC)</span>
          <span class="text-gray-900 dark:text-white">803,233</span>
          <span class="text-gray-500 dark:text-gray-400">Vol(USDT)</span>
          <span class="text-gray-900 dark:text-white">63,286M</span>
        </div>
        <div class="flex items-end gap-1 h-10">
          <div
            v-for="(candle, index) in candlesticks"
            :key="'vol-' + index"
            :class="candle.close >= candle.open ? 'bg-green-500/50' : 'bg-red-500/50'"
            :style="{ height: `${(candle.volume / 2500) * 100}%`, width: '30px' }"
          ></div>
        </div>
      </div>
    </div>
  </div>
</template>
