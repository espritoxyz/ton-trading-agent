import { loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

const proxyTarget = process.env.VITE_BACKEND_PROXY_TARGET || 'http://localhost:8080'

const config = {
  plugins: [vue()],
  define: {
    global: 'window',
  },
  server: {
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false,
        rewrite: (p: string) => p.replace(/^\/api/, ''),
      },
      '/ws': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false,
        ws: true,
      }
    }
  },
  build: {
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
      }
    }
  }
} as any

export default config
