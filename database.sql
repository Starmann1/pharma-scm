/*
-- MySQL Database Setup for Pharmaceutical IMS
-- This script creates the database and all master/transactional tables,
-- including tables required for Purchase Orders (PO) and Goods Received Notes (GRN).
*/

-- 1. Create Database and Switch Context
CREATE DATABASE IF NOT EXISTS pharma_ims;
USE pharma_ims;

SET FOREIGN_KEY_CHECKS = 0;

-- 2. Drop existing tables for a clean slate (Run this script multiple times)
DROP TABLE IF EXISTS GRN_Item;
DROP TABLE IF EXISTS Goods_Received_Note;
DROP TABLE IF EXISTS PurchaseOrder_Item;
DROP TABLE IF EXISTS Purchase_Order;
DROP TABLE IF EXISTS Stock_Inventory;
DROP TABLE IF EXISTS Drug_Master;
DROP TABLE IF EXISTS Supplier_Master;
DROP TABLE IF EXISTS Location_Master;
DROP TABLE IF EXISTS Role_Permission;
DROP TABLE IF EXISTS User_Master;
DROP TABLE IF EXISTS Role_Master;
DROP TABLE IF EXISTS Permission_Master;

-- ---------------------------------------------------------------------
-- MASTER TABLES (Primary Data)
-- ---------------------------------------------------------------------

