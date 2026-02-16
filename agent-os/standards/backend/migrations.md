## Database migrations (Liquibase)

This project uses **Liquibase** changelogs (see `projects/lpg-ehl/lpg-ehl-service/src/main/resources/db/changelog/`).

### Rules

- **Never modify an applied changeset**: Add a new changeset for any evolution (treat history as immutable).
- **Prefer small, focused changesets**: One logical change per changeset.
- **Use clear IDs and filenames**: IDs should be unique and descriptive; filenames should explain intent (e.g. `010-add-foo-bar.yaml`).
- **Keep compatibility in mind**: Prefer additive, backwards-compatible changes when possible (especially when the edge can be deployed/updated gradually).
- **Schema vs data**: Prefer separating schema changes from data backfills. If backfills are required, document runtime impact and failure modes.
- **Indexes and locking**: Be careful adding indexes on large tables; document expected lock/impact. (Edge deployments may still have strict uptime requirements.)

### Review checklist

- [ ] Changeset is additive or clearly justified
- [ ] Existing changesets were not edited
- [ ] Rollback is included when it is cheap and unambiguous (don’t invent unsafe rollbacks)
- [ ] Production impact is documented (locks, time, disk)
