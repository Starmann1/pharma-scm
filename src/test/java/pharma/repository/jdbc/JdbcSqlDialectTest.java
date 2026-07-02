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
}
