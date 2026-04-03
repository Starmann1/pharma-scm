USE pharma_ims;
SET FOREIGN_KEY_CHECKS = 0;

-- Rename and populate target drugs
UPDATE material_master SET material_code = 'DRG007', formulation = 'Tablet', strength = '300mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG-061';
UPDATE material_master SET material_code = 'DRG008', formulation = 'Tablet', strength = '500mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG-062';
UPDATE material_master SET material_code = 'DRG009', formulation = 'Capsule', strength = '50mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG-063';
UPDATE material_master SET material_code = 'DRG010', formulation = 'Tablet', strength = '50mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG-064';

-- Populate formulation/strength for other sample drugs
UPDATE material_master SET formulation = 'Tablet', strength = '500mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG001';
UPDATE material_master SET formulation = 'Capsule', strength = '250mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG002';
UPDATE material_master SET formulation = 'Tablet', strength = '10mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG003';
UPDATE material_master SET formulation = 'Tablet', strength = '400mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG004';
UPDATE material_master SET formulation = 'Chewable Tablet', strength = '500mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG005';
UPDATE material_master SET formulation = 'Tablet', strength = '50mg', unit_of_measure = 'UNIT' WHERE material_code = 'DRG006';

-- Standardize UOM for packaging
UPDATE material_master SET unit_of_measure = 'UNIT' WHERE material_code LIKE 'PM-%';

-- Update all references to renamed drug codes
UPDATE purchaseorder_item SET drug_id = 'DRG010' WHERE drug_id = 'DRG-064';
UPDATE purchaseorder_item SET drug_id = 'DRG009' WHERE drug_id = 'DRG-063';
UPDATE purchaseorder_item SET drug_id = 'DRG008' WHERE drug_id = 'DRG-062';
UPDATE purchaseorder_item SET drug_id = 'DRG007' WHERE drug_id = 'DRG-061';

UPDATE grn_item SET drug_id = 'DRG010' WHERE drug_id = 'DRG-064';
UPDATE grn_item SET drug_id = 'DRG009' WHERE drug_id = 'DRG-063';
UPDATE grn_item SET drug_id = 'DRG008' WHERE drug_id = 'DRG-062';
UPDATE grn_item SET drug_id = 'DRG007' WHERE drug_id = 'DRG-061';

UPDATE stock_inventory SET material_code = 'DRG010' WHERE material_code = 'DRG-064';
UPDATE stock_inventory SET material_code = 'DRG009' WHERE material_code = 'DRG-063';
UPDATE stock_inventory SET material_code = 'DRG008' WHERE material_code = 'DRG-062';
UPDATE stock_inventory SET material_code = 'DRG007' WHERE material_code = 'DRG-061';

UPDATE bom_header SET material_code = 'DRG010' WHERE material_code = 'DRG-064';
UPDATE bom_header SET material_code = 'DRG009' WHERE material_code = 'DRG-063';
UPDATE bom_header SET material_code = 'DRG008' WHERE material_code = 'DRG-062';
UPDATE bom_header SET material_code = 'DRG007' WHERE material_code = 'DRG-061';

SET FOREIGN_KEY_CHECKS = 1;
