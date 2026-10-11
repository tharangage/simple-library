# E03 – Book catalogue & availability

**Jira:** [SCRUM-7](https://tharangage.atlassian.net/browse/SCRUM-7)

**Goal:** Logged-in users see every book (copy) with its current status. Admins can add books.
**Requirement refs:** General actions #2, #3 · Frontend #2 · ISBN rules
**Assumptions:** A-03, A-04, A-10

### US-03.1 Books page (FE · M) · [SCRUM-25](https://tharangage.atlassian.net/browse/SCRUM-25)
As a user, I want to see all books with their status, so that I know what I can borrow or reserve.
- `/books` (default page after login): a table/list showing title, author, ISBN and status.
- Status badges: **Available** · **Borrowed** · **On hold** (held for someone's reservation) · **Borrowed by you** · **Reserved by you** · **Ready for you**.
- Server-side paging (page size 20) and sort by title/author. Paging and sort are kept in the URL query.
- Loading, empty ("No books yet") and error states. Responsive on mobile widths.
- Action buttons per row (Borrow / Reserve) appear here once E04/E05 are done.

### US-03.2 Availability details in the book list API (BE · M) · [SCRUM-26](https://tharangage.atlassian.net/browse/SCRUM-26)
As the web app, I want each book to come with its status relative to the caller, so that the UI doesn't need extra calls.
- `GET /apis/v1/books` items: `id, isbn, title, author, status (AVAILABLE|BORROWED|ON_HOLD), borrowedByMe, reservedByMe, readyForMe, queueLength`.
- Never exposes other borrowers' ids, names or emails (A-10, GDPR).
- No N+1 queries: status is computed in one query, or two at most, per page (covered by a test).

### US-03.3 Search and filter (BE + FE · M · nice-to-have) · [SCRUM-27](https://tharangage.atlassian.net/browse/SCRUM-27)
As a user, I want to search by title, author or ISBN and filter to "available only", so that I can find a book quickly.
- `GET /apis/v1/books?q=…&available=true`: case-insensitive *contains* on title/author, exact match on ISBN.
- Search box is debounced (300 ms). The query is kept in the URL.

### US-03.4 Admin: register a book (FE · S) · [SCRUM-28](https://tharangage.atlassian.net/browse/SCRUM-28) – *implemented, see "Implementation notes" below*
As an admin, I want to add a book copy from the web app, so that I don't need Swagger for daily work.
- "Add book" form (ISBN, title, author), visible only to `admin`.
- 409 for an ISBN with a different title/author shows the API message next to the fields.
- Option to "add another copy" of the book just created.

### US-03.5 Close ISBN first-registration race (BE · S) · [SCRUM-29](https://tharangage.atlassian.net/browse/SCRUM-29)
As the library, I want two concurrent registrations of a new ISBN to keep the title/author rule.
- Option: an `isbn_title` table with ISBN as primary key holding title/author (or a pessimistic lock on ISBN). Concurrency test proves only one variant survives.
- Removes the known issue from `.claude/CLAUDE.md`.

## Implementation notes
**US-03.4 Admin: register a book** (branch `feature/US-03.4-admin-add-book`)
- Backend: `POST /apis/v1/books` now needs the role `admin` (403 for a user or a token without roles, 401 without a token). `GET /apis/v1/books` is unchanged (any valid token). Before this change any logged-in user could register books.
- `GET /apis/v1/me` returns `roles` (`["user", "admin"]` as far as the caller has them; other Keycloak roles are left out), so the web app can show admin features. The server still enforces every role rule: hiding a link is only courtesy.
- Web app: page `/books/new` (inside `RequireAuth` and `RequireAdmin`; others see "You don't have access"). Fields ISBN, title, author with the same rules as the API, inline errors linked with `aria-describedby`, submit disabled while sending, field errors of a 400 next to their fields, the API message of a 409 ("Title and Author should be same for same ISBN") in an alert above the form.
- After a success the book list is reloaded and a status line offers *Add another copy* (sends the same values again, counting copies in a row), *Add a different book* (clears the form) and a link back to the books.
- An *Add book* link is shown to admins in the header navigation and on the Books page.
