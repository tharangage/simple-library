import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthProvider } from 'react-oidc-context'
import { RouterProvider } from 'react-router'
import { router } from './app/router.jsx'
import { oidcConfig } from './auth/oidcConfig.js'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Retry only when retrying can help: network errors and 5xx, at most twice.
      // A 401/403/404 will give the same answer every time.
      retry: (failureCount, error) => failureCount < 2 && (error.status === 0 || error.status >= 500),
      refetchOnWindowFocus: false,
    },
  },
})

/**
 * Keycloak sends the browser back to "/auth/callback?code=...&state=...". After the tokens are fetched,
 * go to the page the user originally asked for and remove the code from the address bar.
 */
function onSigninCallback(user) {
  router.navigate(user?.state?.returnTo ?? '/books', { replace: true })
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AuthProvider {...oidcConfig} onSigninCallback={onSigninCallback}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </AuthProvider>
  </StrictMode>,
)
