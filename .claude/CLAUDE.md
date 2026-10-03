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
Base package: `com.ascendion.roshan.simple_library`
```
config/      AppConfig (ModelMapper), OpenApiConfig (Swagger OAuth2), SecurityConfig
controller/  BookController, BorrowerController, BookBorrowerController
dto/         BookCreateRequest, BorrowerCreateRequest, BorrowBookRequest (Bean Validation)
entity/      Book, Borrower (UUID String ids, JPA auditing dates)
exception/   NotFoundException, CustomResponseEntityExceptionHandler
repository/  BookRepository, BorrowerRepository (JpaRepository<_, String>)
service/     BookService, BorrowerService, BookBorrowerService
```
Other: `Dockerfile`, `docker-compose.yaml` (app only), `keycloak/docker-compose.yml`
(Keycloak + Postgres), `k8s/`, `Jenkinsfile`, `archive/` (old files — ignore).

## API
| Method | Path | Result |
|---|---|---|
| POST | `/apis/v1/books` | 201, registers a book |
| GET | `/apis/v1/books` | 200, `Page<Book>` |
| POST | `/apis/v1/borrowers` | 200, registers a borrower |
| POST | `/apis/v1/borrowers/{borrower-id}/books` | 200, borrow (`{"bookId": "<uuid>"}`) |
| DELETE | `/apis/v1/borrowers/{borrower-id}/books/{book-id}` | 200, return |

Business rules:
- Several copies of a book may share an ISBN, but title **and** author must match the existing one → otherwise 409.
- Borrower email is unique (`DataIntegrityViolationException` → `IllegalStateException("Email already exists")`).
- A borrowed book can't be borrowed again; only the borrower who has it can return it.

Error mapping (`CustomResponseEntityExceptionHandler`):
`NotFoundException` → 404, `IllegalStateException` / `DataIntegrityViolationException` → 409,
bean validation → 400 with a `{field: message}` map, any other `RuntimeException` → 500.
The body for 404/409/500 is the plain message string.

## Security
- Stateless OAuth2 resource server; JWTs issued by Keycloak realm **`library`**.
- Open without a token: `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`, all `OPTIONS`.
- Everything else just needs a valid token. **There are no role checks yet**, so test
  "no token → 401" and "valid `jwt()` → 2xx"; don't invent 403 tests unless roles are added.
- `issuer-uri` = `${app.keycloak.public-url}/realms/${app.keycloak.realm}` (default `http://localhost:8080/realms/library`).
  It must equal the token's `iss` claim, i.e. the URL the browser used.
- Swagger UI client id: `library-swagger` (public client, PKCE, scope `openid`).

## Profiles & running
| Profile | DB | Notes |
|---|---|---|
| `h2` (default in `application.yaml`) | in-memory `jdbc:h2:mem:simple-library` | H2 console at `/h2-console` (sa / password) |
| `dev` | PostgreSQL from `SPRING_DATASOURCE_*` / `application.yaml` | `ddl-auto: update`, SQL logging. Default in `docker-compose.yaml` |
| `prod` | PostgreSQL from env vars | `application-prod.yaml` is currently empty |

Commands (run in WSL Ubuntu from the repo root):
```bash
./mvnw clean verify                         # build + all tests
./mvnw test -Dtest=BookServiceTest          # one test class
./mvnw test -Dtest='BookServiceTest#registerBook*'
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
docker compose up -d --build                # app in Docker → http://localhost:8081/swagger-ui/index.html
```
Environment: Windows 11 + WSL Ubuntu. Keycloak (host port **8080**) and PostgreSQL run in
Docker in WSL (compose project `keycloak`, network `keycloak_default`).
- **Port clash:** Keycloak owns 8080, so run the app locally on **8081** (as above or in IDE run config).
- If `./mvnw` fails with `/bin/sh^M`, the file has CRLF endings: `sed -i 's/\r$//' mvnw`.

## Testing conventions
- Tests mirror the main package under `src/test/java`, named `<ClassName>Test`.
- Service tests: `@ExtendWith(MockitoExtension.class)`, `@Mock` repositories / `ModelMapper`, `@InjectMocks` (see `BookBorrowerServiceTest`).
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
- `BorrowBookRequest` has `@Builder`; `BookCreateRequest` / `BorrowerCreateRequest` have only getters —
  build them via JSON in MockMvc tests, or `ReflectionTestUtils.setField` in unit tests.
- Assertions: AssertJ preferred for new tests; test names `method_condition_expectedResult`.

## Known issues (don't "fix" silently — mention them)
- `BookService.listBooks` ignores the `Pageable` (`new PageImpl<>(findAll())`) — paging/size params have no effect.
- `POST /apis/v1/borrowers` returns 200, not 201 like books.
- `Book.isBorrowed` + Lombok `@Data` produces getter `isBorrowed()` / setter `setBorrowed()`; JSON field is `borrowed`.
- `application-prod.yaml` is empty; `docker-compose.yaml` comment mentions `h2` but defaults to `dev`.
- `Jenkinsfile` builds with `-DskipTests`.
