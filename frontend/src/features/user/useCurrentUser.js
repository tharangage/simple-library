import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useAuth } from 'react-oidc-context'
import { fetchMe } from '../../api/me.js'

/**
 * The signed-in user's profile (GET /apis/v1/me), loaded once after login and cached under ['currentUser', sub].
 * Same shape for every user: `data.userId`, `data.firstname`, `data.lastname`, `data.email`, `data.mobile` and `data.borrower`.
 * `data.borrower` is the library membership (`id`, `borrowedCount`, `maxItems`, `remainingSlots`); the first
 * call makes the backend create it for a user with the role `user` (US-02.4). It is null for someone who is not a
 * member (a desk-only admin), so pages check `data?.borrower` before showing borrower features.
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
    queryFn: () => fetchMe({ token }),
    enabled: Boolean(token),
    // Fresh for a minute, or until something (like a borrow) invalidates it. An admin can return a book
    // meanwhile, so the counts are asked for again when a page that shows them opens later.
    staleTime: 60 * 1000,
  })

  const { data, error } = query
  useEffect(() => {
    // Development aid only: the response holds personal data, so production builds do not print it.
    if (log && data && import.meta.env.DEV) console.log('[library] GET /apis/v1/me', data)
  }, [log, data])
  useEffect(() => {
    if (log && error) console.warn(`[library] GET /apis/v1/me failed (${error.status}): ${error.message}`)
  }, [log, error])

  return query
}
