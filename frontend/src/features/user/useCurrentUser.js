import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useAuth } from 'react-oidc-context'
import { fetchMyBorrower } from '../../api/borrowers.js'

/**
 * The signed-in user's Borrower and item counts (GET /apis/v1/borrowers/me), loaded once after login and
 * cached under ['currentUser', sub]. The first call also makes the backend create the Borrower (US-02.4).
 * Read `data.id`, `data.borrowedCount`, `data.maxItems` and `data.remainingSlots` from any page.
 * An admin without the `user` role gets 403: `data` stays undefined and pages show no borrower features.
 *
 * @param {{ log?: boolean }} [options]  `log: true` (used once, in Layout) prints the result to the browser
 *   console in development builds. Other callers leave it off so the line is not repeated.
 */
export function useCurrentUser({ log = false } = {}) {
  const user = useAuth().user
  const token = user?.access_token
  const sub = user?.profile?.sub
  const query = useQuery({
    // The user's id is part of the key, so another user never sees (or skips the provisioning for) a cached answer.
    queryKey: ['currentUser', sub],
    queryFn: () => fetchMyBorrower({ token }),
    enabled: Boolean(token),
    // Fresh until something (like a borrow) invalidates it.
    staleTime: Infinity,
  })

  const { data, error } = query
  useEffect(() => {
    // Development aid only: the response holds personal data, so production builds do not print it.
    if (log && data && import.meta.env.DEV) console.log('[library] GET /apis/v1/borrowers/me', data)
  }, [log, data])
  useEffect(() => {
    if (log && error) console.warn(`[library] GET /apis/v1/borrowers/me failed (${error.status}): ${error.message}`)
  }, [log, error])

  return query
}
