## API endpoint standards and conventions

This codebase is Kotlin + Spring Boot. The primary REST API lives in the edge applications (`projects/lpg-ehl/lpg-ehl-webapp` and, when enabled, `projects/lpg-ehl/lpg-ehl-app-headless` via profiles).

### Design rules

- **Adapters stay thin**: Controllers should be a thin adapter layer over services (business logic lives in `lpg-ehl-service`).
- **Versioned paths**: Use a stable, versioned base path (prefer `/api/v1/...` unless the existing code uses a different convention).
- **Resource-oriented URLs**: Use nouns; keep nesting shallow; use query params for filters/pagination.
- **Explicit DTOs**: Use request/response DTOs at the API boundary; avoid leaking JPA entities directly.
- **Consistent status codes**:
  - `200`/`201` for success
  - `400` for validation/contract errors
  - `404` for missing resources
  - `409` for state conflicts (common in device/payment flows)
  - `503` for dependencies not ready (serial transport/payment terminal)
- **Error shape**: Prefer a consistent JSON error format. Centralize mapping in a `@ControllerAdvice` (or equivalent) rather than ad-hoc `try/catch` in each controller.
- **OpenAPI is a contract**:
  - The payment terminal Mono server contract is `openapi-payment-terminal.yaml`.
  - If `openapi.yaml` is used for the edge API, keep it aligned with controllers and DTOs.

### Observability and safety

- **No secrets in logs**: Never log credentials/tokens; be mindful of payment identifiers.
- **Hardware interactions are stateful**: For operations that command the dispenser or payment terminal, document preconditions and idempotency expectations in the endpoint KDoc and/or OpenAPI description.
