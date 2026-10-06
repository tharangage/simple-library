import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Keycloak's redirect URI and the backend's CORS list both name http://localhost:5173,
    // so fail loudly instead of silently moving to 5174 when the port is busy.
    port: 5173,
    strictPort: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    // Tests must not depend on a developer's .env files.
    env: {
      VITE_API_BASE_URL: 'http://api.test',
      VITE_KEYCLOAK_URL: 'http://keycloak.test',
      VITE_KEYCLOAK_REALM: 'library',
      VITE_KEYCLOAK_CLIENT_ID: 'library-web',
    },
  },
})
