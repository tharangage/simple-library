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
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{js,jsx}'],
      // Wiring only (no logic of its own), or test helpers.
      exclude: ['src/main.jsx', 'src/app/router.jsx', 'src/auth/oidcConfig.js', 'src/test/**', 'src/**/*.test.{js,jsx}'],
      reporter: ['text-summary', 'html', 'json-summary', 'lcov'],
      // `npm run test:coverage` fails if coverage drops below these. Raise them as tests grow; never lower.
      thresholds: { lines: 80, statements: 80, functions: 80, branches: 75 },
    },
  },
})
