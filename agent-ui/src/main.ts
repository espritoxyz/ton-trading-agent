import { createApp } from 'vue'
import App from './App.vue'
import './assets/tailwind.css'
import { initAuth } from './composables/useAuth'
import { loadTheme, applyTheme } from './composables/useTheme'
import '../landing/src/style.css'
import VueYandexMetrika from 'vue3-yandex-metrika'

// Ensure theme class is set before we mount so styles render correctly
if (typeof window !== 'undefined') {
  const t = loadTheme()
  applyTheme(t)
}

async function bootstrap() {
  const app = createApp(App)

  // Configure Yandex Metrika
  const metrikaId = import.meta.env.VITE_YANDEX_METRIKA_ID
  if (metrikaId && metrikaId !== 'YOUR_METRIKA_ID') {
    app.use(VueYandexMetrika, {
      id: metrikaId,
      env: import.meta.env.MODE,
      debug: import.meta.env.DEV,
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
