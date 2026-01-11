/*
-- refresh_production_data_100.sql
-- Description: Wipes database (except Supplier_Master) and adds exactly 100 records.
-- Purpose: Refresh production panel data.
*/

USE pharma_ims;

-- 1. Disable Foreign Key Checks to allow truncation
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Truncate Tables
TRUNCATE TABLE System_Audit_Trail;
TRUNCATE TABLE Production_Order;
TRUNCATE TABLE BOM_Details;
TRUNCATE TABLE BOM_Header;
TRUNCATE TABLE GRN_Item;
TRUNCATE TABLE Goods_Received_Note;
TRUNCATE TABLE PurchaseOrder_Item;
TRUNCATE TABLE Purchase_Order;
TRUNCATE TABLE Stock_Inventory;
TRUNCATE TABLE Drug_Master;
TRUNCATE TABLE Location_Master;
TRUNCATE TABLE User_Master;

-- 3. Enable Foreign Key Checks
SET FOREIGN_KEY_CHECKS = 1;

-- 4. Insert Exactly 100 Records

-- User_Master (3 records)
INSERT INTO User_Master (user_id, username, password_hash, full_name, role) VALUES
(1, 'admin', 'adminpass', 'System Administrator', 'Admin'),
(2, 'prod_mgr', 'prodpass', 'John Production', 'Production Manager'),
(3, 'qa_lead', 'qapass', 'Sarah Quality', 'QA Analyst');

-- Location_Master (5 records)
INSERT INTO Location_Master (location_code, location_name, description, capacity) VALUES
('LOC-WH01', 'Main Warehouse', 'Central storage for raw materials', 10000),
('LOC-PROD', 'Production Floor', 'Manufacturing zone', 0),
('LOC-COLD', 'Cold Storage', 'Temperature controlled unit', 2000),
('LOC-PACK', 'Packaging Area', 'Final packing and labeling', 5000),
('LOC-QC', 'QC Laboratory', 'Quality control and testing', 100);

