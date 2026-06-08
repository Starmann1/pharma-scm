# Agentic Pharmaceutical Supply Chain Management System Architecture

## Project Vision

The Agentic Pharmaceutical Supply Chain Management System is an enterprise desktop platform for upstream pharmaceutical manufacturing operations. It is intended to evolve from a conventional Java Swing and MySQL inventory management system into an intelligent, agent-assisted pharmaceutical manufacturing platform.

The platform will support supplier qualification, procurement, raw material control, inventory management, production planning, batch manufacturing, in-process quality control, quality assurance, batch release, auditability, compliance monitoring, risk analysis, and knowledge-assisted decision support.

The architecture is designed for two implementation versions:

- **Version 1:** JADE and Google ADK hybrid multi-agent system.
- **Version 2:** Pure Google ADK agent architecture.

Version 1 must preserve the reusable application core so Version 2 can reuse the database layer, repository layer, service layer, DTO layer, UI layer, logging layer, and RAG layer without major rewrites.

## System Objectives

- Provide a controlled pharmaceutical manufacturing operations platform for upstream supply chain workflows.
- Maintain clear separation between UI, business services, persistence, agents, AI reasoning, and knowledge retrieval.
- Enforce auditability, traceability, and controlled decision boundaries for regulated operations.
- Enable agentic orchestration without allowing agents to bypass core business rules.
- Support future AI-assisted workflows through stable service contracts and DTOs.
- Keep the database and repository model stable across JADE and Google ADK implementations.
- Provide a migration path from JADE-based agents to Google ADK-based agents with minimum disruption to business logic.
- Centralize logging and audit trail handling for operational and AI-assisted decisions.

## Functional Requirements

### Supplier Management

- Maintain supplier master data, approval status, license information, GSTIN, payment terms, and contact details.
- Support supplier approval and rejection workflows.
- Track supplier audit history and supplier performance.
- Provide supplier scoring inputs for procurement and production feasibility decisions.

### Procurement

- Create, update, and track purchase orders.
- Link goods receipt to approved suppliers and expected materials.
- Prevent procurement from unapproved or rejected suppliers.
- Support procurement recommendations from supplier and inventory signals.

### Raw Material Management

- Maintain material master data including material type, unit of measure, formulation, strength, storage conditions, reorder levels, and preferred suppliers.
- Support raw materials, packaging materials, intermediates, and finished goods.
- Expose material data through service-layer contracts for UI, agents, and AI workflows.

### Inventory Management

- Track stock by material, batch, location, quantity, reserved quantity, available quantity, unit cost, manufacturing date, expiry date, and QC status.
- Support quarantine, approved, rejected, in-production, and released inventory states.
- Track inventory transactions and material movement.
- Support replenishment analysis and manufacturing feasibility checks.

### Production Planning

- Maintain production orders, production batches, bill of materials, and material consumption.
- Check material availability and capacity before execution.
- Support batch genealogy and traceability from raw material batches to finished batches.

### Quality Assurance and Quality Control

- Manage quality status transitions for materials and batches.
- Support batch release, quarantine, rejection, and recall analysis.
- Provide QA records and decision inputs for agent-assisted workflows.

### Compliance Monitoring

- Maintain audit trails for critical business actions.
- Track compliance records, deviations, CAPA records, and release decisions.
- Support future agent-assisted compliance checks through services.

### Agentic Operations

- Introduce JADE agents for operational coordination, inventory analysis, production feasibility, supplier analysis, QA checks, compliance checks, risk assessment, AI reasoning, and knowledge retrieval.
- Use structured request and response envelopes for agent communication.
- Route agent decisions through application services rather than direct SQL.

### AI and RAG

- Integrate Google ADK and Gemini through a controlled AI layer.
- Integrate LangChain4j for retrieval and future document-grounded reasoning.
- Persist AI decisions, knowledge documents, document chunks, and trace metadata.
- Keep AI output advisory unless explicitly approved by business workflow rules.

## Non Functional Requirements

### Maintainability

- Business rules must live in services, not Swing UI classes, JADE behaviours, or AI prompts.
- SQL must be isolated in repository implementations.
- DTOs must define stable contracts between UI, services, agents, and AI workflows.

### Modularity

- Layers must communicate through explicit interfaces and DTOs.
- Repository interfaces must be independent from JDBC implementation details.
- Agent orchestration must be replaceable without rewriting services.

### Reliability

- Database access must use short-lived pooled connections.
- Long-running agent workflows must use transaction identifiers and traceable responses.
- Failures must be logged and surfaced through structured errors.

### Compliance and Auditability

