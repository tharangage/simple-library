# Simple Library – documentation

| Folder | What lives there |
|---|---|
| [`requirements/`](requirements/) | Source requirements (as received) and the gap analysis against the current code |
| [`product/`](product/) | Product backlog: epics, user stories, milestones, assumptions |
| [`product/epics/`](product/epics/) | One file per epic with its stories and acceptance criteria |
| [`decisions/`](decisions/) | Open decisions and Architecture Decision Records (ADRs) |
| [`architecture/`](architecture/) | Mermaid architecture diagrams (context, layers, flows, data model, deployment) |
| [`setup/`](setup/) | Environment setup: Keycloak clients and realm settings, Docker Hub images, running on Kubernetes (kind) |
| [`design/`](design/) | UI designs in Figma: file links, frames, field mapping, open questions |

Start with [`product/backlog.md`](product/backlog.md).

## Conventions
- **Epic IDs** `E01`…, **story IDs** `US-<epic>.<n>` (e.g. `US-04.2`). IDs never get reused; drop a story by marking it *Dropped*.
- **Layer tag** on each story: `FE` (React app), `BE` (Spring Boot API), `IAM` (Keycloak), `OPS` (build/run/observe).
- **Size**: `S` ≈ ≤ 1 day, `M` ≈ 2–3 days, `L` ≈ 1 week. If a story is bigger than `L`, split it.
- **Status**: `Proposed` → `Ready` → `In progress` → `Done`. All stories start as *Proposed*. Jira (project `SCRUM`) is where status is tracked; each story heading in `product/epics/` is followed by a `**Status:**` line that mirrors Jira, with the sprint and, for work in progress, what is left.
- **Assumptions** are `A-xx` and **decisions** are `D-xx`. Both are listed in the backlog and in `decisions/`.
