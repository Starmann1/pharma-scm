-- PostgreSQL v1.1 foreign-key integrity smoke checks.
-- These should all return zero rows after migration.

SELECT 'material_master.preferred_supplier_id' AS check_name, COUNT(*) AS orphan_count
FROM material_master m
LEFT JOIN supplier_master s ON s.supplier_id = m.preferred_supplier_id
WHERE m.preferred_supplier_id IS NOT NULL AND s.supplier_id IS NULL
UNION ALL
SELECT 'purchase_order.supplier_id', COUNT(*)
FROM purchase_order po
LEFT JOIN supplier_master s ON s.supplier_id = po.supplier_id
WHERE s.supplier_id IS NULL
UNION ALL
SELECT 'purchase_order_item.po_id', COUNT(*)
FROM purchase_order_item poi
LEFT JOIN purchase_order po ON po.po_id = poi.po_id
WHERE po.po_id IS NULL
UNION ALL
SELECT 'purchase_order_item.material_code', COUNT(*)
FROM purchase_order_item poi
LEFT JOIN material_master m ON m.material_code = poi.material_code
WHERE m.material_code IS NULL
UNION ALL
SELECT 'goods_received_note.po_id', COUNT(*)
FROM goods_received_note grn
LEFT JOIN purchase_order po ON po.po_id = grn.po_id
WHERE po.po_id IS NULL
UNION ALL
SELECT 'grn_item.grn_id', COUNT(*)
FROM grn_item gi
LEFT JOIN goods_received_note grn ON grn.grn_id = gi.grn_id
WHERE grn.grn_id IS NULL
UNION ALL
SELECT 'grn_item.material_code', COUNT(*)
FROM grn_item gi
LEFT JOIN material_master m ON m.material_code = gi.material_code
WHERE m.material_code IS NULL
UNION ALL
SELECT 'stock_inventory.material_code', COUNT(*)
FROM stock_inventory si
LEFT JOIN material_master m ON m.material_code = si.material_code
WHERE m.material_code IS NULL
UNION ALL
SELECT 'stock_inventory.location_code', COUNT(*)
FROM stock_inventory si
LEFT JOIN location_master l ON l.location_code = si.location_code
WHERE l.location_code IS NULL
UNION ALL
SELECT 'bom_header.material_code', COUNT(*)
FROM bom_header bh
LEFT JOIN material_master m ON m.material_code = bh.material_code
WHERE m.material_code IS NULL
UNION ALL
SELECT 'bom_details.bom_id', COUNT(*)
FROM bom_details bd
LEFT JOIN bom_header bh ON bh.bom_id = bd.bom_id
WHERE bh.bom_id IS NULL
UNION ALL
SELECT 'bom_details.ingredient_material_code', COUNT(*)
FROM bom_details bd
LEFT JOIN material_master m ON m.material_code = bd.ingredient_material_code
WHERE m.material_code IS NULL
UNION ALL
SELECT 'production_order.bom_id', COUNT(*)
FROM production_order po
LEFT JOIN bom_header bh ON bh.bom_id = po.bom_id
WHERE bh.bom_id IS NULL;
