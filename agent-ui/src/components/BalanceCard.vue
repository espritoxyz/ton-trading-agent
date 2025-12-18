<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import { accessToken, userId } from '../composables/useAuth.ts'
import { balanceUsd, loadingBalance, refreshBalance, balanceError } from '../composables/useBalance.ts'
import Tabs from './Tabs.vue'
import TabPane from './TabPane.vue'
import TabLabel from './TabLabel.vue'
import AssetCard from './AssetCard.vue'
import tonIcon from '../assets/tokens/ton.png'
import usdtIcon from '../assets/tokens/usdt.png'
import stonfiIcon from '../assets/tokens/stonfi.png'

const loggedIn = computed(() => !!accessToken.value)
const activeTab = ref('assets')

// Mock data for tokens - в будущем можно заменить на реальные данные из API
const tokens = ref([
  {
    name: 'Toncoin',
    ticker: 'TON',
    amount: '1250.5',
    usdValue: '6,250.25',
    icon: tonIcon
  },
  {
    name: 'Tether',
    ticker: 'USDT',
    amount: '8,500.0',
    usdValue: '8,500.00',
    icon: usdtIcon 
  },
  {
    name: 'STON.fi',
    ticker: 'STONFI',
    amount: '500.0',
    usdValue: '250.50',
    icon: stonfiIcon 
  }
])

onMounted(async () => {
  if (loggedIn.value) await refreshBalance()
})
</script>

<template>
  <div v-if="!loggedIn" class="rounded-2xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
    <div class="text-sm text-gray-500 dark:text-gray-400">
      Login to see your balance.
    </div>
  </div>

  <Tabs v-else v-model="activeTab">
    <template #labels>
      <TabLabel id="assets" label="Assets" />
      <TabLabel id="orders" label="Orders" />
      <TabLabel id="activity" label="Activity" />
    </template>

    <TabPane id="assets" label="Assets" :default="true">
      <div class="space-y-2">
        <AssetCard
          v-for="token in tokens"
          :key="token.ticker"
          :asset="token"
        />
      </div>
    </TabPane>

    <TabPane id="orders" label="Orders">
      <div class="text-sm text-gray-500 dark:text-gray-400 py-2">Orders will be displayed here</div>
    </TabPane>

    <TabPane id="activity" label="Activity">
      <div class="text-sm text-gray-500 dark:text-gray-400 py-2">Activity will be displayed here</div>
    </TabPane>
  </Tabs>
</template>
