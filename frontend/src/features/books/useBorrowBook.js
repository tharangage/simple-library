import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from 'react-oidc-context'
import { borrowBook } from '../../api/borrowers.js'
import { useToast } from '../../components/useToast.js'
import { borrowErrorMessage } from './borrowErrors.js'

/**
 * Borrow one book. Call `mutate({ borrowerId, book, maxItems })`.
 * Afterwards (success or not) the book list and the user's counts are loaded again, so the page matches the
 * server. "My books" (US-04.3) will refresh from here too once it exists.
 */
export function useBorrowBook() {
  const token = useAuth().user?.access_token
  const queryClient = useQueryClient()
  const toast = useToast()

  return useMutation({
    mutationFn: ({ borrowerId, book }) => borrowBook({ token, borrowerId, bookId: book.id }),
    onSuccess: (_result, { book }) => toast.success(`“${book.title}” is yours. Enjoy reading!`),
    onError: (error, { maxItems }) => toast.error(borrowErrorMessage(error, maxItems)),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['books'] }),
        queryClient.invalidateQueries({ queryKey: ['currentUser'] }),
      ]),
  })
}
