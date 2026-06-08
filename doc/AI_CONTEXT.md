# AI Context

## Project Overview
**Agentic Pharmaceutical Supply Chain Management System (Agentic Pharma SCM)**
An enterprise desktop application evolving from a traditional monolithic Java Swing + MySQL application into a multi-agent orchestrated system (JADE/Google ADK).

## Business Domain
Upstream Pharmaceutical Manufacturing Operations. Focuses strictly on:
- Supplier Management
- Procurement (Purchase Orders, GRN)
- Raw Material and Inventory Management
- Production Planning & Batch Execution
- In-Process Quality Control (IPQC) & Quality Assurance
- Compliance and Auditability

## Technology Stack
- Java 21
- Java Swing / AWT
- MySQL 8+
- JDBC & HikariCP
- Maven
- SLF4J & Logback
- JADE 4.6.0 (Version 1 Orchestration)
- Google ADK (Version 2 Orchestration)
- Gemini & LangChain4j (AI & RAG)
- dotenv-java

## Architecture Rules
This application adheres to strict **Clean Architecture** layered boundaries:
- **UI Layer**: Java Swing. Handles presentation only.
- **Agent Layer**: JADE/ADK. Orchestrates workflows, delegates tasks, performs reasoning.
- **Service Layer**: Single source of truth for all business rules, validations, and transaction boundaries.
- **Repository Layer**: JDBC implementations. The **ONLY** layer allowed to execute SQL.
- **Database Layer**: MySQL. Enforces referential integrity.

## Strict Coding Constraints (MANDATORY)

1. **NO JDBC IN UI**: Swing ActionListeners must never contain SQL or JDBC calls.
2. **NO JDBC IN AGENTS**: Agents must not interact with the database directly.
3. **NO BUSINESS LOGIC IN AGENTS**: Agents must delegate to `pharma.service` classes for all validation, rule enforcement, and state changes. Agents only orchestrate the calls.
4. **NO DIRECT DB ACCESS FROM AI**: AI models and ADK tools cannot query MySQL directly. They must use predefined Service or Repository Java methods.
5. **AGENTS MUST USE SERVICES**: All agent state mutations must flow through `pharma.config.ApplicationServices`.
6. **SERVICES MUST USE REPOSITORIES**: Services must not contain raw SQL strings. They must call `pharma.repository` interfaces.
7. **ONLY REPOSITORIES EXECUTE SQL**: All `SELECT`, `INSERT`, `UPDATE`, `DELETE` statements belong strictly within `pharma.repository.jdbc` classes.

## Agent Definitions
- **CoordinatorAgent**: Central task router and UI gateway.
- **InventoryAgent**: Tracks stock levels and reserves material.
- **ProductionAgent**: Checks BOM feasibility and plans batches.
- **SupplierAgent**: Manages supplier selection and scoring.
- **QAAgent**: Enforces IPQC rules and batch release gates.
- **ComplianceAgent**: Systemic transaction auditor.
- **RiskAnalysisAgent**: Predictive modeling for delays/stockouts.
- **AIReasoningAgent**: Bridge to Gemini for unstructured data analysis.
- **KnowledgeAgent**: Bridge to LangChain4j for RAG/SOP retrieval.

## Database Rules
- Use `PreparedStatement` exclusively to prevent SQL injection.
- Ensure all business state changes (Approvals, Rejects, Shipments) are logged in `system_audit_trail`.
- Maintain referential integrity (Foreign Keys) at the MySQL level.

## AI & RAG Rules
- **AI is Advisory**: LLM outputs are never allowed to directly mutate the database without passing through a deterministic Service validation rule.
- **Traceability**: All AI reasoning decisions must be logged in the `ai_decisions` table with a prompt summary and confidence score.
- **Grounding**: AI conclusions regarding Quality or Compliance must cite specific documents retrieved via the `KnowledgeAgent` from the RAG vector store.

## Git & Development Rules
- Use meaningful commit messages.
- Always implement interfaces before concrete classes (e.g., `SupplierRepository` before `SupplierJdbcRepository`).
- Write agent logic in a linear, step-by-step manner so it can be easily ported from JADE Behaviours to Google ADK Tools in Phase 13.
