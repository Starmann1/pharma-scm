-- 05_validate.sql
-- Description: Validates data migration success and checks for orphaned Foreign Keys.

\echo 'Validating row counts...'
SELECT 'user_master' AS table_name, count(*) AS row_count FROM user_master UNION ALL
SELECT 'supplier_master', count(*) FROM supplier_master UNION ALL
SELECT 'material_master', count(*) FROM material_master UNION ALL
SELECT 'purchase_order', count(*) FROM purchase_order UNION ALL
SELECT 'goods_received_note', count(*) FROM goods_received_note UNION ALL
SELECT 'stock_inventory', count(*) FROM stock_inventory UNION ALL
SELECT 'production_order', count(*) FROM production_order UNION ALL
SELECT 'production_batch', count(*) FROM production_batch;

\echo 'Checking for orphaned Foreign Keys (Sample)...'

-- Check if any GRNs point to a non-existent PO
SELECT COUNT(*) AS orphaned_grns
FROM goods_received_note g
LEFT JOIN purchase_order p ON g.po_id = p.po_id
WHERE g.po_id IS NOT NULL AND p.po_id IS NULL;

-- Check if any Batch Genealogy records point to non-existent batches
SELECT COUNT(*) AS orphaned_genealogy_records
FROM batch_genealogy bg
LEFT JOIN production_batch pb1 ON bg.parent_batch_no = pb1.batch_no
LEFT JOIN production_batch pb2 ON bg.child_batch_no = pb2.batch_no
WHERE pb1.batch_no IS NULL OR pb2.batch_no IS NULL;
