# Simple library requirement in a nutshell

## Task

Create a RESTful API that manages a simple library system.

### General actions

The API should allow an API user to:

1. Register a new borrower to the library.
2. Register a new book to the library.
3. Get a list of all books in the library.

### Actions on behalf of a borrower

The API should allow an API user, acting for a borrower, to:

1. Borrow a book with a particular book id (see Book in Data models).
2. Return a borrowed book.

## Data models

| Model | Fields |
| --- | --- |
| Borrower | Unique id, name, email address |
| Book | Unique id, ISBN number, title, author |

### ISBN rules

An ISBN number uniquely identifies a book in the following way:

- 2 books with the same title and same author but different ISBN numbers are considered different books.
- 2 books with the same ISBN number must have the same title and same author.
- Multiple copies of books with the same ISBN number are allowed in the system.

## Requirements

1. Use a programming language and framework of your choice to create the project.
   - Use of Java 25 and the Spring Boot framework is an added bonus.
2. Make it configurable to run in multiple environments.
3. Use a package manager to manage project dependencies.
4. Implement proper data validation and error handling.
5. Use a database to store borrower and book data.
   - Justify your choice of database.
6. Implement REST API endpoints for each action mentioned above.
7. Register multiple books with the same ISBN number as books with different ids.
8. Ensure that no more than one member is borrowing the same book (same book id) at a time.
9. Provide clear documentation for how to use your API.
10. Document all your assumptions for any requirements not explicitly stated in this task.

## Nice to have

1. Unit tests and unit test coverage.
2. Clean code.
3. Containerization and CI/CD tools, used in a declarative manner.
   - Docker and Kubernetes preferred.
4. Conformance (to a certain extent) to the [12 Factor App](https://12factor.net/) principles.

## User Access

Restrict all backend REST APIs to use only for authenticated users who are registered in the Simple library system. Use Keycloak to register users in a realm called "library" and integrate keyclaok to the backend and frontend applications via OpenID-Connect.

## API Documentation

Integrate Swagger-UI into the backend service and enable authentication via Keycloak under "library" realm.&#32;

## Frontend Web Application

Use a ReactJS and JavaScript (not TypeScript) based simple web application to fulfill the following user operations.

End-Users can;

1. can register itself giving the valid email and valid mobile no. In later development stages, email/mobile validation can be done. But for now (assuming first stages as a MVP mindset), email/mobile should not be validated for existence. But require them in a valid format. For MVP, a flat user role can be used as "user"; all APIs except user-registration should be authorized for "user" role permission.
2. Registered users can see all books in the library with borrowed status.
3. Registered users can borrow any available books with a maximum limit of 3 per given time.
4. Registered users can reserve if the required book is already borrowed, but the maximum limit is 3 per given time for the sum of both borrowed and reserved.

Admin Users can;

Note: Admin users are not self-created via the frontend web application, but Keycloak admin can provision new admin users via Keycloak Admin Console with the "admin" role.

1. Can update borrowed status once return the book physically at the library contact point. That mean book-return REST-API and UI-Page can be accessed to  users who have "admin" role permission.

## Non-functional Requirements

Logging/Tracing

Each API actions (like request, response, error metadata) should be logged and published via Open-Telemetry. Do not log any privacy data and comply with the General Data Protection Regulation (GDPR). Better instrumentation at the JVM level without modifying the codebase.

Exception Handling

Used new checked/unchecked exception classes if required in business logic implementations. And use common error response for each REST-APIs.
