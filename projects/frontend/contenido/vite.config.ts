/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { tanstackRouter } from '@tanstack/router-plugin/vite'
import { federation } from '@module-federation/vite'
import { fileURLToPath } from 'url'
import { dirname, resolve } from 'path'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

const PUERTO = 4205

export default defineConfig({
  plugins: [
    // tanstackRouter() va antes que react(): genera routeTree.gen.ts a partir
    // de src/routes/ y el plugin de React necesita verlo ya generado.
    tanstackRouter({
      target: 'react',
      autoCodeSplitting: true,
      routesDirectory: './src/routes',
      generatedRouteTree: './src/routeTree.gen.ts',
    }),
    react(),
    tailwindcss(),
    // * Remoto de Module Federation: expone './mount' (mount/unmount, ver
    // * @drivehub/mf-contract). El shell es Angular, así que no se comparte
    // * React con él: el remoto lleva su propio React dentro del bundle.
    federation({
      name: 'contenido',
      filename: 'remoteEntry.js',
      exposes: {
        './mount': './src/mount.ts',
      },
      shared: {},
      dts: false,
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
  },
  server: {
    host: true,
    port: PUERTO,
    strictPort: true,
    // * URLs absolutas a este dev server: el código del remoto se ejecuta
    // * dentro del shell (otro origen) y tiene que seguir pidiendo sus chunks acá.
    origin: `http://localhost:${PUERTO}`,
    watch: {
      usePolling: true,
    },
  },
  build: {
    target: 'esnext',
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