- Critical state changes must be recorded in audit logs.
- AI-assisted recommendations must be traceable to inputs, agent traces, and source documents.
- Batch genealogy and inventory status changes must be preserved for recall and investigation workflows.

### Security

- User access must be controlled by role and permission rules.
- Agents and AI components must not bypass authorization-sensitive service methods.
- Secrets must be externalized through environment configuration.

### Performance

- JDBC access must use HikariCP connection pooling.
- Read-heavy workflows should use indexed database queries.
- RAG workflows should chunk documents and support future vector indexes.

### Testability

- Services must be testable with mocked repository interfaces.
- Agent workflows must be testable through DTO envelopes and gateway abstractions.
- Repository tests should validate SQL behavior against representative schema fixtures.

### Migration Readiness

- JADE-specific classes must remain in the agent layer.
- Google ADK integration must consume the same service contracts as JADE agents.
- Shared domain services must not depend on JADE, Google ADK, Gemini, or LangChain4j.

## Technology Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Desktop UI | Java Swing / AWT |
| Build | Maven |
| Database | MySQL 8+ |
| Database Access | JDBC |
| Connection Pooling | HikariCP |
| Logging | SLF4J, Logback |
| Agent Framework V1 | JADE 4.6.0 |
| Agent Framework V2 | Google ADK |
| AI Reasoning | Gemini |
| RAG / LLM Integration | LangChain4j |
| Configuration | dotenv-java |
| Testing | JUnit 5, Mockito |
| Serialization | Jackson |

## Package Structure

Current package structure:

```text
pharma
|-- App.java
|-- agent
|   |-- core
|   |-- ontology
|   `-- platform
|-- config
|-- dto
|-- events
|-- gui
|   `-- components
|-- model
|-- repository
|   `-- jdbc
|-- service
`-- resources
    `-- logback.xml
```

Recommended package ownership:

| Package | Responsibility |
| --- | --- |
| `pharma.gui` | Swing panels, dialogs, and user interaction only. |
| `pharma.config` | Application wiring, service construction, environment configuration. |
| `pharma.service` | Business use cases, validation, workflow rules, transaction boundaries. |
| `pharma.repository` | Persistence contracts. |
| `pharma.repository.jdbc` | JDBC SQL implementation of repository contracts. |
| `pharma.model` | Domain entities and persisted business objects. |
| `pharma.dto` | Cross-layer data transfer contracts, especially agent and AI payloads. |
| `pharma.events` | Domain event contracts and in-process event publishing. |
| `pharma.agent.core` | Shared JADE base classes and agent lifecycle support. |
| `pharma.agent.platform` | JADE platform manager, gateway, request correlation, agent bootstrap. |
| `pharma.agent.ontology` | Agent names, actions, statuses, message constants. |
| `pharma.ai` | Future Google ADK, Gemini, and AI orchestration adapters. |
| `pharma.rag` | Future document ingestion, chunking, retrieval, and grounding services. |

## Layered Architecture

The platform uses a layered architecture with strict dependency direction. Each layer has one responsibility and must call only the layer below it or stable DTO/model contracts.

```text
+---------------------------------------------------------------+
|                         Swing UI Layer                        |
|              panels, dialogs, forms, dashboards               |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|                      Controller / App Wiring                  |
|             App.java, ApplicationServices, gateways           |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|                         Service Layer                         |
|      business rules, validation, workflows, authorization      |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|                       Repository Layer                        |
|            persistence interfaces and JDBC adapters            |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|                         Database Layer                        |
|                 MySQL schema, HikariCP, JDBC                  |
+---------------------------------------------------------------+
```

Agent and AI orchestration sit beside the UI layer as alternate clients of the service layer:

```text
+-------------------+        +-------------------+
|     Swing UI      |        |   Agent Layer     |
|   Human Actions   |        |  JADE / ADK V2    |
+---------+---------+        +---------+---------+
          |                            |
          v                            v
+------------------------------------------------+
|                 Service Layer                  |
|      single source of business rules           |
+----------------------+-------------------------+
                       |
                       v
