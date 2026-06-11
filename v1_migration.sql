CREATE TABLE IF NOT EXISTS ai_decisions (
    transaction_id VARCHAR(255) PRIMARY KEY,
    task_type VARCHAR(100),
    prompt_summary TEXT,
    confidence_score DECIMAL(5,4),
    raw_output TEXT,
    requires_human_review BOOLEAN DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    reviewed_by INT DEFAULT NULL,
    review_reason TEXT,
    reviewed_at DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE purchase_order ADD COLUMN actual_delivery_date DATE NULL;
