# v1.1 PostgreSQL Runtime Configuration

Phase 2 adds a PostgreSQL-capable runtime configuration path for the v1.1 branch.

The default remains MySQL so existing v1-style local runs are not redirected accidentally.

## Profiles

Set `PHARMA_DB_PROFILE` to choose the database profile:

| Value | Dialect | Runtime keys |
| --- | --- | --- |
| unset, `mysql`, `v1` | MySQL | `DB_URL`, `DB_USER`, `DB_PASS` |
| `postgres`, `postgresql`, `pg`, `v1.1`, `v11` | PostgreSQL | `POSTGRES_DB_URL`, `POSTGRES_DB_USER`, `POSTGRES_DB_PASS` |

## MySQL Default

Existing v1 configuration continues to work:

```properties
DB_URL=jdbc:mysql://localhost:3306/pharma_ims
DB_USER=root
DB_PASS=yourpassword
```

## PostgreSQL v1.1

Use separate PostgreSQL keys so v1 MySQL credentials and v1.1 PostgreSQL credentials can coexist locally:

```properties
PHARMA_DB_PROFILE=v1.1
POSTGRES_DB_URL=jdbc:postgresql://localhost:5432/pharma_ims_v11
POSTGRES_DB_USER=pharma_v11
POSTGRES_DB_PASS=change-me
```

## Safety Notes

- Do not replace `DB_URL` with PostgreSQL values while running v1.
- Do not point v1.1 at the v1 MySQL schema.
- HikariCP remains the only application connection pool.
- PostgreSQL startup skips the MySQL-only optional schema patching in `DatabaseService`.
- Repository SQL conversion is not part of Phase 2. Most repositories still contain MySQL table names and will require a dedicated dialect migration phase before PostgreSQL runtime traffic is considered complete.
