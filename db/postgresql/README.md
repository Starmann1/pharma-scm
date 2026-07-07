# PostgreSQL Schema Design for v1.1

This directory contains PostgreSQL-only database artifacts for the v1.1 upgrade path.

These files must not be used by v1. The current v1 branch remains MySQL-based.

## Safety Boundary

- Do not edit or replace the existing MySQL scripts in the repository.
- Do not run these scripts against MySQL.
- Do not point the v1 runtime `.env` at PostgreSQL.
- Keep HikariCP as the only application connection pool.
- Keep UI and JADE/LangChain4j agents behind services, repositories, and `PharmaGateway`.

## Execution Order

Run these scripts against an empty PostgreSQL database provisioned for v1.1:

1. `schema/V001__baseline_schema.sql`
2. `schema/V002__agentic_tables.sql`
3. `schema/V003__observability_tables.sql`
4. `seed/V010__seed_roles_permissions.sql`
5. `seed/V011__seed_admin_user.sql`
6. `seed/V012__seed_locations.sql`
7. `seed/V013__seed_reference_data.sql`

## Data Migration (ETL)

For migrating existing data from MySQL (Phase 10), run the scripts in `db/postgresql/migration/` in this order:

1. Run `01_export_mysql.ps1` to dump MySQL tables to CSV files.
2. Run `02_transform.sql` to apply necessary transformations.
3. Run `03_load_postgresql.sql` using psql to load the CSVs.
4. Run `04_reset_sequences.sql` to fix identity sequences.
5. Run `05_validate.sql` to ensure data integrity.

## Naming Policy

PostgreSQL identifiers use lowercase snake_case. This avoids quoted mixed-case identifiers and keeps repository/JPA mappings predictable.

Examples:

| v1 MySQL table | v1.1 PostgreSQL table |
| --- | --- |
| `Supplier_Master` | `supplier_master` |
| `Material_Master` | `material_master` |
| `PurchaseOrder_Item` | `purchase_order_item` |
| `System_Audit_Trail` | `system_audit_trail` |
| `Stock_Inventory` | `stock_inventory` |

## Timestamp Policy

Tables use `created_at` and `updated_at` where mutable records need lifecycle tracking.

`updated_at` is maintained by the shared PostgreSQL trigger function in `V001__baseline_schema.sql`. Application code may still set `updated_at` explicitly when needed, but the database keeps a safe default.

## v1.1 Integration Notes

- Keep MySQL Connector/J for the existing default profile and add the PostgreSQL JDBC driver only on the v1.1 branch.
- Use the profile and environment keys documented in `runtime-configuration.md`.
- Use the repository dialect migration notes in `repository-dialect-migration.md` when converting data-access code.
- Use a v1.1-specific JDBC URL such as `jdbc:postgresql://localhost:5432/pharma_ims_v11`.
- Convert repository SQL before switching runtime traffic.
- Fix direct `DriverManager.getConnection(...)` UI usages before production use.
- Run backend and agent regression before starting the Vaadin UI migration.

## Review Checklist

- No `AUTO_INCREMENT`.
- No `USE database`.
- No `SET FOREIGN_KEY_CHECKS`.
- No MySQL session variables.
- No `ON DUPLICATE KEY UPDATE`.
- No hardcoded credentials.
- No changes to v1 MySQL runtime files.
