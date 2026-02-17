<script setup>
const stars = Array.from({ length: 150 }, () => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 3}s`,
  size: Math.random() > 0.7 ? '3px' : '2px'
}))

const stats = [
  { value: '2024', label: 'Founded' },
  { value: '27,344+', label: 'Smart contracts scanned' },
  { value: '857', label: 'Honeypots confirmed' },
  { value: '< 0.4%', label: 'False positive rate' },
  { value: '99.9%', label: 'Production uptime' },
]

const values = [
  {
    number: '01',
    title: 'You Stay in Control',
    description: 'We never custody your funds. Trades execute directly from your wallet through decentralized exchanges. Your keys, your crypto — we just make it smarter.',
    icon: '🔐'
  },
  {
    number: '02',
    title: 'AI Assists, You Decide',
    description: 'Our AI surfaces signals, patterns, and intelligence — but every trade is your decision. We\'re a copilot, not an autopilot. We\'re honest about what AI can and can\'t predict.',
    icon: '🧠'
  },
  {
    number: '03',
    title: 'Security is Non-Negotiable',
    description: 'We come from the security world. Risk detection isn\'t an afterthought — it\'s embedded in every product we build. Before we help you trade, we help you stay safe.',
    icon: '🛡️'
  },
  {
    number: '04',
    title: 'Transparency by Default',
    description: 'Our fees are disclosed upfront. Our AI\'s limitations are clearly stated. We don\'t promise guaranteed returns — we promise to give you better information than you\'d have without us.',
    icon: '🔍'
  },
]
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
            <a href="/" class="flex items-center gap-3">
              <svg width="40" height="40" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" fill="#6366f1"/>
                <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M28.1159 52.8038C27.4167 52.1278 26.4821 51.75 25.5096 51.75L24.3595 51.75C20.9764 51.75 19.323 55.8766 21.7702 58.2126L45.4106 80.7784C46.8597 82.1616 49.1401 82.1616 50.5891 80.7784L74.2295 58.2126C76.6767 55.8766 75.0233 51.75 71.6403 51.75L70.4901 51.75C69.5175 51.75 68.583 52.1278 67.8837 52.8038L50.6062 69.5055C49.1526 70.9105 46.847 70.9105 45.3935 69.5054L28.1159 52.8038Z" fill="white"/>
              </svg>
              <span class="text-2xl font-bold gradient-text">Esprito App</span>
            </a>
            <div class="hidden md:flex items-center gap-8">
              <a href="/" class="text-gray-300 hover:text-white transition">Home</a>
              <a href="/roadmap" class="text-gray-300 hover:text-white transition">Roadmap</a>
              <a href="/blog" class="text-gray-300 hover:text-white transition">Blog</a>
              <a href="/app" class="cta-button px-6 py-2 rounded-full font-semibold">
                Launch App
              </a>
            </div>
          </div>
        </div>
      </nav>

      <!-- Hero Section -->
      <section class="pt-32 pb-16 px-4 sm:px-6 lg:px-8">
        <div class="max-w-5xl mx-auto text-center">
          <div class="inline-block mb-6 px-6 py-2 glass-card cosmic-glow">
            <span class="text-sm font-semibold gradient-text">🛡️ Built from the Security World Up</span>
          </div>
          <h1 class="text-5xl md:text-7xl font-bold mb-6 leading-tight">
            Built by people who broke the blockchain to
            <span class="gradient-text"> make it safer.</span>
          </h1>
          <p class="text-xl md:text-2xl text-gray-300 mb-10 leading-relaxed max-w-3xl mx-auto">
            Esprito was founded in 2024 by a team with deep roots in blockchain security research. We started by protecting traders from on-chain fraud — and evolved into building the AI copilot serious DeFi traders have been waiting for.
          </p>
          <div class="flex flex-col sm:flex-row gap-4 justify-center">
            <a href="/app" class="px-8 py-4 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full hover:opacity-90 transition font-semibold text-lg">
              Launch App
            </a>
            <a href="/careers" class="px-8 py-4 glass-card hover:bg-white/10 transition font-semibold text-lg rounded-full">
              Join Our Team
            </a>
          </div>
        </div>
      </section>

      <!-- Stats Section -->
      <section class="py-12 px-4 sm:px-6 lg:px-8">
        <div class="max-w-5xl mx-auto">
          <div class="grid grid-cols-2 md:grid-cols-5 gap-4">
            <div
              v-for="stat in stats"
              :key="stat.label"
              class="glass-card p-6 text-center stat-card"
            >
              <div class="text-2xl md:text-3xl font-bold gradient-text mb-2">{{ stat.value }}</div>
              <div class="text-gray-400 text-sm">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- Our Story -->
      <section class="py-20 px-4 sm:px-6 lg:px-8">
        <div class="max-w-5xl mx-auto">
          <div class="grid md:grid-cols-2 gap-16 items-center">
            <div>
              <h2 class="text-4xl font-bold mb-6">
                Our <span class="gradient-text">Story</span>
              </h2>
              <p class="text-gray-300 leading-relaxed mb-6">
                Esprito began as a <strong class="text-white">blockchain security lab</strong>. Our founding team built one of the first bytecode-level smart contract analyzers for the TON ecosystem — a tool capable of detecting honeypots, hidden fees, and malicious contract logic before users lost funds.
              </p>
              <p class="text-gray-300 leading-relaxed mb-6">
                Our honeypot detector was adopted by <strong class="text-white">STON.fi</strong>, one of TON's leading decentralized exchanges. In production since February 2025, it has scanned over <strong class="text-white">27,000 smart contracts</strong>, catching 857 confirmed honeypots with a near-zero false positive rate of under 0.4%.
              </p>
              <p class="text-gray-300 leading-relaxed mb-6">
                That hands-on experience taught us something important: security is only half the problem. Traders also need better intelligence — faster signals, smarter execution, and AI that genuinely understands on-chain dynamics. That insight shaped Esprito AI.
              </p>
              <p class="text-gray-300 leading-relaxed">
                Today, we're building the AI trading copilot for the next generation of DeFi on multiple blockchains — combining the deep blockchain expertise we built in security research with conversational AI and real-time on-chain analytics.
              </p>
            </div>

            <div class="space-y-6">
              <!-- Timeline -->
              <div class="relative pl-8 border-l border-white/10 space-y-8">
                <div class="relative story-item">
                  <div class="absolute -left-11 w-6 h-6 rounded-full bg-gradient-to-r from-cosmic-500 to-purple-600 flex items-center justify-center">
                    <div class="w-2 h-2 rounded-full bg-white"></div>
                  </div>
                  <div class="glass-card p-5">
                    <div class="text-sm text-cosmic-400 font-semibold mb-1">2024</div>
                    <h3 class="text-white font-bold mb-2">Founded as a Security Lab</h3>
                    <p class="text-gray-400 text-sm">Built the first bytecode-level smart contract analyzer for TON, detecting honeypots and malicious contract logic.</p>
                  </div>
                </div>

                <div class="relative story-item">
                  <div class="absolute -left-11 w-6 h-6 rounded-full bg-gradient-to-r from-purple-500 to-pink-600 flex items-center justify-center">
                    <div class="w-2 h-2 rounded-full bg-white"></div>
                  </div>
                  <div class="glass-card p-5">
                    <div class="text-sm text-purple-400 font-semibold mb-1">February 2025</div>
                    <h3 class="text-white font-bold mb-2">STON.fi Partnership</h3>
                    <p class="text-gray-400 text-sm">Our honeypot detector went live in production on STON.fi, scanning thousands of contracts daily.</p>
                  </div>
                </div>

                <div class="relative story-item">
                  <div class="absolute -left-11 w-6 h-6 rounded-full bg-gradient-to-r from-blue-500 to-cyan-600 flex items-center justify-center">
                    <div class="w-2 h-2 rounded-full bg-white"></div>
                  </div>
                  <div class="glass-card p-5">
                    <div class="text-sm text-blue-400 font-semibold mb-1">Q1 2026</div>
                    <h3 class="text-white font-bold mb-2">Esprito AI Launches</h3>
                    <p class="text-gray-400 text-sm">The AI trading copilot goes live — natural language trading, real-time analytics, and on-chain security built in from day one.</p>
                  </div>
                </div>

                <div class="relative story-item">
                  <div class="absolute -left-11 w-6 h-6 rounded-full bg-gradient-to-r from-green-500 to-emerald-600 flex items-center justify-center">
                    <div class="w-2 h-2 rounded-full bg-white"></div>
                  </div>
                  <div class="glass-card p-5">
                    <div class="text-sm text-green-400 font-semibold mb-1">2026 — 2027</div>
                    <h3 class="text-white font-bold mb-2">Multi-Chain Expansion</h3>
                    <p class="text-gray-400 text-sm">Expanding to Ethereum, Solana, BSC and beyond — bringing AI-powered DeFi trading to every major chain.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Mission Section -->
      <section class="py-20 px-4 sm:px-6 lg:px-8">
        <div class="max-w-4xl mx-auto">
          <div class="glass-card p-12 cosmic-glow text-center">
            <div class="text-5xl mb-6">🌌</div>
            <h2 class="text-4xl font-bold mb-6">
              Our <span class="gradient-text">Mission</span>
            </h2>
            <p class="text-xl md:text-2xl text-gray-300 leading-relaxed italic">
              "We believe every trader deserves the kind of intelligence that was previously only available to hedge funds and insiders."
            </p>
            <div class="mt-8 h-px bg-gradient-to-r from-transparent via-white/20 to-transparent"></div>
            <p class="text-gray-300 leading-relaxed mt-8 max-w-2xl mx-auto">
              Esprito's mission is to give any DeFi trader — regardless of experience or capital — AI-powered tools that surface real alpha, execute at the best prices, and keep them safe from on-chain risk. All without giving up self-custody.
            </p>
          </div>
        </div>
      </section>

      <!-- Values Section -->
      <section class="py-20 px-4 sm:px-6 lg:px-8">
        <div class="max-w-6xl mx-auto">
          <div class="text-center mb-16">
            <h2 class="text-4xl md:text-5xl font-bold mb-4">
              What We <span class="gradient-text">Stand For</span>
            </h2>
            <p class="text-xl text-gray-300">The principles that guide everything we build</p>
          </div>

          <div class="grid md:grid-cols-2 gap-8">
            <div
              v-for="value in values"
              :key="value.number"
              class="value-card glass-card p-8 group transition-all duration-300"
            >
              <div class="flex items-start gap-5">
                <div class="flex-shrink-0">
                  <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-cosmic-500/20 to-purple-600/20 border border-cosmic-500/30 flex items-center justify-center text-2xl group-hover:scale-110 transition-transform duration-300">
                    {{ value.icon }}
                  </div>
                </div>
                <div>
                  <div class="text-cosmic-400 text-sm font-bold mb-1">{{ value.number }}</div>
                  <h3 class="text-xl font-bold text-white mb-3 group-hover:text-cosmic-300 transition-colors duration-300">{{ value.title }}</h3>
                  <p class="text-gray-400 leading-relaxed">{{ value.description }}</p>
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
              Ready to trade <span class="gradient-text">smarter?</span>
            </h2>
            <p class="text-xl text-gray-300 mb-8">
              Join traders using Esprito AI to trade easier and smarter on DEXes.
            </p>
            <div class="flex flex-col sm:flex-row gap-4 justify-center">
              <a href="/app" class="px-8 py-4 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full hover:opacity-90 transition font-semibold text-lg">
                Launch App
              </a>
              <a href="/careers" class="px-8 py-4 glass-card hover:bg-white/10 transition font-semibold text-lg rounded-full">
                Join Our Team
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
                  <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
                  <path fill-rule="evenodd" clip-rule="evenodd" d="M28.1159 52.8038C27.4167 52.1278 26.4821 51.75 25.5096 51.75L24.3595 51.75C20.9764 51.75 19.323 55.8766 21.7702 58.2126L45.4106 80.7784C46.8597 82.1616 49.1401 82.1616 50.5891 80.7784L74.2295 58.2126C76.6767 55.8766 75.0233 51.75 71.6403 51.75L70.4901 51.75C69.5175 51.75 68.583 52.1278 67.8837 52.8038L50.6062 69.5055C49.1526 70.9105 46.847 70.9105 45.3935 69.5054L28.1159 52.8038Z" fill="white"/>
                </svg>
                <span class="text-xl font-bold">Esprito App</span>
              </div>
              <p class="text-gray-400 text-sm">The future of decentralized trading powered by AI.</p>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Product</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/#features" class="hover:text-white transition">Features</a></li>
                <li><a href="/roadmap" class="hover:text-white transition">Roadmap</a></li>
                <li><a href="/app" class="hover:text-white transition">Launch App</a></li>
              </ul>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Company</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/about" class="hover:text-white transition">About Us</a></li>
                <li><a href="/blog" class="hover:text-white transition">Blog</a></li>
                <li><a href="/careers" class="hover:text-white transition">Careers</a></li>
                <li><a href="mailto:support@esprito.app" class="hover:text-white transition">Contact</a></li>
              </ul>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Legal</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/terms" class="hover:text-white transition">Terms of Service</a></li>
                <li><a href="/privacy" class="hover:text-white transition">Privacy Policy</a></li>
              </ul>
            </div>
          </div>

          <div class="pt-8 border-t border-white/10 flex flex-col md:flex-row justify-between items-center gap-4">
            <p class="text-gray-400 text-sm">© 2026 Esprito Tech QFZ LLC. All rights reserved.</p>
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

.stat-card {
  opacity: 0;
  animation: fadeInUp 0.6s ease forwards;
}

.stat-card:nth-child(1) { animation-delay: 0.1s; }
.stat-card:nth-child(2) { animation-delay: 0.2s; }
.stat-card:nth-child(3) { animation-delay: 0.3s; }
.stat-card:nth-child(4) { animation-delay: 0.4s; }
.stat-card:nth-child(5) { animation-delay: 0.5s; }

.story-item {
  opacity: 0;
  animation: fadeInUp 0.6s ease forwards;
}

.story-item:nth-child(1) { animation-delay: 0.2s; }
.story-item:nth-child(2) { animation-delay: 0.4s; }
.story-item:nth-child(3) { animation-delay: 0.6s; }
.story-item:nth-child(4) { animation-delay: 0.8s; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.value-card {
  background: radial-gradient(ellipse at top left, rgba(99, 102, 241, 0.08) 0%, transparent 60%);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.value-card:hover {
  background: radial-gradient(ellipse at top left, rgba(99, 102, 241, 0.15) 0%, rgba(255, 255, 255, 0.03) 60%);
  transform: translateY(-2px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}
</style>