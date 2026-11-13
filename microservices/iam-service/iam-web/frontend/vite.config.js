import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  // iam-service serves the built SPA from classpath:/static (index.html at http://localhost:9000/).
  base: '/',
  build: {
    // Output straight into the Spring module's static resources: index.html + assets/.
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  plugins: [react(), tailwindcss()],
  server: {
    // Dev only (npm run dev): http://localhost:5173 — also registered as a redirect URI.
    port: 5173,
    strictPort: true,
  },
})