-- 3. Supplier Master Table
CREATE TABLE IF NOT EXISTS Supplier_Master (
    supplier_id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    address TEXT,
    email VARCHAR(255),
    phone_number VARCHAR(20),
    gstin VARCHAR(50),
    drug_license_number VARCHAR(100),
    payment_terms VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. Drug Master Table (Includes Foreign Key to Supplier)
CREATE TABLE IF NOT EXISTS Drug_Master (
    material_code VARCHAR(50) PRIMARY KEY,
    brand_name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    manufacturer VARCHAR(255),
    formulation VARCHAR(100),
    strength VARCHAR(100),
    schedule_category VARCHAR(50),
    storage_conditions TEXT,
    reorder_level INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    
    preferred_supplier_id INT, 
    material_type VARCHAR(50) DEFAULT 'FINISHED_GOOD',
    unit_of_measure VARCHAR(50) DEFAULT 'NOS',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Define the Foreign Key constraint for preferred supplier
    FOREIGN KEY (preferred_supplier_id) REFERENCES Supplier_Master(supplier_id) 
        ON DELETE SET NULL 
        ON UPDATE CASCADE
);

-- 5. Location Master Table
CREATE TABLE IF NOT EXISTS Location_Master (
    location_code VARCHAR(50) PRIMARY KEY,
    location_name VARCHAR(255) NOT NULL,
    description TEXT,
    capacity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 6. Role Master Table
CREATE TABLE IF NOT EXISTS Role_Master (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT
);

-- 7. Permission Master Table
CREATE TABLE IF NOT EXISTS Permission_Master (
    permission_id INT AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(100) UNIQUE NOT NULL,
    module VARCHAR(100),
    description TEXT
);

-- 8. Role_Permission Junction Table
CREATE TABLE IF NOT EXISTS Role_Permission (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES Role_Master(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES Permission_Master(permission_id) ON DELETE CASCADE
);

-- 9. User Master Table (Using plaintext passwords for simple AuthService.java match)
CREATE TABLE IF NOT EXISTS User_Master (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role_id INT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES Role_Master(role_id) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- TRANSACTIONAL & INVENTORY TABLES (Fixed Unimplemented Features)
-- ---------------------------------------------------------------------

-- 7. Purchase Order Header Table (Needed for PurchaseOrder.java functionality)
CREATE TABLE IF NOT EXISTS Purchase_Order (
    po_id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id INT NOT NULL,
    order_date DATE NOT NULL,
    expected_date DATE,
    total_amount DECIMAL(10, 2) DEFAULT 0.00,
    status VARCHAR(50) DEFAULT 'Pending', -- Pending, Shipped, Received
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES Supplier_Master(supplier_id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE
);

-- 8. Purchase Order Item/Line Table
CREATE TABLE IF NOT EXISTS PurchaseOrder_Item (
    po_item_id INT AUTO_INCREMENT PRIMARY KEY,
    po_id INT NOT NULL,
    drug_id VARCHAR(50) NOT NULL, -- Links to Drug_Master.material_code
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    
    FOREIGN KEY (po_id) REFERENCES Purchase_Order(po_id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    FOREIGN KEY (drug_id) REFERENCES Drug_Master(material_code) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE
);

-- 9. Goods Received Note Header Table (Needed for GRN.java functionality)
CREATE TABLE IF NOT EXISTS Goods_Received_Note (
    grn_id INT AUTO_INCREMENT PRIMARY KEY,
    po_id INT NOT NULL,
    received_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    received_by VARCHAR(255),
    status VARCHAR(50) DEFAULT 'Verified',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (po_id) REFERENCES Purchase_Order(po_id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE
);

-- 10. GRN Item/Line Table (Contains batch details for inventory)
CREATE TABLE IF NOT EXISTS GRN_Item (
    grn_item_id INT AUTO_INCREMENT PRIMARY KEY,
    grn_id INT NOT NULL,
    drug_id VARCHAR(50) NOT NULL, -- Links to Drug_Master.material_code
    batch_number VARCHAR(100) NOT NULL,
    quantity_received INT NOT NULL,
    expiry_date DATE NOT NULL,
    
    FOREIGN KEY (grn_id) REFERENCES Goods_Received_Note(grn_id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    FOREIGN KEY (drug_id) REFERENCES Drug_Master(material_code) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE
);

-- 11. Stock/Inventory Table (The core warehouse stock levels)
CREATE TABLE IF NOT EXISTS Stock_Inventory (
    stock_id INT AUTO_INCREMENT PRIMARY KEY,
    material_code VARCHAR(50) NOT NULL,
    location_code VARCHAR(50) NOT NULL,
    batch_number VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    unit_cost DECIMAL(10, 2),
    mfg_date DATE,
    exp_date DATE,
    qc_status VARCHAR(50) DEFAULT 'RELEASED',
    parent_batch_id TEXT,
    production_order_id INT,
    
    -- Ensures a specific batch of a drug is tracked only once per location
    UNIQUE KEY (material_code, location_code, batch_number),
    
    FOREIGN KEY (material_code) REFERENCES Drug_Master(material_code) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    FOREIGN KEY (location_code) REFERENCES Location_Master(location_code) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
        
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


-- ---------------------------------------------------------------------
-- SAMPLE DATA INSERTION
-- ---------------------------------------------------------------------

-- Sample Suppliers
INSERT INTO Supplier_Master (supplier_name, contact_person, email, phone_number, gstin, drug_license_number, payment_terms) VALUES
('MedSupply Corp', 'John Smith', 'john@medsupply.com', '+91-9876543210', '29ABCDE1234F1Z5', 'DL-12345', 'Net 30'),
('PharmaDistributors Ltd', 'Jane Doe', 'jane@pharmadist.com', '+91-9876543211', '29FGHIJ5678K2Z6', 'DL-67890', 'Net 45'),
('HealthCare Supplies', 'Bob Johnson', 'bob@healthcare.com', '+91-9876543212', '29LMNOP9012Q3Z7', 'DL-11223', 'Net 60');

-- Sample Drugs (using supplier IDs from above: 1, 2)
INSERT INTO Drug_Master (material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id) VALUES
('DRG001', 'Paracet-500', 'Paracetamol', 'XYZ Pharma', 'Tablet', '500mg', 'OTC', 'Store below 30Â°C', 100, TRUE, 1),
('DRG002', 'Amox-250', 'Amoxicillin', 'ABC Labs', 'Capsule', '250mg', 'Schedule H', 'Store in cool place', 50, TRUE, 2),
('DRG003', 'Cetiriz-10', 'Cetirizine', 'DEF Pharma', 'Tablet', '10mg', 'OTC', 'Store below 25Â°C', 75, TRUE, 1);

-- Sample Locations
INSERT INTO Location_Master (location_code, location_name, description, capacity) VALUES
('LOC-A1', 'Warehouse Zone A1', 'General medicines storage', 5000),
('LOC-B2', 'Cold Storage B2', 'Temperature controlled storage', 2000),
('LOC-C3', 'Retail Counter C3', 'Front desk inventory', 500);

-- Sample Roles
INSERT INTO Role_Master (role_name, description) VALUES
('Admin', 'System Administrator with all permissions'),
('Production Manager', 'Manages BOMs and Production Orders'),
('Quality Analyst', 'Manages Quality Control and Batch Release'),
('Warehouse Manager', 'Manages Inventory and Receipts'),
('Procurement Manager', 'Manages Suppliers and Purchase Orders'),
('Viewer', 'Read-only access to system data');

-- Sample Permissions (Extracted precisely from Master Prompt)
INSERT INTO Permission_Master (permission_name, module, description) VALUES
('MANAGE_USERS', 'Admin', 'Manage Users and Roles'),
('VIEW_AUDIT_TRAIL', 'Admin', 'View system audit trails'),
('CREATE_DRUG', 'Drug', 'Create Drug'),
('EDIT_DRUG', 'Drug', 'Edit Drug'),
('DELETE_DRUG', 'Drug', 'Delete Drug'),
('VIEW_DRUG', 'Drug', 'View Drug'),
('VIEW_INVENTORY', 'Inventory', 'View stock inventory'),
('ADJUST_STOCK', 'Inventory', 'Manually adjust stock levels'),
('TRANSFER_STOCK', 'Inventory', 'Transfer stock between locations'),
('MANAGE_LOCATIONS', 'Location', 'Manage warehouse locations'),
('ADD_SUPPLIER', 'Supplier', 'Add new Supplier'),
('EDIT_SUPPLIER', 'Supplier', 'Edit Supplier details'),
('DELETE_SUPPLIER', 'Supplier', 'Delete Supplier'),
('VIEW_SUPPLIERS', 'Supplier', 'View Suppliers'),
('CREATE_PO', 'Purchase', 'Create Purchase Orders'),
('EDIT_PO', 'Purchase', 'Edit Purchase Orders'),
('VIEW_PO', 'Purchase', 'View Purchase Orders'),
('RECEIVE_PO', 'Purchase', 'Receive Purchase Orders (GRN)'),
('CREATE_PRODUCTION_ORDER', 'Production', 'Create new production orders'),
('EXECUTE_PRODUCTION_RUN', 'Production', 'Execute production runs'),
('UPDATE_QC_STATUS', 'QC', 'Update quality control status generically'),
('RELEASE_BATCH', 'QC', 'Release batch from quarantine'),
('REJECT_BATCH', 'QC', 'Reject batch'),
('VIEW_BATCH_TRACEABILITY', 'QC', 'View genealogy and traceability'),
('VIEW_REPORTS', 'Reports', 'View Analytics and Reports'),
('EXPORT_REPORTS', 'Reports', 'Export Reports'),
('VIEW_BOM', 'BOM', 'View BOM'),
('MANAGE_BOM', 'BOM', 'Create and Edit BOM');

-- Sample Role Permissions (Mapping)
-- Admin (Role 1): Gets all permissions
INSERT INTO Role_Permission (role_id, permission_id) SELECT 1, permission_id FROM Permission_Master;

-- Production Manager (Role 2)
INSERT INTO Role_Permission (role_id, permission_id) VALUES
(2, 6), (2, 7), (2, 27), (2, 28), (2, 19), (2, 20), (2, 25);

-- Quality Analyst (Role 3)
INSERT INTO Role_Permission (role_id, permission_id) VALUES
(3, 6), (3, 7), (3, 21), (3, 22), (3, 23), (3, 24), (3, 25);

-- Warehouse Manager (Role 4)
INSERT INTO Role_Permission (role_id, permission_id) VALUES
(4, 6), (4, 7), (4, 8), (4, 9), (4, 10), (4, 17), (4, 18), (4, 25);

-- Procurement Manager (Role 5)
INSERT INTO Role_Permission (role_id, permission_id) VALUES
(5, 6), (5, 11), (5, 12), (5, 14), (5, 15), (5, 16), (5, 17), (5, 18), (5, 25);

-- Viewer (Role 6)
INSERT INTO Role_Permission (role_id, permission_id) VALUES
(6, 6), (6, 7), (6, 25);

-- Sample Users
INSERT INTO User_Master (username, password_hash, full_name, role_id, is_active) VALUES
('admin', 'adminpass', 'System Administrator', 1, TRUE), 
('prod_mgr', 'prodpass', 'John Doe (Production Manager)', 2, TRUE),
('qa_analyst', 'qapass', 'Dr. Sarah Williams (QA)', 3, TRUE),
('warehouse_mgr', 'whpass', 'Mike Brown (Warehouse)', 4, TRUE),
('proc_mgr', 'procpass', 'Emily Davis (Procurement)', 5, TRUE),
('viewer_user', 'viewerpass', 'Tom Viewer (Read Only)', 6, TRUE);


-- Sample Transactional Data (to populate PO and GRN tables)

-- 1. Create a Purchase Order (from MedSupply Corp - ID 1)
INSERT INTO Purchase_Order (supplier_id, order_date, expected_date, total_amount, status) VALUES
(1, '2024-10-20', '2024-10-27', 1000.00, 'Pending');
SET @last_po_id = LAST_INSERT_ID(); -- Get the ID of the newly created PO

-- 2. Add Items to that Purchase Order
INSERT INTO PurchaseOrder_Item (po_id, drug_id, quantity, unit_price) VALUES
(@last_po_id, 'DRG001', 500, 1.50),
(@last_po_id, 'DRG003', 250, 1.00);

-- 3. Create a Goods Received Note for that PO (Simulating a full receipt)
INSERT INTO Goods_Received_Note (po_id, received_date, received_by, status) VALUES
(@last_po_id, NOW(), 'admin', 'Verified');
SET @last_grn_id = LAST_INSERT_ID(); -- Get the ID of the newly created GRN

-- 4. Add Items (Batches) to the GRN
INSERT INTO GRN_Item (grn_id, drug_id, batch_number, quantity_received, expiry_date) VALUES
(@last_grn_id, 'DRG001', 'B-1001-A', 500, '2025-12-31'),
(@last_grn_id, 'DRG003', 'C-3001-Z', 250, '2026-06-30');


-- Final Stock/Inventory (Should reflect the GRN)
INSERT INTO Stock_Inventory (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date) VALUES
('DRG001', 'LOC-A1', 'B-1001-A', 500, 1.50, '2024-01-01', '2025-12-31'), -- From the new GRN
('DRG003', 'LOC-A1', 'C-3001-Z', 250, 1.00, '2024-01-01', '2026-06-30'), -- From the new GRN
('DRG002', 'LOC-B2', 'BATCH002B', 100, 2.75, '2024-03-15', '2026-03-15'); -- Existing sample

-- Verify the setup
SELECT 'Database setup complete!' AS Status;
SELECT COUNT(*) AS Supplier_Count FROM Supplier_Master;
SELECT COUNT(*) AS Drug_Count FROM Drug_Master;
SELECT COUNT(*) AS Location_Count FROM Location_Master;
SELECT COUNT(*) AS User_Count FROM User_Master;
SELECT COUNT(*) AS Stock_Count FROM Stock_Inventory;
SELECT COUNT(*) AS PurchaseOrder_Count FROM Purchase_Order;
SELECT COUNT(*) AS GRN_Count FROM Goods_Received_Note;

use pharma_ims;
show tables;
select * from drug_master;
select * from goods_received_note;
select * from grn_item;
select * from location_master;
select * from purchase_order;
select * from purchaseorder_item;
select * from stock_inventory;
select * from supplier_master;
select * from user_master;


-- inserting some more records
INSERT INTO Supplier_Master (supplier_name, contact_person, email, phone_number, gstin, drug_license_number, payment_terms) VALUES
('MedSupply Corp', 'John Smith', 'john@medsupply.com', '91-9876500001', '29ABCDE1284F1Z5', 'DL-12345', 'Net 30'),
('PharmaDistributors Ltd', 'Jane Doe', 'jane@pharmadist.com', '91-9876500002', '29FGHIJ5678K2Z6', 'DL-67890', 'Net 45'),
('HealthCare Supplies', 'Bob Johnson', 'bob@healthcare.com', '91-9876500003', '29LMNOP9012Q3Z7', 'DL-11223', 'Net 60'),
('Zenith Meds', 'Alice Lee', 'alice@zenithmed.com', '91-9876500004', '29PQRST3456L4Z8', 'DL-33445', 'Net 15'),
('Apollo Medex', 'Tom Kim', 'tom@apollomedex.com', '91-9876500005', '29UVWXY7890S8Z9', 'DL-55667', 'Net 30');

INSERT INTO Drug_Master (material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id) VALUES
('DRG005', 'Ibupro-X', 'Ibuprofen', 'ZYX Labs', 'Tablet', '400mg', 'OTC', 'Cool dry', 50, TRUE, 2),
('DRG006', 'Azithro-250', 'Azithromycin', 'PQR Pharma', 'Tablet', '250mg', 'Schedule H', 'Cool dry', 60, TRUE, 2),
('DRG007', 'Metfor-500', 'Metformin', 'MetHealth Ltd', 'Tablet', '500mg', 'OTC', 'Below 30C', 80, TRUE, 3),
('DRG008', 'Atorva-20', 'Atorvastatin', 'Statin Corp', 'Tablet', '20mg', 'Schedule H', 'Cool dry', 30, TRUE, 4),
('DRG009', 'Pantop-40', 'Pantoprazole', 'GastroInc', 'Tablet', '40mg', 'OTC', 'Below 25C', 40, TRUE, 5),
('DRG010', 'Amlo-5', 'Amlodipine', 'BP Care', 'Tablet', '5mg', 'OTC', 'Cool', 100, TRUE, 1),
('DRG011', 'Montair-10', 'Montelukast', 'AllergyFree', 'Tablet', '10mg', 'Schedule H', 'Dry place', 20, TRUE, 3);

INSERT INTO Purchase_Order (supplier_id, order_date, expected_date, total_amount, status) VALUES
(1, '2025-09-01', '2025-09-05', 2400.00, 'Pending'),
(2, '2025-09-02', '2025-09-08', 1700.00, 'Pending'),
(3, '2025-09-03', '2025-09-09', 2300.00, 'Pending'),
(4, '2025-09-04', '2025-09-10', 800.00, 'Pending');

INSERT INTO PurchaseOrder_Item (po_id, drug_id, quantity, unit_price) VALUES
(4, 'DRG004', 200, 2.00),
(4, 'DRG005', 150, 5.50),
(4, 'DRG006', 100, 3.30),

(5, 'DRG007', 300, 6.00),
(5, 'DRG008', 100, 6.50),
(5, 'DRG009', 250, 1.20),

(6, 'DRG010', 140, 7.30),
(6, 'DRG001', 180, 1.50),
(6, 'DRG002', 200, 2.10),

(7, 'DRG003', 130, 0.85),
(7, 'DRG004', 120, 2.50),
(7, 'DRG005', 300, 5.60);

INSERT INTO Goods_Received_Note (po_id, received_date, received_by, status) VALUES
(4, '2025-09-06', 'john', 'Verified'),
(5, '2025-09-09', 'jane', 'Verified'),
(6, '2025-09-12', 'admin', 'Verified'),
(7, '2025-09-13', 'alice', 'Partial');

INSERT INTO GRN_Item (grn_id, drug_id, batch_number, quantity_received, expiry_date) VALUES
(3, 'DRG004', 'IBU-BT-2025', 200, '2027-01-15'),
(3, 'DRG005', 'AZI-B-501', 150, '2026-05-14'),
(4, 'DRG007', 'ATOR-SEP25', 300, '2027-09-13'),
(5, 'DRG010', 'MONT-Nov25', 140, '2026-11-30'),
(6, 'DRG001', 'PARA-NEW01', 180, '2026-12-01');


INSERT INTO Stock_Inventory (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date) VALUES
('DRG004', 'LOC-B1', 'IBU-BT-2025', 200, 2.00, '2025-01-01', '2027-01-15'),
('DRG005', 'LOC-A2', 'AZI-B-501', 150, 5.50, '2025-02-01', '2026-05-14'),
('DRG007', 'LOC-C3', 'ATOR-SEP25', 300, 6.00, '2025-03-01', '2027-09-13'),
('DRG008', 'LOC-D4', 'PANTO-BB', 100, 6.50, '2025-04-01', '2027-01-01'),
('DRG010', 'LOC-E5', 'MONT-Nov25', 140, 7.30, '2025-05-01', '2026-11-30'),
('DRG001', 'LOC-A1', 'PARA-NEW01', 180, 1.50, '2025-06-01', '2026-12-01');

INSERT INTO Location_Master (location_code, location_name, description, capacity) VALUES
('LOC-D2', 'Warehouse Zone D4', 'Bulk raw materials', 3500),
('LOC-E5', 'Retail Counter E5', 'High-demand Rx at point of sale', 800),
('LOC-F6', 'Cold Rack F6', 'Refrigerated injectables', 1500),
('LOC-G7', 'Floorstock G7', 'OTC fast-movers', 1000),
('LOC-H8', 'Counting Room H8', 'Loose tablets/capsules', 600),
('LOC-I9', 'Quarantine I9', 'Defective/damaged goods isolation area', 250),
('LOC-J10', 'Warehouse Zone J10', 'Bulk syringes and disposables', 2200),
('LOC-K11', 'Warehouse K11', 'Medical device storage', 1200),
('LOC-L12', 'Sample Stash L12', 'Doctor/pharma sample storage', 350),
('LOC-M13', 'Returns M13', 'Returned or expired medicine hold', 300);

INSERT INTO Supplier_Master (supplier_name, contact_person, email, phone_number, gstin, drug_license_number, payment_terms) VALUES
('Shenzhen MedTech Co.',         'Li Wei',             'li.wei@shenzhenmt.com',      '+86-755-88888888',  'CNHG123456789', 'CH-DL98765', 'Net 60'),
('BioPharm Paris SAS',           'ClÃ©ment Dubois',     'c.dubois@bioparis.fr',       '+33-1-23456789',    'FR9988776655', 'FR-LC33221', 'Net 30'),
('Zeeland Pharma BV',            'Sven Janssen',       's.janssen@zeelandpharma.nl', '+31-10-1234567',    'NL1234123412', 'NLPOL78901', 'Net 45'),
('MediCare Mumbai Pvt Ltd',      'Priya Nair',         'priya@medicaremumbai.com',   '+91-22-29876543',   'INABCD9876LZ', 'IN-MDL3214', 'Net 30'),
('Kyoto Medics Inc.',            'Haruki Sato',        'hsato@kyotomedics.jp',       '+81-75-6612345',    'JP1234432112', 'JP-12345',   'Net 60'),
('VitalCare Brasil Ltda.',       'Carla Ferreira',     'carla@vitalcare.com.br',     '+55-11-99887766',   'BR1122334455', 'BR-DL11122', 'Net 40'),
('Sigma Biologika AG',           'Marius Keller',      'mkeller@sigmabio.de',        '+49-69-110022334',  'DE6655443322', 'DE-LC09876', 'Net 50'),
('Sydney Pharma Group',           'Chloe Turner',       'chloe@sydneypharma.com.au',  '+61-2-98765432',    'AU9988665522', 'AU-DL56438', 'Net 35'),
('Cairo MedSolutions',            'Omar El-Sayed',      'omar@cairomedsolutions.eg',  '+20-2-24681012',    'EG5544332211', 'EG-DL98342', 'Net 60'),
('Helsinki Health Oy',            'Janne Virtanen',     'janne@healthoy.fi',          '+358-9-1234567',    'FI4433221100', 'FI-33442',   'Net 30'),
('Warsaw Apteka Sp. z o.o.',      'Katarzyna Nowak',    'k.nowak@apteka.pl',          '+48-22-9876543',    'PL998832211',  'PL-LC99812', 'Net 45'),
('Bangkok PharmaHub Co. Ltd',     'Thanawat Chaiyasit', 'thanawat@pharmahub.co.th',   '+66-2-22334455',    'TH1122446688', 'TH-DL88866', 'Net 40'),
('Milano MedImporter SRL',        'Giulia Romano',      'giulia@medimporter.it',      '+39-2-88892435',    'IT1234789001', 'IT-56100',   'Net 60'),
('Johannesburg MedSupp36 Ltd',    'Amanda Nkosi',       'amanda.nkosi@medsupp36.co.za', '+27-11-2345678',  'ZA112245555',  'ZA-00987',   'Net 30'),
('Istanbul Saglik A.S.',          'Ahmet Yilmaz',       'ahmet@saglik.com.tr',        '+90-212-5557890',   'TR5588776611', 'TR888001',   'Net 50');


INSERT INTO Supplier_Master (supplier_name, contact_person, email, phone_number, gstin, drug_license_number, payment_terms) VALUES
('Osaka Biologics KK',       'Ryo Tanaka',         'r.tanaka@osakabio.co.jp',         '+81-6-98765432',    'JP0987234512', 'JP-BIO8798', 'Net 60'),
('Meditech Egypt LLC',       'Sara Fathy',         'sara@meditecheg.com',             '+20-2-21122334',    'EG3344556677', 'EG-MED1234', 'Net 30'),
('Marseille Medicaments SAS','Lucas Garnier',      'lucas.g@medmarseille.fr',         '+33-4-91234567',    'FR7766122345', 'FR-PH9090',  'Net 45'),
('Jakarta MediLogistik PT',  'Rina Wijaya',        'r.wijaya@jakartamedilog.id',      '+62-21-1238989',    'ID2233441122', 'ID-DL2267',  'Net 30'),
('Zurich Lifecare AG',       'Hans Meier',         'h.meier@zurichlifecare.ch',       '+41-44-9876543',    'CH6655442233', 'CH-ZLC5566', 'Net 50'),
('Strides Arcolab', 'Gunasekaran M',       			'skyguna@arcolab.in',             '+91-33-24332211',   'IN1122334455', 'IN-KPE7681', 'Net 30'),
('Stockholm Biofarma AB',    'Anna Lindqvist',     'anna.l@biofarma.se',              '+46-8-98765432',    'SE5566778899', 'SE-AB3344',  'Net 60'),
('Budapest HealthTrade Zrt', 'DÃ³ra SzabÃ³',         'd.szabo@healthtrade.hu',           '+36-1-2345678',    'HU5566788991', 'HU-BHT1937', 'Net 40'),
('Santiago Farma SA',        'Cristian Rojas',     'crojas@santifarma.cl',            '+56-2-23456789',    'CL3498576231', 'CL-SA9087',  'Net 30'),
('Seoul MedExport Co.',      'Min-jun Kim',        'mj.kim@seoulmedexport.kr',         '+82-2-12345678',   'KR4455123399', 'KR-ME5472',  'Net 45');

INSERT INTO Drug_Master (material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id) VALUES
('DRG012', 'Lipicure',           'Atorvastatin',     'Sun Pharma',           'Tablet',   '10mg',  'H', 'Below 30C', 60, 1, 15),
('DRG013', 'Losar',              'Losartan',         'Zydus',                'Tablet',   '50mg',  'H', 'Below 30C', 40, 1, 16),
('DRG014', 'Telmikind',          'Telmisartan',      'Mankind',              'Tablet',   '40mg',  'H', 'Below 30C', 25, 1, 17),
('DRG015', 'Glycomet',           'Metformin',        'USV',                  'Tablet',   '500mg', 'OTC', 'Cool dry', 70, 1, 18),
('DRG016', 'Glynase',            'Glibenclamide',    'Ipca',                 'Tablet',   '5mg',   'H', 'Store cool', 30, 1, 19),
('DRG017', 'Ciplactin',          'Cyproheptadine',   'Cipla',                'Tablet',   '4mg',   'OTC', 'Below 25C', 10, 1, 20),
('DRG018', 'Omez',               'Omeprazole',       'Dr. Reddy\'s',         'Capsule',  '20mg',  'OTC', 'Dry', 35, 1, 21),
('DRG019', 'Ecosprin',           'Aspirin',          'USV',                  'Tablet',   '75mg',  'OTC', 'Cool', 50, 1, 22),
('DRG020', 'Clavam',             'Amox+Clav Acid',   'Alkem',                'Tablet',   '625mg', 'H', 'Cool', 25, 1, 23),
('DRG021', 'Augmentin',          'Amox+Clav Acid',   'GSK',                  'Tablet',   '625mg', 'H', 'Cool', 45, 1, 24),
('DRG022', 'Foracort',           'Bud+Form',         'Cipla',                'Inhaler',  '200mcg', 'H', 'Cool', 15, 1, 25),
('DRG023', 'Asthalin',           'Salbutamol',       'Cipla',                'Inhaler',  '100mcg', 'OTC', 'Cool', 20, 1, 26),
('DRG024', 'Zifi',               'Cefixime',         'FDC Ltd',              'Tablet',   '200mg', 'H', 'Room temp', 35, 1, 27),
('DRG025', 'Dolo',               'Paracetamol',      'Micro Labs',           'Tablet',   '650mg', 'OTC', 'Room temp', 100, 1, 28),
('DRG026', 'Taxim-O',            'Cefixime',         'Alkem',                'Tablet',   '200mg', 'H', 'Room temp', 35, 1, 29),
('DRG027', 'Monocef',            'Ceftriaxone',      'Aristo Pharma',        'Injection', '1g',   'H', 'Cool', 15, 1, 30),
('DRG028', 'Emeset',             'Ondansetron',      'Cipla',                'Tablet',   '4mg',   'OTC', 'Room temp', 10, 1, 31),
('DRG029', 'Dexona',             'Dexamethasone',    'Zydus',                'Tablet',   '0.5mg', 'H', 'Cool', 15, 1, 32),
('DRG030', 'Solvin',             'PCM+CPM',          'Ipca',                 'Syrup',    '5ml',  'OTC', 'Room temp', 50, 1, 33),
('DRG031', 'Shelcal',            'Ca+Vit D3',        'Torrent Pharma',       'Tablet',   '500mg', 'OTC', 'Room temp', 30, 1, 34),
('DRG032', 'Combiflam',          'IBU+PCM',          'Sanofi',               'Tablet',   '400mg+325mg','OTC','Room temp',100,1,35),
('DRG033', 'Amlopres',           'Amlodipine',       'Cipla',                'Tablet',   '5mg',   'OTC', 'Cool', 60, 1, 36),
('DRG034', 'Concor',             'Bisoprolol',       'Merck',                'Tablet',   '5mg',   'H', 'Cool', 40, 1, 37),
('DRG035', 'Rantac',             'Ranitidine',       'JB Chemicals',         'Tablet',   '150mg', 'OTC', 'Room temp',20, 1, 38),
('DRG036', 'Zinetac',            'Ranitidine',       'Glaxo',                'Tablet',   '150mg', 'OTC', 'Cool',20,1,39),
('DRG037', 'Suprax',             'Cefixime',         'Astellas',             'Tablet',   '200mg', 'H', 'Cool',35,1,15),
('DRG038', 'Meftal',             'Mefenamic acid',   'Blue Cross',           'Tablet',   '500mg', 'OTC', 'Cool',40,1,16),
('DRG039', 'Allegra',            'Fexofenadine',     'Sanofi Aventis',       'Tablet',   '120mg', 'OTC', 'Cool',30,1,17),
('DRG040', 'Levocet',            'Levocetirizine',   'Glenmark',             'Tablet',   '5mg',   'OTC', 'Room temp',35,1,18),
('DRG041', 'Eliquis',            'Apixaban',         'Pfizer',               'Tablet',   '5mg',   'H', 'Room temp',15,1,19),
('DRG042', 'Glimestar',          'Glimepiride',      'Mankind',              'Tablet',   '2mg',   'OTC', 'Cool',20,1,20),
('DRG043', 'Lantus',             'Insulin Glargine', 'Sanofi',               'Injection', '100IU','H', 'Refrigerate',10,1,21),
('DRG044', 'Thyronorm',          'Thyroxine',        'Abbott',               'Tablet',   '50mcg', 'OTC', 'Cool',30,1,22),
('DRG045', 'NovoRapid',          'Insulin Aspart',   'Novo Nordisk',         'Injection','100IU', 'H', 'Refrigerate',10,1,23),
('DRG046', 'Xarelto',            'Rivaroxaban',      'Bayer',                'Tablet',   '20mg',  'H', 'Room temp',10,1,24),
('DRG047', 'Teneligliptin',      'Teneligliptin',    'Glenmark',             'Tablet',   '20mg',  'H', 'Cool',30,1,25),
('DRG048', 'Mixtard',            'Insulin',          'Novo Nordisk',         'Injection','30IU',  'H', 'Refrigerate',10,1,26),
('DRG049', 'Pradaxa',            'Dabigatran',       'Boehringer',           'Capsule',  '150mg', 'H', 'Cool',15,1,27),
('DRG050', 'Januvia',            'Sitagliptin',      'MSD',                  'Tablet',   '100mg', 'H', 'Cool',20,1,28),
('DRG051', 'Trulicity',          'Dulaglutide',      'Eli Lilly',            'Injection','1.5mg', 'H', 'Refrigerate',5,1,29),
('DRG052', 'Forxiga',            'Dapagliflozin',    'AstraZeneca',          'Tablet',   '10mg',  'H', 'Cool', 30,1,30),
('DRG053', 'Zyvox',              'Linezolid',        'Pfizer',               'Tablet',   '600mg', 'H', 'Cool',5,1,31),
('DRG054', 'Prevenar 13',        'Pneumococcal conjugate', 'Wyeth',           'Injection','0.5ml', 'S', 'Refrigerate',10,1,32),
('DRG055', 'Rebif',              'Interferon beta',  'Merck',                'Injection','44mcg', 'H', 'Refrigerate',7,1,33),
('DRG056', 'Valcyte',            'Valganciclovir',   'Roche',                'Tablet',   '450mg', 'H', 'Cool',5,1,34),
('DRG057', 'Clexane',            'Enoxaparin',       'Sanofi',               'Injection','40mg',  'H', 'Refrigerate',10,1,35),
('DRG058', 'Zinetac',            'Ranitidine',       'Glaxo',                'Tablet',   '300mg', 'OTC', 'Cool',20,1,36),
('DRG059', 'Herclon',            'Trastuzumab',      'Roche',                'Injection','440mg', 'H', 'Refrigerate',5,1,37),
('DRG060', 'Lenalid',            'Lenalidomide',     'Natco Pharma',         'Capsule',  '10mg',  'H', 'Cool',5,1,38);
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

-- ---------------------------------------------------------------------
-- ADDITIONAL EXTENDED SAMPLE DATA
-- ---------------------------------------------------------------------

-- Additional Suppliers
INSERT INTO supplier_master (supplier_id, supplier_name, contact_person, address, email, phone_number, gstin, payment_terms) VALUES
(4, 'Global APIs Inc.', 'David Miller', '405 API Blvd, CA', 'david.miller@globalapis.com', '1555123456', 'GST33445', 'Net 30'),
(5, 'EuroPharma Packaging', 'Sophie Laurent', '22 Rue Pasteur, Paris', 's.laurent@europharma.eu', '3319876543', 'GST55667', 'Net 45');

-- Additional Drugs/Materials
INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active) VALUES
('RM-IBU-API', 'Ibuprofen API', 'Ibuprofen', 'Global APIs Inc.', 4, 'RAW_MATERIAL', 'KG', 1),
('RM-MCC', 'Microcrystalline Cellulose', 'MCC Excipient', 'EuroPharma Packaging', 5, 'RAW_MATERIAL', 'KG', 1),
('PM-BOTTLE-100', 'HDPE Bottle 100ml', 'Packaging Bottle', 'EuroPharma Packaging', 5, 'PACKAGING', 'NOS', 1),
('DRG004', 'Ibuprofen 400mg Tablet', 'Ibuprofen', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1);

-- Additional Bill of Materials
INSERT INTO bom_header (bom_id, material_code, version_number, is_active, effective_date, description) VALUES
(3, 'DRG004', 1, 1, '2025-01-01', 'BOM for Ibuprofen 400mg Tablet (per 1000 tablets)');

INSERT INTO bom_details (bom_id, ingredient_material_code, required_qty, uom, sequence_number) VALUES
(3, 'RM-IBU-API', 0.4000, 'KG', 10),
(3, 'RM-MCC', 0.2000, 'KG', 20),
(3, 'PM-BOTTLE-100', 10.0000, 'NOS', 30); -- 100 tablets per bottle -> 10 bottles for 1000 tablets

-- Additional Purchase Orders
INSERT INTO purchase_order (po_id, supplier_id, order_date, expected_date, status) VALUES
(4, 4, '2025-10-05', '2025-10-10', 'Received'),
(5, 5, '2025-10-06', '2025-10-12', 'Received');

-- Additional Purchase Order Items
INSERT INTO purchaseorder_item (po_id, drug_id, quantity, unit_price) VALUES
(4, 'RM-IBU-API', 300.0000, 20.00),
(5, 'RM-MCC', 500.0000, 4.00),
(5, 'PM-BOTTLE-100', 2000.0000, 0.50);

-- Additional Stock Inventory
INSERT INTO stock_inventory (material_code, location_code, batch_number, quantity, reserved_quantity, unit_cost, mfg_date, exp_date, qc_status) VALUES
('RM-IBU-API', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-IBU-001', 300.0000, 0, 20.00, '2025-09-01', '2028-09-01', 'RELEASED'),
('RM-MCC', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-MCC-001', 500.0000, 0, 4.00, '2025-09-15', '2027-09-15', 'RELEASED'),
('PM-BOTTLE-100', 'PACKAGING_WAREHOUSE', 'BATCH-PM-BOT-001', 2000.0000, 0, 0.50, '2025-09-10', '2030-09-10', 'RELEASED'),
('DRG004', 'FINISHED_GOODS_WAREHOUSE', 'BATCH-IBU400-001', 10000.0000, 0, 0.60, '2025-11-20', '2027-11-19', 'RELEASED');

-- Additional Inventory Transactions
INSERT INTO inventory_transaction (material_code, batch_number, location_code, transaction_type, quantity, reference_type, reference_id, performed_by, notes) VALUES
('RM-IBU-API', 'BATCH-RM-IBU-001', 'RAW_MATERIAL_WAREHOUSE', 'GRN_RECEIPT', 300.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('RM-MCC', 'BATCH-RM-MCC-001', 'RAW_MATERIAL_WAREHOUSE', 'GRN_RECEIPT', 500.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('PM-BOTTLE-100', 'BATCH-PM-BOT-001', 'PACKAGING_WAREHOUSE', 'GRN_RECEIPT', 2000.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance');

-- ---------------------------------------------------------------------
-- 15 ADDITIONAL SAMPLE DATA EXTENSIONS
-- ---------------------------------------------------------------------

-- 1. Suppliers
INSERT INTO supplier_master (supplier_id, supplier_name, contact_person, address, email, phone_number, gstin, payment_terms) VALUES
(6, 'Kemico Industries', 'Mark Taylor', '808 Tech Park, IL', 'sales@kemico.com', '1231231234', 'GST77889', 'Net 30'),
(7, 'Alpha Blisters Ltd', 'Karen O Connor', '99 Packaging Way, UK', 'karen@alphablister.co.uk', '441234567', 'GST44556', 'Net 60');

-- 2. New Drugs/Materials (Raw Materials, Packaging, Excipients, Finished Goods)
INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active) VALUES
('RM-VITC-API', 'Ascorbic Acid API', 'Vitamin C', 'Kemico Industries', 6, 'RAW_MATERIAL', 'KG', 1),
('RM-ZINC-OX', 'Zinc Oxide', 'Zinc Supplement', 'Kemico Industries', 6, 'RAW_MATERIAL', 'KG', 1),
('RM-SUCROSE', 'Sucrose Excipient', 'Sucrose', 'HealthCare Supplies', 2, 'RAW_MATERIAL', 'KG', 1),
('RM-COLORANT', 'Orange Colorant', 'Food Color', 'Zenith Meds', 3, 'RAW_MATERIAL', 'KG', 1),
('PM-BOTTLE-60', 'HDPE Bottle 60ml', 'Packaging Bottle', 'Alpha Blisters Ltd', 7, 'PACKAGING', 'NOS', 1),
('PM-BOTTLE-CAP', 'Child-restant Cap', 'Cap Closure', 'Alpha Blisters Ltd', 7, 'PACKAGING', 'NOS', 1),
('PM-LABEL-VIT', 'Vitamin C Label', 'Printed Label', 'MedSupply Corp', 1, 'PACKAGING', 'ROLL', 1),
('DRG005', 'Vit-C Orange Chewable', 'Vitamin C 500mg', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1),
('DRG006', 'Zinc Boost 50mg', 'Zinc Oxide', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1);

-- 3. Bill of Materials
INSERT INTO bom_header (bom_id, material_code, version_number, is_active, effective_date, description) VALUES
(4, 'DRG005', 1, 1, '2025-01-01', 'BOM for Vit-C Orange Chewable 500mg (per 1000 tablets)'),
(5, 'DRG006', 1, 1, '2025-01-01', 'BOM for Zinc Boost 50mg (per 1000 tablets)');

INSERT INTO bom_details (bom_id, ingredient_material_code, required_qty, uom, sequence_number) VALUES
-- Vit-C BOM
(4, 'RM-VITC-API', 0.5000, 'KG', 10),
(4, 'RM-SUCROSE', 0.2000, 'KG', 20),
(4, 'RM-COLORANT', 0.0100, 'KG', 30),
(4, 'PM-BOTTLE-60', 20.0000, 'NOS', 40),
(4, 'PM-BOTTLE-CAP', 20.0000, 'NOS', 50),
(4, 'PM-LABEL-VIT', 0.0500, 'ROLL', 60),

-- Zinc Boost BOM
(5, 'RM-ZINC-OX', 0.0500, 'KG', 10),
(5, 'RM-SUCROSE', 0.1500, 'KG', 20),
(5, 'PM-BOTTLE-60', 20.0000, 'NOS', 30),
(5, 'PM-BOTTLE-CAP', 20.0000, 'NOS', 40);

-- 4. Purchase Orders
INSERT INTO purchase_order (po_id, supplier_id, order_date, expected_date, status) VALUES
(6, 6, '2025-10-15', '2025-10-20', 'Received'),
(7, 7, '2025-10-16', '2025-10-21', 'Received'),
(8, 2, '2025-10-17', '2025-10-22', 'Received');

-- 5. Purchase Order Items
INSERT INTO purchaseorder_item (po_id, drug_id, quantity, unit_price) VALUES
(6, 'RM-VITC-API', 500.0000, 15.00),
(6, 'RM-ZINC-OX', 200.0000, 10.00),
(7, 'PM-BOTTLE-60', 10000.0000, 0.40),
(7, 'PM-BOTTLE-CAP', 10000.0000, 0.15),
(8, 'RM-SUCROSE', 1000.0000, 2.00);

-- 6. Stock Inventory (Adding inventory for the new components)
INSERT INTO stock_inventory (material_code, location_code, batch_number, quantity, reserved_quantity, unit_cost, mfg_date, exp_date, qc_status) VALUES
('RM-VITC-API', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-VITC-001', 500.0000, 0, 15.00, '2025-09-10', '2027-09-10', 'RELEASED'),
('RM-ZINC-OX', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-ZINC-001', 200.0000, 0, 10.00, '2025-09-12', '2028-09-12', 'RELEASED'),
('RM-SUCROSE', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-SUCR-001', 1000.0000, 0, 2.00, '2025-08-01', '2026-08-01', 'RELEASED'),
('RM-COLORANT', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-COL-001', 50.0000, 0, 25.00, '2025-09-01', '2027-09-01', 'RELEASED'),
('PM-BOTTLE-60', 'PACKAGING_WAREHOUSE', 'BATCH-PM-BOT60-001', 10000.0000, 0, 0.40, '2025-09-05', '2030-09-05', 'RELEASED'),
('PM-BOTTLE-CAP', 'PACKAGING_WAREHOUSE', 'BATCH-PM-CAP-001', 10000.0000, 0, 0.15, '2025-09-06', '2030-09-06', 'RELEASED'),
('PM-LABEL-VIT', 'PACKAGING_WAREHOUSE', 'BATCH-PM-LBLVIT-001', 200.0000, 0, 3.00, '2025-09-15', '2030-09-15', 'RELEASED'),
('DRG005', 'FINISHED_GOODS_WAREHOUSE', 'BATCH-VITC-001', 5000.0000, 0, 0.70, '2025-11-25', '2027-11-24', 'RELEASED');

-- 7. Inventory Transactions for these new stocks
INSERT INTO inventory_transaction (material_code, batch_number, location_code, transaction_type, quantity, reference_type, reference_id, performed_by, notes) VALUES
('RM-VITC-API', 'BATCH-RM-VITC-001', 'RAW_MATERIAL_WAREHOUSE', 'GRN_RECEIPT', 500.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('RM-ZINC-OX', 'BATCH-RM-ZINC-001', 'RAW_MATERIAL_WAREHOUSE', 'GRN_RECEIPT', 200.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('RM-SUCROSE', 'BATCH-RM-SUCR-001', 'RAW_MATERIAL_WAREHOUSE', 'GRN_RECEIPT', 1000.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('RM-COLORANT', 'BATCH-RM-COL-001', 'RAW_MATERIAL_WAREHOUSE', 'GRN_RECEIPT', 50.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('PM-BOTTLE-60', 'BATCH-PM-BOT60-001', 'PACKAGING_WAREHOUSE', 'GRN_RECEIPT', 10000.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance'),
('PM-BOTTLE-CAP', 'BATCH-PM-CAP-001', 'PACKAGING_WAREHOUSE', 'GRN_RECEIPT', 10000.0000, 'INITIAL', 'OP_BAL', 1, 'Opening Balance');

-- ---------------------------------------------------------------------
-- ADDITIONAL EXTENDED SAMPLE DATA (from more_sample_data.sql)
-- ---------------------------------------------------------------------

-- 5 New Suppliers
INSERT INTO supplier_master (supplier_name, contact_person, address, email, phone_number, gstin, payment_terms) VALUES
('Apex Pharma', 'Chris Evans', '12 Industrial Way, WA', 'chris@apexpharma.com', '1235557890', 'GST22334', 'Net 30'),
('Nova Meds', 'Sarah Connor', '34 Bio Lane, CA', 'sarah@novameds.com', '9875554321', 'GST33445', 'Net 45'),
('Giga APIs', 'Elon Musk', '56 Tech Blvd, TX', 'elon@gigaapis.com', '1112223333', 'GST44556', 'Net 60'),
('Prime Packaging', 'Optimus Prime', '78 Carton St, NY', 'optimus@primepack.com', '4445556666', 'GST55667', 'Net 30'),
('Global Excipients', 'Bruce Wayne', '90 Gotham Ave, NJ', 'bruce@globalexcipients.com', '7778889999', 'GST66778', 'Net 45');

-- 10 New Drugs / Materials
INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active)
SELECT 'RM-ASPIRIN-API', 'Aspirin API', 'Acetylsalicylic acid', 'Apex Pharma', supplier_id, 'RAW_MATERIAL', 'KG', 1 FROM supplier_master WHERE supplier_name = 'Apex Pharma';

INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active)
SELECT 'RM-MAIZE', 'Maize Starch', 'Maize Starch Excipient', 'Global Excipients', supplier_id, 'RAW_MATERIAL', 'KG', 1 FROM supplier_master WHERE supplier_name = 'Global Excipients';

INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active)
SELECT 'PM-BOTTLE-200', 'HDPE Bottle 200ml', 'Packaging Bottle', 'Prime Packaging', supplier_id, 'PACKAGING', 'NOS', 1 FROM supplier_master WHERE supplier_name = 'Prime Packaging';

INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active)
SELECT 'PM-CAP-200', 'Bottle Cap 200ml', 'Bottle Cap', 'Prime Packaging', supplier_id, 'PACKAGING', 'NOS', 1 FROM supplier_master WHERE supplier_name = 'Prime Packaging';

INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active)
SELECT 'PM-LABEL-ASP', 'Aspirin Label', 'Printed Label', 'Prime Packaging', supplier_id, 'PACKAGING', 'ROLL', 1 FROM supplier_master WHERE supplier_name = 'Prime Packaging';

INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active) VALUES
('DRG-061', 'Aspirin 300mg Tablet', 'Aspirin', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1),
('DRG-062', 'Aspirin 500mg Tablet', 'Aspirin', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1);

INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active)
SELECT 'RM-TRAMADOL', 'Tramadol API', 'Tramadol HCl', 'Giga APIs', supplier_id, 'RAW_MATERIAL', 'KG', 1 FROM supplier_master WHERE supplier_name = 'Giga APIs';

INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active)
SELECT 'RM-DICLOFENAC', 'Diclofenac API', 'Diclofenac Sodium', 'Nova Meds', supplier_id, 'RAW_MATERIAL', 'KG', 1 FROM supplier_master WHERE supplier_name = 'Nova Meds';

INSERT INTO drug_master (material_code, brand_name, generic_name, manufacturer, preferred_supplier_id, material_type, unit_of_measure, is_active) VALUES
('DRG-063', 'Tramadol 50mg Capsule', 'Tramadol', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1),
('DRG-064', 'Diclofenac 50mg Tablet', 'Diclofenac', 'Internal', NULL, 'FINISHED_GOOD', 'NOS', 1);


-- 3 New Purchase Orders
INSERT INTO purchase_order (supplier_id, order_date, expected_date, status)
SELECT supplier_id, '2026-03-01', '2026-03-10', 'Received' FROM supplier_master WHERE supplier_name = 'Apex Pharma';
SET @po1 = LAST_INSERT_ID();

INSERT INTO purchaseorder_item (po_id, drug_id, quantity, unit_price) VALUES
(@po1, 'RM-ASPIRIN-API', 1000.00, 10.00);


INSERT INTO purchase_order (supplier_id, order_date, expected_date, status)
SELECT supplier_id, '2026-03-02', '2026-03-12', 'Pending' FROM supplier_master WHERE supplier_name = 'Nova Meds';
SET @po2 = LAST_INSERT_ID();

INSERT INTO purchaseorder_item (po_id, drug_id, quantity, unit_price) VALUES
(@po2, 'RM-DICLOFENAC', 500.00, 20.00);


INSERT INTO purchase_order (supplier_id, order_date, expected_date, status)
SELECT supplier_id, '2026-03-03', '2026-03-15', 'Received' FROM supplier_master WHERE supplier_name = 'Prime Packaging';
SET @po3 = LAST_INSERT_ID();

INSERT INTO purchaseorder_item (po_id, drug_id, quantity, unit_price) VALUES
(@po3, 'PM-BOTTLE-200', 10000.00, 0.50),
(@po3, 'PM-CAP-200', 10000.00, 0.10);


-- 2 New Goods Received Notes
INSERT INTO goods_received_note (po_id, received_date, received_by, status) VALUES
(@po1, '2026-03-08', 'admin', 'Verified');
SET @grn1 = LAST_INSERT_ID();

INSERT INTO grn_item (grn_id, drug_id, batch_number, quantity_received, expiry_date) VALUES
(@grn1, 'RM-ASPIRIN-API', 'BATCH-RM-ASP-001', 1000.00, '2028-03-01');

INSERT INTO stock_inventory (material_code, location_code, batch_number, quantity, reserved_quantity, unit_cost, mfg_date, exp_date, qc_status) VALUES
('RM-ASPIRIN-API', 'RAW_MATERIAL_WAREHOUSE', 'BATCH-RM-ASP-001', 1000.00, 0, 10.00, '2026-01-01', '2028-03-01', 'RELEASED');


INSERT INTO goods_received_note (po_id, received_date, received_by, status) VALUES
(@po3, '2026-03-10', 'admin', 'Verified');
SET @grn2 = LAST_INSERT_ID();

INSERT INTO grn_item (grn_id, drug_id, batch_number, quantity_received, expiry_date) VALUES
(@grn2, 'PM-BOTTLE-200', 'BATCH-PM-B200-001', 10000.00, '2030-03-01');

INSERT INTO stock_inventory (material_code, location_code, batch_number, quantity, reserved_quantity, unit_cost, mfg_date, exp_date, qc_status) VALUES
('PM-BOTTLE-200', 'PACKAGING_WAREHOUSE', 'BATCH-PM-B200-001', 10000.00, 0, 0.50, '2026-02-01', '2030-03-01', 'RELEASED');

-- 6. Enable Foreign Key Checks
SET FOREIGN_KEY_CHECKS = 1;
