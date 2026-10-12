# Simple Library – project guide for Claude

REST API for a simple library (books, borrowers, borrowing/returning). Keycloak is the
identity provider. A ReactJS front end (separate repo/folder, not in this repository) and
Swagger UI both log users in through Keycloak and call this API with bearer tokens.

## Hard rules
- Backend: Java 17 + Spring Boot only. Front end: ReactJS with **JavaScript — never TypeScript**.
- H2 is the default for development and tests; PostgreSQL is optional (profiles `dev` / `prod`).
- Never call a real Keycloak or PostgreSQL from tests. Mock JWTs with `spring-security-test`.
- Don't commit secrets. Credentials in `application.yaml` / compose files are local-only defaults.
- Don't change production code silently while writing tests — report suspected bugs instead.

## Tech stack (from pom.xml)
- Spring Boot **3.5.0** (parent), Java **17**, Maven **3.9.9** via wrapper (`./mvnw`)
- Starters: web, data-jpa, validation, security, oauth2-resource-server
- springdoc-openapi-starter-webmvc-ui **2.8.8** (Swagger UI with Keycloak "Authorize" button, PKCE)
- ModelMapper **3.2.3** (DTO → entity, bean in `config/AppConfig`)
- Lombok (`@Data`, `@Getter`, `@Builder`, `@Slf4j`) — annotation processor configured in the compiler plugin
- Drivers: H2 and PostgreSQL (runtime scope)
- Test: spring-boot-starter-test (JUnit 5, Mockito, AssertJ, MockMvc), spring-security-test

## Project layout
All backend code lives in `backend/` (pom.xml, mvnw, src, config/, lombok.config, Dockerfile). Run Maven from there.
Base package: `com.ascendion.roshan.simple_library`
```
config/      AppConfig (ModelMapper), OpenApiConfig (Swagger OAuth2), SecurityConfig
controller/  BookController, BorrowerController, BorrowedBookController
dto/         BookCreateRequest, BorrowerCreateRequest (Bean Validation), BorrowedBookResponse
entity/      Book, Borrower (UUID String ids, JPA auditing dates)
exception/   NotFoundException, CustomResponseEntityExceptionHandler
repository/  BookRepository, BorrowerRepository (JpaRepository<_, String>)
service/     BookService, BorrowerService, BorrowedBookService, RegistrationService, KeycloakAdminClient (service account `library-backend`; secret `KEYCLOAK_ADMIN_CLIENT_SECRET`, see docs/setup/keycloak-registration.md)
```
Other: `backend/Dockerfile`, `docker-compose.yaml` (app only), `keycloak/docker-compose.yml`
(Keycloak + Postgres), `k8s/`, `Jenkinsfile`, `archive/` (old files — ignore).

## API
| Method | Path | Result |
|---|---|---|
| POST | `/apis/v1/books` | 201, registers a book. Role `admin` only (403 otherwise) |
| GET | `/apis/v1/books` | 200, `Page<Book>` |
| POST | `/apis/v1/registrations` | 201, **public** self-registration: creates the Keycloak user (Admin API) + Borrower; 409 `EMAIL_ALREADY_REGISTERED` |
| POST | `/apis/v1/borrowers` | 200, registers a borrower |
| GET | `/apis/v1/me` | 200, role `user` or `admin` (a token with neither gets 403): the same profile for everybody `{userId, firstname, lastname, email, roles, mobile, borrower}`. `roles` lists `user` / `admin` as far as the caller has them (the web app shows or hides admin features with it); `mobile` is the Borrower's, null without one. `borrower` (`BorrowerProfile`) is `{id, borrowedCount, maxItems, remainingSlots}` for a caller with role `user` (created on the first call, US-02.4) and `null` otherwise. The web app calls it after login |
| POST | `/apis/v1/borrowed-books/{book-id}/borrowers/{borrower-id}` | 200 (empty body, no request body), borrow. 409 body `BOOK_NOT_AVAILABLE` or `BORROW_LIMIT_REACHED` (max 3 items, `app.library.max-items-per-borrower`) |
| GET | `/apis/v1/borrowed-books?q=` | 200, `Page<BorrowedBookResponse>` of books that are out with the borrower's name and email (`q` matches book id, ISBN, title, borrower email; default 5 per page, newest first). Role `admin` only. Feeds the Returns page |
| DELETE | `/apis/v1/borrowed-books/{book-id}/borrowers/{borrower-id}` | 200, return (role `admin`; the Returns page at `/admin/returns` uses it) |

