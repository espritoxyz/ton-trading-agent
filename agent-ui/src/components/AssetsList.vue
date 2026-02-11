<template>
  <div class="assets-list">
    <!-- Loading State -->
    <div v-if="loadingAssets" class="flex justify-center py-6">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-cyan-500"></div>
    </div>

    <!-- Error State -->
    <div v-else-if="assetsError" class="text-red-400 text-sm p-4 bg-red-900/20 rounded-lg">
      {{ assetsError }}
    </div>

    <!-- Empty State -->
    <div v-else-if="assets.length === 0" class="text-gray-400 text-sm text-center py-6">
      No assets found. Make a deposit to get started!
    </div>

    <!-- Assets List -->
    <div v-else class="space-y-2">
      <!-- Visible Assets (top 5 or 10) -->
      <div
          v-for="asset in displayedAssets"
          :key="asset.id"
          class="asset-item flex items-center gap-3 p-3 rounded-lg bg-gray-800/50 hover:bg-gray-700/50 transition-colors"
      >
        <!-- Asset Icon -->
        <div class="asset-icon flex-shrink-0">
          <img
              v-if="asset.imageUrl"
              :src="asset.imageUrl"
              :alt="asset.symbol || 'Token'"
              class="w-10 h-10 rounded-full object-cover"
              @error="handleImageError"
          />
          <div
              v-else
              class="w-10 h-10 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-white font-bold text-sm"
          >
            {{ getInitials(asset.symbol || asset.name) }}
          </div>
        </div>

        <!-- Asset Info -->
        <div class="flex-1 min-w-0">
          <div class="flex items-baseline gap-2">
            <span class="font-semibold text-white">{{ asset.symbol || 'Unknown' }}</span>
            <span v-if="asset.name && asset.name !== asset.symbol" class="text-xs text-gray-400 truncate">
              {{ asset.name }}
            </span>
          </div>
          <div class="flex items-center gap-1.5 text-xs text-gray-400">
            <span
                v-if="asset.address !== 'TON'"
                @click="copyAddress(asset.address, asset.id)"
                class="truncate cursor-pointer hover:text-cyan-400 transition-colors"
                title="Click to copy address"
            >
              {{ formatAddress(asset.address) }}
            </span>
            <span v-else class="truncate">{{ formatAddress(asset.address) }}</span>
            <!-- Action Icons -->
            <div class="flex items-center gap-1 flex-shrink-0">
              <!-- Copy Address (only for jettons, not for TON) -->
              <button
                  v-if="asset.address !== 'TON'"
                  @click="copyAddress(asset.address, asset.id)"
                  :class="{'copied': copiedAssetId === asset.id}"
                  class="copy-btn p-0.5 hover:text-cyan-400 transition-all duration-200"
                  title="Copy address"
              >
                <svg v-if="copiedAssetId !== asset.id" class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                </svg>
                <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
              </button>
              <!-- TonViewer -->
              <a
                  :href="getTonViewerUrl(asset.address)"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="p-0.5 hover:opacity-80 transition-opacity flex items-center"
                  title="View on TonViewer"
              >
                <img
                    src="https://www.google.com/s2/favicons?domain=tonviewer.com&sz=64"
                    alt="TonViewer"
                    class="w-3.5 h-3.5"
                />
              </a>
            </div>
          </div>
        </div>

        <!-- Asset Amount -->
        <div class="text-right flex-shrink-0">
          <div class="font-mono font-semibold text-white">
            {{ asset.readableAmount }} {{ asset.symbol }}
          </div>
          <div v-if="asset.unitPrice" class="text-xs text-gray-400">
            ${{ formatPrice(asset.unitPrice) }} / {{ asset.symbol }}
          </div>
        </div>
      </div>

      <!-- Show More/Less Button -->
      <button
          v-if="assets.length > displayLimit"
          @click="toggleShowAll"
          class="w-full py-2 text-sm text-cyan-400 hover:text-cyan-300 transition-colors flex items-center justify-center gap-2"
      >
        <span>{{ showAll ? 'Show Less' : `Show All (${assets.length - displayLimit} more)` }}</span>
        <svg
            :class="{ 'rotate-180': showAll }"
            class="w-4 h-4 transition-transform"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref} from 'vue'

interface Props {
  displayLimit?: number
}

const props = withDefaults(defineProps<Props>(), {
  displayLimit: 5
})

// Inject wallet state from parent
const walletState = inject<any>('walletState')
if (!walletState) {
  throw new Error('WalletState not provided')
}

const { assets, loadingWalletState: loadingAssets, walletStateError: assetsError } = walletState
const showAll = ref(false)
const copiedAssetId = ref<number | null>(null)

const displayedAssets = computed(() => {
  if (showAll.value) {
    return assets.value
  }
  return assets.value.slice(0, props.displayLimit)
})

const toggleShowAll = () => {
  showAll.value = !showAll.value
}

const getInitials = (text?: string) => {
  if (!text) return '?'
  return text.substring(0, 2).toUpperCase()
}

const formatAddress = (address: string) => {
  if (address === 'TON') return 'Native Token'
  if (address.length <= 12) return address
  return `${address.substring(0, 6)}...${address.substring(address.length - 6)}`
}

const handleImageError = (event: Event) => {
  // Hide broken image
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}

const formatPrice = (price: number) => {
  if (price >= 1) {
    // For prices >= 1, show 2 decimals
    return price.toFixed(2)
  } else if (price >= 0.01) {
    // For prices >= 0.01, show up to 4 decimals
    return price.toFixed(4).replace(/\.?0+$/, '')
  } else if (price >= 0.0001) {
    // For prices >= 0.0001, show up to 6 decimals
    return price.toFixed(6).replace(/\.?0+$/, '')
  } else {
    // For very small prices, show up to 8 decimals
    return price.toFixed(8).replace(/\.?0+$/, '')
  }
}

const copyAddress = async (address: string, assetId: number) => {
  try {
    await navigator.clipboard.writeText(address)
    copiedAssetId.value = assetId
    // Reset after 2 seconds
    setTimeout(() => {
      copiedAssetId.value = null
    }, 2000)
  } catch (err) {
    console.error('Failed to copy address:', err)
  }
}

const getTonViewerUrl = (address: string) => {
  if (address === 'TON') {
    // For native TON, just link to tonviewer homepage or TON currency page
    return 'https://tonviewer.com/'
  }
  return `https://tonviewer.com/${address}`
}
</script>

<style scoped>
.asset-item {
  border: 1px solid rgba(99, 102, 241, 0.1);
}

.asset-item:hover {
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
