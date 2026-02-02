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
    <div class="flex items-center gap-2 mb-4">
      <div class="text-2xl">👤</div>
      <div class="text-lg font-semibold gradient-text">User Account</div>
    </div>

    <div v-if="!accessToken" class="space-y-3">
      <input
          v-model="username"
          type="email"
          placeholder="Email"
          autocomplete="username"
          class="w-full rounded-xl bg-white/10 border border-white/20 px-4 py-3 text-sm text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-transparent transition"
      />
      <input
          v-model="password"
          type="password"
          placeholder="Password"
          autocomplete="current-password"
          class="w-full rounded-xl bg-white/10 border border-white/20 px-4 py-3 text-sm text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-transparent transition"
      />
      <button
          class="cosmic-button w-full rounded-xl px-4 py-3 text-sm font-semibold text-white disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="loggingIn || !username || !password"
          @click="onLogin"
      >
        {{ loggingIn ? 'Logging in…' : 'Login' }}
      </button>

      <div class="flex items-center justify-between mt-3">
        <p v-if="authError" class="text-xs text-red-400">{{ authError }}</p>
        <button class="text-xs text-cosmic-400 hover:text-cosmic-300 transition font-medium" @click="openRegister">
          Create Account
        </button>
      </div>

      <p class="text-xs text-gray-400 mt-4 pt-3 border-t border-white/10">
        Dev mode: Direct grant authentication
      </p>
    </div>

    <div v-else class="space-y-4">
      <div class="space-y-1">
        <div class="text-xs text-gray-400">Email</div>
        <div class="text-sm text-white font-medium">{{ email ?? '—' }}</div>
      </div>

      <div class="space-y-1">
        <div class="text-xs text-gray-400">Subject (sub)</div>
        <div class="text-xs font-mono text-gray-300 bg-white/5 px-2 py-1 rounded">{{ subject ?? '—' }}</div>
      </div>

      <div class="space-y-1">
        <div class="text-xs text-gray-400">User ID</div>
        <div class="text-xs font-mono text-gray-300 bg-white/5 px-2 py-1 rounded">{{ userId ?? '—' }}</div>
      </div>

      <div class="flex gap-2 pt-2">
        <button class="flex-1 rounded-xl bg-white/10 px-4 py-2 text-sm font-medium text-white hover:bg-white/20 transition border border-white/20" @click="refreshProfile">
          Refresh
        </button>
        <button class="rounded-xl bg-gradient-to-r from-red-500 to-pink-600 px-4 py-2 text-sm font-semibold text-white hover:opacity-90 transition" @click="onLogout">
          Logout
        </button>
      </div>
    </div>

    <RegisterModal v-if="showRegister" @registered="onRegistered" @close="closeRegister" />
  </div>
</template>
