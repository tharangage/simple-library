# E10 – Delivery, environments & documentation

**Jira:** [SCRUM-14](https://tharangage.atlassian.net/browse/SCRUM-14)

**Goal:** One command brings up the full stack on WSL, every environment is config-only (12-factor), and the docs cover API use and assumptions.
**Requirement refs:** Requirements #2, #3, #9, #10 · Nice-to-have #3, #4
**Decisions:** D-06, D-07

### US-10.1 Full local stack in compose (OPS · M) · [SCRUM-58](https://tharangage.atlassian.net/browse/SCRUM-58)
- One compose file (or `include`s) with Keycloak (realm `library` imported), PostgreSQL, backend, frontend and otel-lgtm on one network.
- `docker compose up -d --build` gives you the app at http://localhost:5173, the API/Swagger at http://localhost:8081 and Keycloak at http://localhost:8080.
- Fixes the known compose issues (`dev` profile without a datasource URL, misleading comments). The default profile is settled per D-06.

### US-10.2 User & API documentation (OPS · S) · [SCRUM-59](https://tharangage.atlassian.net/browse/SCRUM-59)
- `docs/api/`: how to get a token (Swagger / curl with PKCE), endpoint guide per role, error codes.
- `docs/operations/run-locally.md` for WSL: ports, profiles, the `mvnw` CRLF fix.
- Assumptions register (from backlog) and database choice justification (H2 for dev/tests, PostgreSQL for shared environments: transactions, row locks, constraints).

### US-10.3 CI/CD & deployment hardening (OPS · M) · [SCRUM-60](https://tharangage.atlassian.net/browse/SCRUM-60)
- `Jenkinsfile` runs tests (remove `-DskipTests`), or Jenkins is retired in favour of GitHub Actions. Decide and document.
- k8s manifests for the frontend (Deployment, Service, Ingress, ConfigMap for runtime config). The backend gets OTel env vars and probes (`/actuator/health`).
- Schema migrations with Flyway (replace `ddl-auto: update` outside H2), needed once Reservation and new columns land.

### US-10.4 Java 25 upgrade (BE · M · optional) · [SCRUM-61](https://tharangage.atlassian.net/browse/SCRUM-61)
- Only if D-07 = 25: update `pom.xml`, Dockerfile base image and CI. Check Lombok / SpotBugs / JaCoCo compatibility. Update `.claude/CLAUDE.md` hard rule.
