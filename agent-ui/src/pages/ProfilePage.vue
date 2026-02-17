<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  UserCircleIcon,
  WalletIcon,
  Cog6ToothIcon,
  ShieldCheckIcon,
  BellIcon,
  DocumentTextIcon,
  ArrowRightOnRectangleIcon,
  PencilIcon,
  CheckIcon,
  XMarkIcon,
  ClipboardDocumentIcon
} from '@heroicons/vue/24/outline'

// User data
const user = ref({
  name: 'Anonymous User',
  email: 'user@example.com',
  avatar: '',
  joinDate: 'February 2026',
  tier: 'Standard',
  kycStatus: 'Not verified'
})

// Wallet data
const wallets = ref([
  {
    id: 1,
    name: 'TON Wallet',
    address: 'UQC7...x3Kp',
    fullAddress: 'UQC7VZ9PvB2xKj8nM3hL5kR9qW2eY4cX1zA7bD6fH0gI3Kp',
    balance: '1,234.56',
    currency: 'TON',
    isConnected: true
  },
  {
    id: 2,
    name: 'USDT Wallet',
    address: '0x7a3...9f2e',
    fullAddress: '0x7a3B5c8D9e1F2a3B4c5D6e7F8a9B0c1D2e3F4a5B9f2e',
    balance: '5,678.90',
    currency: 'USDT',
    isConnected: true
  }
])

// Trading stats
const tradingStats = ref({
  totalTrades: 156,
  winRate: 67.3,
  totalPnL: '+$12,456.78',
  avgTradeSize: '$2,345.00',
  favoriteAsset: 'BTCUSDT'
})

// Settings sections
const settingsSections = [
  { id: 'profile', label: 'Profile Settings', icon: UserCircleIcon },
  { id: 'wallets', label: 'Wallets', icon: WalletIcon },
  { id: 'security', label: 'Security', icon: ShieldCheckIcon },
  { id: 'notifications', label: 'Notifications', icon: BellIcon },
  { id: 'preferences', label: 'Trading Preferences', icon: Cog6ToothIcon },
  { id: 'history', label: 'Trade History', icon: DocumentTextIcon },
]

const activeSection = ref('profile')

// Edit mode
const isEditing = ref(false)
const editedName = ref(user.value.name)
const editedEmail = ref(user.value.email)

function startEditing() {
  editedName.value = user.value.name
  editedEmail.value = user.value.email
  isEditing.value = true
}

function saveChanges() {
  user.value.name = editedName.value
  user.value.email = editedEmail.value
  isEditing.value = false
}

function cancelEditing() {
  isEditing.value = false
}

// Copy address
const copiedAddress = ref<string | null>(null)

function copyAddress(address: string) {
  navigator.clipboard.writeText(address)
  copiedAddress.value = address
  setTimeout(() => {
    copiedAddress.value = null
  }, 2000)
}

// Notifications settings
const notifications = ref({
  priceAlerts: true,
  tradeExecutions: true,
  newsUpdates: false,
  marketAnalysis: true,
  securityAlerts: true
})

// Trading preferences
const preferences = ref({
  defaultLeverage: '10x',
  slippageTolerance: '0.5%',
  confirmTrades: true,
  autoStopLoss: false
})
</script>

