-- 04_reset_sequences.sql
-- Description: Resets PostgreSQL sequences after data load.

SELECT setval('user_master_user_id_seq', COALESCE((SELECT MAX(user_id) FROM user_master), 1), true);
SELECT setval('supplier_master_supplier_id_seq', COALESCE((SELECT MAX(supplier_id) FROM supplier_master), 1), true);
SELECT setval('supplier_audit_log_id_seq', COALESCE((SELECT MAX(id) FROM supplier_audit_log), 1), true);
SELECT setval('system_audit_trail_audit_id_seq', COALESCE((SELECT MAX(audit_id) FROM system_audit_trail), 1), true);
SELECT setval('purchase_order_po_id_seq', COALESCE((SELECT MAX(po_id) FROM purchase_order), 1), true);
SELECT setval('purchase_order_item_po_item_id_seq', COALESCE((SELECT MAX(po_item_id) FROM purchase_order_item), 1), true);
SELECT setval('goods_received_note_grn_id_seq', COALESCE((SELECT MAX(grn_id) FROM goods_received_note), 1), true);
SELECT setval('grn_item_grn_item_id_seq', COALESCE((SELECT MAX(grn_item_id) FROM grn_item), 1), true);
SELECT setval('bom_header_bom_id_seq', COALESCE((SELECT MAX(bom_id) FROM bom_header), 1), true);
SELECT setval('bom_details_bom_detail_id_seq', COALESCE((SELECT MAX(bom_detail_id) FROM bom_details), 1), true);
SELECT setval('production_order_production_id_seq', COALESCE((SELECT MAX(production_id) FROM production_order), 1), true);
SELECT setval('inventory_transaction_transaction_id_seq', COALESCE((SELECT MAX(transaction_id) FROM inventory_transaction), 1), true);
SELECT setval('production_material_consumption_consumption_id_seq', COALESCE((SELECT MAX(consumption_id) FROM production_material_consumption), 1), true);
SELECT setval('event_log_event_id_seq', COALESCE((SELECT MAX(event_id) FROM event_log), 1), true);
SELECT setval('qa_records_qa_id_seq', COALESCE((SELECT MAX(qa_id) FROM qa_records), 1), true);
SELECT setval('deviation_records_deviation_id_seq', COALESCE((SELECT MAX(deviation_id) FROM deviation_records), 1), true);
SELECT setval('capa_records_capa_id_seq', COALESCE((SELECT MAX(capa_id) FROM capa_records), 1), true);
SELECT setval('compliance_records_compliance_id_seq', COALESCE((SELECT MAX(compliance_id) FROM compliance_records), 1), true);
