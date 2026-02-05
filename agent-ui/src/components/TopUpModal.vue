<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, defineEmits } from 'vue'
import { initiateDeposit, checkDepositStatus, type DepositRequest } from '../composables/useDeposit'
import { Wallet, Copy, CheckCircle, ExternalLink, Loader, AlertCircle, Clock } from 'lucide-vue-next'

const emits = defineEmits(['close', 'completed'])

const deposit = ref<DepositRequest | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const copiedAddress = ref(false)
const copiedCode = ref(false)
const pollInterval = ref<number | null>(null)
const status = ref<'pending' | 'completed' | 'expired'>('pending')
const amountTon = ref<string | null>(null)
const assetSymbol = ref<string>('TON')
const transactionHash = ref<string | null>(null)
const usdValue = ref<number | null>(null)
const assetType = ref<string | null>(null)

const timeRemaining = ref('')

async function initDeposit() {
    loading.value = true
    error.value = null
    try {
        deposit.value = await initiateDeposit()
        startPolling()
        updateTimeRemaining()
    } catch (e: any) {
        error.value = e?.message ?? 'Failed to create deposit request'
    } finally {
        loading.value = false
    }
}

async function pollStatus() {
    if (!deposit.value) return

    try {
        const statusData = await checkDepositStatus(deposit.value.depositRequestId)

        if (statusData.status === 'COMPLETED') {
            status.value = 'completed'
            amountTon.value = statusData.amountTon
            assetSymbol.value = statusData.jettonSymbol || statusData.assetType || 'TON'
            assetType.value = statusData.assetType
            transactionHash.value = statusData.transactionHash
            usdValue.value = statusData.usdValue
            stopPolling()
            emits('completed')
        } else if (statusData.status === 'EXPIRED') {
            status.value = 'expired'
            stopPolling()
        }
    } catch (e) {
        console.error('Failed to poll deposit status:', e)
    }
}

function startPolling() {
    if (pollInterval.value) return
    pollInterval.value = window.setInterval(pollStatus, 5000)
}

function stopPolling() {
    if (pollInterval.value) {
        clearInterval(pollInterval.value)
        pollInterval.value = null
    }
}

function updateTimeRemaining() {
    if (!deposit.value) return

    const expiresAt = new Date(deposit.value.expiresAt)
    const now = new Date()
    const diff = expiresAt.getTime() - now.getTime()

    if (diff <= 0) {
        timeRemaining.value = 'Expired'
        status.value = 'expired'
        stopPolling()
        return
    }

    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    timeRemaining.value = `${hours}h ${minutes}m`

    setTimeout(updateTimeRemaining, 60000) // Update every minute
}

async function copyToClipboard(text: string, type: 'address' | 'code') {
    try {
        await navigator.clipboard.writeText(text)
        if (type === 'address') {
            copiedAddress.value = true
            setTimeout(() => copiedAddress.value = false, 2000)
        } else {
            copiedCode.value = true
            setTimeout(() => copiedCode.value = false, 2000)
        }
    } catch (e) {
        console.error('Failed to copy:', e)
    }
}

function onClose() {
    stopPolling()
    emits('close')
}

const tonViewerLink = computed(() => {
    return transactionHash.value
        ? `https://tonviewer.com/transaction/${transactionHash.value}`
        : null
})

onMounted(() => {
    initDeposit()
})

onUnmounted(() => {
    stopPolling()
})
</script>

