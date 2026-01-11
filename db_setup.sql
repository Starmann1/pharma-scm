/*
-- MySQL Database Setup for Pharmaceutical IMS
-- This script creates the database and all master/transactional tables,
-- including tables required for Purchase Orders (PO) and Goods Received Notes (GRN).
*/

-- 1. Create Database and Switch Context
CREATE DATABASE IF NOT EXISTS pharma_ims;
USE pharma_ims;

-- 2. Drop existing tables for a clean slate (Run this script multiple times)
DROP TABLE IF EXISTS GRN_Item;
DROP TABLE IF EXISTS Goods_Received_Note;
DROP TABLE IF EXISTS PurchaseOrder_Item;
DROP TABLE IF EXISTS Purchase_Order;
DROP TABLE IF EXISTS Stock_Inventory;
DROP TABLE IF EXISTS Drug_Master;
DROP TABLE IF EXISTS Supplier_Master;
DROP TABLE IF EXISTS Location_Master;
DROP TABLE IF EXISTS User_Master;

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

-- 6. User Master Table (Using plaintext passwords for simple AuthService.java match)
CREATE TABLE IF NOT EXISTS User_Master (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
('DRG001', 'Paracet-500', 'Paracetamol', 'XYZ Pharma', 'Tablet', '500mg', 'OTC', 'Store below 30°C', 100, TRUE, 1),
('DRG002', 'Amox-250', 'Amoxicillin', 'ABC Labs', 'Capsule', '250mg', 'Schedule H', 'Store in cool place', 50, TRUE, 2),
('DRG003', 'Cetiriz-10', 'Cetirizine', 'DEF Pharma', 'Tablet', '10mg', 'OTC', 'Store below 25°C', 75, TRUE, 1);

-- Sample Locations
INSERT INTO Location_Master (location_code, location_name, description, capacity) VALUES
('LOC-A1', 'Warehouse Zone A1', 'General medicines storage', 5000),
('LOC-B2', 'Cold Storage B2', 'Temperature controlled storage', 2000),
('LOC-C3', 'Retail Counter C3', 'Front desk inventory', 500);

-- Sample Users (Passwords are plaintext: 'adminpass', 'pharmacistpass', 'staffpass')
INSERT INTO User_Master (username, password_hash, full_name, role, is_active) VALUES
('admin', 'adminpass', 'System Administrator', 'Admin', TRUE), 
('pharmacist', 'pharmacistpass', 'Dr. Sarah Williams', 'Pharmacist', TRUE),
('staff', 'staffpass', 'Mike Brown', 'Staff', TRUE);


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
('BioPharm Paris SAS',           'Clément Dubois',     'c.dubois@bioparis.fr',       '+33-1-23456789',    'FR9988776655', 'FR-LC33221', 'Net 30'),
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
('Budapest HealthTrade Zrt', 'Dóra Szabó',         'd.szabo@healthtrade.hu',           '+36-1-2345678',    'HU5566788991', 'HU-BHT1937', 'Net 40'),
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