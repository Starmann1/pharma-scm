# Agent Architecture

## Overview

The Agentic Pharmaceutical Supply Chain Management System employs a multi-agent architecture to automate, coordinate, and optimize upstream supply chain operations.

**Version 1** relies on a JADE (Java Agent Development Framework) based platform for operational orchestration, ensuring separation of business logic (handled by Services) from task coordination (handled by Agents).
**Version 2** will migrate this orchestration layer to a pure Google ADK (Agent Development Kit) architecture.

## Agent Definitions

### 1. CoordinatorAgent
*   **Purpose**: Acts as the central orchestrator and gateway between the external UI/API layer and the internal agent ecosystem.
*   **Responsibilities**: Receives tasks from the `AgentGateway`, delegates them to specific domain agents, aggregates responses, and returns the final decision/status to the requester.
*   **Input Messages**: `AgentRequestEnvelope` (from Gateway), `REQUEST` (internal task requests).
*   **Output Messages**: `AgentResponseEnvelope` (to Gateway), `REQUEST` (to domain agents).
*   **Behaviours**: `CyclicBehaviour` for listening to gateway requests, `AchieveREInitiator` for managing delegated tasks.
*   **Dependencies**: `AgentGateway`.
*   **Failure Handling**: If a domain agent fails to respond within the deadline, marks the task as FAILED and returns an error response.
*   **Escalation Rules**: Escalates unresolvable workflow deadlocks to human operators via UI alerts.

### 2. InventoryAgent
*   **Purpose**: Monitors and manages inventory levels, predicting shortages and tracking material movements.
*   **Responsibilities**: Validates material availability, proposes replenishment orders, and tracks quarantine/release states in conjunction with QAAgent.
*   **Input Messages**: `REQUEST` (Check availability, Reserve stock, Replenish).
*   **Output Messages**: `INFORM` (Stock levels, Reservations confirmed), `FAILURE` (Out of stock).
*   **Behaviours**: `TickerBehaviour` (periodic stock level checks), `AchieveREResponder` (handling requests).
*   **Dependencies**: `InventoryService`, `MaterialService`.
*   **Failure Handling**: Rolls back temporary reservations if the broader transaction fails.
*   **Escalation Rules**: Alerts CoordinatorAgent if critical raw materials fall below safety thresholds and no suppliers are available.

### 3. ProductionAgent
*   **Purpose**: Orchestrates production planning and batch manufacturing feasibility.
*   **Responsibilities**: Verifies BOM (Bill of Materials) availability, checks machine/line capacity, and initiates batch records.
*   **Input Messages**: `REQUEST` (Plan batch, Check feasibility).
*   **Output Messages**: `INFORM` (Batch planned, Feasibility report), `FAILURE` (Cannot produce).
*   **Behaviours**: `AchieveREResponder` (responds to planning requests).
*   **Dependencies**: `ProductionService`.
*   **Failure Handling**: Aborts production planning if BOM is incomplete or QA rejects components.
*   **Escalation Rules**: Escalates to human planner if production delays threaten delivery schedules.

### 4. SupplierAgent
*   **Purpose**: Manages supplier interactions, performance scoring, and procurement eligibility.
*   **Responsibilities**: Checks supplier approval status, tracks audit expiry, and scores suppliers based on past delivery performance.
*   **Input Messages**: `REQUEST` (Verify supplier, Score supplier).
*   **Output Messages**: `INFORM` (Supplier valid, Score data), `FAILURE` (Supplier rejected/unapproved).
*   **Behaviours**: `AchieveREResponder`.
*   **Dependencies**: `SupplierService`.
*   **Failure Handling**: Defaults to 'Unapproved' if supplier status cannot be verified.
*   **Escalation Rules**: Flags suppliers with expired licenses for manual compliance review.

### 5. QAAgent
*   **Purpose**: Enforces quality control and assurance gates.
*   **Responsibilities**: Evaluates IPQC results, manages quarantine/release transitions, and prevents usage of rejected batches.
*   **Input Messages**: `REQUEST` (Verify batch quality, Request release).
*   **Output Messages**: `INFORM` (Approved/Released), `FAILURE` (Quarantined/Rejected).
*   **Behaviours**: `AchieveREResponder`.
*   **Dependencies**: `QAService`.
*   **Failure Handling**: Denies release if test results are missing or inconclusive.
*   **Escalation Rules**: Escalates critical Out-of-Specification (OOS) results to human QA Manager.

### 6. ComplianceAgent
*   **Purpose**: Monitors systemic compliance against regulatory frameworks (e.g., FDA, GMP).
*   **Responsibilities**: Audits transaction logs, verifies digital signatures, and cross-checks CAPA (Corrective and Preventive Actions) records.
*   **Input Messages**: `REQUEST` (Verify compliance, Audit transaction).
*   **Output Messages**: `INFORM` (Compliant), `FAILURE` (Non-compliant).
*   **Behaviours**: `CyclicBehaviour` (listening to audit event streams), `AchieveREResponder`.
*   **Dependencies**: `ComplianceService`, `AuditService`.
*   **Failure Handling**: Blocks non-compliant transactions immediately.
*   **Escalation Rules**: Escalates critical regulatory breaches to the Compliance Officer.

