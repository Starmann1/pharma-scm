USE pharma_ims;

SET @supplier_status_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Supplier_Master'
      AND COLUMN_NAME = 'supplier_status'
);
SET @sql := IF(
    @supplier_status_exists = 0,
    'ALTER TABLE Supplier_Master ADD COLUMN supplier_status VARCHAR(20) DEFAULT ''PENDING''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @approved_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Supplier_Master'
      AND COLUMN_NAME = 'approved_at'
);
SET @sql := IF(
    @approved_at_exists = 0,
    'ALTER TABLE Supplier_Master ADD COLUMN approved_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rejected_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Supplier_Master'
      AND COLUMN_NAME = 'rejected_at'
);
SET @sql := IF(
    @rejected_at_exists = 0,
    'ALTER TABLE Supplier_Master ADD COLUMN rejected_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @remarks_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Supplier_Master'
      AND COLUMN_NAME = 'remarks'
);
SET @sql := IF(
    @remarks_exists = 0,
    'ALTER TABLE Supplier_Master ADD COLUMN remarks TEXT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS supplier_audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id INT NOT NULL,
    action VARCHAR(20),
    remarks TEXT,
    performed_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES Supplier_Master(supplier_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

UPDATE Supplier_Master
SET supplier_status = 'APPROVED',
    approved_at = COALESCE(approved_at, CURRENT_TIMESTAMP)
WHERE supplier_status IS NULL
   OR supplier_status = 'PENDING';
