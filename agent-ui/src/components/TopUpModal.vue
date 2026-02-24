<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { initiateDeposit, type DepositSession } from '../composables/useDeposit'
import { Wallet, Copy, CheckCircle, Loader, AlertCircle, Clock, AlertTriangle, Info, Zap } from 'lucide-vue-next'

const emits = defineEmits(['close'])

const deposit = ref<DepositSession | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const copiedAddress = ref(false)

async function startDepositSession() {
    loading.value = true
    error.value = null
    try {
        deposit.value = await initiateDeposit()
    } catch (e: any) {
        error.value = e?.message ?? 'Failed to start deposit session'
    } finally {
        loading.value = false
    }
}

async function copyToClipboard(text: string) {
    try {
        await navigator.clipboard.writeText(text)
        copiedAddress.value = true
        setTimeout(() => copiedAddress.value = false, 2000)
    } catch (e) {
        console.error('Failed to copy:', e)
    }
}

function onClose() {
    emits('close')
}

const timeRemaining = computed(() => {
    if (!deposit.value) return ''

    const expiresAt = new Date(deposit.value.expiresAt)
    const now = new Date()
    const diff = expiresAt.getTime() - now.getTime()

    if (diff <= 0) return 'Expired'

    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

    return `${hours}h ${minutes}m`
})

onMounted(() => {
    startDepositSession()
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
                    <p class="mt-4 text-sm text-gray-600 dark:text-gray-400">Starting deposit session...</p>
                </div>

                <div v-else-if="error" class="flex flex-col items-center justify-center py-12">
                    <AlertCircle :size="48" class="text-red-500" />
                    <p class="mt-4 text-sm text-red-600 dark:text-red-400">{{ error }}</p>
                    <button
                        @click="startDepositSession"
                        class="mt-4 px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition"
                    >
                        Retry
                    </button>
                </div>

                <div v-else-if="deposit" class="space-y-6">
                    <!-- Gas Reserve Warning -->
                    <div class="bg-amber-50 dark:bg-amber-900/20 border border-amber-300 dark:border-amber-700 rounded-xl px-4 py-3">
                        <div class="flex items-center gap-2 text-xs text-amber-800 dark:text-amber-300">
                            <AlertTriangle :size="14" class="text-amber-500 dark:text-amber-400 flex-shrink-0" />
                            <span>Keep at least <strong>0.3 TON</strong> for gas fees</span>
                            <div class="relative group ml-1">
                                <Info :size="13" class="text-amber-500 dark:text-amber-400 cursor-help" />
                                <div class="tooltip-box absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-64 p-2.5 bg-gray-900 dark:bg-gray-800 text-white text-xs rounded-lg opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-20 shadow-xl leading-relaxed">
                                    The TON blockchain requires a small amount of TON to pay for network transaction fees (gas). Without this reserve, swaps and other operations may fail. This is a blockchain requirement — not a service fee.
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Wallet Address -->
                    <div>
                        <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block">
                            Your Deposit Address
                        </label>
                        <div class="flex gap-2">
                            <div class="flex-1 bg-gray-50 dark:bg-white/10 border-2 border-gray-300 dark:border-white/20 rounded-xl px-4 py-3 font-mono text-xs text-gray-900 dark:text-white break-all">
                                {{ deposit.walletAddress }}
                            </div>
                            <button
                                @click="copyToClipboard(deposit.walletAddress)"
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
                        <h4 class="text-sm font-semibold text-gray-900 dark:text-white">How to deposit:</h4>
                        <ol class="space-y-2 text-xs text-gray-700 dark:text-gray-300">
                            <li class="flex gap-2">
                                <span class="font-bold">1.</span>
                                <span>Copy your deposit address above</span>
                            </li>
                            <li class="flex gap-2">
                                <span class="font-bold">2.</span>
                                <span>Open your TON wallet (Tonkeeper, Tonhub, etc.)</span>
                            </li>
                            <li class="flex gap-2">
                                <span class="font-bold">3.</span>
                                <span>Send TON or any supported Jettons to this address</span>
                            </li>
                        </ol>
                    </div>

                    <!-- Session Stats -->
                    <div class="flex gap-3">
                        <div class="flex-1 flex items-center gap-2.5 bg-gray-50 dark:bg-white/5 border border-gray-200 dark:border-white/10 rounded-xl px-3 py-2.5">
                            <Zap :size="16" class="text-emerald-500 flex-shrink-0" />
                            <div class="text-xs leading-tight">
                                <p class="font-semibold text-gray-900 dark:text-white">12–24 sec</p>
                                <p class="text-gray-500 dark:text-gray-400 mt-0.5">detection time</p>
                            </div>
                        </div>
                        <div class="flex-1 flex items-center gap-2.5 bg-gray-50 dark:bg-white/5 border border-gray-200 dark:border-white/10 rounded-xl px-3 py-2.5">
                            <Clock :size="16" class="text-blue-500 flex-shrink-0" />
                            <div class="text-xs leading-tight">
                                <p class="font-semibold text-gray-900 dark:text-white">{{ timeRemaining }}</p>
                                <p class="text-gray-500 dark:text-gray-400 mt-0.5">session remaining</p>
                            </div>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.cosmic-glow {
    box-shadow: 0 0 50px rgba(16, 185, 129, 0.2);
}

.gradient-text {
    background: linear-gradient(135deg, #10b981 0%, #06b6d4 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.tooltip-box::after {
    content: '';
    position: absolute;
    top: 100%;
    left: 50%;
    transform: translateX(-50%);
    border: 5px solid transparent;
    border-top-color: #111827;
}
</style>
