import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useAuth } from 'react-oidc-context'
import { fetchBooks } from '../../api/books.js'

/**
 * Loads one page of books with TanStack Query.
 * The query key lists everything the result depends on; when page or sort changes,
 * the key changes and a new request is made. Pages already loaded are cached.
 */
export function useBooks({ page, size, sort }) {
  const token = useAuth().user?.access_token
  return useQuery({
    queryKey: ['books', { page, size, sort }],
    queryFn: () => fetchBooks({ token, page, size, sort }),
    enabled: Boolean(token),
    // Keep showing the current page while the next one loads, instead of flashing "Loading…".
    placeholderData: keepPreviousData,
  })
}
