# Workflows

## Overview

The following workflows represent the core operational loops within the Agentic Pharmaceutical Supply Chain Management System. Each workflow involves human actors, deterministic agents, and AI advisory components.

---

## 1. Material Procurement

*   **Actors**: Procurement Officer, CoordinatorAgent, SupplierAgent, InventoryAgent.
*   **Trigger**: Inventory level of a raw material falls below the reorder point, or a human manually initiates a PO request.
*   **Input**: `MaterialCode`, `RequiredQuantity`, `TargetDate`.
*   **Processing Steps**:
    1. `InventoryAgent` detects low stock and sends an alert.
    2. `CoordinatorAgent` receives the alert and queries `SupplierAgent` for approved suppliers of the material.
    3. `SupplierAgent` returns a list of approved suppliers ranked by performance score.
    4. `CoordinatorAgent` generates a Draft Purchase Order.
    5. Procurement Officer reviews and approves the PO in the UI.
*   **Agent Interaction Sequence**:
    `InventoryAgent` -> (INFORM) -> `CoordinatorAgent` -> (REQUEST) -> `SupplierAgent` -> (INFORM) -> `CoordinatorAgent`.
*   **Expected Output**: An Approved Purchase Order sent to the supplier.
*   **Failure Scenarios**: No approved suppliers exist -> Escalates to Procurement Manager.

```text
InventoryAgent    CoordinatorAgent      SupplierAgent        UI/Officer
      |                  |                    |                   |
      |-- Low Stock ---->|                    |                   |
      |                  |-- Find Suppliers ->|                   |
      |                  |<-- Ranked List ----|                   |
      |                  |-- Draft PO --------------------------->|
      |                  |<------------------------- Approve PO --|
```

---

## 2. Inventory Replenishment

*   **Actors**: Warehouse Operator, CoordinatorAgent, QAAgent.
*   **Trigger**: Goods Received Note (GRN) is entered upon material delivery.
*   **Input**: `PONumber`, `BatchNumber`, `ReceivedQuantity`.
*   **Processing Steps**:
    1. Operator logs GRN.
    2. System creates inventory records in `QUARANTINE` status.
    3. `CoordinatorAgent` notifies `QAAgent` that a new batch requires sampling and testing.
*   **Agent Interaction Sequence**:
    `UI` -> (REQUEST) -> `CoordinatorAgent` -> (REQUEST) -> `QAAgent`.
*   **Expected Output**: Quarantined inventory awaiting QA approval.

---

## 3. Manufacturing Feasibility Analysis

*   **Actors**: Production Planner, ProductionAgent, InventoryAgent, RiskAnalysisAgent.
*   **Trigger**: Planner requests feasibility for a new Production Order.
*   **Input**: `FinishedGoodCode`, `TargetQuantity`, `TargetDate`.
*   **Processing Steps**:
    1. `ProductionAgent` retrieves the Bill of Materials (BOM).
    2. `ProductionAgent` queries `InventoryAgent` for available, `RELEASED` stock of all BOM components.
    3. `ProductionAgent` queries `RiskAnalysisAgent` for potential delay risks.
    4. Aggregates responses and presents a Feasibility Report.
*   **Agent Interaction Sequence**:
    `CoordinatorAgent` -> (REQUEST) -> `ProductionAgent`
    `ProductionAgent` -> (REQUEST) -> `InventoryAgent`
    `ProductionAgent` -> (REQUEST) -> `RiskAnalysisAgent`
*   **Expected Output**: A Go/No-Go feasibility report with risk mitigation notes.
*   **Failure Scenarios**: Insufficient raw materials -> ProductionAgent suggests a revised lower target quantity.

---

## 4. Production Planning

