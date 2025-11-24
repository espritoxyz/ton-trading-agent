<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { login, logout, loggingIn, accessToken, email, subject, userId, refreshProfile, authError } from '../composables/useAuth.ts'
import { refreshBalance } from '../composables/useBalance.ts'

const username = ref('')
const password = ref('')

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
</script>

<template>
  <div class="rounded-2xl bg-white p-4 ring-1 ring-gray-200">
    <div class="text-sm font-semibold text-gray-900">User</div>

    <div v-if="!accessToken" class="mt-3 space-y-2">
      <input
          v-model="username"
          type="email"
          placeholder="Email"
          autocomplete="username"
          class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
      />
      <input
          v-model="password"
          type="password"
          placeholder="Password"
          autocomplete="current-password"
          class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
      />
      <button
          class="w-full rounded-xl bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
          :disabled="loggingIn || !username || !password"
          @click="onLogin"
      >
        {{ loggingIn ? 'Logging in…' : 'Login' }}
      </button>
      <p v-if="authError" class="text-xs text-red-600">{{ authError }}</p>
      <p class="text-xs text-gray-500">Dev flow: direct grant → access token stored in sessionStorage.</p>
    </div>

    <div v-else class="mt-3 space-y-2">
      <div class="text-xs text-gray-500">Email</div>
      <div class="text-sm text-gray-900">{{ email ?? '—' }}</div>

      <div class="text-xs text-gray-500 mt-2">Subject (sub)</div>
      <div class="text-sm font-mono text-gray-900">{{ subject ?? '—' }}</div>

      <div class="text-xs text-gray-500 mt-2">Local userId</div>
      <div class="text-sm font-mono text-gray-900">{{ userId ?? '—' }}</div>

      <div class="mt-3">
        <button class="rounded-lg bg-gray-100 px-3 py-2 text-sm ring-1 ring-gray-200 hover:bg-gray-200" @click="refreshProfile">
          Refresh Profile
        </button>
        <button class="ml-2 rounded-xl bg-gray-800 px-3 py-2 text-sm font-medium text-white hover:bg-gray-900" @click="onLogout">
          Logout
        </button>
      </div>
    </div>
  </div>
</template>
