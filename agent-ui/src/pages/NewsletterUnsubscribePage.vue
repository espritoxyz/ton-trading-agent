<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Loader2, CheckCircle, XCircle } from 'lucide-vue-next'
import axios from 'axios'

const props = defineProps<{
  token: string
}>()

const apiBase = (import.meta as any).env?.VITE_BACKEND_URL || '/api'

const loading = ref(true)
const status = ref<'unsubscribed' | 'already-unsubscribed' | 'error' | null>(null)

const stars = Array.from({ length: 120 }, () => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 3}s`,
  size: Math.random() > 0.7 ? '3px' : '2px'
}))

onMounted(async () => {
  document.documentElement.classList.add('dark')

  try {
    const response = await axios.get(`${apiBase}/newsletter/unsubscribe/${props.token}`)
    const msg: string = response.data?.message?.toLowerCase() ?? ''
    if (msg.includes('already')) {
      status.value = 'already-unsubscribed'
    } else {
      status.value = 'unsubscribed'
    }
  } catch (e: any) {
    status.value = 'error'
  } finally {
    loading.value = false
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
        <div v-if="loading" class="flex flex-col items-center gap-4 py-4">
          <Loader2 :size="56" class="text-cosmic-400 animate-spin" />
          <h2 class="text-xl font-bold text-white">Processing…</h2>
          <p class="text-gray-400 text-sm">Just a moment please.</p>
        </div>

        <!-- Unsubscribed -->
        <div v-else-if="status === 'unsubscribed'" class="flex flex-col items-center gap-4 py-4">
          <div class="w-20 h-20 rounded-full bg-green-500/20 flex items-center justify-center mb-2">
            <CheckCircle :size="48" class="text-green-400" />
          </div>
          <h2 class="text-2xl font-bold text-white">You're unsubscribed</h2>
          <p class="text-gray-300 text-sm leading-relaxed">
            You've been successfully removed from the Esprito AI newsletter.
            You won't receive any more emails from us.
          </p>
          <a
            href="/"
            class="mt-4 inline-block px-8 py-3 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full font-semibold hover:opacity-90 transition text-white"
          >
            Back to Home
          </a>
          <a href="/#subscribe" class="text-sm text-gray-400 hover:text-gray-300 transition">
            Changed your mind? Subscribe again
          </a>
        </div>

        <!-- Already unsubscribed -->
        <div v-else-if="status === 'already-unsubscribed'" class="flex flex-col items-center gap-4 py-4">
          <div class="w-20 h-20 rounded-full bg-blue-500/20 flex items-center justify-center mb-2">
            <CheckCircle :size="48" class="text-blue-400" />
          </div>
          <h2 class="text-2xl font-bold text-white">Already unsubscribed</h2>
          <p class="text-gray-300 text-sm">You were already unsubscribed from our newsletter.</p>
          <a
            href="/"
            class="mt-4 inline-block px-8 py-3 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full font-semibold hover:opacity-90 transition text-white"
          >
            Back to Home
          </a>
        </div>

        <!-- Error -->
        <div v-else class="flex flex-col items-center gap-4 py-4">
          <div class="w-20 h-20 rounded-full bg-red-500/20 flex items-center justify-center mb-2">
            <XCircle :size="48" class="text-red-400" />
          </div>
          <h2 class="text-2xl font-bold text-white">Invalid link</h2>
          <p class="text-gray-300 text-sm leading-relaxed">
            This unsubscribe link is invalid or has already been used.
          </p>
          <a
            href="/"
            class="mt-4 inline-block px-8 py-3 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full font-semibold hover:opacity-90 transition text-white"
          >
            Back to Home
          </a>
        </div>

      </div>
    </div>
  </div>
</template>
