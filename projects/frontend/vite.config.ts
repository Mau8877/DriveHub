/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { tanstackRouter } from '@tanstack/router-plugin/vite'
import { fileURLToPath } from 'url'
import { dirname, resolve } from 'path'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

export default defineConfig({
  // tanstackRouter() va antes que react() -- genera routeTree.gen.ts a partir
  // de src/routes/ y el plugin de React necesita verlo ya generado.
  plugins: [
    tanstackRouter({
      target: 'react',
      autoCodeSplitting: true,
      routesDirectory: './src/routes',
      generatedRouteTree: './src/routeTree.gen.ts',
    }),
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
  },
  server: {
    host: true,
    port: Number(process.env.PORT) || 5173,
    watch: {
      usePolling: true,
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.ts',
    // * Node 25+ trae un `localStorage` experimental que tapa el de jsdom y rompe los tests que lo usan;
    // * se apaga para que la suite pase igual en cualquier versión de Node.
    execArgv: ['--no-experimental-webstorage'],
  },
})
