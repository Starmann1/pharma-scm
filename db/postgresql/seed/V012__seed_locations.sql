-- V012__seed_locations.sql
-- Description: Inserts standard locations used in the Pharmaceutical SCM

INSERT INTO location_master (location_code, location_name, description, capacity) VALUES 
('WH-MAIN', 'Main Warehouse', 'Primary storage location', 10000),
('QC-HOLD', 'QC Hold Area', 'Quarantine location for quality checks', 2000),
('REJECTED', 'Rejected Materials', 'Disposal location for rejected goods', 1000),
('MFG-01', 'Manufacturing Floor 01', 'Active production floor', 5000)
ON CONFLICT (location_code) DO NOTHING;
