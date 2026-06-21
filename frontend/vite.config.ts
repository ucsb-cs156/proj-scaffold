import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 3000,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'build',
  },
  resolve: {
    alias: {
      main: path.resolve(__dirname, './src/main'),
      fixtures: path.resolve(__dirname, './src/fixtures'),
      tests: path.resolve(__dirname, './src/tests'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './vitest.setup.js',
    include: ['src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
    coverage: {
      enabled: true,
      provider: 'v8',
      include: ['src/main/**'],
      thresholds: {
        lines: 7.1,
        statements: 6.9,
        branches: 2.7,
        functions: 6.8,
      },
      reporter: ['html', 'text-summary'],
    },
  },
})
