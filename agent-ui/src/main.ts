import { createApp } from 'vue'
import App from './App.vue'
import './assets/tailwind.css'
import { initAuth } from './composables/useAuth'
import { loadTheme, applyTheme } from './composables/useTheme'
import '../landing/src/style.css'
import VueYandexMetrika from 'vue3-yandex-metrika'

// Ensure theme class is set before we mount so styles render correctly
if (typeof window !== 'undefined') {
  // Landing page always uses dark theme (it doesn't support light theme)
  const currentPath = window.location.pathname
  const isLandingPage = currentPath === '/' || currentPath === '/roadmap' || currentPath === '/privacy' || currentPath === '/blog' || currentPath === '/terms'

  if (isLandingPage) {
    // Force dark theme for landing pages
    applyTheme('dark')
  } else {
    // Load saved theme for app pages
    const t = loadTheme()
    applyTheme(t)
  }
}

async function bootstrap() {
  const app = createApp(App)

  // Configure Yandex Metrika
  const metrikaId = import.meta.env.VITE_YANDEX_METRIKA_ID
  if (metrikaId && metrikaId !== 'YOUR_METRIKA_ID') {
    app.use(VueYandexMetrika, {
      id: metrikaId,
      router: false,              // We use custom routing in App.vue with manual tracking
      env: 'production',          // Always 'production' to enable tracking
      debug: import.meta.env.DEV, // In dev mode, logs to console instead of sending data
      options: {
        clickmap: true,           // Track clicks
        trackLinks: true,         // Track external links
        accurateTrackBounce: true,// Accurate bounce rate
        webvisor: true,           // Enable Webvisor
        trackHash: true,          // Track hash changes (for SPA)
        ecommerce: false          // Disable e-commerce tracking
      }
    })
  }

  // We use a lightweight internal routing in src/App.vue (history API), no vue-router required
  app.mount('#app')

  // Run auth initialization in background so mount is not delayed
  try {
    initAuth().catch(() => {})
  } catch (e) {
    // ignore
  }
}

bootstrap()
