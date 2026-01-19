import { createApp } from 'vue'
import App from './App.vue'
import './assets/tailwind.css'
import { initAuth } from './composables/useAuth'

async function bootstrap() {
  await initAuth()
  createApp(App).mount('#app')
}

bootstrap()
