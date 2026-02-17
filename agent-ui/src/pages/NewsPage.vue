<script setup lang="ts">
import { ref, computed } from 'vue'
import { MagnifyingGlassIcon, FunnelIcon, ArrowTopRightOnSquareIcon, ClockIcon } from '@heroicons/vue/24/outline'

// News categories
const categories = ['All', 'Market', 'Regulation', 'DeFi', 'NFT', 'Technology', 'Analysis']
const selectedCategory = ref('All')

// Search
const searchQuery = ref('')

// News items
interface NewsItem {
  id: number
  title: string
  summary: string
  category: string
  source: string
  time: string
  imageUrl?: string
  isHot?: boolean
}

const newsItems = ref<NewsItem[]>([
  {
    id: 1,
    title: 'Bitcoin Breaks $79,000 Resistance Level Amid Strong Institutional Demand',
    summary: 'Bitcoin reached a new high as institutional investors continue to accumulate, with major hedge funds increasing their positions significantly.',
    category: 'Market',
    source: 'CryptoNews',
    time: '15 min ago',
    isHot: true
  },
  {
    id: 2,
    title: 'Gnosis Co-Founder Criticizes Crypto Industry\'s Shift Towards Centralization',
    summary: 'In a recent interview, the co-founder of Gnosis expressed concerns about the increasing centralization trends in the cryptocurrency space.',
    category: 'Analysis',
    source: 'DeFi Daily',
    time: '34 min ago'
  },
  {
    id: 3,
    title: 'Dubai Diamond Inventory Tokenized on XRP Ledger',
    summary: 'A major Dubai-based diamond company has announced the tokenization of its entire inventory on the XRP Ledger blockchain.',
    category: 'Technology',
    source: 'Blockchain Today',
    time: '1 hour ago'
  },
  {
    id: 4,
    title: 'SEC Approves New Framework for Cryptocurrency ETF Applications',
    summary: 'The U.S. Securities and Exchange Commission has published new guidelines that could streamline the approval process for crypto ETFs.',
    category: 'Regulation',
    source: 'Financial Times',
    time: '2 hours ago'
  },
  {
    id: 5,
    title: 'Ethereum Layer 2 Solutions See Record Transaction Volume',
    summary: 'Arbitrum and Optimism combined processed over 5 million transactions in the last 24 hours, setting new records for L2 scaling solutions.',
    category: 'DeFi',
    source: 'ETH Daily',
    time: '3 hours ago'
  },
  {
    id: 6,
    title: 'TON Network Announces Major Protocol Upgrade',
    summary: 'The Open Network has announced a significant protocol upgrade that will improve transaction speeds and reduce gas fees by 40%.',
    category: 'Technology',
    source: 'TON News',
    time: '4 hours ago',
    isHot: true
  },
  {
    id: 7,
    title: 'NFT Market Shows Signs of Recovery with Blue-Chip Collections Leading',
    summary: 'After months of decline, the NFT market is showing positive momentum with CryptoPunks and BAYC seeing increased trading activity.',
    category: 'NFT',
    source: 'NFT Insider',
    time: '5 hours ago'
  },
  {
    id: 8,
    title: 'Central Banks Worldwide Accelerate CBDC Development',
    summary: 'Over 130 countries are now exploring or developing central bank digital currencies, with several pilot programs launching this quarter.',
    category: 'Regulation',
    source: 'Reuters',
    time: '6 hours ago'
  },
])

// Filtered news
const filteredNews = computed(() => {
  let filtered = newsItems.value

  if (selectedCategory.value !== 'All') {
    filtered = filtered.filter(item => item.category === selectedCategory.value)
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(item =>
      item.title.toLowerCase().includes(query) ||
      item.summary.toLowerCase().includes(query)
    )
  }

  return filtered
})

// Featured news (first hot item or first item)
const featuredNews = computed(() => {
  return newsItems.value.find(item => item.isHot) || newsItems.value[0]
})

function getCategoryColor(category: string): string {
  const colors: Record<string, string> = {
    Market: 'bg-green-500/20 text-green-400',
    Regulation: 'bg-blue-500/20 text-blue-400',
    DeFi: 'bg-purple-500/20 text-purple-400',
    NFT: 'bg-pink-500/20 text-pink-400',
    Technology: 'bg-cosmic-500/20 text-cosmic-400',
    Analysis: 'bg-yellow-500/20 text-yellow-400',
  }
  return colors[category] || 'bg-gray-500/20 text-gray-400'
}
</script>

