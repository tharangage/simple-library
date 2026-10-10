# E04 – Borrowing (limit 3) & "My books"

**Jira:** [SCRUM-8](https://tharangage.atlassian.net/browse/SCRUM-8)

**Goal:** Users borrow available books themselves, up to 3 items at a time (loans + reservations), and can see what they have.
**Requirement refs:** Borrower actions #1 · Requirement #8 · Frontend #3
**Assumptions:** A-05, A-07

### US-04.1 Borrow with a limit of 3 (BE · M) · [SCRUM-30](https://tharangage.atlassian.net/browse/SCRUM-30)
As a user, I want to borrow an available book, so that I can take it home. The library wants no more than 3 items per user.
- `POST /apis/v1/me/loans {bookId}` → 201 with the loan (`bookId, title, borrowedDate`).
- Rules (each with its own error `code`, US-08.2):
  - Book not found → 404 `BOOK_NOT_FOUND`.
  - Book borrowed, or on hold for someone else → 409 `BOOK_NOT_AVAILABLE`.
  - Active loans + reservations ≥ 3 → 409 `BORROW_LIMIT_REACHED`.
  - Book `READY` for this user → allowed, and the reservation becomes `FULFILLED` (E05). Loan count stays ≤ 3 because the reservation turns into the loan.
- Concurrency: the borrower row is locked (`SELECT … FOR UPDATE`) together with the existing book lock, so two parallel borrows can't make it 4. Integration test with parallel requests.
- Limit value is configurable (`app.library.max-items-per-borrower: 3`).

### US-04.2 Borrow from the books page (FE · M) · [SCRUM-31](https://tharangage.atlassian.net/browse/SCRUM-31)
As a user, I want a *Borrow* button on available books, so that borrowing is one click.
- Button only on `AVAILABLE` (or `Ready for you`) rows. Confirmation dialog shows the title and how many slots remain.
- On success: toast message, then refresh of the list and of "My books".
- When the user has 3 items, the buttons are disabled with a tooltip "You have reached the limit of 3 books (loans + reservations)".
- API errors (409 codes) are shown as clear messages, not raw text.
- The header shows "x of 3 used", from `GET /apis/v1/me`.

### US-04.3 "My books" page (BE + FE · M) · [SCRUM-32](https://tharangage.atlassian.net/browse/SCRUM-32)
As a user, I want to see what I've borrowed and reserved, so that I know what to return or pick up.
- BE: `GET /apis/v1/me/loans` (title, author, ISBN, book id, borrowed date). `GET /apis/v1/me/reservations` (E05).
- FE: `/my-books` with two sections. **On loan**: borrowed date, and a note that returns happen at the library desk. **Reservations**: queue position, or "Ready – pick up by <date>".
- Empty states with a link to the catalogue.
