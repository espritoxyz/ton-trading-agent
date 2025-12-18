<script setup lang="ts">
interface Asset {
  name: string
  ticker: string
  amount: string
  usdValue: string
  icon: string | null
}

defineProps<{
  asset: Asset
}>()
</script>

<template>
  <div class="flex items-center justify-between rounded-lg p-3 bg-gray-200/30 dark:bg-gray-700/30 hover:bg-gray-200/80 dark:hover:bg-gray-700/80 transition-colors">
    <div class="flex items-center gap-3">
      <div class="flex h-10 w-10 items-center justify-center rounded-full bg-white dark:bg-gray-700 overflow-hidden">
        <img
          v-if="asset.icon"
          :src="asset.icon"
          :alt="asset.name"
          class="w-full h-full object-cover"
        />
        <div v-else class="flex h-full w-full items-center justify-center bg-gray-200 text-gray-400 text-xs font-medium dark:bg-gray-600 dark:text-gray-500">
          {{ asset.ticker.substring(0, 2) }}
        </div>
      </div>
      <div>
        <div class="text-sm font-semibold text-gray-900 dark:text-gray-100">{{ asset.name }}</div>
        <div class="text-xs text-gray-500 dark:text-gray-400">
          <span class="font-mono">{{ asset.amount }}</span>
          <span class="ml-1">{{ asset.ticker }}</span>
        </div>
      </div>
    </div>
    <div class="text-right">
      <div class="text-sm font-semibold text-gray-900 dark:text-gray-100">${{ asset.usdValue }}</div>
    </div>
  </div>
</template>

