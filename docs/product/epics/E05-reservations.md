# E05 – Reservations

**Goal:** When a book (copy) is borrowed, a user can join a queue for it. On return, the book is held for the first person in the queue.
**Requirement refs:** Frontend #4
**Decisions:** D-02 (stories assume per-copy reservation) · **Assumptions:** A-05, A-06, A-07

Reservation states: `WAITING` → `READY` (book returned, held for this user until `expiresAt`) → `FULFILLED` (user borrowed it) · or `CANCELLED` (by user) / `EXPIRED` (hold period passed).

### US-05.1 Reserve a borrowed book (BE · M)
As a user, I want to reserve a borrowed book, so that I'm next in line when it comes back.
- New `Reservation` entity: `id, book, borrower, status, createdDate, readyDate, expiresAt`, indexed on `(book, status)`.
- `POST /apis/v1/me/reservations {bookId}` → 201 with queue position.
- Rules (409 with codes):
  - Book is `AVAILABLE` → `BOOK_AVAILABLE_BORROW_INSTEAD`.
  - The user is borrowing this book → `ALREADY_BORROWED_BY_YOU`.
  - The user already has an active reservation on it → `ALREADY_RESERVED`.
  - Loans + active reservations ≥ 3 → `BORROW_LIMIT_REACHED` (same counter as US-04.1, same locking).
- The queue is FIFO by `createdDate`.

### US-05.2 Hand-over on return (BE · M)
As the next user in the queue, I want the returned book held for me, so that nobody else takes it.
- When a book is returned (US-06.1) and has `WAITING` reservations, the first becomes `READY` with `expiresAt = now + hold period` (default 3 days, `app.library.hold-days`). The book status becomes `ON_HOLD`.
- While `ON_HOLD`, only that user can borrow it (US-04.1). Others get `BOOK_NOT_AVAILABLE`.
- Return and hand-over happen in the same transaction.

### US-05.3 Expire uncollected holds (BE · M)
As the library, I want unclaimed holds to pass to the next person, so that books don't sit on the shelf.
- A scheduled job (default every 15 min, configurable) moves `READY` reservations past `expiresAt` to `EXPIRED`. The next `WAITING` one becomes `READY`, or the book becomes `AVAILABLE`.
- Idempotent and safe with more than one app instance (ShedLock or `FOR UPDATE SKIP LOCKED`). Tested with a fixed `Clock`.

### US-05.4 Cancel a reservation (BE + FE · S)
As a user, I want to cancel a reservation I no longer need, so that I free a slot.
- `DELETE /apis/v1/me/reservations/{id}` → 204. Only the owner can cancel. Someone else's reservation → 404, so its existence isn't revealed.
- Cancelling a `READY` reservation hands the book to the next in queue (same logic as US-05.3).
- FE: *Cancel* button on "My books" with a confirmation.

### US-05.5 Reserve from the books page (FE · S)
As a user, I want a *Reserve* button on borrowed books, so that reserving is one click.
- Shown on `BORROWED` / `ON_HOLD` rows that aren't mine. Disabled at the limit (same tooltip as US-04.2).
- If another copy with the same ISBN is `AVAILABLE`, the dialog suggests borrowing that copy instead.
- After reserving: toast "You are #n in the queue", then the list refreshes.
