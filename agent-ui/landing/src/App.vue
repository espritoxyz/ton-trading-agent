<script setup>
import { ref, onMounted } from 'vue'

const email = ref('')
const subscribed = ref(false)
const loading = ref(false)

const handleSubscribe = async () => {
  if (!email.value || !email.value.includes('@')) return

  loading.value = true
  // TODO: Implement actual email subscription API call
  setTimeout(() => {
    subscribed.value = true
    loading.value = false
    email.value = ''
  }, 1000)
}

const handleImageError = (e) => {
  console.log('Image failed to load:', e.target.src)
  // Keep the image element but it will show broken image icon
}

// Generate random stars
const stars = Array.from({ length: 150 }, () => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 3}s`,
  size: Math.random() > 0.7 ? '3px' : '2px'
}))

// Animated statistics (like ston.fi)
const stats = ref([
  { label: 'Total Volume', value: '$0', target: 407810, prefix: '$', suffix: '' },
  { label: 'Active Users', value: '0', target: 324, prefix: '', suffix: '+' },
  { label: 'Transactions', value: '0', target: 3090, prefix: '', suffix: '' },
])

const animateStats = () => {
  stats.value.forEach((stat, index) => {
    let current = 0
    const step = stat.target / 50
    const interval = setInterval(() => {
      current += step
      if (current >= stat.target) {
        current = stat.target
        clearInterval(interval)
      }
      stat.value = stat.prefix + Math.floor(current).toLocaleString() + stat.suffix
    }, 30)
  })
}

onMounted(() => {
  // Animate stats after a short delay
  setTimeout(animateStats, 500)
})
</script>

<template>
  <div class="relative min-h-screen overflow-hidden">
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
    <div class="orb w-72 h-72 bg-blue-500 top-1/2 left-1/3" style="animation-delay: 10s" />

    <!-- Main Content -->
    <div class="relative z-10">
      <!-- Navigation -->
      <nav class="fixed top-0 w-full glass-card border-b border-white/5 backdrop-blur-xl z-50">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex justify-between items-center h-20">
            <div class="flex items-center gap-3">
              <svg width="40" height="40" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" fill="#6366f1"/>
                <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M28.1159 52.8038C27.4167 52.1278 26.4821 51.75 25.5096 51.75L24.3595 51.75C20.9764 51.75 19.323 55.8766 21.7702 58.2126L45.4106 80.7784C46.8597 82.1616 49.1401 82.1616 50.5891 80.7784L74.2295 58.2126C76.6767 55.8766 75.0233 51.75 71.6403 51.75L70.4901 51.75C69.5175 51.75 68.583 52.1278 67.8837 52.8038L50.6062 69.5055C49.1526 70.9105 46.847 70.9105 45.3935 69.5054L28.1159 52.8038Z" fill="white"/>
              </svg>
              <span class="text-2xl font-bold gradient-text">Esprito App</span>
            </div>
            <div class="hidden md:flex items-center gap-8">
              <a href="#features" class="text-gray-300 hover:text-white transition">Features</a>
              <a href="#roadmap" class="text-gray-300 hover:text-white transition">Roadmap</a>
              <a href="#subscribe" class="text-gray-300 hover:text-white transition">Subscribe</a>
              <a href="/app/" class="cta-button px-6 py-2 rounded-full font-semibold">
                Launch App
              </a>
            </div>
          </div>
        </div>
      </nav>

      <!-- Hero Section -->
      <section class="pt-32 pb-20 px-4 sm:px-6 lg:px-8">
        <div class="max-w-7xl mx-auto">
          <div class="text-center max-w-4xl mx-auto">
            <div class="inline-block mb-6 px-6 py-2 glass-card cosmic-glow">
              <span class="text-sm font-semibold gradient-text">🚀 The Future of DEX Trading</span>
            </div>

            <h1 class="text-5xl md:text-7xl font-bold mb-8 leading-tight">
              Trade Crypto with
              <span class="gradient-text animate-pulse-slow"> AI-Powered Chat</span>
            </h1>

            <p class="text-xl md:text-2xl text-gray-300 mb-12 leading-relaxed">
              Say goodbye to complex swap interfaces and confusing protocols.
              Just chat naturally with our AI agent to trade tokens on TON DEXes.
            </p>

            <div class="flex flex-col sm:flex-row gap-4 justify-center items-center">
              <a href="/app/" class="cta-button w-full sm:w-auto px-8 py-4 rounded-full font-semibold text-lg">
                Start Trading Now
              </a>
              <a href="#features" class="w-full sm:w-auto px-8 py-4 glass-card hover:bg-white/10 transition font-semibold text-lg rounded-full border border-white/10">
                Learn More
              </a>
            </div>

            <!-- Live Stats (inspired by ston.fi) -->
            <div class="mt-16 grid grid-cols-3 gap-8 max-w-2xl mx-auto">
              <div v-for="stat in stats" :key="stat.label" class="text-center stat-item">
                <div class="text-3xl md:text-4xl font-bold gradient-text mb-2">{{ stat.value }}</div>
                <div class="text-gray-400 text-sm">{{ stat.label }}</div>
              </div>
            </div>

            <!-- Real Chat Screenshot -->
            <div class="mt-20 relative">
              <div class="absolute inset-0 bg-gradient-to-r from-cosmic-500/20 to-purple-500/20 blur-3xl" />
              <div class="relative glass-card p-4 cosmic-glow animate-float">
                <img
                  src="/images/chat-demo.png"
                  alt="Esprito App Chat Interface - Natural conversation for crypto trading"
                  class="rounded-xl w-full shadow-2xl"
                  @error="handleImageError"
                />
                <div class="absolute bottom-8 left-8 glass-card px-4 py-2">
                  <p class="text-sm font-semibold">✨ Just chat naturally to trade</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section id="features" class="py-20 px-4 sm:px-6 lg:px-8">
        <div class="max-w-7xl mx-auto">
          <div class="text-center mb-16">
            <h2 class="text-4xl md:text-5xl font-bold mb-4 gradient-text">Why Choose Esprito App?</h2>
            <p class="text-xl text-gray-300">Trading crypto has never been this simple</p>
          </div>

          <div class="grid md:grid-cols-3 gap-8">
            <div class="feature-card glass-card p-8 transition group">
              <div class="mb-6 group-hover:scale-110 transition">
                <span class="text-4xl">🛸</span>
              </div>
              <h3 class="text-2xl font-bold mb-4 group-hover:text-cosmic-400 transition">Natural Conversations</h3>
              <p class="text-gray-300 leading-relaxed">
                No more navigating complex interfaces. Just tell our AI what you want to do:
                "Swap 50 TON to USDT" or "Check my portfolio balance."
              </p>
            </div>

            <div class="feature-card glass-card p-8 transition group">
              <div class="mb-6 group-hover:scale-110 transition">
                <span class="text-4xl">🌠</span>
              </div>
              <h3 class="text-2xl font-bold mb-4 group-hover:text-cosmic-400 transition">Lightning Fast</h3>
              <p class="text-gray-300 leading-relaxed">
                Our AI finds the best rates across multiple DEXes in seconds.
                No manual comparisons needed—just instant, optimized trades.
              </p>
            </div>

            <div class="feature-card glass-card p-8 transition group">
              <div class="mb-6 group-hover:scale-110 transition">
                <span class="text-4xl">🛡️</span>
              </div>
              <h3 class="text-2xl font-bold mb-4 group-hover:text-cosmic-400 transition">Secure & Private</h3>
              <p class="text-gray-300 leading-relaxed">
                Your keys, your crypto. We never have access to your funds.
                All trades execute directly from your wallet on-chain.
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- Tonviewer Verification Section -->
      <section class="py-20 px-4 sm:px-6 lg:px-8 relative">
        <!-- Space background -->
        <div class="absolute inset-0 bg-gradient-to-br from-space-purple/20 to-space-blue/20" />

        <div class="max-w-6xl mx-auto relative z-10">
          <div class="grid md:grid-cols-2 gap-12 items-center">
            <div>
              <div class="inline-block mb-6 px-6 py-2 glass-card cosmic-glow">
                <span class="text-sm font-semibold gradient-text">🔍 Full Transparency</span>
              </div>

              <h2 class="text-4xl md:text-5xl font-bold mb-6">
                Verify Every <span class="gradient-text">Transaction</span>
              </h2>

              <p class="text-xl text-gray-300 mb-6 leading-relaxed">
                All transactions are user-approved and can be verified immediately on Tonviewer.
                Complete transparency, full control.
              </p>

              <ul class="space-y-4 mb-8">
                <li class="flex items-start gap-3">
                  <span class="text-2xl">✅</span>
                  <div>
                    <p class="font-semibold mb-1">User Approval Required</p>
                    <p class="text-gray-400 text-sm">Every trade requires your explicit confirmation before execution</p>
                  </div>
                </li>
                <li class="flex items-start gap-3">
                  <span class="text-2xl">🔗</span>
                  <div>
                    <p class="font-semibold mb-1">On-Chain Verification</p>
                    <p class="text-gray-400 text-sm">View transaction details on Tonviewer instantly after execution</p>
                  </div>
                </li>
                <li class="flex items-start gap-3">
                  <span class="text-2xl">📋</span>
                  <div>
                    <p class="font-semibold mb-1">Complete Transaction History</p>
                    <p class="text-gray-400 text-sm">Track all your trades with full blockchain transparency</p>
                  </div>
                </li>
              </ul>

              <a href="https://tonviewer.com" target="_blank" class="inline-flex items-center gap-2 text-cosmic-400 hover:text-cosmic-300 transition font-semibold">
                Learn more about Tonviewer
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3" />
                </svg>
              </a>
            </div>

            <div class="glass-card p-4 cosmic-glow">
              <img
                src="/images/tonviewer-demo.png"
                alt="Tonviewer Transaction Verification - Complete transparency"
                class="rounded-xl w-full shadow-2xl"
                @error="handleImageError"
              />
              <p class="text-center text-sm text-gray-400 mt-4">
                Example: Verified swap transaction on Tonviewer
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- Footer -->
      <footer class="py-12 px-4 sm:px-6 lg:px-8 border-t border-white/10">
        <div class="max-w-7xl mx-auto">
          <div class="grid md:grid-cols-4 gap-8 mb-12">
            <div>
              <div class="flex items-center gap-3 mb-4">
                <svg width="32" height="32" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" fill="#6366f1"/>
                </svg>
                <span class="text-xl font-bold">Esprito App</span>
              </div>
              <p class="text-gray-400 text-sm">
                The future of decentralized trading powered by AI.
              </p>
            </div>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
/* Animated CTA button (inspired by ston.fi) */
.cta-button {
  background: linear-gradient(90deg, #6366f1 0%, #a855f7 50%, #6366f1 100%);
  background-size: 200% 100%;
  animation: gradientFlow 3s ease infinite;
  box-shadow: 0 0 30px rgba(99, 102, 241, 0.4);
  transition: all 0.3s ease;
}

.cta-button:hover {
  box-shadow: 0 0 50px rgba(99, 102, 241, 0.6);
  transform: translateY(-2px);
}

@keyframes gradientFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* Feature cards with radial gradient (inspired by ston.fi) */
.feature-card {
  background: radial-gradient(ellipse at top, rgba(99, 102, 241, 0.1) 0%, transparent 70%);
  border: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.5), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-card:hover {
  background: radial-gradient(ellipse at top, rgba(99, 102, 241, 0.2) 0%, rgba(255, 255, 255, 0.05) 70%);
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

/* Stats animation */
.stat-item {
  opacity: 0;
  animation: fadeInUp 0.6s ease forwards;
}

.stat-item:nth-child(1) { animation-delay: 0.2s; }
.stat-item:nth-child(2) { animation-delay: 0.4s; }
.stat-item:nth-child(3) { animation-delay: 0.6s; }

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Star animation enhancement */
.star {
  animation: twinkle 3s infinite;
}
</style>
