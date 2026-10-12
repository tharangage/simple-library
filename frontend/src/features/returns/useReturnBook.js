import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from 'react-oidc-context'
import { returnBook } from '../../api/borrowedBooks.js'
import { useToast } from '../../components/useToast.js'
import { returnErrorMessage } from './returnErrors.js'

/**
 * Mark a borrowed book as returned. Call `mutate(borrowedBook)` with one item of the borrowed-books list.
 * Afterwards (success or not) the borrowed books, the book list and the user's counts are loaded again.
 */
export function useReturnBook() {
  const token = useAuth().user?.access_token
  const queryClient = useQueryClient()
  const toast = useToast()

  return useMutation({
    mutationFn: (borrowedBook) => returnBook({ token, borrowerId: borrowedBook.borrowerId, bookId: borrowedBook.bookId }),
    onSuccess: (_result, borrowedBook) => toast.success(`“${borrowedBook.title}” is back on the shelf.`),
    onError: (error) => toast.error(returnErrorMessage(error)),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['borrowedBooks'] }),
        queryClient.invalidateQueries({ queryKey: ['books'] }),
        queryClient.invalidateQueries({ queryKey: ['currentUser'] }),
      ]),
  })
}
