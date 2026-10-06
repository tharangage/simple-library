# E08 – API contract & common error response

**Goal:** Every API returns errors in one documented format, and the API shape is fixed *before* the React app depends on it.
**Requirement refs:** Exception Handling · Requirement #4, #9
**Decisions:** D-05, D-08

### US-08.1 Common error response (BE · M)
As an API consumer, I want every error in the same JSON format, so that I can handle errors in one place.
- All 4xx/5xx use `application/problem+json` (RFC 9457 `ProblemDetail`) with extra properties:
  ```json
  { "type": "about:blank", "title": "Conflict", "status": 409,
    "detail": "You have reached the limit of 3 books.",
    "instance": "/apis/v1/me/loans",
    "code": "BORROW_LIMIT_REACHED", "traceId": "4bf92f35…",
    "errors": { "email": "must be a well-formed email address" } }
  ```
- 401/403 from Spring Security use the same format (custom `AuthenticationEntryPoint` / `AccessDeniedHandler`).
- 500 never leaks stack traces or exception messages: generic `detail`, details only in logs.
- Replaces the current plain-string bodies. This is a **breaking change**, done before the frontend starts.

### US-08.2 Domain exceptions & error-code catalogue (BE · S)
- Exceptions: `BookNotFoundException`, `BookNotAvailableException`, `BorrowLimitReachedException`, `ReservationNotAllowedException`, `EmailAlreadyRegisteredException`, `IsbnConflictException`… all extend a base `LibraryException(code, httpStatus)`. They're unchecked, so `@Transactional` rolls back.
- One handler maps them to status + `code`.
- Catalogue of codes in `docs/api/error-codes.md`. The frontend maps codes to user messages.

### US-08.3 OpenAPI completeness (BE · S)
- Every operation has a summary, its required role, request/response examples and the error schema for each possible status.
- Tags: Registration, Books, Me, Loans (admin). `info` holds a version and contact.
- The `openapi.json` is exported in CI as a build artifact (optional: generate a JS client for the frontend).

### US-08.4 Settle API shape before the UI (BE · S)
- `GET /apis/v1/books` uses Spring Data `PagedModel` (`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`): `{content, page:{size, number, totalElements, totalPages}}`.
- Create endpoints return **201 + `Location`** (borrower registration currently returns 200).
- New user-facing paths go under `/apis/v1/me/**` and admin paths under `/apis/v1/loans/**` (see E01/E04/E06). Old paths are deprecated in OpenAPI.
