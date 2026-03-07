-- Database Upgrade Script for Manufacturer-Centric SCM
-- Ensure we are using the correct database
USE pharma_ims;

-- 1. Disable Foreign Key Checks to allow smooth truncation and schema modification
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Modify Existing Tables to match new requirements
-- Phase 1: drug_master (Ensure material_type is present - it already is, but we ensure quantities in other tables are decimal)
-- Phase 3: stock_inventory (Add reserved_quantity, available_quantity)
ALTER TABLE stock_inventory 
    MODIFY COLUMN quantity DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
    ADD COLUMN reserved_quantity DECIMAL(12,4) DEFAULT 0.0000 AFTER quantity,
    ADD COLUMN available_quantity DECIMAL(12,4) GENERATED ALWAYS AS (quantity - reserved_quantity) STORED AFTER reserved_quantity;

-- Modify other tables to use DECIMAL(12,4) for quantities for consistency
ALTER TABLE grn_item MODIFY COLUMN quantity_received DECIMAL(12,4) NOT NULL;
ALTER TABLE purchaseorder_item MODIFY COLUMN quantity DECIMAL(12,4) NOT NULL;

-- 3. Create New Tables

-- Phase 2: inventory_transaction
CREATE TABLE IF NOT EXISTS inventory_transaction (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    material_code VARCHAR(50),
    batch_number VARCHAR(100),
    location_code VARCHAR(50),
    transaction_type VARCHAR(50),  -- e.g., GRN_RECEIPT, PRODUCTION_CONSUMPTION, PRODUCTION_OUTPUT, QC_RELEASE, QC_REJECT, STOCK_TRANSFER, ADJUSTMENT
    quantity DECIMAL(12,4),
    reference_type VARCHAR(50),    -- e.g., GRN, PO, PROD_ORDER
    reference_id VARCHAR(50),
    performed_by INT,
    transaction_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (material_code) REFERENCES drug_master(material_code) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (location_code) REFERENCES location_master(location_code) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (performed_by) REFERENCES user_master(user_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Phase 4: production_material_consumption
CREATE TABLE IF NOT EXISTS production_material_consumption (
    consumption_id INT AUTO_INCREMENT PRIMARY KEY,
    production_order_id INT,
    material_code VARCHAR(50),
    batch_number VARCHAR(100),
    required_qty DECIMAL(12,4),
    consumed_qty DECIMAL(12,4),
    uom VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (production_order_id) REFERENCES production_order(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (material_code) REFERENCES drug_master(material_code) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Phase 5: production_batch
CREATE TABLE IF NOT EXISTS production_batch (
    batch_id INT AUTO_INCREMENT PRIMARY KEY,
    production_order_id INT,
    material_code VARCHAR(50),
    batch_number VARCHAR(100) UNIQUE,
    quantity DECIMAL(12,4),
    mfg_date DATE,
    expiry_date DATE,
    qc_status VARCHAR(50) DEFAULT 'QUARANTINE',
    location_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (production_order_id) REFERENCES production_order(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (material_code) REFERENCES drug_master(material_code) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (location_code) REFERENCES location_master(location_code) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Phase 6: batch_genealogy
CREATE TABLE IF NOT EXISTS batch_genealogy (
    genealogy_id INT AUTO_INCREMENT PRIMARY KEY,
    parent_batch VARCHAR(100),
    child_batch VARCHAR(100),
    production_order_id INT,
    relationship_type VARCHAR(50), -- e.g., CONSUMED_IN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (production_order_id) REFERENCES production_order(order_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Phase 7: event_log
CREATE TABLE IF NOT EXISTS event_log (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100),      -- e.g., LOW_STOCK, PO_CREATED, MATERIAL_RECEIVED, PRODUCTION_STARTED, PRODUCTION_COMPLETED, QC_FAILED, QC_APPROVED, BATCH_CREATED
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    status VARCHAR(50)
);

-- 4. Safe Truncation of All Target Data
TRUNCATE TABLE batch_genealogy;
TRUNCATE TABLE production_batch;
TRUNCATE TABLE production_material_consumption;
TRUNCATE TABLE inventory_transaction;
TRUNCATE TABLE event_log;

TRUNCATE TABLE system_audit_trail;
TRUNCATE TABLE production_order;
TRUNCATE TABLE grn_item;
TRUNCATE TABLE goods_received_note;
TRUNCATE TABLE purchaseorder_item;
TRUNCATE TABLE purchase_order;
TRUNCATE TABLE stock_inventory;
TRUNCATE TABLE bom_details;
TRUNCATE TABLE bom_header;
TRUNCATE TABLE drug_master;
TRUNCATE TABLE supplier_master;
TRUNCATE TABLE location_master;

-- DO NOT truncate user_master, role_master, permission_master, or role_permission 
-- as they drive RBAC and auth, unless you specifically want to reset users. 
-- We'll just ensure ID 1 (Admin) exists for foreign keys.

-- Ensure admin user exists for 'performed_by'
INSERT IGNORE INTO role_master (role_id, role_name, description) VALUES (1, 'Admin', 'System Administrator');
INSERT IGNORE INTO user_master (user_id, username, password_hash, full_name, role_id) VALUES (1, 'admin', 'admin123', 'System Admin', 1);

-- 5. Insert New Manufacturer-Centric Sample Data

-- Locations
INSERT INTO location_master (location_code, location_name, description, capacity) VALUES 
('RAW_MATERIAL_WAREHOUSE', 'RM Warehouse', 'Storage for active and inactive raw materials', 5000),
('PACKAGING_WAREHOUSE', 'PM Warehouse', 'Storage for packaging materials', 2000),
('PRODUCTION_FLOOR', 'Production Area', 'Active manufacturing area', 1000),
('QC_HOLD', 'Quality Control Hold', 'Quarantine area for tests', 500),
('FINISHED_GOODS_WAREHOUSE', 'FG Warehouse', 'Released finished goods storage', 5000);

-- Suppliers
INSERT INTO supplier_master (supplier_id, supplier_name, contact_person, address, email, phone_number, gstin, payment_terms) VALUES
(1, 'MedSupply Corp', 'John Doe', '123 Pharma Ind, NY', 'sales@medsupply.com', '1234567890', 'GST12345', 'Net 30'),
(2, 'HealthCare Supplies', 'Jane Smith', '45 Biotech Park, NJ', 'contact@healthcaresupplies.com', '0987654321', 'GST98765', 'Net 45'),
(3, 'Zenith Meds', 'Alex Roe', '78 API Hub, TX', 'info@zenithmeds.com', '1122334455', 'GST11223', 'Net 60');

-- Drugs / Materials (Strict material_type: RAW_MATERIAL, PACKAGING, INTERMEDIATE, FINISHED_GOOD)
INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active) VALUES
('RM-PARA-API', 'Paracetamol API', 'Paracetamol', 'Zenith Meds', 3, 'RAW_MATERIAL', 'KG', 1),
('RM-STARCH', 'Corn Starch', 'Starch Excipient', 'MedSupply Corp', 1, 'RAW_MATERIAL', 'KG', 1),
('RM-MAGNESIUM', 'Magnesium Stearate', 'Magnesium Stearate', 'MedSupply Corp', 1, 'RAW_MATERIAL', 'KG', 1),
('RM-LACTOSE', 'Lactose Monohydrate', 'Lactose', 'HealthCare Supplies', 2, 'RAW_MATERIAL', 'KG', 1),
('RM-AMOX-API', 'Amoxicillin API', 'Amoxicillin Trihydrate', 'Zenith Meds', 3, 'RAW_MATERIAL', 'KG', 1),
('RM-CET-API', 'Cetirizine API', 'Cetirizine HCl', 'Zenith Meds', 3, 'RAW_MATERIAL', 'KG', 1),
('PM-ALU-FOIL', 'Alu-Alu Blister Foil', 'Primary Packaging Foil', 'HealthCare Supplies', 2, 'PACKAGING', 'ROLL', 1),
('PM-PVC-FILM', 'PVC Blister Film', 'Primary Packaging Film', 'HealthCare Supplies', 2, 'PACKAGING', 'ROLL', 1),
('PM-CARTON', 'Printed Cartons', 'Secondary Packaging Cartons', 'MedSupply Corp', 1, 'PACKAGING', 'NOS', 1),
('INT-PARA-GRANULES', 'Paracetamol Granules', 'Paracetamol 500mg Granules blend', 'Internal', NULL, 'INTERMEDIATE', 'KG', 1),
('DRG001', 'Paracetamol 500mg Tablet', 'Paracetamol', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1),
('DRG002', 'Amoxicillin 250mg Capsule', 'Amoxicillin', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1),
('DRG003', 'Cetirizine 10mg Tablet', 'Cetirizine HCl', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1);

-- Bill of Materials Header
INSERT INTO bom_header (bom_id, material_code, version_number, is_active, effective_date, description) VALUES
(1, 'DRG001', 1, 1, '2025-01-01', 'BOM for Paracetamol 500mg Tablet (per 1000 tablets)'),
(2, 'DRG002', 1, 1, '2025-01-01', 'BOM for Amoxicillin 250mg Capsule (per 1000 capsules)');

-- Bill of Materials Details
INSERT INTO bom_details (bom_id, ingredient_material_code, required_qty, uom, sequence_number) VALUES
-- Paracetamol 500mg (1000 NOS requires 0.5kg API + Excipients + Packaging)
(1, 'RM-PARA-API', 0.5000, 'KG', 10),
(1, 'RM-STARCH', 0.2000, 'KG', 20),
(1, 'RM-MAGNESIUM', 0.0500, 'KG', 30),
(1, 'PM-ALU-FOIL', 1.0000, 'ROLL', 40),
(1, 'PM-CARTON', 100.0000, 'NOS', 50),
-- Amoxicillin 250mg (1000 NOS requires 0.25kg API + Excipients + Packaging)
(2, 'RM-AMOX-API', 0.2500, 'KG', 10),
(2, 'RM-LACTOSE', 0.1500, 'KG', 20),
(2, 'PM-PVC-FILM', 1.0000, 'ROLL', 30),
(2, 'PM-CARTON', 100.0000, 'NOS', 40);

-- Purchase Orders (For Raw Materials & Packaging)
INSERT INTO purchase_order (po_id, supplier_id, order_date, expected_date, status) VALUES
(1, 3, '2025-10-01', '2025-10-05', 'Received'),
(2, 1, '2025-10-02', '2025-10-06', 'Received'),
(3, 2, '2025-10-03', '2025-10-07', 'Received');

-- Purchase Order Items
INSERT INTO purchaseorder_item (po_id, drug_id, quantity, unit_price) VALUES
(1, 'RM-PARA-API', 500.0000, 15.00),
(1, 'RM-CET-API', 50.0000, 85.00),
(2, 'RM-STARCH', 1000.0000, 2.50),
(2, 'RM-MAGNESIUM', 200.0000, 5.00),
(2, 'PM-CARTON', 50000.0000, 0.10),
(3, 'PM-ALU-FOIL', 1000.0000, 12.00),
(3, 'PM-PVC-FILM', 500.0000, 8.00),
(3, 'RM-LACTOSE', 800.0000, 3.50),
(3, 'RM-AMOX-API', 400.0000, 25.00);

-- Initial Stock Inventory (Received from GRN previously)
-- We insert raw materials & packaging into RAW_MATERIAL_WAREHOUSE and PACKAGING_WAREHOUSE
-- Note: available_quantity is auto-generated as (quantity - reserved_quantity)
INSERT INTO stock_inventory (material_code, location_code, batch_number, quantity, reserved_quantity, unit_cost, mfg_date, exp_date, qc_status) VALUES
('RM-PARA-API', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-PARA-001', 500.0000, 0, 15.00, '2025-08-01', '2028-08-01', 'RELEASED'),
('RM-STARCH', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-STARCH-001', 1000.0000, 0, 2.50, '2025-09-01', '2027-09-01', 'RELEASED'),
('RM-MAGNESIUM', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-MAG-001', 200.0000, 0, 5.00, '2025-09-15', '2027-09-15', 'RELEASED'),
('PM-ALU-FOIL', 'PACKAGING_WAREHOUSE', 'BATCH-PM-ALU-001', 1000.0000, 0, 12.00, '2025-09-20', '2030-09-20', 'RELEASED'),
('PM-CARTON', 'PACKAGING_WAREHOUSE', 'BATCH-PM-CRT-001', 50000.0000, 0, 0.10, '2025-09-25', '2030-09-25', 'RELEASED'),
('RM-AMOX-API', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-AMOX-001', 400.0000, 0, 25.00, '2025-08-10', '2027-08-10', 'RELEASED'),
('RM-LACTOSE', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-LAC-001', 800.0000, 0, 3.50, '2025-09-01', '2028-09-01', 'RELEASED'),
('PM-PVC-FILM', 'PACKAGING_WAREHOUSE', 'BATCH-PM-PVC-001', 500.0000, 0, 8.00, '2025-09-21', '2030-09-21', 'RELEASED');

-- Insert Initial Transactions representing the opening stock entry
INSERT INTO inventory_transaction (material_code, batch_number, location_code, transaction_type, quantity, reference_type, reference_id, performed_by, notes) VALUES
('RM-PARA-API', 'BATCH-RM-PARA-001', 'RAW_MATERIAL_WAREHOUSE', 'GRN_RECEIPT', 500.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('RM-STARCH', 'BATCH-RM-STARCH-001', 'RAW_MATERIAL_WAREHOUSE', 'GRN_RECEIPT', 1000.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('RM-MAGNESIUM', 'BATCH-RM-MAG-001', 'RAW_MATERIAL_WAREHOUSE', 'GRN_RECEIPT', 200.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('PM-ALU-FOIL', 'BATCH-PM-ALU-001', 'PACKAGING_WAREHOUSE', 'GRN_RECEIPT', 1000.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('PM-CARTON', 'BATCH-PM-CRT-001', 'PACKAGING_WAREHOUSE', 'GRN_RECEIPT', 50000.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance');

-- Finished Goods (to simulate previously completed production)
INSERT INTO stock_inventory (material_code, location_code, batch_number, quantity, reserved_quantity, unit_cost, mfg_date, exp_date, qc_status) VALUES
('DRG001', 'FINISHED_GOODS_WAREHOUSE', 'BATCH-PARA500-001', 50000.0000, 0, 0.40, '2025-11-01', '2027-10-31', 'RELEASED'),
('DRG002', 'FINISHED_GOODS_WAREHOUSE', 'BATCH-AMOX250-001', 20000.0000, 0, 0.85, '2025-11-15', '2027-11-14', 'RELEASED');

-- Insert events for the initial setup
INSERT INTO event_log (event_type, entity_type, entity_id, details, status) VALUES
('SYSTEM_INITIALIZATION', 'SYSTEM', '1', 'Manufacturer-Centric SCM initialized with sample data', 'SUCCESS');

-- 6. Enable Foreign Key Checks
SET FOREIGN_KEY_CHECKS = 1;
