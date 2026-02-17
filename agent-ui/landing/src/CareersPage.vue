<script setup>
const stars = Array.from({ length: 150 }, () => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 3}s`,
  size: Math.random() > 0.7 ? '3px' : '2px'
}))

const perks = [
  { icon: '🌍', title: 'Fully Remote', description: 'Work from anywhere. We\'re a distributed team across multiple time zones — async-first culture.' },
  { icon: '🚀', title: 'Early-Stage Impact', description: 'Join at the ground floor. Your work ships to thousands of users and shapes the product direction.' },
  { icon: '🧠', title: 'Deep Tech Stack', description: 'Work at the intersection of AI, blockchain, and DeFi — some of the most interesting problems in tech.' },
  { icon: '💰', title: 'Competitive Comp', description: 'Market-rate salary, token allocation, and equity in a fast-growing Web3 company.' },
  { icon: '🛡️', title: 'Security-First Culture', description: 'We come from the security world. You\'ll work with people who take correctness, reliability, and safety seriously.' },
  { icon: '⚡', title: 'Move Fast', description: 'No bureaucracy. Ideas go from chat to production in days, not months.' },
]

const openRoles = [
  {
    title: 'Senior Backend Engineer',
    team: 'Engineering',
    type: 'Full-time · Remote',
    tags: ['Kotlin', 'Java', 'Spring Boot', 'TON Blockchain'],
    description: 'Build the core backend services powering our AI trading engine — order management, strategy execution, real-time monitoring, and blockchain integrations.',
  },
  {
    title: 'AI/ML Engineer',
    team: 'AI',
    type: 'Full-time · Remote',
    tags: ['Python', 'LLM', 'NLP', 'RAG'],
    description: 'Design and improve the AI models that interpret user trading commands, generate market signals, and power our conversational trading interface.',
  },
  {
    title: 'Frontend Engineer',
    team: 'Engineering',
    type: 'Full-time · Remote',
    tags: ['Vue.js', 'TypeScript', 'TailwindCSS'],
    description: 'Build the web application and Telegram Mini App interfaces that make complex DeFi operations feel simple and intuitive.',
  },
  {
    title: 'Blockchain Engineer',
    team: 'Engineering',
    type: 'Full-time · Remote',
    tags: ['TON', 'Ethereum', 'Solana', 'Smart Contracts'],
    description: 'Own our multi-chain integrations — from TON and STON.fi to Ethereum, Solana, and beyond. Build the DEX routing, contract interaction, and on-chain analytics layers.',
  },
  {
    title: 'Smart Contract Security Researcher',
    team: 'Security',
    type: 'Full-time · Remote',
    tags: ['TON', 'FunC', 'Tact', 'Security Research'],
    description: 'Extend our honeypot detection and smart contract analysis capabilities. Work on the tools that keep Esprito users safe from on-chain threats.',
  },
  {
    title: 'Growth & Community Lead',
    team: 'Growth',
    type: 'Full-time · Remote',
    tags: ['DeFi', 'Crypto', 'Community', 'Marketing'],
    description: 'Drive user acquisition and build the Esprito community across Telegram, X, Reddit, and the broader DeFi ecosystem.',
  },
]

const teamColors = {
  'Engineering': 'from-cosmic-500 to-purple-600',
  'AI': 'from-blue-500 to-cyan-600',
  'Security': 'from-green-500 to-emerald-600',
  'Growth': 'from-orange-500 to-amber-600',
}
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
              <a href="/about" class="text-gray-300 hover:text-white transition">About</a>
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
            <span class="text-sm font-semibold gradient-text">🚀 We're Hiring</span>
          </div>
          <h1 class="text-5xl md:text-7xl font-bold mb-6 leading-tight">
            Build the future of
            <span class="gradient-text"> DeFi trading</span>
          </h1>
          <p class="text-xl md:text-2xl text-gray-300 mb-10 leading-relaxed max-w-3xl mx-auto">
            Join a small, high-conviction team at the intersection of AI, blockchain security, and decentralized finance. We're building tools that give every trader an edge.
          </p>
          <a href="mailto:careers@esprito.app" class="inline-block px-8 py-4 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full hover:opacity-90 transition font-semibold text-lg">
            View Open Roles ↓
          </a>
        </div>
      </section>

      <!-- Why Esprito Section -->
      <section class="py-20 px-4 sm:px-6 lg:px-8">
        <div class="max-w-6xl mx-auto">
          <div class="text-center mb-16">
            <h2 class="text-4xl md:text-5xl font-bold mb-4">
              Why <span class="gradient-text">Esprito?</span>
            </h2>
            <p class="text-xl text-gray-300">What it's like to work here</p>
          </div>

          <div class="grid md:grid-cols-3 gap-8">
            <div
              v-for="perk in perks"
              :key="perk.title"
              class="perk-card glass-card p-8 group transition-all duration-300"
            >
              <div class="text-4xl mb-5 group-hover:scale-110 transition-transform duration-300">{{ perk.icon }}</div>
              <h3 class="text-xl font-bold text-white mb-3 group-hover:text-cosmic-300 transition-colors duration-300">{{ perk.title }}</h3>
              <p class="text-gray-400 leading-relaxed">{{ perk.description }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Open Roles Section -->
      <section class="py-20 px-4 sm:px-6 lg:px-8">
        <div class="max-w-4xl mx-auto">
          <div class="text-center mb-16">
            <h2 class="text-4xl md:text-5xl font-bold mb-4">
              Open <span class="gradient-text">Roles</span>
            </h2>
            <p class="text-xl text-gray-300">All positions are fully remote</p>
          </div>

          <div class="space-y-6">
            <div
              v-for="role in openRoles"
              :key="role.title"
              class="role-card glass-card p-8 group transition-all duration-300"
            >
              <div class="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2">
                    <span
                      class="text-xs font-bold px-3 py-1 rounded-full text-white"
                      :class="`bg-gradient-to-r ${teamColors[role.team]}`"
                    >
                      {{ role.team }}
                    </span>
                    <span class="text-gray-400 text-sm">{{ role.type }}</span>
                  </div>
                  <h3 class="text-2xl font-bold text-white mb-3 group-hover:text-cosmic-300 transition-colors duration-300">
                    {{ role.title }}
                  </h3>
                  <p class="text-gray-400 leading-relaxed mb-4">{{ role.description }}</p>
                  <div class="flex flex-wrap gap-2">
                    <span
                      v-for="tag in role.tags"
                      :key="tag"
                      class="text-xs px-3 py-1 rounded-full glass-card text-gray-300 border border-white/10"
                    >
                      {{ tag }}
                    </span>
                  </div>
                </div>
                <div class="flex-shrink-0">
                  <a
                    :href="`mailto:careers@esprito.app?subject=Application: ${role.title}`"
                    class="inline-block px-6 py-3 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full hover:opacity-90 transition font-semibold text-sm whitespace-nowrap"
                  >
                    Apply Now
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Don't see a fit? -->
      <section class="py-12 px-4 sm:px-6 lg:px-8">
        <div class="max-w-4xl mx-auto">
          <div class="glass-card p-12 cosmic-glow text-center">
            <div class="text-4xl mb-4">💌</div>
            <h2 class="text-3xl font-bold mb-4">
              Don't see a fit?
            </h2>
            <p class="text-xl text-gray-300 mb-8">
              We're always on the lookout for exceptional people. If you're passionate about DeFi, AI, or blockchain security — we want to hear from you.
            </p>
            <a
              href="mailto:careers@esprito.app?subject=General Application"
              class="inline-block px-8 py-4 bg-gradient-to-r from-cosmic-500 to-purple-600 rounded-full hover:opacity-90 transition font-semibold text-lg"
            >
              Send us your CV
            </a>
            <p class="text-gray-400 text-sm mt-4">careers@esprito.app</p>
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

.perk-card {
  background: radial-gradient(ellipse at top, rgba(99, 102, 241, 0.08) 0%, transparent 70%);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.perk-card:hover {
  background: radial-gradient(ellipse at top, rgba(99, 102, 241, 0.18) 0%, rgba(255, 255, 255, 0.03) 70%);
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.role-card {
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
}

.role-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  background: rgba(99, 102, 241, 0.05);
  transform: translateY(-2px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}
</style>