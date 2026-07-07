-- V010__seed_roles_permissions.sql
-- Description: Inserts base roles and permissions for Pharma IMS

-- 1. Insert Base Roles
INSERT INTO role_master (role_name, description) VALUES 
('Admin', 'System Administrator with full access'),
('QA Manager', 'Quality Assurance Manager for QC Hold, Release, and Audits'),
('Warehouse Manager', 'Manages inventory, GRN, and stock transfers'),
('Procurement', 'Manages Purchase Orders and Suppliers'),
('Production', 'Manages Production Orders and BOMs')
ON CONFLICT (role_name) DO NOTHING;

-- 2. Insert Base Permissions
INSERT INTO permission_master (permission_name, module, description) VALUES 
-- System Admin
('MANAGE_USERS', 'Admin', 'Create, update, and delete users'),
('MANAGE_ROLES', 'Admin', 'Manage roles and permissions'),
-- QA
('APPROVE_QA', 'Quality', 'Approve quality assurance tests'),
('REJECT_QA', 'Quality', 'Reject quality assurance tests'),
('VIEW_QA_REPORTS', 'Quality', 'View QA compliance reports'),
('UPDATE_QC_STATUS', 'Quality', 'Update quality control status'),
('VIEW_BATCH_TRACEABILITY', 'Quality', 'View batch traceability'),
-- Warehouse
('CREATE_GRN', 'Warehouse', 'Create Goods Received Note'),
('TRANSFER_STOCK', 'Warehouse', 'Transfer stock between locations'),
('ADJUST_STOCK', 'Warehouse', 'Adjust inventory levels'),
('VIEW_INVENTORY', 'Warehouse', 'View inventory and stock levels'),
('MANAGE_LOCATIONS', 'Locations', 'Manage warehouse locations'),
('RECEIVE_PO', 'Warehouse', 'Receive purchase orders (GRN)'),
-- Procurement
('CREATE_PO', 'Procurement', 'Create Purchase Orders'),
('APPROVE_PO', 'Procurement', 'Approve Purchase Orders'),
('MANAGE_SUPPLIERS', 'Procurement', 'Manage Supplier Master data'),
('VIEW_SUPPLIERS', 'Procurement', 'View supplier information'),
('VIEW_PO', 'Procurement', 'View purchase orders'),
-- Production
('CREATE_PRODUCTION_ORDER', 'Production', 'Create Production Orders'),
('MANAGE_BOM', 'Production', 'Manage Bill of Materials'),
('RECORD_CONSUMPTION', 'Production', 'Record material consumption'),
('VIEW_BOM', 'Production', 'View bill of materials'),
-- Materials
('VIEW_DRUG', 'Materials', 'View master drug/material data'),
-- Reports
('VIEW_REPORTS', 'Reports', 'View system reports')
ON CONFLICT (permission_name) DO UPDATE SET module = EXCLUDED.module;

-- 3. Map Permissions to Roles
-- Map Admin (All Permissions)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM role_master r, permission_master p 
WHERE r.role_name = 'Admin'
ON CONFLICT DO NOTHING;

-- Map QA Manager
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM role_master r, permission_master p 
WHERE r.role_name = 'QA Manager' AND p.module = 'Quality'
ON CONFLICT DO NOTHING;

-- Map Warehouse Manager
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM role_master r, permission_master p 
WHERE r.role_name = 'Warehouse Manager' AND p.module IN ('Warehouse', 'Locations')
ON CONFLICT DO NOTHING;

-- Map Procurement
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM role_master r, permission_master p 
WHERE r.role_name = 'Procurement' AND p.module = 'Procurement'
ON CONFLICT DO NOTHING;

-- Map Production
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM role_master r, permission_master p 
WHERE r.role_name = 'Production' AND p.module = 'Production'
ON CONFLICT DO NOTHING;
