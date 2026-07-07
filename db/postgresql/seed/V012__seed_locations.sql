-- V012__seed_locations.sql
-- Description: Inserts standard locations used in the Pharmaceutical SCM

INSERT INTO location_master (location_code, location_name, location_type, capacity, current_occupancy, is_active) VALUES 
('WH-MAIN', 'Main Warehouse', 'STORAGE', 10000.0, 0.0, TRUE),
('QC-HOLD', 'QC Hold Area', 'QUARANTINE', 2000.0, 0.0, TRUE),
('REJECTED', 'Rejected Materials', 'REJECTED', 1000.0, 0.0, TRUE),
('MFG-01', 'Manufacturing Floor 01', 'PRODUCTION', 5000.0, 0.0, TRUE)
ON CONFLICT (location_code) DO NOTHING;