Business rules:
- Several copies of a book may share an ISBN, but title **and** author must match the existing one → otherwise 409.
- Borrower email is unique (`DataIntegrityViolationException` → `IllegalStateException("Email already exists")`).
- A borrowed book can't be borrowed again; only the borrower who has it can be returned by that borrower id, and only an admin may call return (library desk).
- Borrow: an admin may borrow for any borrower (library desk); everybody else only for their own Borrower (403 otherwise). A borrower holds at most 3 books.

Error mapping (`CustomResponseEntityExceptionHandler`):
`NotFoundException` → 404, `AccessDeniedException` → 403, `IllegalStateException` / `DataIntegrityViolationException` → 409,
bean validation → 400 with a `{field: message}` map, any other `RuntimeException` → 500.
The body for 404/409/500 is the plain message string.

## Security
- Stateless OAuth2 resource server; JWTs issued by Keycloak realm **`library`**.
- Open without a token: `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`, all `OPTIONS`, `POST /apis/v1/registrations`.
- Roles come from `realm_access.roles` (`user` → `ROLE_USER`, `admin` → `ROLE_ADMIN`, converter in `SecurityConfig`).
  Role rules exist **only** for `GET /apis/v1/borrowed-books` (`admin`), `GET /apis/v1/me` and `POST .../borrowed-books/{book-id}/borrowers/{id}` (`user` or `admin`) and
  `POST /apis/v1/books`, `DELETE .../borrowed-books/{book-id}/borrowers/{id}` (`admin`); a token without the role → 403. Tests use
  `jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))`. All other endpoints (`GET /books`, `POST /borrowers`) still just need a valid token (E01 adds the rest).
- Caller context: `CurrentUserInterceptor` (all `/apis/**`) puts the token details (`CurrentUser`: `Jwt`, authorities, `isUser()`/`isAdmin()`) into a `ThreadLocal`
  (`CurrentUserHolder.get()`/`require()`) and clears it after the request. Empty when the request has no token. Use it instead of reading the security context in controllers.
- Borrower provisioning (US-02.4): `BorrowerProvisioningInterceptor` (registered in `WebConfig` for borrow only, after `CurrentUserInterceptor`; `GET /me` calls the service itself because it returns the Borrower)
  creates the Borrower for every caller with `ROLE_USER`; it does not pass the Borrower on. Borrow checks it:
  an admin may use any borrower id in the path (library desk), everybody else only their own (else 403); an admin without `user` gets no Borrower.
  Return is admin-only and does not provision.
- `issuer-uri` = `${app.keycloak.public-url}/realms/${app.keycloak.realm}` (default `http://localhost:8080/realms/library`).
  It must equal the token's `iss` claim, i.e. the URL the browser used.
- Swagger UI client id: `library-swagger` (public client, PKCE, scope `openid`).

## Profiles & running
| Profile | DB | Notes |
|---|---|---|
| `h2` | in-memory `jdbc:h2:mem:simple-library` | H2 console at `/h2-console` (sa / password) |
| `dev` | PostgreSQL from `SPRING_DATASOURCE_*` / `application.yaml` | `ddl-auto: update`, SQL logging. Default in `docker-compose.yaml` |
| `local` (default in `application.yaml`) | PostgreSQL on `localhost:5432` (`application-local.yaml`) | Runs on port 8081 |
| `prod` | PostgreSQL from env vars | no `application-prod.yaml` exists |

Commands (run in WSL Ubuntu from `backend/`):
```bash
./mvnw clean verify                         # build + all tests
./mvnw test -Dtest=BookServiceTest          # one test class
./mvnw test -Dtest='BookServiceTest#registerBook*'
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
(cd .. && docker compose up -d --build)             # app in Docker → http://localhost:8081/swagger-ui/index.html
```
Environment: Windows 11 + WSL Ubuntu. Keycloak (host port **8080**) and PostgreSQL run in
Docker in WSL (compose project `keycloak`, network `keycloak_default`).
- **Port clash:** Keycloak owns 8080, so run the app locally on **8081** (as above or in IDE run config).
- If `./mvnw` fails with `/bin/sh^M`, the file has CRLF endings: `sed -i 's/\r$//' mvnw`.

