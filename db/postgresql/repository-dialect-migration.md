# Repository Dialect Migration

Phase 3 introduces a repository-level SQL dialect foundation for the v1.1 PostgreSQL upgrade.

This phase does not require PostgreSQL to be installed. It compiles and unit-tests SQL dialect selection without opening a live database connection.

## What Phase 3 Covers

- Centralizes table-name mapping in `JdbcSqlDialect`.
- Keeps MySQL table names for the default v1 profile.
- Uses lowercase snake_case PostgreSQL table names for the v1.1 profile.
- Isolates boolean literal differences used by JDBC SQL.
- Isolates MySQL `DATE_SUB(...)` versus PostgreSQL interval arithmetic.
- Converts the smaller agent-facing JDBC repositories to use the dialect helper.

Converted repositories:

- `AIDecisionJdbcRepository`
- `InventoryJdbcRepository`
- `MaterialJdbcRepository`
- `RiskJdbcRepository`
- `SupplierJdbcRepository`

## What Phase 3 Does Not Cover

- It does not convert the large legacy SQL surface still inside `DatabaseService`.
- It does not refactor Swing UI classes that still call `DatabaseService` directly.
- It does not run PostgreSQL integration tests.
- It does not copy data from MySQL to PostgreSQL.

## Current Runtime Meaning

When `PHARMA_DB_PROFILE` is unset or set to `mysql`, repositories continue using the existing MySQL table names.

When `PHARMA_DB_PROFILE=v1.1`, converted repositories target the PostgreSQL schema names introduced under `db/postgresql/schema/`.

## Next Required Migration Work

Before v1.1 can be considered PostgreSQL-runtime-complete:

1. Convert or retire legacy `DatabaseService` SQL methods used by the UI and agents.
2. Remove remaining direct `DriverManager.getConnection(...)` usages from Swing classes.
3. Add PostgreSQL seed/reference data scripts.
4. Run schema validation against a local PostgreSQL database.
5. Run workflow smoke tests for login, inventory, supplier ranking, procurement, QA, risk, and AI decision review.
