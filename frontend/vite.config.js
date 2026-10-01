import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export default defineConfig({
  base: process.env.VITE_DEMO_MODE === 'true'
    ? '/FastfoodDelivery-microservice-monorepo/'
    : '/',
  plugins: [react()],
  resolve: {
    alias: {},
  },
  server: {
    fs: {
      allow: [
        '..',
        path.resolve(__dirname, './shared'),
      ],
    },
    proxy: {
      '/osrm': {
        target: 'https://router.project-osrm.org',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/osrm/, ''),
      },
    },
  },
  optimizeDeps: {
    include: ['leaflet', 'leaflet-routing-machine'],
  },
})
