<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { login, logout, loggingIn, accessToken, email, subject, userId, refreshProfile, authError } from '../composables/useAuth.ts'
import { refreshBalance } from '../composables/useBalance.ts'
import RegisterModal from './RegisterModal.vue'

const username = ref('')
const password = ref('')
const showRegister = ref(false)

async function onLogin() {
  await login(username.value, password.value)
  password.value = ''
  await Promise.all([refreshProfile(), refreshBalance()])
}
function onLogout() {
  logout()
}
onMounted(async () => {
  if (accessToken.value) {
    await Promise.all([refreshProfile(), refreshBalance()])
  }
})

function openRegister() { showRegister.value = true }
function closeRegister() { showRegister.value = false }
function onRegistered(data: any) {
  closeRegister()
}
</script>

<template>
  <div class="glass-card p-6 transition-all duration-300 hover:shadow-lg hover:shadow-cosmic-500/20">
    <div class="flex items-center gap-3 mb-5">
      <div class="w-12 h-12 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-600 flex items-center justify-center text-xl shadow-lg">
        👤
      </div>
      <div>
        <div class="text-lg font-semibold gradient-text">Account</div>
        <div class="text-xs text-gray-500 dark:text-gray-400">User Profile</div>
      </div>
    </div>

    <div v-if="!accessToken" class="space-y-3">
      <div>
        <label class="text-xs text-gray-600 dark:text-gray-400 mb-1.5 block">Email Address</label>
        <input
          v-model="username"
          type="email"
          placeholder="your@email.com"
          autocomplete="username"
          class="w-full rounded-xl bg-gray-100 dark:bg-white/10 border border-gray-300 dark:border-white/20 px-4 py-3 text-sm text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-transparent transition"
        />
      </div>

      <div>
        <label class="text-xs text-gray-600 dark:text-gray-400 mb-1.5 block">Password</label>
        <input
          v-model="password"
          type="password"
          placeholder="••••••••"
          autocomplete="current-password"
          class="w-full rounded-xl bg-gray-100 dark:bg-white/10 border border-gray-300 dark:border-white/20 px-4 py-3 text-sm text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-transparent transition"
        />
      </div>

      <div v-if="authError" class="flex items-center gap-2 p-3 rounded-xl bg-red-100 dark:bg-red-500/10 border border-red-300 dark:border-red-500/30">
        <div class="text-lg">⚠️</div>
        <div class="text-xs text-red-700 dark:text-red-300">{{ authError }}</div>
      </div>

      <button
        class="cosmic-button w-full rounded-xl px-4 py-3 text-sm font-semibold text-white disabled:opacity-50 disabled:cursor-not-allowed mt-4"
        :disabled="loggingIn || !username || !password"
        @click="onLogin"
      >
        <span v-if="loggingIn" class="flex items-center justify-center gap-2">
          <span class="animate-spin">⟳</span>
          <span>Logging in...</span>
        </span>
        <span v-else class="flex items-center justify-center gap-2">
          <span>🚀</span>
          <span>Sign In</span>
        </span>
      </button>

      <div class="text-center mt-4 pt-4 border-t border-gray-200 dark:border-white/10">
        <span class="text-xs text-gray-600 dark:text-gray-400">Don't have an account? </span>
        <button class="text-xs text-cosmic-600 dark:text-cosmic-400 hover:text-cosmic-700 dark:hover:text-cosmic-300 transition font-semibold" @click="openRegister">
          Create one
        </button>
      </div>
    </div>

    <div v-else class="space-y-4">
      <div class="p-4 rounded-xl bg-gradient-to-br from-cosmic-100 to-purple-100 dark:from-cosmic-500/20 dark:to-purple-600/20 border border-cosmic-300 dark:border-cosmic-500/30">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-600 flex items-center justify-center shadow-lg">
            <span class="text-lg">✓</span>
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-xs text-gray-600 dark:text-gray-400 mb-0.5">Logged in as</div>
            <div class="text-sm text-gray-900 dark:text-white font-medium truncate">{{ email ?? '—' }}</div>
          </div>
        </div>
      </div>

      <div class="flex gap-2">
        <button
          class="flex-1 rounded-xl bg-gray-100 dark:bg-white/10 px-4 py-2.5 text-sm font-medium text-gray-900 dark:text-white hover:bg-gray-200 dark:hover:bg-white/20 transition border border-gray-300 dark:border-white/20 flex items-center justify-center gap-2"
          @click="refreshProfile"
        >
          <span class="text-base">🔄</span>
          <span>Refresh</span>
        </button>
        <button
          class="rounded-xl bg-gradient-to-r from-red-500 to-pink-600 px-4 py-2.5 text-sm font-semibold text-white hover:opacity-90 transition flex items-center gap-2 shadow-md"
          @click="onLogout"
        >
          <span class="text-base">🚪</span>
          <span>Logout</span>
        </button>
      </div>
    </div>

    <RegisterModal v-if="showRegister" @registered="onRegistered" @close="closeRegister" />
  </div>
</template>
