# E06 – Returns at the library desk (admin)

**Jira:** [SCRUM-10](https://tharangage.atlassian.net/browse/SCRUM-10)

**Goal:** When a book is handed back physically, an admin marks it as returned. Users can't return books themselves.
**Requirement refs:** Admin users #1 · Borrower actions #2
**Assumptions:** A-02 · **Note:** this replaces the current rule "only the borrower who has it can return it".

### US-06.1 Admin return API (BE · M) · [SCRUM-38](https://tharangage.atlassian.net/browse/SCRUM-38)
As an admin, I want to mark a borrowed book as returned, so that it becomes available, or passes to the next reservation.
- `DELETE /apis/v1/borrowed-books/{book-id}/borrowers/{borrower-id}` (role `admin`) → 200, empty body. Book not borrowed by that borrower → 409. Unknown book → 404. (Decision: the existing return call is kept and moved to the new path, instead of a new `DELETE .../{bookId}` that answers 204 with `BOOK_NOT_BORROWED`.)
- Clears `borrowedBy` / `borrowedDate`. The hand-over to the next reservation (US-05.2) is **not** part of this story yet, because reservations (E05) do not exist.
- `GET /apis/v1/borrowed-books?q=…` (role `admin`): borrowed books with book title/ISBN/id, borrower id, name and email, and borrowed date. Paged (default 5, newest first). This is the only place borrower identities are shown, and only to admins.
- Users calling either endpoint → 403. Tests updated for the changed business rule.

### US-06.2 Returns page (FE · M) · [SCRUM-39](https://tharangage.atlassian.net/browse/SCRUM-39)
As an admin, I want a returns screen at the desk, so that I can find the loan quickly and close it.
- `/admin/returns`: search by book id, ISBN, title or borrower email. The list shows current loans.
- *Mark returned* → confirmation dialog (title + borrower name) → success toast. (Once E05 exists: if a reservation was waiting, the toast also says "Now on hold for the next reservation".)
- Works with a barcode scanner that types the book id and presses Enter (focus starts in the search box).

### US-06.3 Role-aware navigation and guards (FE · S) · [SCRUM-40](https://tharangage.atlassian.net/browse/SCRUM-40)
As a user or admin, I want to see only the screens I may use.
- Nav for `user`: Books, My books. Nav for `admin`: Books, Returns, Add book.
- Opening an admin URL without the role shows a "You don't have access" page. The API still enforces roles, since UI hiding is not security.
- Roles are read from the ID/access token `realm_access.roles`.

## Implementation notes
**US-06.1 + US-06.2 Admin returns** (branch `feature/US-06-admin-returns`, PR #22)
- Backend: `GET /apis/v1/borrowed-books?q=` (admin) is served by `BorrowedBookController` via `BorrowedBookService.listBorrowedBooks` and a JPQL projection `BookRepository.findBorrowedBooks` (`BorrowedBookResponse`). `q` is matched case-insensitively against book id, ISBN, title and borrower email; `%` and `_` in `q` are escaped.
- API paths renamed (breaking; the old `/apis/v1/borrowers/{id}/books...` paths are gone): borrow is `POST /apis/v1/borrowed-books/{book-id}/borrowers/{borrower-id}` (user for their own borrower, or admin; no request body, so `BorrowBookRequest` was removed) and return is `DELETE` on the same path (admin only). `BookBorrowerController` was merged into `BorrowedBookController`; `BookBorrowerService` became `BorrowedBookService`.
- `BorrowerProvisioningInterceptor` now acts on `POST` only, because borrow and return share one path; a return never creates a Borrower for the caller.
- `SecurityConfig`: the borrow and return rules come before the catch-all `/apis/v1/borrowed-books/**` (admin) rule. The first matching rule wins.
- Web app: page `/admin/returns` (inside `RequireAuth` and `RequireAdmin`), "Returns" link in the header for admins. Search form with the search text and page kept in the URL; focus starts in the search box and returns there after a search or a return (barcode scanner). *Mark returned* → confirmation dialog → toast; a 409 explains that the book was already returned and the list reloads. API calls live in `api/borrowedBooks.js` (`fetchBorrowedBooks`, `borrowBook`, `returnBook`).
- Not done: hand-over to the next reservation (needs E05); the 204 / `BOOK_NOT_BORROWED` contract of the first draft.
- Tests: backend 153, front end 167 (all pass).
