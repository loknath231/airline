import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The dev server proxies /api to the Spring Boot backend, so no CORS setup is needed.
// Override the target with:  BACKEND_URL=http://host:8080 npm run dev
const target = process.env.BACKEND_URL || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { '/api': { target, changeOrigin: true } } },
  preview: { port: 5173, proxy: { '/api': { target, changeOrigin: true } } },
})