-- Drug_Master (20 records)
-- 8 Raw Materials (APIs)
-- 2 Packaging Materials
-- 10 Finished Goods
INSERT INTO Drug_Master (material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, is_active, material_type, unit_of_measure, preferred_supplier_id) VALUES
-- Raw Materials
('RM-ATM-001', 'Atorvastatin API', 'Atorvastatin', 'API Corp', 'Powder', '99%', 'Schedule H', 1, 'RAW_MATERIAL', 'kg', 1),
('RM-PCM-001', 'Paracetamol API', 'Paracetamol', 'API Corp', 'Powder', '98%', 'OTC', 1, 'RAW_MATERIAL', 'kg', 1),
('RM-IBU-001', 'Ibuprofen API', 'Ibuprofen', 'BioChem', 'Powder', '99.5%', 'Schedule H', 1, 'RAW_MATERIAL', 'kg', 2),
('RM-AMX-001', 'Amoxicillin API', 'Amoxicillin', 'BioChem', 'Powder', '99%', 'Schedule H', 1, 'RAW_MATERIAL', 'kg', 2),
('RM-MET-001', 'Metformin API', 'Metformin', 'PureAPI', 'Powder', '99%', 'Schedule H', 1, 'RAW_MATERIAL', 'kg', 3),
('RM-LZD-001', 'Linezolid API', 'Linezolid', 'PureAPI', 'Powder', '99.9%', 'Schedule H', 1, 'RAW_MATERIAL', 'kg', 3),
('RM-CPM-001', 'Chlorpheniramine API', 'CPM', 'ChemPlus', 'Powder', '99%', 'Schedule H', 1, 'RAW_MATERIAL', 'kg', 1),
('RM-STARCH', 'Corn Starch', 'Starch', 'Excipients Ltd', 'Powder', 'USP', 'OTC', 1, 'EXCIPIENT', 'kg', 2),
-- Packaging
('PKG-BOT-01', 'HDPE Bottle 100ml', 'Bottle', 'PackCo', 'Container', '100ml', 'OTC', 1, 'PACKAGING', 'Unit', 3),
('PKG-BLIS-01', 'Alu-Alu Blister', 'Blister', 'PackCo', 'Foil', '10-count', 'OTC', 1, 'PACKAGING', 'Unit', 3),
-- Finished Goods
('FG-LIP-10', 'Lipicure 10mg', 'Atorvastatin', 'Internal', 'Tablet', '10mg', 'Schedule H', 1, 'FINISHED_GOOD', 'Unit', NULL),
('FG-PCM-500', 'Panadol 500mg', 'Paracetamol', 'Internal', 'Tablet', '500mg', 'OTC', 1, 'FINISHED_GOOD', 'Unit', NULL),
('FG-IBU-400', 'Brufen 400mg', 'Ibuprofen', 'Internal', 'Tablet', '400mg', 'Schedule H', 1, 'FINISHED_GOOD', 'Unit', NULL),
('FG-AMX-250', 'Mox 250mg', 'Amoxicillin', 'Internal', 'Capsule', '250mg', 'Schedule H', 1, 'FINISHED_GOOD', 'Unit', NULL),
('FG-MET-500', 'Glycomet 500mg', 'Metformin', 'Internal', 'Tablet', '500mg', 'Schedule H', 1, 'FINISHED_GOOD', 'Unit', NULL),
('FG-LZD-600', 'Zyvox 600mg', 'Linezolid', 'Internal', 'Tablet', '600mg', 'Schedule H', 1, 'FINISHED_GOOD', 'Unit', NULL),
('FG-SOL-100', 'Solvin Syrup', 'CPM+PCM', 'Internal', 'Syrup', '100ml', 'OTC', 1, 'FINISHED_GOOD', 'Unit', NULL),
('FG-LIP-20', 'Lipicure 20mg', 'Atorvastatin', 'Internal', 'Tablet', '20mg', 'Schedule H', 1, 'FINISHED_GOOD', 'Unit', NULL),
('FG-PCM-650', 'Dolo 650mg', 'Paracetamol', 'Internal', 'Tablet', '650mg', 'OTC', 1, 'FINISHED_GOOD', 'Unit', NULL),
('FG-IBU-200', 'Brufen 200mg', 'Ibuprofen', 'Internal', 'Tablet', '200mg', 'Schedule H', 1, 'FINISHED_GOOD', 'Unit', NULL);

-- BOM_Header (10 records)
INSERT INTO BOM_Header (bom_id, material_code, version_number, is_active, effective_date, description) VALUES
(1, 'FG-LIP-10', 1, 1, '2026-01-01', 'Standard formulation for Lipicure 10mg'),
(2, 'FG-PCM-500', 1, 1, '2026-01-01', 'Standard formulation for Panadol 500mg'),
(3, 'FG-IBU-400', 1, 1, '2026-01-01', 'Standard formulation for Brufen 400mg'),
(4, 'FG-AMX-250', 1, 1, '2026-01-01', 'Standard formulation for Mox 250mg'),
(5, 'FG-MET-500', 1, 1, '2026-01-01', 'Standard formulation for Glycomet 500mg'),
(6, 'FG-LZD-600', 1, 1, '2026-01-01', 'Standard formulation for Zyvox 600mg'),
(7, 'FG-SOL-100', 1, 1, '2026-01-01', 'Standard formulation for Solvin Syrup'),
(8, 'FG-LIP-20', 1, 1, '2026-01-01', 'Enhanced strength Lipicure 20mg'),
(9, 'FG-PCM-650', 1, 1, '2026-01-01', 'High strength Panadol 650mg'),
(10, 'FG-IBU-200', 1, 1, '2026-01-01', 'Lower strength Brufen 200mg');

