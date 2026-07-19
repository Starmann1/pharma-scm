-- V004__add_performance_indexes.sql
-- Description: Adds indexes on frequently joined/queried foreign keys and filtering columns to optimize database performance.

-- 1. Optimize BOM ingredient and recipe lookups
CREATE INDEX IF NOT EXISTS idx_bom_details_bom_id ON bom_details(bom_id);
CREATE INDEX IF NOT EXISTS idx_production_order_bom_id ON production_order(bom_id);

-- 2. Optimize production batch lineage lookups
CREATE INDEX IF NOT EXISTS idx_production_batch_order_id ON production_batch(production_order_id);

-- 3. Optimize inventory status filtering and FEFO queries
CREATE INDEX IF NOT EXISTS idx_stock_inventory_qc_status ON stock_inventory(qc_status);
CREATE INDEX IF NOT EXISTS idx_stock_inventory_material_qc ON stock_inventory(material_code, qc_status);
CREATE INDEX IF NOT EXISTS idx_stock_inventory_exp_date ON stock_inventory(exp_date);
