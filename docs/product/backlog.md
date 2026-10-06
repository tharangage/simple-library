# Simple Library – product backlog

**Scope:** the ReactJS + JavaScript web app (no TypeScript) and the backend, Keycloak and ops work it depends on.
**Sources:** [requirement](../requirements/simple-library-requirement-in-a-nutshell.md) · [gap analysis](../requirements/gap-analysis.md) · [open decisions](../decisions/README.md)

## Personas
| Persona | How they get access | What they do |
|---|---|---|
| **Visitor** | No account | Registers through the web app |
| **User** (Keycloak realm role `user`) | Self-registered | Browses books, borrows (≤ 3), reserves, sees own loans and reservations |
| **Admin** (realm role `admin`) | Created by a Keycloak admin in the Admin Console | Marks books as returned at the library desk, registers books |
| **API user** | Swagger UI / any OIDC client in realm `library` | Calls the REST API directly with a bearer token |

## Epics
| ID | Epic | Layers | Stories | Milestone |
|---|---|---|---|---|
| [E01](epics/E01-identity-and-access.md) | Identity & access (Keycloak realm `library`, roles) | IAM, BE | 6 | M1 |
| [E02](epics/E02-self-registration.md) | User self-registration | BE, FE, IAM | 4 | M2 |
| [E03](epics/E03-book-catalogue.md) | Book catalogue & availability | BE, FE | 5 | M2 |
| [E04](epics/E04-borrowing.md) | Borrowing (limit 3) & "My books" | BE, FE | 3 | M3 |
| [E05](epics/E05-reservations.md) | Reservations | BE, FE | 5 | M4 |
| [E06](epics/E06-returns-admin.md) | Returns at the library desk (admin) | BE, FE | 3 | M3 |
| [E07](epics/E07-frontend-foundation.md) | Web app foundation (React + JS) | FE, OPS | 8 | M1 |
| [E08](epics/E08-api-contract-and-errors.md) | API contract & common error response | BE | 4 | M0 |
| [E09](epics/E09-observability-and-gdpr.md) | Observability (OpenTelemetry) & GDPR | OPS, BE | 5 | M5 |
| [E10](epics/E10-delivery-and-docs.md) | Delivery, environments & documentation | OPS | 4 | M5 |

## Milestones (suggested order)
| Milestone | Goal | Stories |
|---|---|---|
| **M0 – Contract first** | Settle the open decisions (D-01…D-08, D-09, D-10) and fix the API shape *before* the UI depends on it | US-08.1, US-08.2, US-08.4 |
| **M1 – Walking skeleton** | Log in through Keycloak realm `library` from the React app and call one protected API | US-01.1–01.3, US-07.1–07.5 |
| **M2 – Sign up & browse** | A visitor registers, logs in and sees the catalogue with status | E02, US-03.1, US-03.2, US-03.4 |
| **M3 – Borrow & return** | Users borrow (≤ 3), admins mark returns | US-01.4, US-01.5, E04, E06 |
| **M4 – Reservations** | Reserve borrowed books, hand-over on return, expiry | E05 |
| **M5 – Production-ready** | Observability, GDPR review, containers, CI, docs | E09, E10, US-07.6–07.8, US-08.3, US-01.6 |

Testing is part of each story (Definition of Done), not a separate phase.

## Story map (user journey)
```
 Sign up            Log in           Browse            Borrow             Reserve              Return (admin)
 ─────────────────  ───────────────  ────────────────  ─────────────────  ───────────────────  ──────────────────
 US-02.3 form       US-07.2 OIDC     US-03.1 list      US-04.2 borrow btn US-05.5 reserve btn  US-06.2 returns page
 US-02.1 API        US-07.4 shell    US-03.2 status    US-04.1 limit 3    US-05.1 API          US-06.1 admin API
 US-02.2 mobile     US-01.2 roles    US-03.3 search*   US-04.3 My books   US-05.2 hand-over    US-06.3 role nav
 US-02.4 JIT        US-01.4 "me"     US-03.4 add book  ──                 US-05.3 expiry       ──
                                                                          US-05.4 cancel
 (* = nice-to-have)
```

## Assumptions (requirement is silent → our choice)
| ID | Assumption |
|---|---|
| A-01 | One Keycloak user ↔ one Borrower record, linked by the token's `sub` claim. |
| A-02 | Admins are not borrowers; they don't borrow or reserve. |
| A-03 | Only admins register books (API + optional admin UI). |
| A-04 | "Book" means a physical copy; its `id` identifies the copy. Several copies can share an ISBN. |
| A-05 | The limit of 3 counts active loans + reservations in state `WAITING` or `READY`. |
| A-06 | A returned book with a waiting reservation is held for the first person in the queue for **3 days** (configurable), then offered to the next one. |
| A-07 | No due dates, fines or e-mail/SMS notifications in the MVP; users see reservation status in the app. |
| A-08 | Email and mobile are checked for *format* only: email per Bean Validation `@Email` + a `x@y.tld` pattern; mobile in E.164 (`+` then 8–15 digits). |
| A-09 | A borrower's name is first + last name (existing model). |
| A-10 | Any authenticated user (user or admin) can see the catalogue. Users never see *who* borrowed or reserved a book, only whether they did themselves. |
| A-11 | English UI only; responsive (desktop and mobile browsers); WCAG 2.1 AA basics. |
| A-12 | Passwords are set at registration and handled only by Keycloak (never stored or logged by the backend). |

## Definition of Done (every story)
- Acceptance criteria met and demonstrated.
- BE: unit tests (service) + slice tests (`@WebMvcTest`, incl. 401/403 cases) per `.claude/CLAUDE.md`; `./mvnw -Pquality verify` green.
- FE: component tests (Vitest + React Testing Library, API mocked with MSW); `npm run lint && npm test && npm run build` green.
- OpenAPI updated for any API change; no personal data in logs.
- Docs updated (assumptions, run guide) where behaviour changed.
