# E06 – Returns at the library desk (admin)

**Jira:** [SCRUM-10](https://tharangage.atlassian.net/browse/SCRUM-10)

**Goal:** When a book is handed back physically, an admin marks it as returned. Users can't return books themselves.
**Requirement refs:** Admin users #1 · Borrower actions #2
**Assumptions:** A-02 · **Note:** this replaces the current rule "only the borrower who has it can return it".

### US-06.1 Admin return API (BE · M) · [SCRUM-38](https://tharangage.atlassian.net/browse/SCRUM-38)
As an admin, I want to mark a borrowed book as returned, so that it becomes available, or passes to the next reservation.
- `DELETE /apis/v1/loans/{bookId}` (role `admin`) → 204. Book not borrowed → 409 `BOOK_NOT_BORROWED`. Unknown book → 404.
- Clears `borrowedBy` / `borrowedDate` and triggers the hand-over (US-05.2) in the same transaction.
- `GET /apis/v1/loans?q=…` (role `admin`): current loans with book title/ISBN/id, borrower name and email, and borrowed date. Paged. This is the only place borrower identities are shown, and only to admins.
- Users calling either endpoint → 403. Tests updated for the changed business rule.

### US-06.2 Returns page (FE · M) · [SCRUM-39](https://tharangage.atlassian.net/browse/SCRUM-39)
As an admin, I want a returns screen at the desk, so that I can find the loan quickly and close it.
- `/admin/returns`: search by book id, ISBN, title or borrower email. The list shows current loans.
- *Mark returned* → confirmation dialog (title + borrower name) → success toast. If a reservation was waiting, the toast also says "Now on hold for the next reservation".
- Works with a barcode scanner that types the book id and presses Enter (focus starts in the search box).

### US-06.3 Role-aware navigation and guards (FE · S) · [SCRUM-40](https://tharangage.atlassian.net/browse/SCRUM-40)
As a user or admin, I want to see only the screens I may use.
- Nav for `user`: Books, My books. Nav for `admin`: Books, Returns, Add book.
- Opening an admin URL without the role shows a "You don't have access" page. The API still enforces roles, since UI hiding is not security.
- Roles are read from the ID/access token `realm_access.roles`.
