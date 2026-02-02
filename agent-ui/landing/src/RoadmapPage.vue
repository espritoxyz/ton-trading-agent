<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

// Sound effects
const soundEnabled = ref(false)
const sounds = {
  click: new Audio('/sounds/click.mp3'),
  hover: new Audio('/sounds/hover.mp3'),
}

// Set sound volumes
Object.values(sounds).forEach(sound => {
  sound.volume = 0.3
})

const playSound = (soundName) => {
  if (soundEnabled.value && sounds[soundName]) {
    sounds[soundName].currentTime = 0
    sounds[soundName].play().catch(() => {})
  }
}

const toggleSound = () => {
  soundEnabled.value = !soundEnabled.value
  playSound('click')
}

const handleHover = () => {
  playSound('hover')
}

const stars = Array.from({ length: 150 }, () => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 3}s`,
  size: Math.random() > 0.7 ? '3px' : '2px'
}))

const milestones = [
  {
    id: 1,
    quarter: 'Q1 2026',
    title: 'Launch',
    planetImage: 'earth.png',
    color: 'from-blue-500 to-green-500',
    completed: true,
    features: [
      'TON blockchain integration',
      'AI-powered chat interface',
      'Leading TON DEX support (STON.fi)',
      'Real-time market data',
      'Portfolio tracking',
      'Wallet connection'
    ]
  },
  {
    id: 2,
    quarter: 'Q2 2026',
    title: 'Ethereum Expansion',
    planetImage: 'mars.png',
    color: 'from-red-500 to-orange-500',
    completed: false,
    features: [
      'Ethereum blockchain support',
      'Uniswap & SushiSwap integration',
      'AI Trading Advisor (buy/sell signals)',
      'Advanced chart analysis',
      'Gas optimization strategies',
      'MEV protection'
    ]
  },
  {
    id: 3,
    quarter: 'Q3 2026',
    title: 'High-Speed Trading',
    planetImage: 'jupiter.png',
    color: 'from-orange-500 to-yellow-500',
    completed: false,
    features: [
      'Solana blockchain integration',
      'Raydium & Orca DEX support',
      'Trend Trading Strategies',
      'Automated trading bots',
      'Copy trading features',
      'Social trading analytics'
    ]
  },
  {
    id: 4,
    quarter: 'Q4 2026',
    title: 'Multi-Chain Universe',
    planetImage: 'saturn.png',
    color: 'from-yellow-500 to-amber-500',
    completed: false,
    features: [
      'BSC (Binance Smart Chain)',
      'PancakeSwap integration',
      'Tokenized Assets Support',
      'Trade stocks & bonds on-chain',
      'Real-world asset (RWA) trading',
      'Fiat on/off ramps'
    ]
  },
  {
    id: 5,
    quarter: 'Q1 2027',
    title: 'DeFi Integration',
    planetImage: 'uranus.png',
    color: 'from-cyan-500 to-blue-500',
    completed: false,
    features: [
      'Lending & borrowing protocols',
      'Yield farming automation',
      'Liquidity provision strategies',
      'Staking management',
      'Portfolio rebalancing',
      'Risk assessment tools'
    ]
  },
  {
    id: 6,
    quarter: 'Q2 2027',
    title: 'Cross-Chain Bridge',
    planetImage: 'neptune.png',
    color: 'from-blue-600 to-indigo-600',
    completed: false,
    features: [
      'Cross-chain swaps',
      'Bridge aggregator',
      'Multi-chain portfolio view',
      'Arbitrage opportunities',
      'Layer 2 solutions (Optimism, Arbitrum)',
      'ZK-rollups integration'
    ]
  },
  {
    id: 7,
    quarter: 'Q3 2027',
    title: 'AI Intelligence',
    planetImage: 'pluto.png',
    color: 'from-purple-500 to-pink-500',
    completed: false,
    features: [
      'Advanced AI market predictions',
      'Sentiment analysis from social media',
      'Whale tracking & alerts',
      'Smart contract audit AI',
      'Rug pull detection',
      'Portfolio optimization AI'
    ]
  },
  {
    id: 8,
    quarter: 'Q4 2027',
    title: 'Institutional Grade',
    planetImage: 'star.png',
    color: 'from-pink-500 to-rose-600',
    completed: false,
    features: [
      'Institutional trading features',
      'OTC (Over-the-counter) desk',
      'Advanced compliance tools',
      'Multi-signature wallets',
      'API for algorithmic trading',
      'White-label solutions'
    ]
  }
]

const pathProgress = ref(0)
const pathLength = 3000 // Approximate length of the SVG path

// Generate the winding journey path
const journeyPath = computed(() => {
  const totalMilestones = milestones.length
  const segmentHeight = 100 / totalMilestones
  let path = ''

  for (let i = 0; i < totalMilestones; i++) {
    const y = i * segmentHeight + segmentHeight / 2
    const nextY = (i + 1) * segmentHeight + segmentHeight / 2
    const isLeft = i % 2 === 0
    const x = isLeft ? 75 : 25 // Planet positions (percentage)
    const nextX = isLeft ? 25 : 75

    if (i === 0) {
      path += `M ${x}% ${y}%`
    }

    if (i < totalMilestones - 1) {
      // Create a smooth curve to the next planet
      const controlY = y + segmentHeight / 2
      path += ` Q 50% ${controlY}% ${nextX}% ${nextY}%`
    }
  }

  return path
})

// Calculate spaceship position along the journey
const spaceshipPosition = computed(() => {
  const progress = pathProgress.value / 100
  const totalMilestones = milestones.length
  const currentSegment = Math.min(Math.floor(progress * totalMilestones), totalMilestones - 1)
  const segmentProgress = (progress * totalMilestones) - currentSegment

  const segmentHeight = 100 / totalMilestones
  const isLeft = currentSegment % 2 === 0

  // Current planet position
  const startX = isLeft ? 75 : 25
  const startY = currentSegment * segmentHeight + segmentHeight / 2

  // Next planet position
  const endX = isLeft ? 25 : 75
  const endY = (currentSegment + 1) * segmentHeight + segmentHeight / 2

  // Interpolate position (simple bezier approximation)
  const t = segmentProgress
  const midX = 50
  const midY = startY + segmentHeight / 2

  // Quadratic bezier
  const x = (1-t)*(1-t)*startX + 2*(1-t)*t*midX + t*t*endX
  const y = (1-t)*(1-t)*startY + 2*(1-t)*t*midY + t*t*endY

  return {
    left: `${x}%`,
    top: `${y}%`
  }
})

// Calculate spaceship rotation based on direction
const spaceshipRotation = computed(() => {
  const progress = pathProgress.value / 100
  const totalMilestones = milestones.length
  const currentSegment = Math.min(Math.floor(progress * totalMilestones), totalMilestones - 1)
  const isLeft = currentSegment % 2 === 0

  // Point towards next planet
  return isLeft ? 135 : 45
})

// Scroll-based path animation
const updatePathProgress = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight
  const progress = (scrollTop / scrollHeight) * 100
  pathProgress.value = Math.min(progress, 100)
}

onMounted(() => {
  window.addEventListener('scroll', updatePathProgress)
  updatePathProgress()
})

onUnmounted(() => {
  window.removeEventListener('scroll', updatePathProgress)
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
        :style="{
          left: star.left,
          top: star.top,
          animationDelay: star.animationDelay,
          width: star.size,
          height: star.size
        }"
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
            <a href="/" class="flex items-center gap-3">
              <svg width="40" height="40" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" fill="#6366f1"/>
                <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M28.1159 52.8038C27.4167 52.1278 26.4821 51.75 25.5096 51.75L24.3595 51.75C20.9764 51.75 19.323 55.8766 21.7702 58.2126L45.4106 80.7784C46.8597 82.1616 49.1401 82.1616 50.5891 80.7784L74.2295 58.2126C76.6767 55.8766 75.0233 51.75 71.6403 51.75L70.4901 51.75C69.5175 51.75 68.583 52.1278 67.8837 52.8038L50.6062 69.5055C49.1526 70.9105 46.847 70.9105 45.3935 69.5054L28.1159 52.8038Z" fill="white"/>
              </svg>
              <span class="text-2xl font-bold gradient-text">Esprito App</span>
            </a>
            <div class="flex items-center gap-6">
              <button
                @click="toggleSound"
                class="glass-card px-4 py-2 rounded-full hover:bg-white/10 transition flex items-center gap-2"
                :class="soundEnabled ? 'text-cosmic-400' : 'text-gray-400'"
              >
                <span v-if="soundEnabled">🔊</span>
                <span v-else>🔈</span>
                <span class="text-sm hidden sm:inline">Sound</span>
              </button>
              <a href="/" class="text-gray-300 hover:text-white transition">Home</a>
              <a href="/app" class="px-6 py-2 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full hover:opacity-90 transition font-semibold">
                Launch App
              </a>
            </div>
          </div>
        </div>
      </nav>

      <!-- Hero Section -->
      <section class="pt-32 pb-12 px-4 sm:px-6 lg:px-8">
        <div class="max-w-5xl mx-auto text-center">
          <div class="inline-block mb-6 px-6 py-2 glass-card cosmic-glow">
            <span class="text-sm font-semibold gradient-text">🚀 Journey Through Innovation</span>
          </div>

          <h1 class="text-5xl md:text-7xl font-bold mb-6 leading-tight">
            Esprito App trading
            <span class="gradient-text"> Roadmap</span>
          </h1>

          <p class="text-xl md;text-2xl text-gray-300 mb-8 leading-relaxed max-w-3xl mx-auto">
            Travel with us as we build the future of decentralized trading.
            Each milestone represents a quantum leap in making crypto trading accessible to everyone.
          </p>
        </div>
      </section>

      <!-- Roadmap Journey -->
      <section class="py-12 px-4 sm:px-6 lg:px-8">
        <div class="max-w-6xl mx-auto">
          <!-- Journey Path -->
          <div class="relative">
            <!-- SVG Journey Path (curved line connecting planets) -->
            <svg class="absolute inset-0 w-full h-full pointer-events-none hidden md:block" preserveAspectRatio="none">
              <defs>
                <linearGradient id="pathGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="#6366f1" />
                  <stop offset="50%" stop-color="#a855f7" />
                  <stop offset="100%" stop-color="#ec4899" />
                </linearGradient>
                <filter id="glow">
                  <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
                  <feMerge>
                    <feMergeNode in="coloredBlur"/>
                    <feMergeNode in="SourceGraphic"/>
                  </feMerge>
                </filter>
              </defs>
              <!-- Background path -->
              <path
                :d="journeyPath"
                fill="none"
                stroke="rgba(255,255,255,0.1)"
                stroke-width="3"
                stroke-dasharray="10,10"
              />
              <!-- Animated progress path -->
              <path
                :d="journeyPath"
                fill="none"
                stroke="url(#pathGradient)"
                stroke-width="3"
                filter="url(#glow)"
                :stroke-dasharray="pathLength"
                :stroke-dashoffset="pathLength - (pathLength * pathProgress / 100)"
                class="transition-all duration-300"
              />
            </svg>

            <!-- Spaceship traveling along path -->
            <div
              class="absolute w-8 h-8 transform -translate-x-1/2 -translate-y-1/2 z-20 hidden md:block transition-all duration-300"
              :style="spaceshipPosition"
            >
              <div class="relative">
                <div class="absolute inset-0 bg-cosmic-500 rounded-full blur-md opacity-60 animate-pulse"></div>
                <svg viewBox="0 0 24 24" class="w-8 h-8 text-white drop-shadow-lg" :style="{ transform: `rotate(${spaceshipRotation}deg)` }">
                  <path fill="currentColor" d="M12 2L4 14h3v6l5-8h-3l3-10z"/>
                </svg>
              </div>
            </div>

            <!-- Milestones -->
            <div class="space-y-32 relative">
              <div
                v-for="(milestone, index) in milestones"
                :key="milestone.id"
                :class="index % 2 === 0 ? 'md:flex-row' : 'md:flex-row-reverse'"
                class="milestone-item flex flex-col md:flex md:gap-16 items-center relative"
              >
                <!-- Trajectory connector (curved arrow to next planet) -->
                <div
                  v-if="index < milestones.length - 1"
                  class="absolute hidden md:block z-0"
                  :class="index % 2 === 0 ? 'right-1/4 bottom-0' : 'left-1/4 bottom-0'"
                  style="transform: translateY(50%)"
                >
                  <svg
                    :class="index % 2 === 0 ? '' : 'scale-x-[-1]'"
                    width="200"
                    height="120"
                    viewBox="0 0 200 120"
                    class="text-white/20"
                  >
                    <path
                      d="M10 10 Q 100 10 100 60 Q 100 110 190 110"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-dasharray="8,8"
                    />
                    <polygon
                      points="185,105 195,110 185,115"
                      fill="currentColor"
                    />
                  </svg>
                </div>

                <!-- Planet Side -->
                <div
                  class="flex-shrink-0 relative z-10"
                  :class="index % 2 === 0 ? 'md:mr-16' : 'md:ml-16'"
                >
                  <div class="relative">
                    <!-- Planet glow ring -->
                    <div
                      class="absolute inset-0 rounded-full opacity-20 blur-lg animate-pulse"
                      :class="`bg-gradient-to-r ${milestone.color}`"
                      style="transform: scale(1.1)"
                    ></div>

                    <!-- Orbit ring - smaller and subtle -->
                    <div
                      class="absolute inset-0 rounded-full border border-dashed opacity-15 animate-spin-slow"
                      :class="milestone.completed ? 'border-green-400' : 'border-white'"
                      style="transform: scale(1.15); animation-duration: 30s"
                    ></div>

                    <!-- Planet -->
                    <div
                      class="w-56 h-56 md:w-64 md:h-64 rounded-full shadow-2xl animate-float transform hover:scale-105 transition-all duration-500 overflow-hidden bg-transparent relative"
                      :style="`animation-delay: ${index * 0.5}s`"
                      @mouseenter="handleHover"
                    >
                      <img
                        :src="`/planets/${milestone.planetImage}`"
                        :alt="`${milestone.quarter} milestone`"
                        class="w-full h-full object-cover"
                      />

                      <!-- Completed badge -->
                      <div
                        v-if="milestone.completed"
                        class="absolute top-2 right-2 w-10 h-10 bg-green-500 rounded-full flex items-center justify-center text-white text-xl font-bold shadow-lg"
                      >
                        ✓
                      </div>
                    </div>

                    <!-- Planet label -->
                    <div class="absolute -bottom-8 left-1/2 transform -translate-x-1/2 whitespace-nowrap">
                      <span
                        class="text-sm font-bold px-4 py-2 rounded-full shadow-lg"
                        :class="`bg-gradient-to-r ${milestone.color} text-white`"
                      >
                        {{ milestone.quarter }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Content Side -->
                <div
                  class="flex-1 mt-16 md:mt-0 min-h-[280px] flex items-center relative z-10"
                  :class="index % 2 === 0 ? 'md:text-left md:pr-8' : 'md:text-right md:pl-8'"
                >
                  <div
                    class="w-full p-6 md:p-8 transition-all duration-300 group rounded-2xl content-card"
                    @mouseenter="handleHover"
                  >
                    <h3 class="text-2xl md:text-3xl font-bold mb-6 text-white group-hover:text-gray-300 transition-colors duration-300">
                      {{ milestone.title }}
                    </h3>

                    <!-- Features list -->
                    <div
                      class="grid grid-cols-1 gap-3"
                      :class="index % 2 === 0 ? '' : 'md:justify-items-end'"
                    >
                      <div
                        v-for="feature in milestone.features"
                        :key="feature"
                        class="flex items-start gap-3"
                        :class="index % 2 === 0 ? 'flex-row' : 'md:flex-row-reverse'"
                      >
                        <span class="text-cosmic-400 text-lg flex-shrink-0">✦</span>
                        <span class="text-gray-300 group-hover:text-gray-400 transition-colors duration-300 text-sm md:text-base">{{ feature }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- CTA Section -->
      <section class="py-20 px-4 sm:px-6 lg:px-8">
        <div class="max-w-4xl mx-auto text-center">
          <div class="glass-card p-12 cosmic-glow">
            <h2 class="text-4xl font-bold mb-6">
              Join Us on This <span class="gradient-text">Journey</span>
            </h2>
            <p class="text-xl text-gray-300 mb-8">
              Be part of the revolution in decentralized trading. Start using Esprito App today.
            </p>
            <div class="flex flex-col sm:flex-row gap-4 justify-center">
              <a href="/app" class="px-8 py-4 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full hover:opacity-90 transition font-semibold text-lg">
                Launch App
              </a>
              <a href="/" class="px-8 py-4 glass-card hover:bg-white/10 transition font-semibold text-lg rounded-full">
                Back home
              </a>
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

            <div>
              <h4 class="font-semibold mb-4">Product</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/#features" class="hover:text-white transition">Features</a></li>
                <li><a href="/roadmap.html" class="hover:text-white transition">Roadmap</a></li>
                <li><a href="/app" class="hover:text-white transition">Launch App</a></li>
                <li><a href="#" class="hover:text-white transition">Documentation</a></li>
              </ul>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Company</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="#" class="hover:text-white transition">About Us</a></li>
                <li><a href="/blog.html" class="hover:text-white transition">Blog</a></li>
                <li><a href="#" class="hover:text-white transition">Careers</a></li>
                <li><a href="#" class="hover:text-white transition">Contact</a></li>
              </ul>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Legal</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/terms.html" class="hover:text-white transition">Terms of Service</a></li>
                <li><a href="/privacy.html" class="hover:text-white transition">Privacy Policy</a></li>
                <li><a href="/privacy.html" class="hover:text-white transition">Cookie Policy</a></li>
                <li><a href="/terms.html" class="hover:text-white transition">Disclaimer</a></li>
              </ul>
            </div>
          </div>

          <div class="pt-8 border-t border-white/10 flex flex-col md:flex-row justify-between items-center gap-4">
            <p class="text-gray-400 text-sm">
              © 2026 Esprito Tech QFZ LLC. All rights reserved.
            </p>
            <div class="flex items-center gap-6 text-gray-400 text-sm">
              <span>Built with ❤️ for the TON ecosystem</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.star {
  animation: twinkle 3s infinite;
}

.animate-spin-slow {
  animation: spin 20s linear infinite;
}

@keyframes spin {
  from {
    transform: scale(1.4) rotate(0deg);
  }
  to {
    transform: scale(1.4) rotate(360deg);
  }
}

.milestone-item {
  opacity: 0;
  transform: translateY(40px);
  animation: fadeInUp 0.8s ease-out forwards;
}

.milestone-item:nth-child(1) { animation-delay: 0.1s; }
.milestone-item:nth-child(2) { animation-delay: 0.2s; }
.milestone-item:nth-child(3) { animation-delay: 0.3s; }
.milestone-item:nth-child(4) { animation-delay: 0.4s; }
.milestone-item:nth-child(5) { animation-delay: 0.5s; }
.milestone-item:nth-child(6) { animation-delay: 0.6s; }
.milestone-item:nth-child(7) { animation-delay: 0.7s; }
.milestone-item:nth-child(8) { animation-delay: 0.8s; }

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Content card - transparent by default */
.content-card {
  background: transparent;
}

.content-card:hover {
  background: rgba(255, 255, 255, 0.03);
}
</style>
