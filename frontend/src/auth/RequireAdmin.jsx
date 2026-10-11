import { useIsAdmin } from '../features/user/useIsAdmin.js'

/**
 * Wrap pages only admins may use. Put it inside <RequireAuth>: it assumes the user is logged in.
 * Others see a short explanation instead of the page. The API refuses them as well, this is only courtesy.
 */
export function RequireAdmin({ children }) {
  const { isAdmin, isLoading, isError } = useIsAdmin()

  if (isLoading) {
    return (
      <p role="status" className="message">
        Checking your access…
      </p>
    )
  }
  if (isError) {
    return (
      <p role="alert" className="message message--error">
        Could not check your access. Please reload the page.
      </p>
    )
  }
  if (!isAdmin) {
    return (
      <p role="alert" className="message message--error">
        You don't have access to this page. It is for library administrators.
      </p>
    )
  }
  return children
}