-- BOM_Details (20 records)
INSERT INTO BOM_Details (bom_id, ingredient_material_code, required_qty, uom, sequence_number) VALUES
(1, 'RM-ATM-001', 0.0100, 'kg', 1), (1, 'RM-STARCH', 0.1000, 'kg', 2),
(2, 'RM-PCM-001', 0.5000, 'kg', 1), (2, 'RM-STARCH', 0.2000, 'kg', 2),
(3, 'RM-IBU-001', 0.4000, 'kg', 1), (3, 'RM-STARCH', 0.1500, 'kg', 2),
(4, 'RM-AMX-001', 0.2500, 'kg', 1), (4, 'RM-STARCH', 0.1000, 'kg', 2),
(5, 'RM-MET-001', 0.5000, 'kg', 1), (5, 'RM-STARCH', 0.2000, 'kg', 2),
(6, 'RM-LZD-001', 0.6000, 'kg', 1), (6, 'RM-STARCH', 0.2500, 'kg', 2),
(7, 'RM-CPM-001', 0.0040, 'kg', 1), (7, 'PKG-BOT-01', 1.0000, 'Unit', 2),
(8, 'RM-ATM-001', 0.0200, 'kg', 1), (8, 'RM-STARCH', 0.1200, 'kg', 2),
(9, 'RM-PCM-001', 0.6500, 'kg', 1), (9, 'RM-STARCH', 0.2500, 'kg', 2),
(10, 'RM-IBU-001', 0.2000, 'kg', 1), (10, 'RM-STARCH', 0.1000, 'kg', 2);

-- Production_Order (15 records)
INSERT INTO Production_Order (order_id, batch_number, bom_id, planned_qty, actual_qty, status, production_date, completed_date, created_by) VALUES
(1, 'BT-LIP-101', 1, 1000, 1000, 'Released', '2026-01-01', '2026-01-02', 1),
(2, 'BT-PCM-501', 2, 5000, 5000, 'Released', '2026-01-01', '2026-01-02', 1),
(3, 'BT-IBU-401', 3, 2000, NULL, 'In-Production', '2026-01-05', NULL, 2),
(4, 'BT-AMX-251', 4, 3000, 3000, 'Quality-Testing', '2026-01-06', '2026-01-07', 2),
(5, 'BT-MET-501', 5, 4000, NULL, 'Planned', '2026-01-10', NULL, 1),
(6, 'BT-LZD-601', 6, 500, 500, 'Rejected', '2026-01-02', '2026-01-03', 3),
(7, 'BT-SOL-101', 7, 1000, 1000, 'Released', '2026-01-02', '2026-01-03', 1),
(8, 'BT-LIP-201', 8, 800, NULL, 'In-Production', '2026-01-06', NULL, 2),
(9, 'BT-PCM-651', 9, 2500, 2500, 'Quality-Testing', '2026-01-06', '2026-01-07', 2),
(10, 'BT-IBU-201', 10, 1500, NULL, 'Planned', '2026-01-12', NULL, 1),
(11, 'BT-LIP-102', 1, 1200, NULL, 'Planned', '2026-01-15', NULL, 2),
(12, 'BT-PCM-502', 2, 6000, NULL, 'Planned', '2026-01-16', NULL, 2),
(13, 'BT-AMX-252', 4, 3500, NULL, 'Planned', '2026-01-17', NULL, 1),
(14, 'BT-MET-502', 5, 4500, NULL, 'Planned', '2026-01-18', NULL, 1),
(15, 'BT-LZD-602', 6, 600, NULL, 'Planned', '2026-01-19', NULL, 1);

