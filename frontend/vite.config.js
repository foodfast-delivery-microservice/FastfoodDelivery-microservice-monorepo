import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      'firebase/firestore': path.resolve(__dirname, './src/shims/firestore.js'),
    },
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
