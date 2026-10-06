import { useEffect } from 'react'
import { useAuth } from 'react-oidc-context'
import { useLocation } from 'react-router'

/**
 * Wrap pages that need a logged-in user. If nobody is logged in, send the browser to
 * Keycloak and remember where to come back to.
 */
export function RequireAuth({ children }) {
  const auth = useAuth()
  const location = useLocation()
  const mustLogIn = !auth.isLoading && !auth.isAuthenticated && !auth.activeNavigator && !auth.error

  useEffect(() => {
    if (mustLogIn) {
      auth.signinRedirect({ state: { returnTo: location.pathname + location.search } })
    }
  }, [mustLogIn, auth, location])

  if (auth.error) {
    return (
      <p role="alert" className="message message--error">
        Login failed: {auth.error.message}
      </p>
    )
  }
  if (!auth.isAuthenticated) {
    return <p className="message">Redirecting to login…</p>
  }
  return children
}
