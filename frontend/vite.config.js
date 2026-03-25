import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Proxy all backend paths through Vite so cookies stay on the same origin (:5173)
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/login/oauth2': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
