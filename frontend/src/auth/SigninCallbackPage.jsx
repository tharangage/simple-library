import { useAuth } from 'react-oidc-context'

/**
 * Keycloak sends the browser back here with "?code=...&state=...". AuthProvider swaps the code
 * for tokens and then calls onSigninCallback (main.jsx), which navigates to the page the user
 * originally asked for. This page only has to show something in the meantime.
 *
 * A separate route keeps other pages (like HomePage, which redirects logged-in users to /books)
 * from reacting to the login and racing that navigation.
 */
export function SigninCallbackPage() {
  const auth = useAuth()
  if (auth.error) {
    return (
      <p role="alert" className="message message--error">
        Login failed: {auth.error.message}
      </p>
    )
  }
  return <p className="message">Signing you in…</p>
}