### 7. RiskAnalysisAgent
*   **Purpose**: Predicts and assesses operational and supply chain risks.
*   **Responsibilities**: Evaluates risk of supplier failure, inventory stockouts, or production delays using historical data.
*   **Input Messages**: `REQUEST` (Assess risk for PO/Batch).
*   **Output Messages**: `INFORM` (Risk score and mitigation recommendations).
*   **Behaviours**: `AchieveREResponder`.
*   **Dependencies**: `RiskService`.
*   **Failure Handling**: Returns 'Unknown Risk' if data is insufficient, defaulting to conservative workflows.
*   **Escalation Rules**: Escalates 'High Risk' assessments for human override.

### 8. AIReasoningAgent
*   **Purpose**: Bridges deterministic agent workflows with LLM-based reasoning capabilities.
*   **Responsibilities**: Processes unstructured text, generates insights, and evaluates complex heuristics that exceed standard rule engines.
*   **Input Messages**: `REQUEST` (Analyze unstructured data, Propose optimization).
*   **Output Messages**: `INFORM` (AI advisory insight).
*   **Behaviours**: `AchieveREResponder`.
*   **Dependencies**: `Gemini API`, `PromptService`.
*   **Failure Handling**: Returns graceful degradation (fallback to deterministic rules) if AI service is unreachable.
*   **Escalation Rules**: Flags low-confidence AI outputs for human review.

### 9. KnowledgeAgent
*   **Purpose**: Interfaces with the Retrieval-Augmented Generation (RAG) layer.
*   **Responsibilities**: Retrieves SOPs, policy documents, and historical deviation records to ground AI reasoning.
*   **Input Messages**: `REQUEST` (Retrieve context for query).
*   **Output Messages**: `INFORM` (Document chunks and citations).
*   **Behaviours**: `AchieveREResponder`.
*   **Dependencies**: `LangChain4j`, `RetrievalService`.
*   **Failure Handling**: Returns empty context if retrieval fails.
*   **Escalation Rules**: Logs missing documentation for administrator updates.

---

## ACL Message Standards

All inter-agent communication follows FIPA ACL (Agent Communication League) specifications.
- **Performatives Used**:
  - `REQUEST`: To command another agent to perform an action.
  - `INFORM`: To provide requested information or notify of a state change.
  - `FAILURE`: To indicate an action could not be completed.
  - `CFP` (Call for Proposal): Used in Contract Net protocols (e.g., selecting the best supplier).
  - `PROPOSE` / `REJECT_PROPOSAL` / `ACCEPT_PROPOSAL`: Used during negotiations.
- **Ontology**: `PharmaOntology` defines standard vocabulary.
- **Language**: JSON (serialized DTOs).

---

## Message DTO Structure

The platform uses a standardized DTO envelope to bridge the UI/Service layer with the Agent layer.

**`AgentRequestEnvelope<T>`**:
- `transactionId`: UUID for correlation.
- `action`: The requested operation (e.g., `PLAN_PRODUCTION`).
- `requestedByUserId`: Audit trail identity.
- `createdAt`: Timestamp.
- `deadlineMillis`: Timeout for the agent operation.
- `payload`: Generic `T` containing specific request data.

**`AgentResponseEnvelope<T>`**:
- `transactionId`: Matches the request.
- `status`: `SUCCESS`, `FAILED`, `TIMEOUT`.
- `message`: Human-readable outcome.
- `payload`: Generic `T` containing the result data.

---

## Communication Protocols

- **FIPA-Request**: Used for direct, 1-to-1 task delegation (e.g., Coordinator requesting QA check from QAAgent).
- **FIPA-Contract-Net**: Used for 1-to-N negotiation.
  - *Example Usage*: `CoordinatorAgent` sends a `CFP` to `SupplierAgent` and `InventoryAgent` to determine the fastest way to procure a raw material.

---

## Simulation Sandbox

To support "What-If" scenarios without polluting production data, agents operate in an optional **Simulation Sandbox**.
- **Mechanism**: Agents receive a `simulationMode = true` flag in the `AgentRequestEnvelope`.
- **Behavior**: Agents execute their logic, request AI reasoning, and compute outcomes but **do not** commit changes via Services to the Repositories.
- **Result**: Generates a projected impact report (e.g., "If you plan this batch, you will stock out of Material X in 3 days").

---

## Pure Google ADK Migration (Version 2)

Version 1 uses JADE for message transport, lifecycles, and behaviours. Version 2 will replace JADE with Google ADK.

**Migration Strategy**:
1. **Remove JADE Behaviours**: `CyclicBehaviour` and `AchieveREResponder` will be replaced by Google ADK Tool definitions and Agent Nodes.
2. **Replace ACL Messages**: JADE's asynchronous message queue will be replaced by Google ADK's orchestrator routing and direct tool invocations.
3. **Keep Services Intact**: The `ApplicationServices` layer will not change. Google ADK tools will call the exact same `InventoryService.checkAvailability()` methods that the JADE agents called.
4. **Preserve DTOs**: `AgentRequestEnvelope` and `AgentResponseEnvelope` will remain the standard contract for initiating ADK workflows from the Swing UI.
