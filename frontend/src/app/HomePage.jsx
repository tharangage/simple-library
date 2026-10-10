import { useAuth } from 'react-oidc-context'
import { Link, Navigate } from 'react-router'

export function HomePage() {
  const auth = useAuth()

  if (auth.isLoading) return <p className="message">Loading…</p>
  if (auth.isAuthenticated) return <Navigate to="/books" replace />

  return (
    <section className="welcome">
      <h1>Welcome to Simple Library</h1>
      <p>Browse the catalogue, borrow books and reserve the ones that are out.</p>
      {auth.error && (
        <p role="alert" className="message message--error">
          Login failed: {auth.error.message}
        </p>
      )}
      <button type="button" className="button--primary" onClick={() => auth.signinRedirect()}>
        Log in
      </button>
      <p className="muted">
        New here? <Link to="/register">Create an account</Link>
      </p>
    </section>
  )
}
