package pharma.repository.jdbc;

import pharma.config.DatabaseConfig;

final class JdbcSqlDialect {
    enum Table {
        AI_DECISIONS("ai_decisions", "ai_decisions"),
        MATERIAL_MASTER("Material_Master", "material_master"),
        PURCHASE_ORDER("Purchase_Order", "purchase_order"),
        PURCHASE_ORDER_ITEM("PurchaseOrder_Item", "purchase_order_item"),
        STOCK_INVENTORY("Stock_Inventory", "stock_inventory"),
        SUPPLIER_MASTER("Supplier_Master", "supplier_master");

        private final String mysqlName;
        private final String postgresqlName;

        Table(String mysqlName, String postgresqlName) {
            this.mysqlName = mysqlName;
            this.postgresqlName = postgresqlName;
        }
    }

    private final DatabaseConfig.Dialect dialect;

    private JdbcSqlDialect(DatabaseConfig.Dialect dialect) {
        this.dialect = dialect;
    }

    static JdbcSqlDialect from(DatabaseConfig databaseConfig) {
        return forDialect(databaseConfig.getDialect());
    }

    static JdbcSqlDialect forDialect(DatabaseConfig.Dialect dialect) {
        return new JdbcSqlDialect(dialect);
    }

    String table(Table table) {
        if (dialect == DatabaseConfig.Dialect.POSTGRESQL) {
            return table.postgresqlName;
        }
        return table.mysqlName;
    }

    String trueLiteral() {
        return dialect == DatabaseConfig.Dialect.POSTGRESQL ? "TRUE" : "1";
    }

    String daysBeforeCurrentDate(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days must be zero or greater");
        }
        if (dialect == DatabaseConfig.Dialect.POSTGRESQL) {
            return "CURRENT_DATE - INTERVAL '" + days + " days'";
        }
        return "DATE_SUB(CURRENT_DATE, INTERVAL " + days + " DAY)";
    }

    String daysBeforeCurrentDateParameter() {
        if (dialect == DatabaseConfig.Dialect.POSTGRESQL) {
            return "CURRENT_DATE - (? * INTERVAL '1 day')";
        }
        return "DATE_SUB(CURRENT_DATE, INTERVAL ? DAY)";
    }
}