*   **Actors**: Production Planner, ProductionAgent.
*   **Trigger**: Feasibility report is approved.
*   **Input**: `FeasibilityReportID`.
*   **Processing Steps**:
    1. `ProductionAgent` locks inventory allocations (changes status to `IN_PRODUCTION`).
    2. Generates Batch Manufacturing Record (BMR) draft.
    3. Schedules the order in `PLANNED` state.
*   **Expected Output**: `PLANNED` production order and reserved inventory.

---

## 5. Batch Lifecycle

*   **Actors**: Manufacturing Operator, ComplianceAgent.
*   **Trigger**: Operator starts batch execution.
*   **Input**: `BatchNumber`.
*   **Processing Steps**:
    1. Operator logs start time and component consumption.
    2. Operator logs yield.
    3. `ComplianceAgent` continuously audits timestamp sequences and user permissions.
*   **Expected Output**: Completed batch in `QUARANTINE` state.

---

## 6. QA Approval & Batch Release

*   **Actors**: QA Manager, QAAgent, KnowledgeAgent.
*   **Trigger**: QA test results are logged.
*   **Input**: `BatchNumber`, `TestResults`.
*   **Processing Steps**:
    1. QA Manager enters IPQC results.
    2. `QAAgent` evaluates results against specifications.
    3. If a deviation occurs, `QAAgent` requests context from `KnowledgeAgent` regarding similar past deviations.
    4. `QAAgent` recommends PASS or FAIL.
    5. QA Manager performs final Batch Release.
*   **Agent Interaction Sequence**:
    `QAAgent` -> (REQUEST) -> `KnowledgeAgent` -> (INFORM) -> `QAAgent`.
*   **Expected Output**: Batch status changes to `RELEASED` or `REJECTED`.

---

## 7. Supplier Selection

*   **Actors**: SupplierAgent, AIReasoningAgent.
*   **Trigger**: New material requires sourcing, or an existing supplier is disqualified.
*   **Input**: `MaterialCode`.
*   **Processing Steps**:
    1. `SupplierAgent` requests analysis of global supplier databases.
    2. `AIReasoningAgent` parses unstructured market reports or audit documents to gauge supplier viability.
    3. Recommends top 3 candidates to human procurement.
*   **Expected Output**: Ranked list of potential new suppliers.

---

## 8. Root Cause Analysis

*   **Actors**: QA Analyst, AIReasoningAgent, KnowledgeAgent.
*   **Trigger**: A Critical Deviation is logged.
*   **Input**: `DeviationRecordID`.
*   **Processing Steps**:
    1. `AIReasoningAgent` receives the deviation text.
    2. Prompts `KnowledgeAgent` for related CAPAs and SOPs.
    3. Synthesizes a probable root cause report.
*   **Expected Output**: AI-generated Root Cause hypothesis.

---

## 9. Risk Prediction

*   **Actors**: RiskAnalysisAgent.
*   **Trigger**: Daily scheduled cron job.
*   **Processing Steps**:
    1. Agent scans active POs and production batches.
    2. Evaluates historical failure rates and current supplier performance scores.
    3. Flags high-risk operations on the main dashboard.
*   **Expected Output**: Updated `risk_assessments` table.

---

## 10. Knowledge Retrieval

*   **Actors**: Any Agent, KnowledgeAgent.
*   **Trigger**: Query for regulatory guidelines or SOPs.
*   **Input**: `SearchQuery`.
*   **Processing Steps**:
    1. Embeds query.
    2. Performs vector search against `document_chunks`.
    3. Returns top-K chunks with citations.

---

## 11. Simulation Sandbox

*   **Actors**: Human Planner, All Agents.
*   **Trigger**: User initiates a "What-If" scenario.
*   **Input**: `SimulatedInputs`, `simulationMode=true`.
*   **Processing Steps**:
    1. Agents perform their standard feasibility and planning workflows.
    2. Database Service blocks commit transactions, or writes to temporary in-memory tables.
    3. System returns projected timeline and bottlenecks.
*   **Expected Output**: Simulation Report (no state mutated).
