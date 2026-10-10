# Decisions

Open decisions block or shape stories. When one is settled, record it as an ADR in this folder
(`ADR-NNN-short-title.md`: Context · Decision · Consequences) and set the status below to *Decided*.

| ID | Question                              | Options | Recommendation | Affects | Status |
|---|---------------------------------------|---|---|---|---|
| D-01 | How do visitors register?             | **a)** React form → public backend API → Keycloak Admin REST API (service account) creates the user with role `user` + Borrower row · **b)** Keycloak's hosted registration page (with a `mobile` user-profile attribute) + Borrower created on first login | **a**: one consistent UI, matches the "user-registration API" in the requirement; needs a tightly scoped service account and a compensating delete if a step fails | E02, US-01.1 | Decided: option a, implemented in E02 (US-02.1–02.3); to be confirmed by testing against the real Keycloak |
| D-02 | What does a reservation point to?     | **a)** a specific copy (book id) · **b)** a title (ISBN): first returned copy goes to the queue | **a** for the MVP (matches the requirement wording and the per-copy list); revisit **b** later | E05 | Open |
| D-03 | OIDC library in React                 | `react-oidc-context` + `oidc-client-ts` · `keycloak-js` | `react-oidc-context`: standard OIDC + PKCE, not tied to Keycloak, works from plain JS | US-07.2 | Decided: `react-oidc-context` (US-03.1) |
| D-04 | Where does the React app live?        | `frontend/` in this repo (folder exists) · separate repo (as `.claude/CLAUDE.md` says today) | `frontend/` in this repo: one PR can change API + UI together; update `.claude/CLAUDE.md` | E07 | Decided: `frontend/` in this repo (US-03.1) |
| D-05 | Common error response format          | Spring `ProblemDetail` (RFC 9457) + `code`, `traceId`, `errors` · custom JSON | `ProblemDetail`: standard, built into Spring Boot 3 | US-08.1 | Open |
| D-06 | Default Spring profile                | `h2` (project rule) · `local` (current `application.yaml`) | `h2` as default so a fresh clone runs with no database; `local` stays opt-in | E10 | Open |
| D-07 | Java version                          | 17 (project hard rule) · 25 (requirement bonus) | Decide separately from the UI work; if 25, do it as its own story (US-10.4) | US-10.4 | Open |
| D-08 | Paging JSON shape                     | keep `PageImpl` · `PagedModel` (`VIA_DTO`) | Switch now, before the React app consumes it | US-08.4 | Open |
| D-09 | Server-state / data fetching in React | TanStack Query · plain `fetch` + `useEffect` | TanStack Query: caching, refetch after borrow/return, loading/error states | US-07.3 | Decided: TanStack Query (US-03.1) |
| D-10 | UI component library                  | MUI · Mantine · plain CSS / small kit | MUI or Mantine (accessible components, data table, forms) | US-07.5 | Decided for now: plain CSS with variables; revisit when forms arrive (E02) |
