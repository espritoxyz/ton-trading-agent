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
  <div class="fixed inset-0 z-50 flex items-center justify-center">
    <div class="absolute inset-0 bg-black/50" @click="onClose"></div>
    <div class="relative w-full max-w-md rounded-lg bg-white p-6 shadow-lg dark:bg-gray-800">
      <div class="flex items-center justify-between">
        <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100">Register</h3>
        <button class="text-gray-500 hover:text-gray-700" @click="onClose">✕</button>
      </div>

      <div class="mt-4 space-y-3">
        <input v-model="email" type="email" placeholder="Email" class="w-full rounded-lg border px-3 py-2" />
        <input v-model="password" type="password" placeholder="Password" class="w-full rounded-lg border px-3 py-2" />
        <input v-model="displayName" type="text" placeholder="Display name (optional)" class="w-full rounded-lg border px-3 py-2" />

        <div v-if="error" class="text-sm text-red-600">{{ error }}</div>

        <div class="flex justify-end">
          <button class="mr-2 rounded-md px-3 py-2 text-sm bg-gray-100" @click="onClose">Cancel</button>
          <button class="rounded-md bg-indigo-600 px-3 py-2 text-sm text-white" :disabled="submitting || !email || !password" @click="onSubmit">
            {{ submitting ? 'Registering...' : 'Register' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