-- Stock_Inventory (10 records)
INSERT INTO Stock_Inventory (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date, qc_status) VALUES
('RM-ATM-001', 'LOC-WH01', 'RM-ATM-B1', 100, 500.00, '2025-10-01', '2027-10-01', 'RELEASED'),
('RM-PCM-001', 'LOC-WH01', 'RM-PCM-B1', 500, 200.00, '2025-11-01', '2027-11-01', 'RELEASED'),
('RM-IBU-001', 'LOC-WH01', 'RM-IBU-B1', 300, 350.00, '2025-09-01', '2027-09-01', 'RELEASED'),
('RM-AMX-001', 'LOC-WH01', 'RM-AMX-B1', 400, 450.00, '2025-12-01', '2027-12-01', 'RELEASED'),
('RM-MET-001', 'LOC-WH01', 'RM-MET-B1', 600, 150.00, '2025-10-15', '2027-10-15', 'RELEASED'),
('RM-LZD-001', 'LOC-WH01', 'RM-LZD-B1', 50, 1200.00, '2025-11-20', '2027-11-20', 'RELEASED'),
('RM-CPM-001', 'LOC-WH01', 'RM-CPM-B1', 80, 800.00, '2025-12-05', '2028-12-05', 'RELEASED'),
('RM-STARCH', 'LOC-WH01', 'RM-ST-B1', 1000, 50.00, '2025-01-01', '2028-01-01', 'RELEASED'),
('PKG-BOT-01', 'LOC-WH01', 'PKG-BOT-B1', 5000, 2.00, '2025-01-01', '2030-01-01', 'RELEASED'),
('PKG-BLIS-01', 'LOC-WH01', 'PKG-BLI-B1', 10000, 0.50, '2025-01-01', '2030-01-01', 'RELEASED');

-- Purchase_Order (5 records)
INSERT INTO Purchase_Order (po_id, supplier_id, order_date, expected_date, total_amount, status) VALUES
(1, 1, '2026-01-01', '2026-01-05', 5000.00, 'Received'),
(2, 2, '2026-01-02', '2026-01-06', 3500.00, 'Received'),
(3, 3, '2026-01-03', '2026-01-07', 2000.00, 'Pending'),
(4, 1, '2026-01-04', '2026-01-08', 4500.00, 'Shipped'),
(5, 2, '2026-01-05', '2026-01-09', 1200.00, 'Pending');

-- PurchaseOrder_Item (5 records)
INSERT INTO PurchaseOrder_Item (po_id, drug_id, quantity, unit_price) VALUES
(1, 'RM-ATM-001', 10, 500.00),
(2, 'RM-IBU-001', 10, 350.00),
(3, 'PKG-BOT-01', 1000, 2.00),
(4, 'RM-PCM-001', 20, 225.00),
(5, 'RM-STARCH', 24, 50.00);

-- Goods_Received_Note (2 records)
INSERT INTO Goods_Received_Note (grn_id, po_id, received_date, received_by, status) VALUES
(1, 1, '2026-01-05', 'admin', 'Verified'),
(2, 2, '2026-01-06', 'admin', 'Verified');

-- GRN_Item (2 records)
INSERT INTO GRN_Item (grn_id, drug_id, batch_number, quantity_received, expiry_date) VALUES
(1, 'RM-ATM-001', 'GRN-ATM-01', 10, '2028-01-01'),
(2, 'RM-IBU-001', 'GRN-IBU-01', 10, '2028-01-01');

-- System_Audit_Trail (3 records)
INSERT INTO System_Audit_Trail (user_id, action_type, table_name, record_id, old_value, new_value, notes) VALUES
(1, 'DATABASE_REFRESH', 'Multiple', 'N/A', 'Old Data', 'New Sample Data', 'Injected 100 sample records for production panel testing'),
(2, 'PRODUCTION_START', 'Production_Order', '3', 'Planned', 'In-Production', 'Batch BT-IBU-401 started'),
(3, 'QC_FAILURE', 'Production_Order', '6', 'Quality-Testing', 'Rejected', 'Batch BT-LZD-601 failed purity test');

-- Verify Total Count
SELECT 
    (SELECT COUNT(*) FROM User_Master) + 
    (SELECT COUNT(*) FROM Location_Master) + 
    (SELECT COUNT(*) FROM Drug_Master) + 
    (SELECT COUNT(*) FROM BOM_Header) + 
    (SELECT COUNT(*) FROM BOM_Details) + 
    (SELECT COUNT(*) FROM Production_Order) +
    (SELECT COUNT(*) FROM Stock_Inventory) +
    (SELECT COUNT(*) FROM Purchase_Order) +
    (SELECT COUNT(*) FROM PurchaseOrder_Item) +
    (SELECT COUNT(*) FROM Goods_Received_Note) +
    (SELECT COUNT(*) FROM GRN_Item) +
    (SELECT COUNT(*) FROM System_Audit_Trail) AS Total_Record_Count;
