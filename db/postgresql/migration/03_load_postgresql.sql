-- 03_load_postgresql.sql
-- Description: Loads CSV files into PostgreSQL in FK-safe order.

\echo 'Loading Role_Master...'
\copy role_master FROM 'export/Role_Master.csv' DELIMITER ',' CSV;

\echo 'Loading Permission_Master...'
\copy permission_master FROM 'export/Permission_Master.csv' DELIMITER ',' CSV;

\echo 'Loading Role_Permission...'
\copy role_permission FROM 'export/Role_Permission.csv' DELIMITER ',' CSV;

\echo 'Loading User_Master...'
\copy user_master FROM 'export/User_Master.csv' DELIMITER ',' CSV;

\echo 'Loading System_Audit_Trail...'
\copy system_audit_trail FROM 'export/System_Audit_Trail.csv' DELIMITER ',' CSV;

\echo 'Loading Location_Master...'
\copy location_master FROM 'export/Location_Master.csv' DELIMITER ',' CSV;

\echo 'Loading Supplier_Master...'
\copy supplier_master FROM 'export/Supplier_Master.csv' DELIMITER ',' CSV;

\echo 'Loading Supplier_Audit_Log...'
\copy supplier_audit_log FROM 'export/Supplier_Audit_Log.csv' DELIMITER ',' CSV;

\echo 'Loading Material_Master...'
\copy material_master FROM 'export/Material_Master.csv' DELIMITER ',' CSV;

\echo 'Loading Purchase_Order...'
\copy purchase_order FROM 'export/Purchase_Order.csv' DELIMITER ',' CSV;

\echo 'Loading PurchaseOrder_Item...'
\copy purchase_order_item FROM 'export/PurchaseOrder_Item.csv' DELIMITER ',' CSV;

\echo 'Loading Goods_Received_Note...'
\copy goods_received_note FROM 'export/Goods_Received_Note.csv' DELIMITER ',' CSV;

\echo 'Loading GRN_Item...'
\copy grn_item FROM 'export/GRN_Item.csv' DELIMITER ',' CSV;

\echo 'Loading BOM_Header...'
\copy bom_header FROM 'export/BOM_Header.csv' DELIMITER ',' CSV;

\echo 'Loading BOM_Details...'
\copy bom_details FROM 'export/BOM_Details.csv' DELIMITER ',' CSV;

\echo 'Loading Production_Order...'
\copy production_order FROM 'export/Production_Order.csv' DELIMITER ',' CSV;

\echo 'Loading Production_Batch...'
\copy production_batch FROM 'export/Production_Batch.csv' DELIMITER ',' CSV;

\echo 'Loading Stock_Inventory...'
\copy stock_inventory FROM 'export/Stock_Inventory.csv' DELIMITER ',' CSV;

\echo 'Loading Inventory_Transaction...'
\copy inventory_transaction FROM 'export/Inventory_Transaction.csv' DELIMITER ',' CSV;

\echo 'Loading Production_Material_Consumption...'
\copy production_material_consumption FROM 'export/Production_Material_Consumption.csv' DELIMITER ',' CSV;

\echo 'Loading Batch_Genealogy...'
\copy batch_genealogy FROM 'export/Batch_Genealogy.csv' DELIMITER ',' CSV;

\echo 'Loading Event_Log...'
\copy event_log FROM 'export/Event_Log.csv' DELIMITER ',' CSV;

\echo 'Loading QA_Records...'
\copy qa_records FROM 'export/QA_Records.csv' DELIMITER ',' CSV;

\echo 'Loading Deviation_Records...'
\copy deviation_records FROM 'export/Deviation_Records.csv' DELIMITER ',' CSV;

\echo 'Loading CAPA_Records...'
\copy capa_records FROM 'export/CAPA_Records.csv' DELIMITER ',' CSV;

\echo 'Loading Compliance_Records...'
\copy compliance_records FROM 'export/Compliance_Records.csv' DELIMITER ',' CSV;

\echo 'Data load complete.'
