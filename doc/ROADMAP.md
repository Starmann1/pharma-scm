# Implementation Roadmap

## Overview

The transformation of the Agentic Pharmaceutical Supply Chain Management System will be executed in a phased approach. The roadmap begins with refactoring the foundational layers to enforce Clean Architecture, proceeds to deploying deterministic JADE agents (Version 1), introduces AI and RAG capabilities, and culminates in a migration to a Pure Google ADK architecture (Version 2).

---

## Phase 0: Foundation Refactoring

*   **Goals**: Ensure the existing Java Swing application strictly adheres to Clean Architecture before introducing agents.
*   **Deliverables**:
    *   Removal of all SQL statements from Swing UI classes.
    *   Decoupling of business logic from UI event listeners.
*   **Dependencies**: Existing codebase assessment.
*   **Acceptance Criteria**: Application compiles, runs, and passes all basic workflow tests without any direct UI-to-Database calls.
*   **Estimated Complexity**: Medium
*   **Risks**: Uncovering hidden business rules hardcoded in legacy UI components.

## Phase 1: Repository Layer

*   **Goals**: Centralize all data access into formal Repository interfaces and JDBC implementations.
*   **Deliverables**:
    *   `MaterialRepository`, `SupplierRepository`, `InventoryRepository`, `ProductionRepository`, `QARepository`, etc.
    *   Refactored `DatabaseService` (focusing only on HikariCP connection pooling).
*   **Dependencies**: Phase 0.
*   **Acceptance Criteria**: All database interactions flow through repository interfaces. Unit tests pass with mocked repositories.
*   **Estimated Complexity**: Medium
*   **Risks**: Performance regressions if SQL joins are replaced by inefficient iterative repository calls (N+1 queries).

## Phase 2: Service Layer

*   **Goals**: Consolidate all pharmaceutical business logic and transaction boundaries into Services.
*   **Deliverables**:
    *   `SupplierService`, `InventoryService`, `ProductionService`, `QAService`, `ComplianceService`.
    *   Implementation of rollback/commit transaction management.
*   **Dependencies**: Phase 1.
*   **Acceptance Criteria**: Services correctly validate business constraints (e.g., preventing release of Quarantined stock) and throw domain exceptions on failure.
*   **Estimated Complexity**: High
*   **Risks**: Transaction boundary leaks causing partial data commits on failure.

## Phase 3: DTO Layer

*   **Goals**: Establish standard data contracts between UI, Services, and Agents.
*   **Deliverables**:
    *   `AgentRequestEnvelope`, `AgentResponseEnvelope`.
    *   Domain DTOs (e.g., `BatchDTO`, `SupplierScoreDTO`).
*   **Dependencies**: Phase 2.
*   **Acceptance Criteria**: UI and Services communicate exclusively via DTOs, not raw JDBC ResultSets or internal Entities.
*   **Estimated Complexity**: Low
*   **Risks**: DTO bloat (creating too many slightly different DTOs for similar use cases).

## Phase 4: Logging Layer

*   **Goals**: Implement comprehensive, auditable logging.
*   **Deliverables**:
    *   SLF4J + Logback configuration.
    *   Database-backed `System_Audit_Trail` integration within Service methods.
*   **Dependencies**: Phase 2.
*   **Acceptance Criteria**: All critical state changes (e.g., Batch Release, PO Approval) generate an immutable audit log entry.
*   **Estimated Complexity**: Low
*   **Risks**: Excessive logging causing disk space exhaustion or performance drag.

## Phase 5: JADE Infrastructure

*   **Goals**: Scaffold the JADE multi-agent platform alongside the Java application.
*   **Deliverables**:
    *   JADE Bootstrapper.
    *   `BasePharmaAgent`.
    *   `AgentGateway` for bridging Swing UI and JADE asynchronous messages.
*   **Dependencies**: Phase 3.
*   **Acceptance Criteria**: JADE Main Container starts successfully alongside the Swing UI, and the Gateway can route a ping message to an agent.
*   **Estimated Complexity**: Medium
*   **Risks**: Threading deadlocks between Swing's EDT and JADE's internal threading model.

## Phase 6: Operational Agents