+------------------------------------------------+
|              Repository + Database             |
+------------------------------------------------+
```

## UI Layer

The UI layer is implemented using Java Swing and AWT. It includes panels, dialogs, dashboards, and components under `pharma.gui`.

Responsibilities:

- Render operational screens for inventory, materials, suppliers, purchase orders, GRN, production, QA, reports, RBAC, and dashboards.
- Capture user input and display validation feedback.
- Invoke services through `ApplicationServices` or explicit controller objects.
- Display agent and AI recommendations after service-level validation.

Rules:

- No SQL in Swing classes.
- No JDBC imports in Swing classes.
- No direct construction of JDBC repositories from UI classes.
- No business rules embedded in button listeners beyond basic presentation validation.
- No direct calls from UI to AI providers.

## Controller Layer

The current desktop application uses lightweight application wiring rather than a web-style controller package. `App.java`, `ApplicationServices`, and gateway classes act as the composition and orchestration boundary.

Responsibilities:

- Bootstrap the desktop application.
- Construct repositories and services.
- Provide services to UI and agent platform components.
- Coordinate application lifecycle concerns.
- Bridge asynchronous agent requests to synchronous UI expectations where required.

Recommended evolution:

- Introduce explicit controller or facade classes only when UI workflows become too complex for direct service calls.
- Keep controllers thin. They should orchestrate UI-friendly use cases, not contain SQL or domain rules.

## Service Layer

The service layer contains business use cases and operational decision rules. Existing services include material, inventory, supplier, production, QA, compliance, risk, audit, authorization, and database services.

Responsibilities:

- Enforce business rules and workflow invariants.
- Validate supplier approval, material availability, inventory status, batch status, QA transitions, and compliance constraints.
- Coordinate repositories when a use case spans multiple tables.
- Provide stable APIs to UI, JADE agents, Google ADK agents, and AI adapters.
- Publish domain events where useful.
- Decide whether an AI or agent recommendation can be applied.

Rules:

- Services may depend on repository interfaces.
- Services may use DTOs and domain models.
- Services must not depend on Swing UI classes.
- Services must not depend on JADE-specific classes.
- Services must not depend directly on Gemini, Google ADK, or LangChain4j.
- Transactional operations must be explicitly owned by services or repository methods with clearly documented boundaries.

Important refactoring direction:

- `DatabaseService` currently includes connection pooling and legacy data-access methods. The target architecture is for `DatabaseService` to provide infrastructure concerns such as pooled connections and optional schema checks, while SQL-heavy business persistence continues moving into `pharma.repository.jdbc`.

## Repository Layer

The repository layer abstracts persistence behind interfaces under `pharma.repository` and implementations under `pharma.repository.jdbc`.

Responsibilities:

- Execute SQL queries and updates.
- Map database rows to domain models and DTO projections.
- Isolate JDBC details from services, UI, agents, and AI.
- Provide persistence contracts that can be reused by both JADE and Google ADK versions.

Rules:

- Repository interfaces should describe business persistence operations without exposing SQL.
- JDBC implementations are the only application classes allowed to contain SQL for business tables.
- JDBC implementations may use `DatabaseService` to obtain pooled connections.
- Repositories must not call Swing UI classes.
- Repositories must not call JADE agents or AI providers.
- Repositories should throw meaningful exceptions or return explicit empty results instead of hiding failures.

## Database Layer

The database layer is MySQL 8+ accessed through JDBC and HikariCP.

Responsibilities:

- Persist master data, transactional data, audit logs, compliance records, production records, inventory records, agent decisions, AI decisions, and knowledge artifacts.
- Enforce referential integrity through primary keys, foreign keys, constraints, and indexes.
- Support traceability from supplier and raw material receipt through production batches, QA decisions, and release.
- Support future vector search strategy for RAG document chunks.

Database ownership:

- Schema is defined by SQL migration/setup scripts such as `database.sql` and supplemental scripts.
- Application code accesses the database only through repository implementations.
- Connection pooling is provided by HikariCP through `DatabaseService`.

## Agent Layer

The agent layer provides autonomous orchestration for operational workflows. Version 1 uses JADE and Version 2 will migrate to Google ADK.

Current foundations:

- `BasePharmaAgent` provides shared JADE lifecycle support and access to `ApplicationServices`.
- `AgentGateway` correlates request and response envelopes through transaction IDs.
- `AgentRequestEnvelope` and `AgentResponseEnvelope` define structured request-response contracts.
- `AgentNames`, `AgentActions`, and `AgentStatuses` provide ontology constants.

Target Version 1 agent set:

- `CoordinatorAgent`
- `InventoryAgent`
- `ProductionAgent`
- `SupplierAgent`
- `QAAgent`
- `ComplianceAgent`
- `RiskAnalysisAgent`
- `AIReasoningAgent`
- `KnowledgeAgent`

Responsibilities:

- Coordinate operational workflows.
- Request data from services.
- Compose recommendations using service results, AI reasoning, and knowledge retrieval.
- Return structured responses to the UI or workflow initiator.
- Record trace information for auditability.

Rules:

- Agents must use services.
- Agents must not execute SQL.
- Agents must not open JDBC connections.
- Agents must not own business rules that belong in services.
- Agents may orchestrate decisions, but services must validate and apply state changes.

Version 2 migration principle:

- Replace JADE message transport and behaviours with Google ADK tools, agents, and orchestration flows.
- Keep service calls, DTOs, repository contracts, database schema, logging, and RAG contracts stable.

## AI Layer

The AI layer is the controlled integration boundary for Google ADK, Gemini, and AI-assisted decision making.

Responsibilities:

- Convert service and agent context into AI prompts or ADK tool calls.
- Call approved AI models and tools.
- Validate and structure AI responses into DTOs.
- Record AI decision metadata, model name, prompt version, input summary, output summary, confidence, and trace.
- Keep AI output advisory unless approved by services.

Rules:

- AI adapters must not access the database directly.
- AI adapters must not execute SQL.
- AI adapters must call services or RAG services.
- AI prompts must not become the source of business rules.
- AI responses must be validated before display or workflow application.

## RAG Layer

The RAG layer provides knowledge ingestion and retrieval for SOPs, specifications, batch documents, deviation histories, regulatory references, and internal policies.

Responsibilities:

- Store knowledge document metadata.
- Chunk documents into retrievable sections.
- Generate and persist embeddings through an approved embedding strategy.
- Retrieve relevant chunks for AI and agent workflows.
- Provide citations and source traceability for AI reasoning.

Target components:

- `KnowledgeDocumentRepository`
- `DocumentChunkRepository`
- `KnowledgeIngestionService`
- `RetrievalService`
- `RagContextAssembler`
- `KnowledgeAgent`

Rules:

- RAG retrieval may support AI reasoning, but it must not directly mutate operational records.
- Source document references must be retained for every knowledge-grounded recommendation.
- Future vector search can be implemented in MySQL-compatible vector storage or an external vector database behind repository interfaces.

## Logging Layer

The logging layer uses SLF4J and Logback. The current configuration writes to console and `logs/app.log`.

Responsibilities:

- Record application startup, shutdown, and operational errors.
- Record repository failures with enough context for diagnosis.
- Record agent lifecycle events and agent workflow traces.
- Record AI decision metadata without exposing secrets or sensitive raw prompts unnecessarily.
- Support regulated auditability in combination with database audit trails.

Logging categories:

- `pharma.gui`: UI actions and presentation errors.
- `pharma.service`: business workflow execution and validation failures.
- `pharma.repository`: persistence failures and query-level diagnostics without sensitive values.
- `pharma.agent`: agent lifecycle, message routing, behaviours, and escalation.
- `pharma.ai`: model calls, prompt versions, response validation, and failures.
- `pharma.rag`: ingestion, chunking, retrieval, and citation matching.

Logging rules:

- Use SLF4J loggers, not `System.out` or `printStackTrace`, in new code.
- Do not log database passwords, API keys, full secrets, or sensitive regulated content unless explicitly approved.
- Use transaction IDs and agent trace IDs for cross-layer correlation.
- Critical business state changes must be stored in audit tables, not only in log files.

## Dependency Direction Rules

### Allowed Dependencies

```text
UI -> Services -> Repositories -> Database

