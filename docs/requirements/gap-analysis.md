# Gap analysis – requirement vs. current code (2026-10-06)

Source: [`simple-library-requirement-in-a-nutshell.md`](simple-library-requirement-in-a-nutshell.md).
"Current" is based on `main` as of 2026-10-06.

| # | Requirement | Current state | Gap → story |
|---|---|---|---|
| 1 | Register borrower / book, list books (REST) | Implemented (`/apis/v1/books`, `/apis/v1/borrowers`) | Book registration must become admin-only → US-01.3 |
| 2 | Borrow / return a book by book id | Implemented; only the borrower can return | Returns are **admin-only** now → US-06.1 (changes an existing business rule) |
| 3 | ISBN rules, multiple copies per ISBN | Implemented (409 on title/author mismatch) | Concurrent first-registration race (known issue) → US-03.5 |
| 4 | One borrower per book at a time | Implemented (pessimistic lock) | — |
| 5 | Keycloak realm **`library`**, OIDC for backend + frontend | Realm `library` used; Swagger client only; realm not versioned | `library-web` client + realm export → US-01.1, US-07.2 |
| 6 | Roles `user` / `admin`; every API except registration needs `user` | No role checks; every endpoint needs only a valid token | US-01.2, US-01.3 |
| 7 | Self-registration with valid email + mobile (format only) | Borrower has first/last name + email; no mobile; registration needs a token | US-02.1, US-02.2 |
| 8 | Borrow limit of 3 (loans + reservations) | No limit | US-04.1 |
| 9 | Reserve a borrowed book | Not implemented | E05 |
| 10 | Book list shows borrowed status | `borrowed` flag only | Add reservation/hold status + "mine" flags → US-03.2 |
| 11 | Swagger UI authenticated via realm `library` | Implemented | Show required role per operation → US-01.6 |
| 12 | Common error response for every API, custom exceptions | Plain-string bodies for 404/409/500; map for 400 | US-08.1, US-08.2 |
| 13 | OpenTelemetry logging/tracing, JVM-level, GDPR-safe | None | E09 |
| 14 | ReactJS + JavaScript (no TypeScript) web app | `frontend/` folder exists but is empty | E07 + FE stories in E02–E06 |
| 15 | Configurable environments / 12-factor | Profiles exist; default profile conflict (`local` vs `h2`) | D-06 |
| 16 | Java 25 (bonus) | Java 17 (project hard rule) | D-07 |
| 17 | Containerization, declarative CI/CD | Dockerfile, compose, k8s, GitHub Actions, Jenkinsfile (`-DskipTests`) | Frontend image + full stack → E10 |
| 18 | Clear API docs, documented assumptions | README + Swagger; no assumptions register | US-10.2 |

## Things the requirement doesn't say (covered by assumptions in the backlog)
Who registers books · reservation granularity (one copy vs. any copy of an ISBN) · what happens to a reservation when the book comes back · how long a returned book is held for the next user · due dates / fines / notifications · how admins appear in the system (are they borrowers?).
