-- V013__seed_reference_data.sql
-- Description: Inserts sample suppliers and materials

-- 1. Insert Suppliers
INSERT INTO supplier_master (supplier_name, contact_person, email, phone_number, address, supplier_status) VALUES 
('Global API Corp', 'John Smith', 'john@globalapi.com', '+1234567890', '123 API Ave', 'APPROVED'),
('MediPack Solutions', 'Alice Jones', 'alice@medipack.com', '+0987654321', '456 Pack St', 'APPROVED')
ON CONFLICT DO NOTHING;

-- 2. Insert Materials
INSERT INTO material_master (
    material_code, brand_name, generic_name, manufacturer, formulation, strength, 
    schedule_category, storage_conditions, reorder_level, is_active, 
    material_type, unit_of_measure
) VALUES 
('RM-001', 'Paracetamol API', 'Paracetamol', 'Global API Corp', 'Powder', '99%', 'Unscheduled', 'Room Temperature', 500, TRUE, 'RAW_MATERIAL', 'KG'),
('PM-001', 'Blister Pack Type A', 'PVC/Alu', 'MediPack Solutions', 'Packaging', 'N/A', 'Unscheduled', 'Room Temperature', 10000, TRUE, 'PACKAGING', 'UNITS'),
('FG-001', 'Paracure 500mg', 'Paracetamol Tablets', 'Internal', 'Tablet', '500mg', 'OTC', 'Room Temperature', 100, TRUE, 'FINISHED_GOOD', 'BOXES')
ON CONFLICT (material_code) DO NOTHING;
