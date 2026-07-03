package pharma.repository.jdbc;

import pharma.config.DatabaseConfig;

final class JdbcSqlDialect {
    enum Table {
        AI_DECISIONS("ai_decisions", "ai_decisions"),
        EVENT_LOG("event_log", "event_log"),
        GOODS_RECEIVED_NOTE("Goods_Received_Note", "goods_received_note"),
        GRN_ITEM("GRN_Item", "grn_item"),
        INVENTORY_TRANSACTION("inventory_transaction", "inventory_transaction"),
        MATERIAL_MASTER("Material_Master", "material_master"),
        PURCHASE_ORDER("Purchase_Order", "purchase_order"),
        PURCHASE_ORDER_ITEM("PurchaseOrder_Item", "purchase_order_item"),
        STOCK_INVENTORY("Stock_Inventory", "stock_inventory"),
        SUPPLIER_MASTER("Supplier_Master", "supplier_master"),
        SYSTEM_AUDIT_TRAIL("System_Audit_Trail", "system_audit_trail");

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

    /**
     * Returns the SQL expression for "current timestamp" — portable across MySQL and PostgreSQL.
     */
    String nowExpression() {
        return "NOW()";
    }

    /**
     * Returns the SQL for upserting into stock_inventory.
     * MySQL uses ON DUPLICATE KEY UPDATE; PostgreSQL uses ON CONFLICT ... DO UPDATE.
     */
    String upsertStockSql() {
        String table = table(Table.STOCK_INVENTORY);
        String insert = "INSERT INTO " + table
                + " (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date, qc_status)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        if (dialect == DatabaseConfig.Dialect.POSTGRESQL) {
            return insert
                    + " ON CONFLICT (material_code, batch_number, location_code)"
                    + " DO UPDATE SET quantity = " + table + ".quantity + EXCLUDED.quantity,"
                    + " qc_status = EXCLUDED.qc_status";
        }
        return insert
                + " ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity),"
                + " qc_status = VALUES(qc_status)";
    }
}