<template>
  <div class="h-full flex flex-col lg:flex-row gap-4 overflow-hidden">
    <!-- Sidebar -->
    <div class="lg:w-64 shrink-0">
      <div class="glass-card p-4">
        <!-- User Avatar & Info -->
        <div class="flex flex-col items-center text-center mb-6">
          <div class="w-20 h-20 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-500 flex items-center justify-center mb-3">
            <UserCircleIcon class="w-12 h-12 text-white" />
          </div>
          <h2 class="font-semibold text-gray-900 dark:text-white">{{ user.name }}</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">{{ user.email }}</p>
          <div class="mt-2 px-3 py-1 rounded-full bg-cosmic-500/20 text-cosmic-600 dark:text-cosmic-400 text-xs font-medium">
            {{ user.tier }} Tier
          </div>
        </div>

        <!-- Navigation -->
        <nav class="space-y-1">
          <button
            v-for="section in settingsSections"
            :key="section.id"
            @click="activeSection = section.id"
            :class="[
              'w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors',
              activeSection === section.id
                ? 'bg-cosmic-500/20 text-cosmic-600 dark:text-cosmic-400 font-medium'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-white/5'
            ]"
          >
            <component :is="section.icon" class="w-5 h-5" />
            {{ section.label }}
          </button>
        </nav>

        <!-- Logout -->
        <button class="w-full flex items-center gap-3 px-3 py-2 mt-4 text-red-500 hover:bg-red-500/10 rounded-lg text-sm transition-colors">
          <ArrowRightOnRectangleIcon class="w-5 h-5" />
          Sign Out
        </button>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex-1 overflow-auto min-h-0">
      <!-- Profile Settings -->
      <div v-if="activeSection === 'profile'" class="space-y-4">
        <div class="glass-card p-6">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Profile Information</h3>
            <button
              v-if="!isEditing"
              @click="startEditing"
              class="flex items-center gap-2 text-sm text-cosmic-600 dark:text-cosmic-400 hover:underline"
            >
              <PencilIcon class="w-4 h-4" />
              Edit
            </button>
            <div v-else class="flex gap-2">
              <button
                @click="saveChanges"
                class="flex items-center gap-1 px-3 py-1 bg-green-500 text-white rounded-lg text-sm"
              >
                <CheckIcon class="w-4 h-4" />
                Save
              </button>
              <button
                @click="cancelEditing"
                class="flex items-center gap-1 px-3 py-1 bg-gray-500 text-white rounded-lg text-sm"
              >
                <XMarkIcon class="w-4 h-4" />
                Cancel
              </button>
            </div>
          </div>

          <div class="grid gap-4">
            <div>
              <label class="block text-sm text-gray-500 dark:text-gray-400 mb-1">Display Name</label>
              <input
                v-if="isEditing"
                v-model="editedName"
                type="text"
                class="w-full px-4 py-2 rounded-lg bg-gray-100 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-cosmic-500"
              />
              <p v-else class="text-gray-900 dark:text-white">{{ user.name }}</p>
            </div>
            <div>
              <label class="block text-sm text-gray-500 dark:text-gray-400 mb-1">Email</label>
              <input
                v-if="isEditing"
                v-model="editedEmail"
                type="email"
                class="w-full px-4 py-2 rounded-lg bg-gray-100 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-cosmic-500"
              />
              <p v-else class="text-gray-900 dark:text-white">{{ user.email }}</p>
            </div>
            <div>
              <label class="block text-sm text-gray-500 dark:text-gray-400 mb-1">Member Since</label>
              <p class="text-gray-900 dark:text-white">{{ user.joinDate }}</p>
            </div>
            <div>
              <label class="block text-sm text-gray-500 dark:text-gray-400 mb-1">KYC Status</label>
              <span class="inline-flex items-center gap-1 px-2 py-1 rounded bg-yellow-500/20 text-yellow-600 dark:text-yellow-400 text-sm">
                {{ user.kycStatus }}
              </span>
            </div>
          </div>
        </div>

        <!-- Trading Stats -->
        <div class="glass-card p-6">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Trading Statistics</h3>
          <div class="grid grid-cols-2 md:grid-cols-5 gap-4">
            <div class="text-center p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ tradingStats.totalTrades }}</div>
              <div class="text-sm text-gray-500 dark:text-gray-400">Total Trades</div>
            </div>
            <div class="text-center p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div class="text-2xl font-bold text-green-500">{{ tradingStats.winRate }}%</div>
              <div class="text-sm text-gray-500 dark:text-gray-400">Win Rate</div>
            </div>
            <div class="text-center p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div class="text-2xl font-bold text-green-500">{{ tradingStats.totalPnL }}</div>
              <div class="text-sm text-gray-500 dark:text-gray-400">Total PnL</div>
            </div>
            <div class="text-center p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ tradingStats.avgTradeSize }}</div>
              <div class="text-sm text-gray-500 dark:text-gray-400">Avg Trade Size</div>
            </div>
            <div class="text-center p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div class="text-2xl font-bold text-cosmic-600 dark:text-cosmic-400">{{ tradingStats.favoriteAsset }}</div>
              <div class="text-sm text-gray-500 dark:text-gray-400">Top Asset</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Wallets -->
      <div v-if="activeSection === 'wallets'" class="space-y-4">
        <div class="glass-card p-6">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Connected Wallets</h3>
            <button class="px-4 py-2 cosmic-button text-white rounded-lg text-sm">
              + Connect Wallet
            </button>
          </div>

          <div class="space-y-4">
            <div
              v-for="wallet in wallets"
              :key="wallet.id"
              class="p-4 bg-gray-50 dark:bg-white/5 rounded-lg"
            >
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-4">
                  <div class="w-10 h-10 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-500 flex items-center justify-center">
                    <WalletIcon class="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <h4 class="font-medium text-gray-900 dark:text-white">{{ wallet.name }}</h4>
                    <div class="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                      <span>{{ wallet.address }}</span>
                      <button
                        @click="copyAddress(wallet.fullAddress)"
                        class="hover:text-cosmic-500"
                      >
                        <ClipboardDocumentIcon v-if="copiedAddress !== wallet.fullAddress" class="w-4 h-4" />
                        <CheckIcon v-else class="w-4 h-4 text-green-500" />
                      </button>
                    </div>
                  </div>
                </div>
                <div class="text-right">
                  <div class="font-semibold text-gray-900 dark:text-white">{{ wallet.balance }} {{ wallet.currency }}</div>
                  <span class="text-xs text-green-500">Connected</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Security -->
      <div v-if="activeSection === 'security'" class="space-y-4">
        <div class="glass-card p-6">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-6">Security Settings</h3>

          <div class="space-y-4">
            <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div>
                <h4 class="font-medium text-gray-900 dark:text-white">Two-Factor Authentication</h4>
                <p class="text-sm text-gray-500 dark:text-gray-400">Add an extra layer of security</p>
              </div>
              <button class="px-4 py-2 bg-green-500 text-white rounded-lg text-sm">Enable</button>
            </div>

            <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div>
                <h4 class="font-medium text-gray-900 dark:text-white">Change Password</h4>
                <p class="text-sm text-gray-500 dark:text-gray-400">Update your account password</p>
              </div>
              <button class="px-4 py-2 border border-gray-300 dark:border-white/20 text-gray-700 dark:text-gray-300 rounded-lg text-sm">Change</button>
            </div>

            <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div>
                <h4 class="font-medium text-gray-900 dark:text-white">Active Sessions</h4>
                <p class="text-sm text-gray-500 dark:text-gray-400">Manage your active sessions</p>
              </div>
              <button class="px-4 py-2 border border-gray-300 dark:border-white/20 text-gray-700 dark:text-gray-300 rounded-lg text-sm">View All</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Notifications -->
      <div v-if="activeSection === 'notifications'" class="space-y-4">
        <div class="glass-card p-6">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-6">Notification Preferences</h3>

          <div class="space-y-4">
            <div v-for="(value, key) in notifications" :key="key" class="flex items-center justify-between p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div>
                <h4 class="font-medium text-gray-900 dark:text-white capitalize">{{ key.replace(/([A-Z])/g, ' $1').trim() }}</h4>
              </div>
              <label class="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" v-model="notifications[key]" class="sr-only peer">
                <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-cosmic-500 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-cosmic-500"></div>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- Trading Preferences -->
      <div v-if="activeSection === 'preferences'" class="space-y-4">
        <div class="glass-card p-6">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-6">Trading Preferences</h3>

          <div class="grid gap-4">
            <div>
              <label class="block text-sm text-gray-500 dark:text-gray-400 mb-2">Default Leverage</label>
              <select
                v-model="preferences.defaultLeverage"
                class="w-full px-4 py-2 rounded-lg bg-gray-100 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white"
              >
                <option>1x</option>
                <option>5x</option>
                <option>10x</option>
                <option>20x</option>
                <option>50x</option>
                <option>100x</option>
              </select>
            </div>

            <div>
              <label class="block text-sm text-gray-500 dark:text-gray-400 mb-2">Slippage Tolerance</label>
              <select
                v-model="preferences.slippageTolerance"
                class="w-full px-4 py-2 rounded-lg bg-gray-100 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white"
              >
                <option>0.1%</option>
                <option>0.5%</option>
                <option>1%</option>
                <option>2%</option>
                <option>5%</option>
              </select>
            </div>

            <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div>
                <h4 class="font-medium text-gray-900 dark:text-white">Confirm Trades</h4>
                <p class="text-sm text-gray-500 dark:text-gray-400">Ask for confirmation before executing trades</p>
              </div>
              <label class="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" v-model="preferences.confirmTrades" class="sr-only peer">
                <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-cosmic-500 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-cosmic-500"></div>
              </label>
            </div>

            <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-white/5 rounded-lg">
              <div>
                <h4 class="font-medium text-gray-900 dark:text-white">Auto Stop-Loss</h4>
                <p class="text-sm text-gray-500 dark:text-gray-400">Automatically set stop-loss for new positions</p>
              </div>
              <label class="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" v-model="preferences.autoStopLoss" class="sr-only peer">
                <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-cosmic-500 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-cosmic-500"></div>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- Trade History -->
      <div v-if="activeSection === 'history'" class="space-y-4">
        <div class="glass-card p-6">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-6">Trade History</h3>

          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="text-left text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-white/10">
                  <th class="pb-3 font-medium">Date</th>
                  <th class="pb-3 font-medium">Pair</th>
                  <th class="pb-3 font-medium">Type</th>
                  <th class="pb-3 font-medium">Size</th>
                  <th class="pb-3 font-medium">Entry</th>
                  <th class="pb-3 font-medium">Exit</th>
                  <th class="pb-3 font-medium">PnL</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100 dark:divide-white/5">
                <tr>
                  <td class="py-3 text-gray-900 dark:text-white">2026-02-03</td>
                  <td class="py-3 text-gray-900 dark:text-white">BTCUSDT</td>
                  <td class="py-3"><span class="text-green-500">Long</span></td>
                  <td class="py-3 text-gray-900 dark:text-white">0.5 BTC</td>
                  <td class="py-3 text-gray-900 dark:text-white">$77,500</td>
                  <td class="py-3 text-gray-900 dark:text-white">$78,900</td>
                  <td class="py-3 text-green-500">+$700.00</td>
                </tr>
                <tr>
                  <td class="py-3 text-gray-900 dark:text-white">2026-02-02</td>
                  <td class="py-3 text-gray-900 dark:text-white">ETHUSDT</td>
                  <td class="py-3"><span class="text-red-500">Short</span></td>
                  <td class="py-3 text-gray-900 dark:text-white">10 ETH</td>
                  <td class="py-3 text-gray-900 dark:text-white">$2,400</td>
                  <td class="py-3 text-gray-900 dark:text-white">$2,350</td>
                  <td class="py-3 text-green-500">+$500.00</td>
                </tr>
                <tr>
                  <td class="py-3 text-gray-900 dark:text-white">2026-02-01</td>
                  <td class="py-3 text-gray-900 dark:text-white">SOLUSDT</td>
                  <td class="py-3"><span class="text-green-500">Long</span></td>
                  <td class="py-3 text-gray-900 dark:text-white">100 SOL</td>
                  <td class="py-3 text-gray-900 dark:text-white">$195</td>
                  <td class="py-3 text-gray-900 dark:text-white">$190</td>
                  <td class="py-3 text-red-500">-$500.00</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
