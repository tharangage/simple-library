# E04 – Borrowing (limit 3) & "My books"

**Jira:** [SCRUM-8](https://tharangage.atlassian.net/browse/SCRUM-8)

**Goal:** Users borrow available books themselves, up to 3 items at a time (loans + reservations), and can see what they have.
**Requirement refs:** Borrower actions #1 · Requirement #8 · Frontend #3
**Assumptions:** A-05, A-07

### US-04.1 Borrow with a limit of 3 (BE · M) · [SCRUM-30](https://tharangage.atlassian.net/browse/SCRUM-30) – *implemented, with the changes listed under "Implementation notes"*
As a user, I want to borrow an available book, so that I can take it home. The library wants no more than 3 items per user.
- `POST /apis/v1/me/loans {bookId}` → 201 with the loan (`bookId, title, borrowedDate`).
- Rules (each with its own error `code`, US-08.2):
  - Book not found → 404 `BOOK_NOT_FOUND`.
  - Book borrowed, or on hold for someone else → 409 `BOOK_NOT_AVAILABLE`.
  - Active loans + reservations ≥ 3 → 409 `BORROW_LIMIT_REACHED`.
  - Book `READY` for this user → allowed, and the reservation becomes `FULFILLED` (E05). Loan count stays ≤ 3 because the reservation turns into the loan.
- Concurrency: the borrower row is locked (`SELECT … FOR UPDATE`) together with the existing book lock, so two parallel borrows can't make it 4. Integration test with parallel requests.
- Limit value is configurable (`app.library.max-items-per-borrower: 3`).

### US-04.2 Borrow from the books page (FE · M) · [SCRUM-31](https://tharangage.atlassian.net/browse/SCRUM-31) – *implemented, with the changes listed under "Implementation notes"*
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

## Implementation notes
Decisions taken while implementing US-04.1 and US-04.2 (branch `feature/US-04.1-04.2-borrow-from-books-page`). They replace the matching wording in the stories above.

**API (US-04.1)**
- The existing `POST /apis/v1/borrowers/{borrower-id}/books {bookId}` is kept; there is **no** `POST /apis/v1/me/loans`. It answers 200 with no body (not 201 with the loan).
- Who may call it (SCRUM-62): an `admin` may borrow for any existing borrower (library desk); everybody else only for their own Borrower, otherwise 403. A token with both roles counts as admin for this check.
- Limit: `app.library.max-items-per-borrower` (default 3). It counts the books a borrower holds; reservations are added with E05.
- Errors are plain-text bodies until US-08.2: 409 `BOOK_NOT_AVAILABLE` (book already borrowed), 409 `BORROW_LIMIT_REACHED`, 404 `Borrower not found` / `Book not found`. The book check comes before the limit check.
- Concurrency: the borrower row and then the book row are locked (`SELECT ... FOR UPDATE`). An integration test sends 8 parallel borrows for one borrower and expects exactly 3 to succeed. Return only locks the book, so there is no deadlock.
- `app.library.max-items-per-borrower` must be at least 1; a smaller value stops the application at startup. Both row locks wait at most 3 seconds (`jakarta.persistence.lock.timeout`), then the request fails with the lock exception, answered as 409.
- `GET /apis/v1/borrowers/me` also returns `borrowedCount`, `maxItems` and `remainingSlots`. The "x of 3 used" figure comes from there, not from `GET /apis/v1/me` (which is the generic token profile, see ADR-001).

**Web app (US-04.2)**
- The Books page shows a *Borrow* button on `AVAILABLE` rows, only for a user who has a Borrower (an admin without role `user` gets none). A confirmation dialog shows the title, the author and the places left.
- Success: a toast, then the book list and the user's counts are loaded again. Failure: a toast with a readable message (`BORROW_LIMIT_REACHED`, `BOOK_NOT_AVAILABLE`, 404, 403, 401, network); after `BOOK_NOT_AVAILABLE` the list is reloaded as well.
- At the limit the button stays visible but is disabled (`aria-disabled`), with the tooltip and screen-reader text "You have reached the limit of 3 books." (the number comes from the server).
- The header shows "x of 3 used" once the Borrower is loaded.
- After a successful borrow focus moves to the page heading (the row lost its button). The toast messages go into two live regions that are always in the page, so screen readers announce them. The counts are cached for 60 seconds and reload after a borrow.

**Not done yet**
- "Ready for you" rows and the reservation rules wait for E05; the refresh of "My books" waits for US-04.3.
- The limit message says "limit of N books", because only loans count today. Change the wording when reservations count too.
