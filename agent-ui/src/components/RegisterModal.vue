<script setup lang="ts">
import { ref, defineEmits } from 'vue'
import { register, login } from '../composables/useAuth'
import { refreshBalance } from '../composables/useBalance'
import { refreshProfile } from '../composables/useAuth'

const emits = defineEmits(['registered','close'])

const email = ref('')
const password = ref('')
const displayName = ref('')
const submitting = ref(false)
const error = ref<string | null>(null)

async function sleep(ms: number) { return new Promise(res => setTimeout(res, ms)) }

async function tryAutoLogin(emailVal: string, passVal: string) {
  const maxAttempts = 5
  let attempt = 0
  while (attempt < maxAttempts) {
    try {
      await login(emailVal, passVal)
      return true
    } catch (e) {
      attempt++
      // if 4xx, break early (bad credentials); otherwise retry
      const is4xx = (e?.response?.status ?? 0) >= 400 && (e?.response?.status ?? 0) < 500
      if (is4xx) throw e
      await sleep( Math.min(1000 * 2.0.pow(attempt), 5000) )
    }
  }
  return false
}

async function onSubmit() {
  error.value = null
  submitting.value = true
  try {
    const data = await register(email.value, password.value, displayName.value)
    // auto-login with retries
    const logged = await tryAutoLogin(email.value, password.value)
    if (!logged) {
      error.value = 'Registered but failed to auto-login — please login manually.'
      emits('registered', data)
      return
    }

    await Promise.all([refreshProfile(), refreshBalance()])
    emits('registered', data)
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? e?.message ?? 'Registration failed'
  } finally {
    submitting.value = false
  }
}

function onClose() {
  emits('close')
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div class="absolute inset-0 bg-black/50 dark:bg-black/70" @click="onClose"></div>
      <div class="relative w-full max-w-md bg-white dark:bg-white/5 backdrop-blur-lg border-2 border-gray-300 dark:border-white/10 rounded-2xl p-8 shadow-2xl cosmic-glow">
        <div class="flex items-center justify-between mb-6 pb-4 border-b-2 border-gray-200 dark:border-white/10">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-600 flex items-center justify-center shadow-lg">
              <span class="text-xl">✨</span>
            </div>
            <div>
              <h3 class="text-xl font-bold text-gray-900 dark:text-white">
                <span class="gradient-text">Create Account</span>
              </h3>
              <p class="text-xs text-gray-600 dark:text-gray-400 mt-0.5">Join Esprito AI</p>
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

        <div class="space-y-4">
          <div>
            <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block flex items-center gap-1.5">
              <span>📧</span>
              <span>Email</span>
            </label>
            <input
              v-model="email"
              type="email"
              placeholder="your@email.com"
              class="w-full rounded-xl bg-gray-50 dark:bg-white/10 border-2 border-gray-300 dark:border-white/20 px-4 py-3 text-sm font-medium text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-cosmic-500 focus:bg-white dark:focus:bg-white/15 transition shadow-sm"
            />
          </div>

          <div>
            <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block flex items-center gap-1.5">
              <span>🔒</span>
              <span>Password</span>
            </label>
            <input
              v-model="password"
              type="password"
              placeholder="••••••••"
              class="w-full rounded-xl bg-gray-50 dark:bg-white/10 border-2 border-gray-300 dark:border-white/20 px-4 py-3 text-sm font-medium text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-cosmic-500 focus:bg-white dark:focus:bg-white/15 transition shadow-sm"
            />
          </div>

          <div>
            <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block flex items-center gap-1.5">
              <span>👤</span>
              <span>Display Name (Optional)</span>
            </label>
            <input
              v-model="displayName"
              type="text"
              placeholder="Your Name"
              class="w-full rounded-xl bg-gray-50 dark:bg-white/10 border-2 border-gray-300 dark:border-white/20 px-4 py-3 text-sm font-medium text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-cosmic-500 focus:bg-white dark:focus:bg-white/15 transition shadow-sm"
            />
          </div>

          <div v-if="error" class="flex items-start gap-3 p-4 rounded-xl bg-red-100 dark:bg-red-500/10 border-2 border-red-400 dark:border-red-500/30 shadow-sm">
            <div class="text-xl flex-shrink-0">⚠️</div>
            <div>
              <div class="text-xs font-bold text-red-900 dark:text-red-300 mb-1">Registration Error</div>
              <div class="text-xs font-medium text-red-800 dark:text-red-400">{{ error }}</div>
            </div>
          </div>

          <div class="flex gap-3 pt-3">
            <button
              class="flex-1 rounded-xl bg-gray-200 dark:bg-white/10 px-4 py-3 text-sm font-bold text-gray-800 dark:text-white hover:bg-gray-300 dark:hover:bg-white/20 transition border-2 border-gray-400 dark:border-white/20 shadow-sm"
              @click="onClose"
            >
              Cancel
            </button>
            <button
              class="cosmic-button flex-1 rounded-xl px-4 py-3 text-sm font-bold text-white disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
              :disabled="submitting || !email || !password"
              @click="onSubmit"
            >
              <span v-if="submitting" class="flex items-center justify-center gap-2">
                <span class="animate-spin">⟳</span>
                <span>Creating...</span>
              </span>
              <span v-else class="flex items-center justify-center gap-2">
                <span>✨</span>
                <span>Create Account</span>
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
