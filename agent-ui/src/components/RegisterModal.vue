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
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
    <div class="absolute inset-0 bg-black/70" @click="onClose"></div>
    <div class="relative w-full max-w-md glass-card p-8 shadow-2xl cosmic-glow animate-float">
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-2">
          <div class="text-2xl">✨</div>
          <h3 class="text-xl font-semibold gradient-text">Create Account</h3>
        </div>
        <button class="text-gray-400 hover:text-white transition text-2xl" @click="onClose">×</button>
      </div>

      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 mb-1 block">Email</label>
          <input
            v-model="email"
            type="email"
            placeholder="your@email.com"
            class="w-full rounded-xl bg-white/10 border border-white/20 px-4 py-3 text-sm text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 transition"
          />
        </div>

        <div>
          <label class="text-xs text-gray-400 mb-1 block">Password</label>
          <input
            v-model="password"
            type="password"
            placeholder="••••••••"
            class="w-full rounded-xl bg-white/10 border border-white/20 px-4 py-3 text-sm text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 transition"
          />
        </div>

        <div>
          <label class="text-xs text-gray-400 mb-1 block">Display Name (Optional)</label>
          <input
            v-model="displayName"
            type="text"
            placeholder="Your Name"
            class="w-full rounded-xl bg-white/10 border border-white/20 px-4 py-3 text-sm text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 transition"
          />
        </div>

        <div v-if="error" class="flex items-center gap-2 p-3 rounded-xl bg-red-500/10 border border-red-500/30">
          <div class="text-lg">⚠️</div>
          <div class="text-sm text-red-300">{{ error }}</div>
        </div>

        <div class="flex gap-3 pt-2">
          <button
            class="flex-1 rounded-xl bg-white/10 px-4 py-3 text-sm font-medium text-white hover:bg-white/20 transition border border-white/20"
            @click="onClose"
          >
            Cancel
          </button>
          <button
            class="cosmic-button flex-1 rounded-xl px-4 py-3 text-sm font-semibold text-white disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="submitting || !email || !password"
            @click="onSubmit"
          >
            {{ submitting ? 'Creating...' : 'Create Account' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
