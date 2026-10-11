import { useCurrentUser } from './useCurrentUser.js'

/**
 * Whether the signed-in user has the `admin` role, read from the `roles` of GET /apis/v1/me.
 * This only decides what the web app shows; the backend still refuses admin actions from anyone else (403).
 * @returns {{ isAdmin: boolean, isLoading: boolean, isError: boolean }}
 */
export function useIsAdmin() {
  const { data, isPending, isError } = useCurrentUser()
  return { isAdmin: data?.roles?.includes('admin') ?? false, isLoading: isPending, isError }
}