<template>
    <Teleport to="body">
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div class="absolute inset-0 bg-black/50 dark:bg-black/70" @click="onClose"></div>
            <div class="relative w-full max-w-md bg-white dark:bg-white/5 backdrop-blur-lg border-2 border-gray-300 dark:border-white/10 rounded-2xl p-8 shadow-2xl cosmic-glow">
                <div class="flex items-center justify-between mb-6 pb-4 border-b-2 border-gray-200 dark:border-white/10">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-full bg-gradient-to-br from-emerald-500 to-cyan-600 flex items-center justify-center shadow-lg">
                            <Wallet :size="20" class="text-white" />
                        </div>
                        <div>
                            <h3 class="text-xl font-bold text-gray-900 dark:text-white">
                                <span class="gradient-text">Deposit</span>
                            </h3>
                            <p class="text-xs text-gray-600 dark:text-gray-400 mt-0.5">Deposit TON or Jettons</p>
                        </div>
                    </div>
                    <button
                        class="w-8 h-8 rounded-lg flex items-center justify-center text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-white/10 hover:text-gray-900 dark:hover:text-white transition"
                        @click="onClose"
                        aria-label="Close"
                    >
                        <span class="text-2xl leading-none">×</span>
                    </button>
                </div>

                <div v-if="loading" class="flex flex-col items-center justify-center py-12">
                    <Loader :size="48" class="text-emerald-500 animate-spin" />
                    <p class="mt-4 text-sm text-gray-600 dark:text-gray-400">Initializing deposit...</p>
                </div>

                <div v-else-if="error" class="flex flex-col items-center justify-center py-12">
                    <AlertCircle :size="48" class="text-red-500" />
                    <p class="mt-4 text-sm text-red-600 dark:text-red-400">{{ error }}</p>
                    <button
                        @click="initDeposit"
                        class="mt-4 px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition"
                    >
                        Retry
                    </button>
                </div>

                <div v-else-if="status === 'completed'" class="flex flex-col items-center justify-center py-12">
                    <!-- Asset Icon -->
                    <div class="mb-4">
                        <img
                            v-if="assetType === 'TON'"
                            src="https://assets.coingecko.com/coins/images/17980/small/ton_symbol.png"
                            alt="TON"
                            class="w-20 h-20 rounded-full"
                        />
                        <div
                            v-else
                            class="w-20 h-20 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-white font-bold text-2xl"
                        >
                            {{ assetSymbol.substring(0, 2) }}
                        </div>
                    </div>
                    <CheckCircle :size="64" class="text-emerald-500" />
                    <h4 class="mt-4 text-2xl font-bold text-gray-900 dark:text-white">Deposit Received!</h4>
                    <p class="mt-2 text-lg font-semibold text-gray-700 dark:text-gray-300">{{ amountTon }} {{ assetSymbol }}</p>
                    <p v-if="usdValue" class="mt-1 text-sm text-gray-600 dark:text-gray-400">≈ ${{ usdValue.toFixed(2) }}</p>
                    <a
                        v-if="tonViewerLink"
                        :href="tonViewerLink"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="mt-4 flex items-center gap-2 text-sm text-emerald-600 dark:text-emerald-400 hover:text-emerald-700 dark:hover:text-emerald-300 transition"
                    >
                        <span>View Transaction</span>
                        <ExternalLink :size="16" />
                    </a>
                    <button
                        @click="onClose"
                        class="mt-6 px-6 py-3 bg-gradient-to-r from-emerald-500 to-cyan-600 text-white font-semibold rounded-xl hover:shadow-lg transition"
                    >
                        Close
                    </button>
                </div>

                <div v-else-if="status === 'expired'" class="flex flex-col items-center justify-center py-12">
                    <Clock :size="64" class="text-orange-500" />
                    <h4 class="mt-4 text-2xl font-bold text-gray-900 dark:text-white">Request Expired</h4>
                    <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">This deposit request has expired. Please create a new one.</p>
                    <button
                        @click="initDeposit"
                        class="mt-6 px-6 py-3 bg-gradient-to-r from-emerald-500 to-cyan-600 text-white font-semibold rounded-xl hover:shadow-lg transition"
                    >
                        Create New Request
                    </button>
                </div>

                <div v-else-if="deposit" class="space-y-6">
                    <!-- Warning -->
                    <div class="bg-amber-50 dark:bg-amber-900/20 border-2 border-amber-300 dark:border-amber-700 rounded-xl p-4">
                        <div class="flex gap-3">
                            <AlertCircle :size="20" class="text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
                            <div class="text-xs text-amber-800 dark:text-amber-300 space-y-1">
                                <p><strong>Important:</strong> Include the code below in the comment/memo field when sending your deposit.</p>
                                <p><strong>One-time use:</strong> This code works for a single transaction. For additional deposits, create a new request.</p>
                            </div>
                        </div>
                    </div>

                    <!-- Status Badge -->
                    <div class="flex items-center justify-between">
                        <span class="inline-flex items-center gap-2 px-3 py-1.5 bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 text-xs font-semibold rounded-lg">
                            <Loader :size="14" class="animate-spin" />
                            Waiting for deposit
                        </span>
                        <span class="text-xs text-gray-600 dark:text-gray-400">
                            Expires in {{ timeRemaining }}
                        </span>
                    </div>

                    <!-- Deposit Code -->
                    <div>
                        <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block">
                            Deposit Code (include in comment)
                        </label>
                        <div class="flex gap-2">
                            <div class="flex-1 bg-gray-50 dark:bg-white/10 border-2 border-gray-300 dark:border-white/20 rounded-xl px-4 py-3 font-mono text-lg font-bold text-center tracking-wider text-gray-900 dark:text-white">
                                {{ deposit.code }}
                            </div>
                            <button
                                @click="copyToClipboard(deposit.code, 'code')"
                                class="px-4 rounded-xl bg-emerald-500 text-white hover:bg-emerald-600 transition flex items-center justify-center"
                                :class="{ 'bg-green-600': copiedCode }"
                            >
                                <CheckCircle v-if="copiedCode" :size="20" />
                                <Copy v-else :size="20" />
                            </button>
                        </div>
                    </div>

                    <!-- Wallet Address -->
                    <div>
                        <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block">
                            Deposit Address (TON or Jettons)
                        </label>
                        <div class="flex gap-2">
                            <div class="flex-1 bg-gray-50 dark:bg-white/10 border-2 border-gray-300 dark:border-white/20 rounded-xl px-4 py-3 font-mono text-xs text-gray-900 dark:text-white break-all">
                                {{ deposit.depositWalletAddress }}
                            </div>
                            <button
                                @click="copyToClipboard(deposit.depositWalletAddress, 'address')"
                                class="px-4 rounded-xl bg-emerald-500 text-white hover:bg-emerald-600 transition flex items-center justify-center"
                                :class="{ 'bg-green-600': copiedAddress }"
                            >
                                <CheckCircle v-if="copiedAddress" :size="20" />
                                <Copy v-else :size="20" />
                            </button>
                        </div>
                    </div>

                    <!-- Instructions -->
                    <div class="bg-gray-50 dark:bg-white/5 rounded-xl p-4 space-y-2">
                        <h4 class="text-sm font-semibold text-gray-900 dark:text-white">Instructions:</h4>
                        <ol class="space-y-2 text-xs text-gray-700 dark:text-gray-300">
                            <li class="flex gap-2">
                                <span class="font-bold">1.</span>
                                <span>Copy the deposit code above</span>
                            </li>
                            <li class="flex gap-2">
                                <span class="font-bold">2.</span>
                                <span>Open your TON wallet</span>
                            </li>
                            <li class="flex gap-2">
                                <span class="font-bold">3.</span>
                                <span>Send TON or Jettons to the address above</span>
                            </li>
                            <li class="flex gap-2">
                                <span class="font-bold">4.</span>
                                <span><strong>Paste the code in the comment/memo field</strong></span>
                            </li>
                            <li class="flex gap-2">
                                <span class="font-bold">5.</span>
                                <span>Wait for confirmation (usually 10-30 seconds)</span>
                            </li>
                            <li class="flex gap-2">
                                <span class="font-bold">6.</span>
                                <span>For additional deposits, click "Deposit" again to get a new code</span>
                            </li>
                        </ol>
                    </div>
                </div>
            </div>
        </div>
    </Teleport>
</template>
