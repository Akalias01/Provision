import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    // Remove crossorigin attributes for Electron compatibility
    {
      name: 'remove-crossorigin',
      transformIndexHtml(html) {
        return html.replace(/ crossorigin/g, '');
      },
    },
  ],
  base: './', // Use relative paths for Electron compatibility
  build: {
    // Don't use module preload - causes issues with file:// protocol
    modulePreload: false,
  },
})
