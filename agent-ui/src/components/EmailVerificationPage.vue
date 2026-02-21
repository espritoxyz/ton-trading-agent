<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { verifyEmail } from '../composables/useAuth'
import { Loader, CheckCircle, XCircle, Clock, AlertCircle } from 'lucide-vue-next'

const props = defineProps<{
  token: string
}>()

const verifying = ref(true)
const status = ref<'success' | 'error' | 'expired' | 'already-verified' | null>(null)
const message = ref('')

onMounted(async () => {
  try {
    const result = await verifyEmail(props.token)

    if (result.success) {
      status.value = 'success'
      message.value = result.message
    } else {
      const msg = result.message.toLowerCase()
      if (msg.includes('expired')) {
        status.value = 'expired'
      } else if (msg.includes('already')) {
        status.value = 'already-verified'
      } else {
        status.value = 'error'
      }
      message.value = result.message
    }
  } catch (e: any) {
    status.value = 'error'
    message.value = e?.response?.data?.message || 'Verification failed'
  } finally {
    verifying.value = false
  }
})

function goToLogin() {
  window.location.href = '/app'
}

function requestNewLink() {
  // For simplicity, just redirect to home where they can request a new link
  window.location.href = '/'
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-cosmic-500/10 via-purple-500/10 to-pink-500/10">
    <div class="w-full max-w-md bg-white dark:bg-white/5 backdrop-blur-lg border-2 border-gray-300 dark:border-white/10 rounded-2xl p-8 shadow-2xl cosmic-glow">

      <!-- Verifying State -->
      <div v-if="verifying" class="flex flex-col items-center justify-center text-center gap-4 py-8">
        <Loader :size="48" class="text-cosmic-500 animate-spin" />
        <h2 class="text-xl font-bold text-gray-900 dark:text-white">Verifying Your Email...</h2>
        <p class="text-sm text-gray-600 dark:text-gray-400">Please wait while we verify your email address.</p>
      </div>

      <!-- Success State -->
      <div v-else-if="status === 'success'" class="flex flex-col items-center justify-center text-center gap-4 py-8">
        <div class="w-20 h-20 rounded-full bg-green-100 dark:bg-green-500/20 flex items-center justify-center">
          <CheckCircle :size="48" class="text-green-600 dark:text-green-400" />
        </div>
        <div>
          <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">Email Verified!</h2>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            Your email has been successfully verified. You can now log in to your account.
          </p>
        </div>
        <button
          @click="goToLogin"
          class="mt-4 cosmic-button rounded-xl px-6 py-3 text-sm font-bold text-white shadow-lg"
        >
          Go to Login
        </button>
      </div>

      <!-- Already Verified State -->
      <div v-else-if="status === 'already-verified'" class="flex flex-col items-center justify-center text-center gap-4 py-8">
        <div class="w-20 h-20 rounded-full bg-blue-100 dark:bg-blue-500/20 flex items-center justify-center">
          <CheckCircle :size="48" class="text-blue-600 dark:text-blue-400" />
        </div>
        <div>
          <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">Already Verified</h2>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            This email has already been verified. You can log in to your account.
          </p>
        </div>
        <button
          @click="goToLogin"
          class="mt-4 cosmic-button rounded-xl px-6 py-3 text-sm font-bold text-white shadow-lg"
        >
          Go to Login
        </button>
      </div>

      <!-- Expired State -->
      <div v-else-if="status === 'expired'" class="flex flex-col items-center justify-center text-center gap-4 py-8">
        <div class="w-20 h-20 rounded-full bg-orange-100 dark:bg-orange-500/20 flex items-center justify-center">
          <Clock :size="48" class="text-orange-600 dark:text-orange-400" />
        </div>
        <div>
          <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">Link Expired</h2>
          <p class="text-sm text-gray-600 dark:text-gray-400 mb-3">
            This verification link has expired. Please request a new verification email.
          </p>
          <p class="text-xs text-gray-500 dark:text-gray-500">
            Verification links are valid for 24 hours from the time they are sent.
          </p>
        </div>
        <button
          @click="requestNewLink"
          class="mt-4 cosmic-button rounded-xl px-6 py-3 text-sm font-bold text-white shadow-lg"
        >
          Request New Link
        </button>
      </div>

      <!-- Error State -->
      <div v-else class="flex flex-col items-center justify-center text-center gap-4 py-8">
        <div class="w-20 h-20 rounded-full bg-red-100 dark:bg-red-500/20 flex items-center justify-center">
          <XCircle :size="48" class="text-red-600 dark:text-red-400" />
        </div>
        <div>
          <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">Verification Failed</h2>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            {{ message || 'We could not verify your email. The link may be invalid or expired.' }}
          </p>
        </div>
        <button
          @click="goToLogin"
          class="mt-4 cosmic-button rounded-xl px-6 py-3 text-sm font-bold text-white shadow-lg"
        >
          Go to Login
        </button>
      </div>

    </div>
  </div>
</template>
