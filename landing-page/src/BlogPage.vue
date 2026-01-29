<script setup>
import { ref, computed } from 'vue'

// Generate random stars
const stars = Array.from({ length: 150 }, () => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 3}s`,
  size: Math.random() > 0.7 ? '3px' : '2px'
}))

const selectedCategory = ref('all')

const categories = [
  { id: 'all', name: 'All Posts', icon: '🌌' },
  { id: 'company', name: 'Company', icon: '🚀' },
  { id: 'product', name: 'Product', icon: '🛸' },
  { id: 'engineering', name: 'Engineering', icon: '🔧' },
  { id: 'research', name: 'Research', icon: '🔭' },
]

const blogPosts = ref([
  {
    id: 1,
    title: 'Introducing Esprito App: The Future of DEX Trading',
    excerpt: 'We are excited to announce the launch of Esprito App, a revolutionary AI-powered trading assistant for the TON ecosystem.',
    category: 'company',
    date: 'January 15, 2026',
    author: 'Esprito Team',
    readTime: '5 min read',
    image: '/images/blog/launch.png',
    featured: true
  },
  {
    id: 2,
    title: 'How Our AI Understands Your Trading Intent',
    excerpt: 'Deep dive into the natural language processing technology that powers Esprito App conversational trading experience.',
    category: 'engineering',
    date: 'January 20, 2026',
    author: 'Engineering Team',
    readTime: '8 min read',
    image: '/images/blog/ai-tech.png',
    featured: false
  },
  {
    id: 3,
    title: 'STON.fi Integration: Best-in-Class DEX Support',
    excerpt: 'Learn how our integration with STON.fi enables seamless token swaps with optimal rates and minimal slippage.',
    category: 'product',
    date: 'January 25, 2026',
    author: 'Product Team',
    readTime: '4 min read',
    image: '/images/blog/stonfi.png',
    featured: false
  },
  {
    id: 4,
    title: 'Security First: How We Protect Your Assets',
    excerpt: 'An overview of our security architecture and why your funds are always safe with Esprito App non-custodial approach.',
    category: 'engineering',
    date: 'January 28, 2026',
    author: 'Security Team',
    readTime: '6 min read',
    image: '/images/blog/security.png',
    featured: false
  },
  {
    id: 5,
    title: 'Q1 2026 Roadmap Update',
    excerpt: 'A look at what we have achieved and what is coming next in our journey to democratize crypto trading.',
    category: 'company',
    date: 'February 1, 2026',
    author: 'Esprito Team',
    readTime: '3 min read',
    image: '/images/blog/roadmap.png',
    featured: false
  },
  {
    id: 6,
    title: 'Understanding TON Blockchain for Traders',
    excerpt: 'A comprehensive guide to the TON blockchain ecosystem and why it is perfect for decentralized trading.',
    category: 'research',
    date: 'February 5, 2026',
    author: 'Research Team',
    readTime: '10 min read',
    image: '/images/blog/ton.png',
    featured: false
  }
])

const filteredPosts = computed(() => {
  if (selectedCategory.value === 'all') {
    return blogPosts.value
  }
  return blogPosts.value.filter(post => post.category === selectedCategory.value)
})

const featuredPost = computed(() => {
  return blogPosts.value.find(post => post.featured)
})

const getCategoryIcon = (categoryId) => {
  const category = categories.find(c => c.id === categoryId)
  return category ? category.icon : '📝'
}

const getCategoryName = (categoryId) => {
  const category = categories.find(c => c.id === categoryId)
  return category ? category.name : categoryId
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
              <a href="/roadmap.html" class="text-gray-300 hover:text-white transition">Roadmap</a>
              <a href="/blog.html" class="text-white font-semibold">Blog</a>
              <a href="http://localhost:5173" target="_blank" class="cta-button px-6 py-2 rounded-full font-semibold">
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
            <span class="text-sm font-semibold gradient-text">📡 Transmissions from the Cosmos</span>
          </div>

          <h1 class="text-5xl md:text-7xl font-bold mb-6 leading-tight">
            Esprito App
            <span class="gradient-text"> Blog</span>
          </h1>

          <p class="text-xl md:text-2xl text-gray-300 mb-8 leading-relaxed max-w-3xl mx-auto">
            News, updates, and insights from our journey to revolutionize decentralized trading.
          </p>
        </div>
      </section>

      <!-- Category Filter -->
      <section class="px-4 sm:px-6 lg:px-8 pb-8">
        <div class="max-w-7xl mx-auto">
          <div class="flex flex-wrap justify-center gap-3">
            <button
              v-for="category in categories"
              :key="category.id"
              @click="selectedCategory = category.id"
              class="px-6 py-3 rounded-full font-medium transition-all duration-300"
              :class="selectedCategory === category.id
                ? 'bg-gradient-to-r from-cosmic-500 to-purple-600 text-white'
                : 'glass-card text-gray-300 hover:bg-white/10'"
            >
              <span class="mr-2">{{ category.icon }}</span>
              {{ category.name }}
            </button>
          </div>
        </div>
      </section>

      <!-- Featured Post -->
      <section v-if="featuredPost && selectedCategory === 'all'" class="px-4 sm:px-6 lg:px-8 pb-12">
        <div class="max-w-7xl mx-auto">
          <div class="glass-card p-8 cosmic-glow hover:bg-white/5 transition-all duration-300 group cursor-pointer">
            <div class="grid md:grid-cols-2 gap-8 items-center">
              <div class="aspect-video bg-gradient-to-br from-cosmic-500/20 to-purple-500/20 rounded-xl overflow-hidden">
                <div class="w-full h-full flex items-center justify-center text-6xl">
                  🚀
                </div>
              </div>
              <div>
                <div class="flex items-center gap-3 mb-4">
                  <span class="px-3 py-1 rounded-full text-xs font-semibold bg-gradient-to-r from-cosmic-500 to-purple-600">
                    Featured
                  </span>
                  <span class="text-gray-400 text-sm">{{ featuredPost.date }}</span>
                </div>
                <h2 class="text-3xl md:text-4xl font-bold mb-4 group-hover:gradient-text transition">
                  {{ featuredPost.title }}
                </h2>
                <p class="text-gray-300 text-lg mb-6 leading-relaxed">
                  {{ featuredPost.excerpt }}
                </p>
                <div class="flex items-center gap-4 text-sm text-gray-400">
                  <span>{{ featuredPost.author }}</span>
                  <span>-</span>
                  <span>{{ featuredPost.readTime }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Blog Posts Grid -->
      <section class="px-4 sm:px-6 lg:px-8 pb-20">
        <div class="max-w-7xl mx-auto">
          <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            <article
              v-for="post in filteredPosts.filter(p => !p.featured || selectedCategory !== 'all')"
              :key="post.id"
              class="glass-card overflow-hidden hover:bg-white/5 transition-all duration-300 group cursor-pointer"
            >
              <!-- Post Image -->
              <div class="aspect-video bg-gradient-to-br from-cosmic-500/10 to-purple-500/10 flex items-center justify-center">
                <span class="text-5xl">{{ getCategoryIcon(post.category) }}</span>
              </div>

              <!-- Post Content -->
              <div class="p-6">
                <div class="flex items-center gap-3 mb-4">
                  <span class="px-3 py-1 rounded-full text-xs font-medium glass-card">
                    {{ getCategoryName(post.category) }}
                  </span>
                  <span class="text-gray-400 text-sm">{{ post.date }}</span>
                </div>

                <h3 class="text-xl font-bold mb-3 group-hover:text-cosmic-400 transition line-clamp-2">
                  {{ post.title }}
                </h3>

                <p class="text-gray-400 text-sm mb-4 line-clamp-3">
                  {{ post.excerpt }}
                </p>

                <div class="flex items-center justify-between text-sm text-gray-500">
                  <span>{{ post.author }}</span>
                  <span>{{ post.readTime }}</span>
                </div>
              </div>
            </article>
          </div>

          <!-- Empty State -->
          <div v-if="filteredPosts.length === 0" class="text-center py-20">
            <span class="text-6xl mb-4 block">🔭</span>
            <h3 class="text-2xl font-bold mb-2">No posts found</h3>
            <p class="text-gray-400">Check back later for updates in this category.</p>
          </div>
        </div>
      </section>

      <!-- Newsletter CTA -->
      <section class="px-4 sm:px-6 lg:px-8 pb-20">
        <div class="max-w-4xl mx-auto">
          <div class="glass-card p-12 cosmic-glow text-center">
            <h2 class="text-3xl md:text-4xl font-bold mb-4">
              Stay Updated with <span class="gradient-text">Cosmic News</span>
            </h2>
            <p class="text-gray-300 mb-8">
              Subscribe to our newsletter for the latest updates and insights.
            </p>
            <div class="flex flex-col sm:flex-row gap-4 justify-center max-w-md mx-auto">
              <input
                type="email"
                placeholder="Enter your email"
                class="flex-1 px-6 py-3 bg-white/10 border border-white/20 rounded-full focus:outline-none focus:ring-2 focus:ring-cosmic-500 placeholder-gray-400"
              />
              <button class="cta-button px-8 py-3 rounded-full font-semibold">
                Subscribe
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Investment Disclaimer -->
      <section class="px-4 sm:px-6 lg:px-8 pb-12">
        <div class="max-w-4xl mx-auto">
          <div class="glass-card p-6 border border-yellow-500/20">
            <h4 class="text-yellow-500 font-semibold mb-2 flex items-center gap-2">
              <span>&#9888;</span> Disclaimer
            </h4>
            <p class="text-gray-400 text-sm leading-relaxed">
              Nothing on this site is investment advice. All information is for informational purposes only. You should not construe any such information or other material as legal, tax, investment, financial, or other advice. Nothing contained on our site constitutes a solicitation, recommendation, endorsement, or offer by esprito.app or any third party service provider to buy or sell any assets, digital coins and tokens, securities or other financial instruments in this or in any other jurisdiction in which such solicitation or offer would be unlawful under the securities laws of such jurisdiction. Please view <a href="/terms.html" class="text-cosmic-400 hover:underline">Terms of Use</a> for more information.
            </p>
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
              <p class="text-gray-400 text-sm">
                The future of decentralized trading powered by AI.
              </p>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Product</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/#features" class="hover:text-white transition">Features</a></li>
                <li><a href="/roadmap.html" class="hover:text-white transition">Roadmap</a></li>
                <li><a href="http://localhost:5173" class="hover:text-white transition">Launch App</a></li>
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

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
