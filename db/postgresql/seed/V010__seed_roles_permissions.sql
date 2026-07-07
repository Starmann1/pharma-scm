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
INSERT INTO permission_master (permission_name, description) VALUES 
-- System Admin
('MANAGE_USERS', 'Create, update, and delete users'),
('MANAGE_ROLES', 'Manage roles and permissions'),
-- QA
('APPROVE_QA', 'Approve quality assurance tests'),
('REJECT_QA', 'Reject quality assurance tests'),
('VIEW_QA_REPORTS', 'View QA compliance reports'),
-- Warehouse
('CREATE_GRN', 'Create Goods Received Note'),
('TRANSFER_STOCK', 'Transfer stock between locations'),
('ADJUST_STOCK', 'Adjust inventory levels'),
('VIEW_INVENTORY', 'View inventory and stock levels'),
-- Procurement
('CREATE_PO', 'Create Purchase Orders'),
('APPROVE_PO', 'Approve Purchase Orders'),
('MANAGE_SUPPLIERS', 'Manage Supplier Master data'),
-- Production
('CREATE_PRODUCTION_ORDER', 'Create Production Orders'),
('MANAGE_BOM', 'Manage Bill of Materials'),
('RECORD_CONSUMPTION', 'Record material consumption')
ON CONFLICT (permission_name) DO NOTHING;

-- 3. Map Permissions to Roles (Assuming IDs match the order of insertion or using subqueries)
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
WHERE r.role_name = 'QA Manager' AND p.permission_name IN ('APPROVE_QA', 'REJECT_QA', 'VIEW_QA_REPORTS')
ON CONFLICT DO NOTHING;

-- Map Warehouse Manager
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM role_master r, permission_master p 
WHERE r.role_name = 'Warehouse Manager' AND p.permission_name IN ('CREATE_GRN', 'TRANSFER_STOCK', 'ADJUST_STOCK', 'VIEW_INVENTORY')
ON CONFLICT DO NOTHING;

-- Map Procurement
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM role_master r, permission_master p 
WHERE r.role_name = 'Procurement' AND p.permission_name IN ('CREATE_PO', 'APPROVE_PO', 'MANAGE_SUPPLIERS', 'VIEW_INVENTORY')
ON CONFLICT DO NOTHING;

-- Map Production
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM role_master r, permission_master p 
WHERE r.role_name = 'Production' AND p.permission_name IN ('CREATE_PRODUCTION_ORDER', 'MANAGE_BOM', 'RECORD_CONSUMPTION', 'VIEW_INVENTORY')
ON CONFLICT DO NOTHING;
