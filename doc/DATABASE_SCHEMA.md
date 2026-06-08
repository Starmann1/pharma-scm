# Database Schema

## Overview

The Agentic Pharmaceutical Supply Chain Management System relies on a MySQL 8+ relational database. The schema is designed to enforce referential integrity and provide auditable tracking of all regulated events.

The following schema design incorporates the **existing application database** tables (from `database.sql`) while introducing **new agentic tables** to support AI and RAG functionalities. This database layer acts as the single source of truth for both Version 1 (JADE Hybrid) and Version 2 (Google ADK) implementations.

---

## 1. Materials Management

**`material_master` (Existing)**
- `material_code` (PK, VARCHAR)
- `brand_name` (VARCHAR)
- `generic_name` (VARCHAR)
- `manufacturer` (VARCHAR)
- `formulation`, `strength`, `schedule_category` (VARCHAR)
- `storage_conditions` (TEXT)
- `reorder_level` (INT)
- `is_active` (BOOLEAN)
- `preferred_supplier_id` (FK -> `supplier_master.supplier_id`)
- `material_type` (ENUM: 'RAW_MATERIAL', 'PACKAGING', 'INTERMEDIATE', 'FINISHED_GOOD')
- `unit_of_measure` (VARCHAR)
- `created_at`, `updated_at` (TIMESTAMP)

## 2. Suppliers

**`supplier_master` (Existing)**
- `supplier_id` (PK, INT, Auto-increment)
- `supplier_name` (VARCHAR)
- `contact_person`, `address`, `email`, `phone_number` (VARCHAR)
- `gstin`, `drug_license_number` (VARCHAR)
- `payment_terms` (VARCHAR)
- `supplier_status` (VARCHAR, DEFAULT 'PENDING')
- `approved_at`, `rejected_at` (TIMESTAMP)
- `remarks` (TEXT)

**`supplier_performance` (New Agentic Table)**
- `id` (PK, INT, Auto-increment)
- `supplier_id` (FK -> `supplier_master.supplier_id`)
- `audit_date` (DATE)
- `score` (DECIMAL)
- `delivery_reliability_pct` (DECIMAL)
- `quality_defect_rate` (DECIMAL)

## 3. Inventory & Warehousing

**`location_master` (Existing)**
- `location_code` (PK, VARCHAR)
- `location_name` (VARCHAR)
- `description` (TEXT)
- `capacity` (INT)

**`stock_inventory` (Existing)**
- `stock_id` (PK, INT, Auto-increment)
- `material_code` (FK -> `material_master.material_code`)
- `location_code` (FK -> `location_master.location_code`)
- `batch_number` (VARCHAR)
- `quantity` (DECIMAL)
- `reserved_quantity` (DECIMAL)
- `available_quantity` (DECIMAL, GENERATED ALWAYS)
- `unit_cost` (DECIMAL)
- `mfg_date`, `exp_date` (DATE)
- `qc_status` (VARCHAR, DEFAULT 'APPROVED')
- *Indexes*: UNIQUE KEY (material_code, location_code, batch_number)

**`inventory_transaction` (Existing)**
- Tracks `GRN_RECEIPT`, `PRODUCTION_CONSUMPTION`, `STOCK_TRANSFER`, etc.

## 4. Procurement

**`purchase_order` (Existing)**
- `po_id` (PK, INT, Auto-increment)
- `supplier_id` (FK -> `supplier_master.supplier_id`)
- `order_date`, `expected_date` (DATE)
- `total_amount` (DECIMAL)
- `status` (VARCHAR: 'Pending', 'Received', etc.)

**`purchaseorder_item` (Existing)**
- `po_item_id` (PK, INT)
- `po_id` (FK -> `purchase_order.po_id`)
- `drug_id` (FK -> `material_master.material_code`)
- `quantity`, `unit_price` (DECIMAL)

**`goods_received_note` & `grn_item` (Existing)**
- Links received batches against POs.

## 5. Production

**`production_order` (Existing/Implied)**
- `order_id` (PK, INT, Auto-increment)
- `finished_material_code` (FK -> `material_master.material_code`)
- `target_quantity` (DECIMAL)
- `status` (VARCHAR: 'PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')

**`production_batch` (Existing)**
- `batch_id` (PK, INT)
- `production_order_id` (FK -> `production_order.order_id`)
- `material_code` (FK -> `material_master.material_code`)
- `batch_number` (VARCHAR, Unique)
- `quantity` (DECIMAL)
- `qc_status` (VARCHAR, DEFAULT 'QUARANTINE')