*   **Goals**: Deploy the primary deterministic agents.
*   **Deliverables**:
    *   `CoordinatorAgent`, `InventoryAgent`, `SupplierAgent`, `ProductionAgent`, `QAAgent`.
*   **Dependencies**: Phase 5.
*   **Acceptance Criteria**: Agents can receive tasks from the UI, successfully call appropriate Services, and return results.
*   **Estimated Complexity**: High
*   **Risks**: Agents duplicating business logic instead of delegating to Services.

## Phase 7: Multi-Agent Workflows

*   **Goals**: Enable agent-to-agent negotiation and orchestration.
*   **Deliverables**:
    *   Implementation of FIPA-Contract-Net for dynamic supplier selection.
    *   End-to-end automated Material Procurement workflow.
*   **Dependencies**: Phase 6.
*   **Acceptance Criteria**: `CoordinatorAgent` successfully negotiates with `SupplierAgent` and `InventoryAgent` to automatically draft a PO when stock is low.
*   **Estimated Complexity**: High
*   **Risks**: Workflow deadlocks or infinite messaging loops between agents.

## Phase 8: Risk Analysis

*   **Goals**: Introduce predictive risk scoring.
*   **Deliverables**:
    *   `RiskAnalysisAgent`.
    *   `RiskService` (historical data evaluation).
*   **Dependencies**: Phase 7.
*   **Acceptance Criteria**: System flags high-risk suppliers or potential stockouts on the UI dashboard based on historical trends.
*   **Estimated Complexity**: Medium
*   **Risks**: Inaccurate risk models causing false positives and alert fatigue.

## Phase 9: Google ADK Integration

*   **Goals**: Begin introducing Google ADK alongside JADE.
*   **Deliverables**:
    *   Google ADK library integration.
    *   Mapping of existing Service methods to ADK Tools.
*   **Dependencies**: Phase 2.
*   **Acceptance Criteria**: ADK tools can successfully execute deterministic Java Service methods.
*   **Estimated Complexity**: Medium
*   **Risks**: Dependency conflicts between JADE and ADK/LangChain4j libraries.

## Phase 10: AIReasoningAgent

*   **Goals**: Enable LLM-based reasoning for complex, unstructured tasks.
*   **Deliverables**:
    *   `AIReasoningAgent`.
    *   Gemini API integration.
*   **Dependencies**: Phase 9.
*   **Acceptance Criteria**: Agent can parse an unstructured supplier audit report and extract a structured performance score DTO.
*   **Estimated Complexity**: High
*   **Risks**: Hallucinations; AI attempting to bypass Service validation boundaries.

## Phase 11: KnowledgeAgent and RAG

*   **Goals**: Ground AI reasoning in pharmaceutical SOPs and regulations.
*   **Deliverables**:
    *   `KnowledgeAgent`.
    *   LangChain4j ingestion pipeline (chunking and embedding).
    *   Vector search implementation.
*   **Dependencies**: Phase 10.
*   **Acceptance Criteria**: `AIReasoningAgent` correctly cites specific SOP document chunks when suggesting a root cause for a deviation.
*   **Estimated Complexity**: High
*   **Risks**: Poor embedding strategy leading to irrelevant document retrieval.

## Phase 12: AI Dashboard

*   **Goals**: Provide human oversight over AI decisions.
*   **Deliverables**:
    *   Swing UI Dashboard for viewing Agent traces, AI prompts, confidences, and RAG citations.
*   **Dependencies**: Phase 11.
*   **Acceptance Criteria**: QA Manager can view the exact SOPs the AI used to recommend a batch release before manually approving it.
*   **Estimated Complexity**: Medium
*   **Risks**: UI clutter making trace analysis difficult for operators.

## Phase 13: Pure Google ADK Migration

*   **Goals**: Decommission JADE (Transition to Version 2).
*   **Deliverables**:
    *   Replacement of JADE Behaviours with ADK orchestration.
    *   Removal of JADE libraries from `pom.xml`.
*   **Dependencies**: Phase 12.
*   **Acceptance Criteria**: All automated workflows function identically using Google ADK without JADE, utilizing the exact same underlying Services and Database.
*   **Estimated Complexity**: High
*   **Risks**: Loss of specific asynchronous message queue benefits previously provided natively by JADE.
