# scaffold

## Local H2 Console (development profile)

When running locally with:

```bash
mvn spring-boot:run
```

the H2 console is available at:

- `http://localhost:8080/h2-console`

Use these login values:

- **JDBC URL:** `jdbc:h2:file:./target/db-development`
- **Username:** `sa`
- **Password:** *(leave blank)*

## Database Migrations (Liquibase)

This project uses Liquibase for schema management and data migrations.

- Master changelog: `src/main/resources/db/migration/changelog-master.json`
- Individual migrations: `src/main/resources/db/migration/changes/*.json`

### How it runs

- **Development:** runs automatically on app startup against the development datasource.
- **Test:** runs automatically before tests against the test datasource.
- **Production:** runs automatically on app startup against PostgreSQL.

Spring is configured with:

- `spring.jpa.hibernate.ddl-auto=none`
- `spring.liquibase.change-log=db/migration/changelog-master.json`

### Existing pin -> userid migration

Current migrations include:

- Initial schema creation for current tables.
- Legacy migration from pin-based schema to userid-based schema.
- Legacy PostgreSQL constraint-name normalization.

### Adding a new migration

1. Add a new JSON changelog file in `src/main/resources/db/migration/changes/`.
2. Use an incremental numeric prefix (for example `004-...json`).
3. Prefer preconditions (`onFail: MARK_RAN`) for compatibility with partially migrated environments.
4. Start the app or run tests to validate the migration.

### Troubleshooting migrations

#### Check migration history

- Inspect Liquibase tracking tables in your database:
	- `DATABASECHANGELOG`
	- `DATABASECHANGELOGLOCK`

In local development, you can view these tables from the H2 console.

#### Reset local H2 and rerun from scratch

For the development profile, the local H2 file lives under `target/`.

Use the helper script:

```bash
./scripts/reset-local-h2.sh
```

To reset and immediately start the app:

```bash
./scripts/reset-local-h2.sh --run
```

Manual equivalent:

1. Stop the app.
2. Delete local H2 files:
- `target/db-development.mv.db`
- `target/db-development.trace.db` (if present)
3. Start the app again with `mvn spring-boot:run`.

Liquibase will recreate the schema and reapply migrations.

#### Common failure patterns

- **Checksum mismatch:** if a previously applied changelog file was edited, Liquibase will fail validation. Preferred fix is to add a new migration file instead of modifying an already-applied one.
- **Partially migrated environment:** use strong preconditions with `onFail: MARK_RAN` in migration files so old/new states can both be handled safely.
- **Locked changelog:** if `DATABASECHANGELOGLOCK` is stuck after an interrupted run, clear the lock row in your local/dev database and restart.