Agents -> Services -> Repositories -> Database

AI -> Services
AI -> RAG Services
RAG Services -> RAG Repositories -> Database

Services -> DTOs / Models / Events
Repositories -> Models / DTO projections
```

### Forbidden Dependencies

```text
UI -X-> JDBC
UI -X-> SQL
UI -X-> Repositories directly, except during transitional refactoring

Agents -X-> JDBC
Agents -X-> SQL
Agents -X-> Repositories directly

AI -X-> JDBC
AI -X-> SQL
AI -X-> Repositories directly

Repositories -X-> UI
Repositories -X-> Agents
Repositories -X-> AI providers

Services -X-> Swing classes
Services -X-> JADE framework classes
Services -X-> Google ADK framework classes
```

## Why UI -> Services -> Repositories -> Database Must Be Enforced

The UI is a presentation layer. It should display data, collect user input, and trigger use cases. It must not decide how pharmaceutical operations are persisted or whether a regulated state transition is valid.

Enforcing `UI -> Services -> Repositories -> Database` provides:

- **Business rule consistency:** The same validation applies whether an operation is initiated from a Swing panel, an agent, or future ADK workflow.
- **Regulatory control:** Critical transitions such as supplier approval, QA release, batch rejection, and inventory movement remain centralized and auditable.
- **Maintainability:** SQL changes remain isolated in repositories instead of spread across UI event handlers.
- **Testability:** Services can be tested without rendering Swing screens, and repositories can be tested independently.
- **Migration readiness:** Future UI improvements or ADK workflows can reuse the same services without duplicating persistence logic.
- **Security:** UI code cannot bypass service-level authorization and validation.

Correct flow:

```text
User Action
    |
    v
