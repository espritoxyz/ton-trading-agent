import { loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

const proxyTarget = process.env.VITE_BACKEND_PROXY_TARGET || 'http://localhost:8080'

const config = {
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false,
        rewrite: (p: string) => p.replace(/^\/api/, ''),
      }
    }
  },
  build: {
    rollupOptions: {
      input: {
        landing: resolve(__dirname, 'landing/index.html'),
        app: resolve(__dirname, 'app/index.html'),
        roadmap: resolve(__dirname, 'landing/roadmap.html'),
        blog: resolve(__dirname, 'landing/blog.html'),
        privacy: resolve(__dirname, 'landing/privacy.html'),
        terms: resolve(__dirname, 'landing/terms.html'),
      }
    }
  }
} as any

export default config