**`batch_genealogy` (Existing)**
- Links parent and child batches for full traceability.

## 6. Quality & Compliance (New Agentic Tables)

**`qa_records`**
- `id` (PK, INT, Auto-increment)
- `batch_number` (VARCHAR, Indexed)
- `test_type` (VARCHAR)
- `result` (ENUM: 'PASS', 'FAIL', 'PENDING')
- `tested_by` (VARCHAR)
- `tested_at` (TIMESTAMP)

**`deviation_records`**
- `id` (PK, INT, Auto-increment)
- `batch_number` (VARCHAR)
- `description` (TEXT)
- `criticality` (ENUM: 'MINOR', 'MAJOR', 'CRITICAL')
- `status` (ENUM: 'OPEN', 'INVESTIGATING', 'CLOSED')

**`capa_records`**
- `id` (PK, INT, Auto-increment)
- `deviation_id` (FK -> `deviation_records.id`)
- `action_plan` (TEXT)
- `due_date` (DATE)
- `status` (ENUM: 'OPEN', 'IMPLEMENTED', 'VERIFIED')

**`compliance_records`**
- `id` (PK, INT, Auto-increment)
- `reference_id` (VARCHAR) -- Generic reference to batch or PO
- `rule_checked` (VARCHAR)
- `is_compliant` (BOOLEAN)
- `checked_at` (TIMESTAMP)

## 7. Core Audit & Tracing

**`system_audit_trail` (Existing)**
- Tracks CRUD events with `old_value` and `new_value` (JSON/TEXT).

**`event_log` (Existing)**
- Application-level business events (`LOW_STOCK`, `QC_APPROVED`).

**`risk_assessments` (New Agentic Table)**
- `id` (PK, INT, Auto-increment)
- `context_type` (VARCHAR) -- e.g., 'SUPPLIER', 'BATCH'
- `context_id` (VARCHAR)
- `risk_score` (DECIMAL)
- `mitigation_notes` (TEXT)
- `assessed_at` (TIMESTAMP)

**`ai_decisions` (New Agentic Table)**
- `id` (PK, BIGINT, Auto-increment)
- `transaction_id` (VARCHAR, Indexed)
- `agent_name` (VARCHAR)
- `prompt_summary` (TEXT)
- `response_summary` (TEXT)
- `confidence_score` (DECIMAL)
- `decision_applied` (BOOLEAN)
- `created_at` (TIMESTAMP)

## 8. RAG Knowledge Base (New Agentic Tables)

**`knowledge_documents`**
- `id` (PK, INT, Auto-increment)
- `title` (VARCHAR)
- `document_type` (ENUM: 'SOP', 'SPECIFICATION', 'POLICY', 'REGULATION')
- `version` (VARCHAR)
- `file_path` (VARCHAR)
- `ingested_at` (TIMESTAMP)

**`document_chunks`**
- `id` (PK, BIGINT, Auto-increment)
- `document_id` (FK -> `knowledge_documents.id`)
- `chunk_index` (INT)
- `content` (TEXT)
- `embedding_id` (VARCHAR) -- Refers to external vector store ID

---

## Future Vector Search Strategy

Currently, MySQL lacks highly optimized native vector search capabilities for massive embedding arrays (though MySQL 9 adds vector support, the current stack is MySQL 8+). 
**Strategy**: 
1. Store document chunks and metadata in the relational `document_chunks` and `knowledge_documents` tables.
2. Store the actual Vector Embeddings in an external vector database (like Milvus, Pinecone, or a local ChromaDB instance) or utilize a LangChain4j in-memory embedding store for early phases.
3. The `embedding_id` in `document_chunks` links the relational metadata to the exact vector representation.
4. When RAG executes, the system retrieves the `embedding_id` from the vector store and fetches the corresponding full text from MySQL.

---

## Architecture Support

### JADE Version (Version 1)
JADE agents interact with this schema strictly via the **Service Layer** and **Repository Layer**. No JADE behaviour will execute SQL directly. `ai_decisions` tracks the reasoning traces of the JADE `AIReasoningAgent`.

### Google ADK Version (Version 2)
Because all database logic is encapsulated in JDBC Repositories and Services, transitioning to Google ADK requires **zero database schema changes**. ADK tools will call the same Java Service methods, logging identical audit trails and leveraging the same relational models.

### RAG Version
The `knowledge_documents` and `document_chunks` tables provide standard relational referential integrity to SOPs and regulations. This ensures that any AI advisory output can reference a traceable, version-controlled document ID stored securely in the local RDBMS.
