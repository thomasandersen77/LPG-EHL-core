## Models and persistence (Kotlin + Spring Data JPA)

In this repo, persistent domain data primarily lives in `projects/lpg-ehl/lpg-ehl-service/`.

### Entity rules

- **Don’t leak entities across the API boundary**: Controllers should expose DTOs; map to/from entities in the service layer.
- **Prefer explicit types**:
  - Use `UUID` identifiers where appropriate.
  - Store enums as strings (`@Enumerated(EnumType.STRING)`) unless there is a strong reason not to.
- **Constraints are part of the design**: Use NOT NULL/UNIQUE/FKs to enforce invariants when the invariant truly belongs in the DB.
- **Timestamps**: Model time explicitly (`Instant`/`OffsetDateTime`/`LocalDateTime`) and be consistent; avoid “magic” string timestamps.
- **Relationships**: Keep JPA relationships intentional; avoid accidental eager loading and N+1s. Prefer `@ManyToOne(fetch = LAZY)` and explicit fetch plans when needed.

### Domain vs persistence

- **Business rules live in services**: Entities should remain simple and stable; orchestration and validation belongs in services.
- **Protocol core stays clean**: `lpg-ehl-core` models should not depend on JPA/Spring.

### Review checklist

- [ ] Entity is in the correct module (`lpg-ehl-service`)
- [ ] DTO mapping exists at boundaries (controller/service)
- [ ] Enum persistence is safe (`STRING`)
- [ ] Relationship fetch strategy is intentional
