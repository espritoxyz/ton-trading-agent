import { createApp } from 'vue'
import App from './App.vue'
import './assets/tailwind.css'
import { initAuth } from './composables/useAuth'
import { loadTheme, applyTheme } from './composables/useTheme'
import '../landing/src/style.css'

// Ensure theme class is set before we mount so styles render correctly
if (typeof window !== 'undefined') {
  const t = loadTheme()
  applyTheme(t)
}

async function bootstrap() {
  const app = createApp(App)
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
