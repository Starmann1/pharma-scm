package pharma.repository.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import pharma.config.DatabaseConfig;

class JdbcSqlDialectTest {
    @Test
    void mysqlDialectKeepsLegacyTableNames() {
        JdbcSqlDialect dialect = JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.MYSQL);

        assertEquals("Supplier_Master", dialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER));
        assertEquals("PurchaseOrder_Item", dialect.table(JdbcSqlDialect.Table.PURCHASE_ORDER_ITEM));
        assertEquals("Stock_Inventory", dialect.table(JdbcSqlDialect.Table.STOCK_INVENTORY));
    }

    @Test
    void postgresqlDialectUsesSnakeCaseTableNames() {
        JdbcSqlDialect dialect = JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.POSTGRESQL);

        assertEquals("supplier_master", dialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER));
        assertEquals("purchase_order_item", dialect.table(JdbcSqlDialect.Table.PURCHASE_ORDER_ITEM));
        assertEquals("stock_inventory", dialect.table(JdbcSqlDialect.Table.STOCK_INVENTORY));
    }

    @Test
    void booleanLiteralMatchesDialect() {
        assertEquals("1", JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.MYSQL).trueLiteral());
        assertEquals("TRUE", JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.POSTGRESQL).trueLiteral());
    }

    @Test
    void dateArithmeticMatchesDialect() {
        assertEquals(
                "DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY)",
                JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.MYSQL).daysBeforeCurrentDate(90));
        assertEquals(
                "CURRENT_DATE - INTERVAL '90 days'",
                JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.POSTGRESQL).daysBeforeCurrentDate(90));
    }

    @Test
    void parameterizedDateArithmeticMatchesDialect() {
        assertEquals(
                "DATE_SUB(CURRENT_DATE, INTERVAL ? DAY)",
                JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.MYSQL).daysBeforeCurrentDateParameter());
        assertEquals(
                "CURRENT_DATE - (? * INTERVAL '1 day')",
                JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.POSTGRESQL).daysBeforeCurrentDateParameter());
    }

    @Test
    void dateArithmeticRejectsNegativeDays() {
        JdbcSqlDialect dialect = JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.POSTGRESQL);

        assertThrows(IllegalArgumentException.class, () -> dialect.daysBeforeCurrentDate(-1));
    }

    @Test
    void upsertStockSqlMatchesDialect() {
        assertEquals(
                "INSERT INTO Stock_Inventory (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date, qc_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity), qc_status = VALUES(qc_status)",
                JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.MYSQL).upsertStockSql());
        assertEquals(
                "INSERT INTO stock_inventory (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date, qc_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (material_code, batch_number, location_code) DO UPDATE SET quantity = stock_inventory.quantity + EXCLUDED.quantity, qc_status = EXCLUDED.qc_status",
                JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.POSTGRESQL).upsertStockSql());
    }

    @Test
    void fefoOrderByMatchesDialect() {
        assertEquals(
                " ORDER BY exp_date ASC, stock_id ASC",
                JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.MYSQL).fefoOrderByClause());
        assertEquals(
                " ORDER BY exp_date ASC NULLS LAST, stock_id ASC",
                JdbcSqlDialect.forDialect(DatabaseConfig.Dialect.POSTGRESQL).fefoOrderByClause());
    }
}
