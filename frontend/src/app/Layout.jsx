import { useAuth } from 'react-oidc-context'
import { Link, NavLink, Outlet } from 'react-router'

/** The frame around every page: header with navigation and the login/logout button. */
export function Layout() {
  const auth = useAuth()
  const profile = auth.user?.profile
  const displayName = profile?.given_name || profile?.preferred_username

  return (
    <div className="app">
      <header className="topbar">
        <Link to="/" className="brand">
          Simple Library
        </Link>
        {auth.isAuthenticated && (
          <nav aria-label="Main">
            <NavLink to="/books">Books</NavLink>
          </nav>
        )}
        <div className="topbar__user">
          {auth.isAuthenticated ? (
            <>
              <span className="muted">Hi, {displayName}</span>
              <button type="button" onClick={() => auth.signoutRedirect()}>
                Log out
              </button>
            </>
          ) : (
            !auth.isLoading && (
              <>
                <Link to="/register">Register</Link>
                <button type="button" className="button--primary" onClick={() => auth.signinRedirect()}>
                  Log in
                </button>
              </>
            )
          )}
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
