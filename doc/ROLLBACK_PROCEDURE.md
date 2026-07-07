# Rollback Procedure: v1.1 PostgreSQL Runtime Cutover

In the event that the PostgreSQL cutover (Phase 12) needs to be rolled back to the legacy MySQL environment in production, follow these steps.

## 1. Environment Variable Reversion
Update the `.env` file or environment variables on the host machine to point back to the legacy MySQL profile:

```env
# Change from postgresql to mysql
PHARMA_DB_PROFILE=mysql
```

Ensure that the legacy MySQL connection variables are still present and correct:
```env
DB_URL=jdbc:mysql://localhost:3306/pharma_ims
DB_USER=root
DB_PASS=legacy_password
```

## 2. Restart Application
Restart the Pharma SCM java process.
The application will automatically detect `PHARMA_DB_PROFILE=mysql`, instantiate the legacy Swing UI pointing to `*JdbcRepository` classes configured for MySQL dialects, and reconnect to the legacy database.

## 3. Post-Rollback Validation
Once restarted, verify the following:
1. Log into the application with legacy admin credentials.
2. Verify that Materials and Inventory screens correctly load data from the MySQL instance.
3. Check application logs to ensure `AgentGateway` and JADE `CoordinatorAgent` have successfully initialized without database connection errors.
4. Perform a test workflow (e.g. create a Purchase Order) to ensure writes are successfully routing to MySQL.

## Note on Data Discrepancy
Any data written to PostgreSQL while it was active will **not** automatically sync back to MySQL. If significant time has passed, a reverse-migration of delta records from PostgreSQL back to MySQL may be required manually via SQL scripts before performing the application rollback.
