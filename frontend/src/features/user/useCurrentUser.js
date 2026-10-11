import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useAuth } from 'react-oidc-context'
import { fetchMyBorrower } from '../../api/borrowers.js'

/**
 * Loads the signed-in user (today: their Borrower from GET /apis/v1/borrowers/me) once after login. The first call also makes the backend
 * create the Borrower (US-02.4). For now the result is only logged to the browser console for
 * development; later pages can read it with this hook (it is cached under one query key).
 * An admin without the `user` role gets 403, which is only logged: nothing in the UI depends on it yet.
 */
export function useCurrentUser() {
  const user = useAuth().user
  const token = user?.access_token
  const sub = user?.profile?.sub
  const query = useQuery({
    // The user's id is part of the key, so another user never sees (or skips the provisioning for) a cached answer.
    queryKey: ['currentUser', sub],
    queryFn: () => fetchMyBorrower({ token }),
    enabled: Boolean(token),
    staleTime: Infinity,
  })

  const { data, error } = query
  useEffect(() => {
    // Development aid only: the response holds personal data, so production builds do not print it.
    if (data && import.meta.env.DEV) console.log('[library] GET /apis/v1/borrowers/me', data)
  }, [data])
  useEffect(() => {
    if (error) console.warn(`[library] GET /apis/v1/borrowers/me failed (${error.status}): ${error.message}`)
  }, [error])

  return query
}
