<script setup lang="ts">
import { ref } from 'vue'
import { register } from '../composables/useAuth'
import { Sparkles, Mail, Lock, User, AlertTriangle, Loader, CheckCircle, Bell } from 'lucide-vue-next'

const emits = defineEmits(['registered','close'])

const email = ref('')
const password = ref('')
const displayName = ref('')
const subscribeToNewsletter = ref(false)
const submitting = ref(false)
const success = ref(false)
const error = ref<string | null>(null)

async function onSubmit() {
  error.value = null
  submitting.value = true
  success.value = false
  try {
    const data = await register(email.value, password.value, displayName.value, subscribeToNewsletter.value)
    success.value = true
    emits('registered', data)

    // Auto-close after 8 seconds
    setTimeout(() => emits('close'), 8000)
  } catch (e: any) {
    console.error('Registration error:', e)
    const errorMessage = e?.response?.data?.message ?? e?.message ?? 'Registration failed'
    error.value = errorMessage

    // If timeout error, show helpful message
    if (e?.code === 'ECONNABORTED' || errorMessage.includes('timeout')) {
      error.value = 'Registration is taking longer than expected. Please wait a moment and check your email.'
    }
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
              <Sparkles :size="20" class="text-white" />
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

        <div v-if="success" class="space-y-4 py-6">
          <div class="flex flex-col items-center justify-center text-center gap-4">
            <div class="w-16 h-16 rounded-full bg-green-100 dark:bg-green-500/20 flex items-center justify-center">
              <CheckCircle :size="32" class="text-green-600 dark:text-green-400" />
            </div>
            <div>
              <h4 class="text-lg font-bold text-gray-900 dark:text-white mb-2">Check Your Email!</h4>
              <p class="text-sm text-gray-600 dark:text-gray-400 mb-1">
                We've sent a verification link to <strong>{{ email }}</strong>
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-500 mt-3 px-4">
                Please check your inbox and click the verification link to activate your account. The link will expire in 24 hours.
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-500 mt-2 italic">
                Don't forget to check your spam folder!
              </p>
            </div>
            <button
              type="button"
              class="mt-4 cosmic-button rounded-xl px-6 py-2.5 text-sm font-bold text-white shadow-lg"
              @click="onClose"
            >
              Got it
            </button>
          </div>
        </div>

        <form v-else class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block flex items-center gap-1.5">
              <Mail :size="14" />
              <span>Email</span>
            </label>
            <input
              v-model="email"
              type="email"
              placeholder="your@email.com"
              required
              class="w-full rounded-xl bg-gray-50 dark:bg-white/10 border-2 border-gray-300 dark:border-white/20 px-4 py-3 text-sm font-medium text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-cosmic-500 focus:bg-white dark:focus:bg-white/15 transition shadow-sm"
            />
          </div>

          <div>
            <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block flex items-center gap-1.5">
              <Lock :size="14" />
              <span>Password</span>
            </label>
            <input
              v-model="password"
              type="password"
              placeholder="••••••••"
              required
              class="w-full rounded-xl bg-gray-50 dark:bg-white/10 border-2 border-gray-300 dark:border-white/20 px-4 py-3 text-sm font-medium text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-cosmic-500 focus:bg-white dark:focus:bg-white/15 transition shadow-sm"
            />
          </div>

          <div>
            <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block flex items-center gap-1.5">
              <User :size="14" />
              <span>Display Name (Optional)</span>
            </label>
            <input
              v-model="displayName"
              type="text"
              placeholder="Your Name"
              class="w-full rounded-xl bg-gray-50 dark:bg-white/10 border-2 border-gray-300 dark:border-white/20 px-4 py-3 text-sm font-medium text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-cosmic-500 focus:bg-white dark:focus:bg-white/15 transition shadow-sm"
            />
          </div>

          <!-- Newsletter subscription opt-in -->
          <label class="flex items-start gap-3 p-3.5 rounded-xl bg-gray-50 dark:bg-white/5 border-2 border-gray-200 dark:border-white/10 hover:border-cosmic-400 dark:hover:border-cosmic-500/50 cursor-pointer transition-colors group">
            <input
              v-model="subscribeToNewsletter"
              type="checkbox"
              class="mt-0.5 w-4 h-4 rounded border-2 border-gray-300 dark:border-white/30 text-cosmic-500 focus:ring-cosmic-500 focus:ring-offset-0 cursor-pointer flex-shrink-0"
            />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1.5 mb-0.5">
                <Bell :size="13" class="text-cosmic-500 dark:text-cosmic-400 flex-shrink-0" />
                <span class="text-xs font-semibold text-gray-800 dark:text-gray-200">Subscribe to newsletter updates</span>
              </div>
              <p class="text-xs text-gray-500 dark:text-gray-400 leading-snug">
                Get product news, feature announcements, and trading insights. Unsubscribe any time.
              </p>
            </div>
          </label>

          <div v-if="error" class="flex items-start gap-3 p-4 rounded-xl bg-red-100 dark:bg-red-500/10 border-2 border-red-400 dark:border-red-500/30 shadow-sm">
            <div class="flex-shrink-0"><AlertTriangle :size="20" class="text-red-600 dark:text-red-400" /></div>
            <div>
              <div class="text-xs font-bold text-red-900 dark:text-red-300 mb-1">Registration Error</div>
              <div class="text-xs font-medium text-red-800 dark:text-red-400">{{ error }}</div>
            </div>
          </div>

          <p class="text-xs text-center text-gray-500 dark:text-gray-400">
            By creating an account, you agree to our
            <a href="/terms" target="_blank" rel="noopener noreferrer" class="text-cosmic-500 hover:text-cosmic-400 underline transition">Terms of Service</a>
            and
            <a href="/privacy" target="_blank" rel="noopener noreferrer" class="text-cosmic-500 hover:text-cosmic-400 underline transition">Privacy Policy</a>.
          </p>

          <div class="flex gap-3 pt-1">
            <button
              type="button"
              class="flex-1 rounded-xl bg-gray-200 dark:bg-white/10 px-4 py-3 text-sm font-bold text-gray-800 dark:text-white hover:bg-gray-300 dark:hover:bg-white/20 transition border-2 border-gray-400 dark:border-white/20 shadow-sm"
              @click="onClose"
            >
              Cancel
            </button>
            <button
              type="submit"
              class="cosmic-button flex-1 rounded-xl px-4 py-3 text-sm font-bold text-white disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
              :disabled="submitting || !email || !password"
            >
              <span v-if="submitting" class="flex items-center justify-center gap-2">
                <Loader :size="16" class="animate-spin" />
                <span>Creating...</span>
              </span>
              <span v-else class="flex items-center justify-center gap-2">
                <Sparkles :size="16" />
                <span>Create Account</span>
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
