# Architecture diagrams

Diagrams are written in [Mermaid](https://mermaid.js.org/) so they are plain text, reviewable in PR diffs
and rendered natively by GitHub. Edit them in place, or paste a block into
[Mermaid Chart](https://www.mermaidchart.com/) for a visual editor.

| Diagram | Purpose |
|---|---|
| [System context](#1-system-context) | Who talks to whom (browser, React, API, Keycloak, DB) |
| [Backend layers](#2-backend-layers) | Packages and dependencies inside the Spring Boot app |
| [Login and API call](#3-login-and-api-call-sequence) | OAuth2 / PKCE flow and JWT validation |
| [Borrow a book](#4-borrow-a-book-sequence) | Transactional borrow with pessimistic lock |
| [Data model](#5-data-model) | Entities and relationships |
| [Deployment](#6-deployment-docker-compose) | Containers and ports (local Docker) |

## 1. System context

```mermaid
flowchart LR
    user([Librarian / User])
    subgraph Browser
        react["React SPA<br/>(JavaScript, Vite)"]
        swagger["Swagger UI"]
    end
    kc["Keycloak<br/>realm: library<br/>:8080"]
    api["Simple Library API<br/>Spring Boot 3.5 / Java 17<br/>:8081"]
    db[("PostgreSQL<br/>(H2 in dev/tests)")]

    user --> react
    user --> swagger
    react -- "login (OIDC + PKCE)" --> kc
    swagger -- "login (client library-swagger)" --> kc
    react -- "REST + Bearer JWT<br/>/apis/v1/**" --> api
    swagger -- "REST + Bearer JWT" --> api
    api -- "fetch JWKS, validate iss" --> kc
    api -- "JPA / Hibernate" --> db
```

## 2. Backend layers

Base package `com.ascendion.roshan.simple_library`.

```mermaid
flowchart TB
    subgraph config
        SecurityConfig
        OpenApiConfig
        AppConfig["AppConfig<br/>(ModelMapper)"]
    end
    subgraph controller
        BookController
        BorrowerController
        BookBorrowerController
    end
    subgraph service
        BookService
        BorrowerService
        BookBorrowerService
    end
    subgraph repository
        BookRepository
        BorrowerRepository
    end
    subgraph entity
        Book
        Borrower
    end
    dto["dto<br/>(requests + response records)"]
    exh["exception<br/>CustomResponseEntityExceptionHandler<br/>NotFoundException"]

    BookController --> BookService
    BorrowerController --> BorrowerService
    BookBorrowerController --> BookBorrowerService
    BookService --> BookRepository
    BorrowerService --> BorrowerRepository
    BookBorrowerService --> BookRepository
    BookBorrowerService --> BorrowerRepository
    BookRepository --> Book
    BorrowerRepository --> Borrower
    controller -. uses .-> dto
    controller -. errors .-> exh
    service -. maps .-> AppConfig
    SecurityConfig -. protects .-> controller
```

## 3. Login and API call (sequence)

```mermaid
sequenceDiagram
    actor U as User
    participant C as React / Swagger UI
    participant K as Keycloak (realm library)
    participant A as Library API

    U->>C: Open app
    C->>K: Authorization request (code + PKCE)
    K->>U: Login page
    U->>K: Credentials
    K-->>C: Authorization code
    C->>K: Token request (code + verifier)
    K-->>C: Access token (JWT)
    C->>A: GET /apis/v1/books (Authorization: Bearer JWT)
    A->>K: Fetch JWKS (cached)
    A->>A: Verify signature and iss claim
    alt valid token
        A-->>C: 200 Page of books
    else missing or invalid token
        A-->>C: 401
    end
```

## 4. Borrow a book (sequence)

`POST /apis/v1/borrowers/{borrower-id}/books`

```mermaid
sequenceDiagram
    participant C as Client
    participant BC as BookBorrowerController
    participant S as BookBorrowerService (@Transactional)
    participant BR as BorrowerRepository
    participant KR as BookRepository
    participant DB as Database

    C->>BC: POST {bookId} + JWT
    BC->>S: borrowBook(borrowerId, request)
    S->>BR: findById(borrowerId)
    BR-->>S: Borrower (or NotFoundException, 404)
    S->>KR: findByIdForUpdate(bookId)
    KR->>DB: SELECT ... FOR UPDATE (pessimistic lock)
    DB-->>KR: Book
    alt already borrowed
        S-->>BC: IllegalStateException (409)
    else available
        S->>S: mark borrowed, set borrowedBy and borrowedDate
        S->>KR: save(book)
        S-->>BC: void
        BC-->>C: 200
    end
```

## 5. Data model

Fields are indicative; check `entity/Book.java` and `entity/Borrower.java` for the source of truth.

```mermaid
erDiagram
    BORROWER ||--o{ BOOK : "currently borrows"
    BORROWER {
        string id PK "UUID"
        string firstname
        string lastname
        string email UK
        datetime createdDate
        datetime lastModifiedDate
    }
    BOOK {
        string id PK "UUID"
        string isbn "copies may share"
        string title
        string author
        boolean borrowed
        string borrowed_by_id FK "nullable"
        datetime borrowedDate
        datetime createdDate
        datetime lastModifiedDate
    }
```

## 6. Deployment (Docker Compose)

```mermaid
flowchart LR
    subgraph host["Host (WSL Ubuntu)"]
        subgraph net["Docker network keycloak_default"]
            webapp["webapp (nginx + React)<br/>host :5173"]
            app["app (Spring Boot)<br/>host :8081 → container :8080"]
            keycloak["keycloak<br/>host :8080"]
            pg[("postgres")]
        end
    end
    browser([Browser])
    browser --> webapp
    browser --> app
    browser --> keycloak
    app -- "JWKS: keycloak:8080" --> keycloak
    app -- "JDBC" --> pg
    keycloak --> pg
```

> `k8s/` holds equivalent manifests for the app and PostgreSQL (`app-deployment`, `app-service`, `postgres-deployment`, `postgres-pvc`).