Swing Panel / Dialog
    |
    v
Service Method
    |
    v
Repository Interface
    |
    v
JDBC Repository Implementation
    |
    v
MySQL
```

## Why Agents -> Services -> Repositories Must Be Enforced

Agents are orchestration components. They can coordinate, negotiate, request analysis, and recommend actions, but they must not become hidden persistence or business-rule engines.

Enforcing `Agents -> Services -> Repositories` provides:

- **Single source of truth:** Agents use the same business services as the UI.
- **Safe autonomy:** Agents can recommend or request actions, but service methods validate whether actions are allowed.
- **Reduced migration cost:** JADE agents can be replaced by Google ADK agents while services and repositories remain stable.
- **Auditability:** Agent decisions can be correlated with service calls and database audit records.
- **Operational safety:** Agents cannot directly change batch, QA, supplier, or inventory records through uncontrolled SQL.
- **Clear responsibilities:** Agents orchestrate workflows; services enforce rules; repositories persist data.

Correct flow:

```text
Agent Message / Behaviour
    |
    v
Agent Handler
    |
    v
Service Method
    |
    v
Repository Interface
    |
    v
JDBC Repository Implementation
    |
    v
MySQL
```

## End-to-End Hybrid Architecture

```text
                           +----------------------+
                           |      Swing UI        |
                           | panels, dialogs      |
                           +----------+-----------+
                                      |
                                      v
+----------------------+   +----------+-----------+   +----------------------+
| External Documents   |   | Application Wiring   |   | JADE Agent Platform  |
| SOPs, specs, QA docs |   | App, services,       |   | V1 behaviours, ACL   |
+----------+-----------+   | gateways             |   +----------+-----------+
           |               +----------+-----------+              |
           v                          |                          v
+----------+-----------+              |               +----------+-----------+
|      RAG Layer       |              |               |     Agent Layer      |
| ingest, chunk,       |              |               | coordinator, domain  |
| retrieve, cite       |              |               | agents, AI agent     |
+----------+-----------+              |               +----------+-----------+
           |                          |                          |
           v                          v                          v
+----------+-----------------------------------------------------+-----------+
|                              Service Layer                                 |
| supplier, material, inventory, production, QA, compliance, risk, audit     |
+----------------------------------+-----------------------------------------+
                                   |
                                   v
+----------------------------------+-----------------------------------------+
|                            Repository Layer                                |
| interfaces plus JDBC implementations                                       |
+----------------------------------+-----------------------------------------+
                                   |
                                   v
+----------------------------------+-----------------------------------------+
|                              Database Layer                                |
| MySQL, HikariCP, audit tables, operational schema, future vector strategy  |
+----------------------------------------------------------------------------+
                                   ^
                                   |
                      +------------+-------------+
                      |        Logging Layer      |
                      | SLF4J, Logback, audit DB |
                      +--------------------------+
```

## Version 1 to Version 2 Migration View

Version 1 introduces JADE as the multi-agent runtime while protecting the application core from JADE-specific dependencies.

```text
Version 1

JADE Agents
    |
    v
Agent DTOs / Gateway
    |
    v
Services
    |
    v
Repositories
    |
    v
MySQL
```

Version 2 replaces JADE with Google ADK orchestration while preserving the core contracts.

```text
Version 2

Google ADK Agents / Tools
    |
    v
Same DTOs / Service Adapters
    |
    v
Same Services
    |
    v
Same Repositories
    |
    v
Same MySQL Schema
```

Stable assets across both versions:

- Database schema.
- Repository interfaces.
- JDBC repository implementations.
- Service APIs.
- DTO contracts.
- Swing UI workflows.
- Logging conventions.
- RAG document and chunk model.
- Audit and AI decision records.

Replaceable assets:

- JADE agent classes.
- JADE behaviours.
- JADE ACL message routing.
- JADE platform bootstrap.

## Architecture Principles

- Keep business logic in services.
- Keep SQL in repositories.
- Keep UI presentation-focused.
- Keep agents orchestration-focused.
- Keep AI advisory and service-validated.
- Keep RAG source-grounded and traceable.
- Keep audit records durable and queryable.
- Keep framework-specific code at the edges.
- Keep DTOs stable, explicit, and serializable.
- Keep migration to Google ADK as a first-class architectural constraint.

