import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useAuth } from 'react-oidc-context'
import { fetchBorrowedBooks } from '../../api/borrowedBooks.js'

/** Loads one page of borrowed books, filtered by the search text. */
export function useBorrowedBooks({ page, size, q }) {
  const token = useAuth().user?.access_token
  return useQuery({
    queryKey: ['borrowedBooks', { page, size, q }],
    queryFn: () => fetchBorrowedBooks({ token, page, size, q }),
    enabled: Boolean(token),
    placeholderData: keepPreviousData,
  })
}