## Testing conventions
- Tests mirror the main package under `backend/src/test/java`, named `<ClassName>Test`.
- Service tests: `@ExtendWith(MockitoExtension.class)`, `@Mock` repositories / `ModelMapper`, `@InjectMocks` (see `BorrowedBookServiceTest`).
- Controller tests: prefer `@WebMvcTest(XController.class)` + `MockMvc` + `@MockitoBean` services
  (Boot 3.5: use `@MockitoBean`, not the deprecated `@MockBean`).
  - `@EnableJpaAuditing` sits on `SimpleLibraryApplication`, so `@WebMvcTest` fails with
    "JPA metamodel must not be empty". Add `@MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;`
    to the test (or move auditing to a separate `@Configuration` class — ask first).
  - Import `SecurityConfig` with `@Import(SecurityConfig.class)` and use `.with(jwt())`.
    Without `jwt()` expect 401.
- Repository tests: `@DataJpaTest` (uses H2).
- Full-context tests: `@SpringBootTest` + `@ActiveProfiles("h2")` only for integration tests
  (existing `BookControllerTest` and `SimpleLibraryApplicationTest` are this style).
- `BookCreateRequest` / `BorrowerCreateRequest` have only getters —
  build them via JSON in MockMvc tests, or `ReflectionTestUtils.setField` in unit tests.
- Assertions: AssertJ preferred for new tests; test names `method_condition_expectedResult`.

## Code quality (all free)
- Local: `./mvnw -Pquality verify` → tests + JaCoCo coverage + SpotBugs/FindSecBugs + PMD + CPD.
  Reports: `backend/target/site/jacoco/index.html`, `backend/target/spotbugsXml.xml`, `backend/target/pmd.xml`, `backend/target/cpd.xml`.
- Config: `backend/config/pmd/ruleset.xml`, `backend/config/spotbugs/exclude.xml` (every exclusion needs a reason),
  `backend/lombok.config` (marks generated code so coverage/SpotBugs skip it). Gates are `quality.*` properties in `pom.xml`.
- CI (`.github/workflows/`): `code-quality.yml` runs the same profile on every PR, uploads findings to
  GitHub code scanning (inline PR annotations), plus dependency review and optional SonarQube Cloud
  (only when the `SONAR_TOKEN` secret exists). `codeql.yml` runs CodeQL `security-and-quality`.
- Before opening a PR, use the **code-quality-reviewer** agent: it runs the tools and adds a
  Spring/security/business-rule review. It is read-only; fix findings yourself or via the main assistant.

## Known issues (don't "fix" silently — mention them)
- Default profile in `application.yaml` is **`local`** (PostgreSQL on localhost:5432, port 8081), not `h2`.
  This conflicts with the "H2 is the default" hard rule — decide which one is right. Tests use `@ActiveProfiles("h2")`.
- `POST /apis/v1/borrowers` returns 200, not 201 like books.
- `Book.isBorrowed` + Lombok produces getter `isBorrowed()` / setter `setBorrowed()`; JSON field is `borrowed`.
- `GET /apis/v1/books` serializes `PageImpl` directly (Spring Data logs a "stability of the JSON structure" warning).
  Switching to `PagedModel` / `VIA_DTO` changes the JSON shape, so coordinate with the React app first.
- Registering two books with the same new ISBN concurrently can bypass the title/author check (no DB constraint).
- JWTs are not checked for audience/`azp`: any token from realm `library` is accepted.
- `docker-compose.yaml` comment mentions `h2` but defaults to `dev`; `dev` has no datasource URL of its own.
- `Jenkinsfile` builds with `-DskipTests`.

## Design notes
- Controllers return response DTOs (`BookResponse`, `BorrowerResponse` records), never JPA entities.
- Borrow/return are `@Transactional` and load the book with `BookRepository.findByIdForUpdate`
  (pessimistic write lock), so concurrent borrows of one copy can't both succeed. Lock timeouts → 409.
- `?sort=<unknown>` → 400 (`PropertyReferenceException`). CORS origins: `app.cors.allowed-origins`.
