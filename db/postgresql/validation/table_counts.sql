-- PostgreSQL v1.1 table-count baseline query.
-- Use after schema creation and after data migration to compare expected row counts.

SELECT 'supplier_master' AS table_name, COUNT(*) AS row_count FROM supplier_master
UNION ALL SELECT 'supplier_audit_log', COUNT(*) FROM supplier_audit_log
UNION ALL SELECT 'material_master', COUNT(*) FROM material_master
UNION ALL SELECT 'location_master', COUNT(*) FROM location_master
UNION ALL SELECT 'role_master', COUNT(*) FROM role_master
UNION ALL SELECT 'permission_master', COUNT(*) FROM permission_master
UNION ALL SELECT 'role_permission', COUNT(*) FROM role_permission
UNION ALL SELECT 'user_master', COUNT(*) FROM user_master
UNION ALL SELECT 'system_audit_trail', COUNT(*) FROM system_audit_trail
UNION ALL SELECT 'purchase_order', COUNT(*) FROM purchase_order
UNION ALL SELECT 'purchase_order_item', COUNT(*) FROM purchase_order_item
UNION ALL SELECT 'goods_received_note', COUNT(*) FROM goods_received_note
UNION ALL SELECT 'grn_item', COUNT(*) FROM grn_item
UNION ALL SELECT 'bom_header', COUNT(*) FROM bom_header
UNION ALL SELECT 'bom_details', COUNT(*) FROM bom_details
UNION ALL SELECT 'production_order', COUNT(*) FROM production_order
UNION ALL SELECT 'stock_inventory', COUNT(*) FROM stock_inventory
UNION ALL SELECT 'inventory_transaction', COUNT(*) FROM inventory_transaction
UNION ALL SELECT 'production_material_consumption', COUNT(*) FROM production_material_consumption
UNION ALL SELECT 'production_batch', COUNT(*) FROM production_batch
UNION ALL SELECT 'batch_genealogy', COUNT(*) FROM batch_genealogy
UNION ALL SELECT 'event_log', COUNT(*) FROM event_log
UNION ALL SELECT 'qa_records', COUNT(*) FROM qa_records
UNION ALL SELECT 'deviation_records', COUNT(*) FROM deviation_records
UNION ALL SELECT 'capa_records', COUNT(*) FROM capa_records
UNION ALL SELECT 'compliance_records', COUNT(*) FROM compliance_records
UNION ALL SELECT 'supplier_delivery_history', COUNT(*) FROM supplier_delivery_history
UNION ALL SELECT 'risk_assessments', COUNT(*) FROM risk_assessments
UNION ALL SELECT 'ai_decisions', COUNT(*) FROM ai_decisions
UNION ALL SELECT 'knowledge_documents', COUNT(*) FROM knowledge_documents
UNION ALL SELECT 'document_chunks', COUNT(*) FROM document_chunks
UNION ALL SELECT 'agent_events', COUNT(*) FROM agent_events
UNION ALL SELECT 'agent_event_details', COUNT(*) FROM agent_event_details
ORDER BY table_name;
