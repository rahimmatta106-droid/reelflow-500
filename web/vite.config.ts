import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  // GitHub Pages project site: https://rahimmatta106-droid.github.io/reelflow-500/
  base: '/reelflow-500/',
  plugins: [react()]
})
