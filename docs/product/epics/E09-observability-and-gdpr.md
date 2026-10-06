# E09 – Observability (OpenTelemetry) & GDPR

**Goal:** Every API action (request, response, error metadata) is traced and logged through OpenTelemetry, with JVM-level instrumentation and no personal data.
**Requirement refs:** Non-functional – Logging/Tracing

### US-09.1 OpenTelemetry Java agent (OPS · S)
As ops, I want tracing and metrics without code changes, so that instrumentation stays out of business code.
- `opentelemetry-javaagent.jar` (pinned version, checksum verified) is added in the Dockerfile. It's enabled with `JAVA_TOOL_OPTIONS=-javaagent:…`, and the same works from the IDE run config.
- Configured through env only: `OTEL_SERVICE_NAME=simple-library`, `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_RESOURCE_ATTRIBUTES=deployment.environment=…`.
- HTTP server spans carry method, route template (not the raw path with ids), status and duration. Errors are recorded as span events.

### US-09.2 Local observability stack (OPS · S)
- Compose service `grafana/otel-lgtm` (Collector + Tempo + Loki + Prometheus + Grafana) on WSL, documented ports.
- Traces, logs and metrics from the backend are visible in Grafana. Logs link to their trace.

### US-09.3 Logs exported with trace correlation (OPS · S)
- The agent's Logback appender bridge exports logs over OTLP. `trace_id` / `span_id` are in the console log pattern.
- One log line per API call comes from the agent / span data (method, route, status, duration, `enduser.id` = pseudonymous `sub` only). No request/response bodies.

### US-09.4 GDPR log & telemetry review (BE + OPS · M)
As the data controller, I want no personal data in logs or telemetry, so that we comply with GDPR.
- Agent settings: no captured request/response headers or parameters, DB statement sanitization on (default), `Authorization` never captured.
- Hibernate SQL bind-parameter logging is off outside `dev`. Exception messages contain no email, name or mobile (e.g. "Email already exists" without the address).
- Review checklist in `docs/operations/gdpr-logging.md` (what is logged, retention, lawful basis). Automated test greps captured logs for e-mail/phone patterns during the integration suite.

### US-09.5 Browser trace propagation (FE · S · nice-to-have)
- The frontend sends a W3C `traceparent` header (OpenTelemetry Web SDK or a simple generator) so UI actions and backend spans line up. CORS allows the header.
