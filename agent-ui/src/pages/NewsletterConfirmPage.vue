<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Loader, CheckCircle, XCircle, Clock } from 'lucide-vue-next'
import axios from 'axios'

const props = defineProps<{
  token: string
}>()

const apiBase = (import.meta as any).env?.VITE_BACKEND_URL || '/api'

const verifying = ref(true)
const status = ref<'confirmed' | 'already-confirmed' | 'expired' | 'error' | null>(null)
const message = ref('')

// Generate random stars (same as LandingPage)
const stars = Array.from({ length: 120 }, () => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 3}s`,
  size: Math.random() > 0.7 ? '3px' : '2px'
}))

onMounted(async () => {
  document.documentElement.classList.add('dark')

  try {
    const response = await axios.get(`${apiBase}/newsletter/confirm/${props.token}`)
    const data = response.data
    if (data.confirmed) {
      status.value = 'confirmed'
    } else {
      const msg: string = data.message?.toLowerCase() ?? ''
      if (msg.includes('expired')) {
        status.value = 'expired'
      } else if (msg.includes('already')) {
        status.value = 'already-confirmed'
      } else {
        status.value = 'error'
      }
    }
    message.value = data.message
  } catch (e: any) {
    status.value = 'error'
    message.value = e?.response?.data?.message || 'Confirmation failed. Please try again.'
  } finally {
    verifying.value = false
  }
})
</script>

<template>
  <div class="relative min-h-screen overflow-hidden flex items-center justify-center p-4">
    <!-- Animated Background -->
    <div class="stars">
      <div
        v-for="(star, i) in stars"
        :key="i"
        class="star"
        :style="{ left: star.left, top: star.top, animationDelay: star.animationDelay, width: star.size, height: star.size }"
      />
    </div>

    <!-- Gradient Orbs -->
    <div class="orb w-96 h-96 bg-cosmic-500 top-0 right-0" />
    <div class="orb w-80 h-80 bg-purple-600 bottom-20 left-10" style="animation-delay: 5s" />

    <!-- Card -->
    <div class="relative z-10 w-full max-w-md">
      <div class="glass-card p-10 cosmic-glow text-center">

        <!-- Loading -->
        <div v-if="verifying" class="flex flex-col items-center gap-4 py-4">
          <Loader :size="56" class="text-cosmic-400 animate-spin" />
          <h2 class="text-xl font-bold text-white">Confirming your subscription…</h2>
          <p class="text-gray-400 text-sm">Just a moment please.</p>
        </div>

        <!-- Success -->
        <div v-else-if="status === 'confirmed'" class="flex flex-col items-center gap-4 py-4">
          <div class="w-20 h-20 rounded-full bg-green-500/20 flex items-center justify-center mb-2">
            <CheckCircle :size="48" class="text-green-400" />
          </div>
          <h2 class="text-2xl font-bold gradient-text">You're subscribed!</h2>
          <p class="text-gray-300 text-sm leading-relaxed">
            Welcome to the Esprito AI newsletter. You'll start receiving updates on new features,
            blockchain integrations, and exclusive trading insights.
          </p>
          <a
            href="/"
            class="mt-4 inline-block px-8 py-3 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full font-semibold hover:opacity-90 transition text-white"
          >
            Back to Home
          </a>
        </div>

        <!-- Already confirmed -->
        <div v-else-if="status === 'already-confirmed'" class="flex flex-col items-center gap-4 py-4">
          <div class="w-20 h-20 rounded-full bg-blue-500/20 flex items-center justify-center mb-2">
            <CheckCircle :size="48" class="text-blue-400" />
          </div>
          <h2 class="text-2xl font-bold text-white">Already subscribed</h2>
          <p class="text-gray-300 text-sm">You're already on our list. Stay tuned for updates!</p>
          <a
            href="/"
            class="mt-4 inline-block px-8 py-3 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full font-semibold hover:opacity-90 transition text-white"
          >
            Back to Home
          </a>
        </div>

        <!-- Expired -->
        <div v-else-if="status === 'expired'" class="flex flex-col items-center gap-4 py-4">
          <div class="w-20 h-20 rounded-full bg-orange-500/20 flex items-center justify-center mb-2">
            <Clock :size="48" class="text-orange-400" />
          </div>
          <h2 class="text-2xl font-bold text-white">Link expired</h2>
          <p class="text-gray-300 text-sm leading-relaxed">
            This confirmation link has expired. Please go back and subscribe again to receive a new link.
          </p>
          <a
            href="/#subscribe"
            class="mt-4 inline-block px-8 py-3 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full font-semibold hover:opacity-90 transition text-white"
          >
            Subscribe again
          </a>
        </div>

        <!-- Error -->
        <div v-else class="flex flex-col items-center gap-4 py-4">
          <div class="w-20 h-20 rounded-full bg-red-500/20 flex items-center justify-center mb-2">
            <XCircle :size="48" class="text-red-400" />
          </div>
          <h2 class="text-2xl font-bold text-white">Confirmation failed</h2>
          <p class="text-gray-300 text-sm leading-relaxed">
            {{ message || 'This link is invalid or has already been used.' }}
          </p>
          <a
            href="/#subscribe"
            class="mt-4 inline-block px-8 py-3 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full font-semibold hover:opacity-90 transition text-white"
          >
            Subscribe again
          </a>
        </div>

      </div>
    </div>
  </div>
</template>