<template>
  <div class="h-full flex flex-col gap-4 overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between flex-wrap gap-4">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Crypto News</h1>

      <!-- Search -->
      <div class="relative">
        <MagnifyingGlassIcon class="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search news..."
          class="pl-10 pr-4 py-2 rounded-lg bg-gray-100 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500"
        />
      </div>
    </div>

    <!-- Category Tabs -->
    <div class="flex gap-2 overflow-x-auto pb-2">
      <button
        v-for="category in categories"
        :key="category"
        @click="selectedCategory = category"
        :class="[
          'px-4 py-2 text-sm rounded-lg transition-colors whitespace-nowrap',
          selectedCategory === category
            ? 'bg-cosmic-500/20 text-cosmic-600 dark:text-cosmic-400 font-medium'
            : 'bg-gray-100 dark:bg-white/5 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
        ]"
      >
        {{ category }}
      </button>
    </div>

    <!-- News Content -->
    <div class="flex-1 overflow-auto min-h-0">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <!-- Featured News -->
        <div v-if="featuredNews && selectedCategory === 'All'" class="lg:col-span-2 glass-card overflow-hidden">
          <div class="aspect-video bg-gradient-to-br from-cosmic-600/30 to-purple-600/30 flex items-center justify-center">
            <div class="text-6xl opacity-20">
              <svg class="w-24 h-24" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
              </svg>
            </div>
          </div>
          <div class="p-6">
            <div class="flex items-center gap-3 mb-3">
              <span :class="['px-2 py-1 rounded text-xs font-medium', getCategoryColor(featuredNews.category)]">
                {{ featuredNews.category }}
              </span>
              <span v-if="featuredNews.isHot" class="px-2 py-1 rounded text-xs font-medium bg-red-500/20 text-red-400">
                HOT
              </span>
            </div>
            <h2 class="text-xl font-bold text-gray-900 dark:text-white mb-3">
              {{ featuredNews.title }}
            </h2>
            <p class="text-gray-600 dark:text-gray-400 mb-4">
              {{ featuredNews.summary }}
            </p>
            <div class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-4 text-gray-500 dark:text-gray-400">
                <span>{{ featuredNews.source }}</span>
                <span class="flex items-center gap-1">
                  <ClockIcon class="w-4 h-4" />
                  {{ featuredNews.time }}
                </span>
              </div>
              <button class="flex items-center gap-1 text-cosmic-600 dark:text-cosmic-400 hover:underline">
                Read more <ArrowTopRightOnSquareIcon class="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        <!-- News List -->
        <div :class="selectedCategory === 'All' ? 'lg:col-span-1' : 'lg:col-span-3'">
          <div :class="[
            'grid gap-4',
            selectedCategory === 'All' ? 'grid-cols-1' : 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3'
          ]">
            <div
              v-for="item in filteredNews.filter(n => selectedCategory !== 'All' || n.id !== featuredNews?.id)"
              :key="item.id"
              class="glass-card p-4 hover:bg-gray-50 dark:hover:bg-white/5 transition-colors cursor-pointer"
            >
              <div class="flex items-center gap-2 mb-2">
                <span :class="['px-2 py-0.5 rounded text-xs font-medium', getCategoryColor(item.category)]">
                  {{ item.category }}
                </span>
                <span v-if="item.isHot" class="px-2 py-0.5 rounded text-xs font-medium bg-red-500/20 text-red-400">
                  HOT
                </span>
              </div>
              <h3 class="font-medium text-gray-900 dark:text-white mb-2 line-clamp-2">
                {{ item.title }}
              </h3>
              <p class="text-sm text-gray-600 dark:text-gray-400 mb-3 line-clamp-2">
                {{ item.summary }}
              </p>
              <div class="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
                <span>{{ item.source }}</span>
                <span class="flex items-center gap-1">
                  <ClockIcon class="w-3 h-3" />
                  {{ item.time }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Market Pulse Section -->
      <div class="mt-6 glass-card p-4">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Market Pulse</h3>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div class="text-center">
            <div class="text-2xl font-bold text-green-500">+2.78%</div>
            <div class="text-sm text-gray-500 dark:text-gray-400">BTC 24h</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-green-500">+3.76%</div>
            <div class="text-sm text-gray-500 dark:text-gray-400">ETH 24h</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-gray-900 dark:text-white">72</div>
            <div class="text-sm text-gray-500 dark:text-gray-400">Fear & Greed</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-cosmic-600 dark:text-cosmic-400">$2.8T</div>
            <div class="text-sm text-gray-500 dark:text-gray-400">Total Market Cap</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
